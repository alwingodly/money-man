package com.alwin.moneymanager.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val MILLIS_PER_MONTH = 30L * 24 * 60 * 60 * 1000

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Emi: add an explicit end date, backfilled from the existing start date + total months.
        db.execSQL("ALTER TABLE emi ADD COLUMN endDateMillis INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE emi SET endDateMillis = startDateMillis + (totalMonths * $MILLIS_PER_MONTH)"
        )

        // Expense categories become user-managed rows instead of a fixed enum.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expense_category` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_category_name` ON `expense_category` (`name`)"
        )
        db.execSQL("INSERT INTO expense_category (id, name) VALUES (1, 'Food'), (2, 'Travel')")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expense_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `amount` REAL NOT NULL, `note` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `expense_category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expense_categoryId` ON `expense_new` (`categoryId`)"
        )
        db.execSQL(
            """
            INSERT INTO expense_new (id, categoryId, amount, note, dateMillis)
            SELECT id, CASE category WHEN 'FOOD' THEN 1 WHEN 'TRAVEL' THEN 2 ELSE 1 END, amount, note, dateMillis
            FROM expense
            """.trimIndent()
        )
        db.execSQL("DROP TABLE expense")
        db.execSQL("ALTER TABLE expense_new RENAME TO expense")

        // Bank Balance was dropped as a feature.
        db.execSQL("DROP TABLE IF EXISTS bank_balance")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // EMI due reminders: opt-in per EMI, off by default for existing rows.
        db.execSQL("ALTER TABLE emi ADD COLUMN notificationEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE emi ADD COLUMN reminderDaysBefore INTEGER NOT NULL DEFAULT 3")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Payment method per expense: credit card vs everything else (savings/cash). Existing
        // rows default to "not credit card" since we have no way to know retroactively.
        db.execSQL("ALTER TABLE expense ADD COLUMN isCreditCard INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Original principal borrowed, used to derive total interest paid (monthlyAmount *
        // totalMonths - loanAmount). Existing EMIs default to 0 ("unknown") since we have no way
        // to know the original principal retroactively; the interest stat stays hidden for those
        // until the user fills it in via Edit EMI.
        db.execSQL("ALTER TABLE emi ADD COLUMN loanAmount REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Every expense query filters and/or sorts by dateMillis (period totals, day-detail
        // lookup, Home's recent-activity preview) — this was previously an unindexed full-table
        // scan + sort. Pure index creation, no data change.
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_dateMillis` ON `expense` (`dateMillis`)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Collapse the per-direction debt accounts into ONE account per person (Khatabook-style).
        // `debt` loses isOwedToMe; `debt_entry.isCharge` becomes `isGiven` (you gave = +, you got =
        // −). Old accounts for the same name are merged, and entries repointed to the canonical
        // (lowest-id) account. Full table rebuilds so the result exactly matches the new entities.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`personName` TEXT NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "`createdDateMillis` INTEGER NOT NULL, " +
                "`dueDateMillis` INTEGER, " +
                "`notificationEnabled` INTEGER NOT NULL, " +
                "`isSettled` INTEGER NOT NULL)"
        )
        // One row per person: keep the lowest id as canonical, earliest created date, and any due
        // date / reminder that was set. isSettled left 0 — it self-heals on the next entry change.
        db.execSQL(
            "INSERT INTO debt_new (id, personName, note, createdDateMillis, dueDateMillis, notificationEnabled, isSettled) " +
                "SELECT MIN(id), personName, '', MIN(createdDateMillis), MAX(dueDateMillis), MAX(notificationEnabled), 0 " +
                "FROM debt GROUP BY personName COLLATE NOCASE"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt_entry_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`debtId` INTEGER NOT NULL, " +
                "`isGiven` INTEGER NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`dateMillis` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "FOREIGN KEY(`debtId`) REFERENCES `debt`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        // isGiven = (old account direction == old isCharge): for a "they owe me" account a charge was
        // you giving (+); for a "you owe them" account a charge was you receiving (−). Repoint each
        // entry to the canonical account for its person.
        db.execSQL(
            "INSERT INTO debt_entry_new (id, debtId, isGiven, amount, dateMillis, note) " +
                "SELECT e.id, " +
                "(SELECT MIN(d2.id) FROM debt d2 WHERE d2.personName = d.personName COLLATE NOCASE), " +
                "(d.isOwedToMe = e.isCharge), e.amount, e.dateMillis, e.note " +
                "FROM debt_entry e JOIN debt d ON d.id = e.debtId"
        )

        db.execSQL("DROP TABLE debt_entry")
        db.execSQL("DROP TABLE debt")
        db.execSQL("ALTER TABLE debt_new RENAME TO debt")
        db.execSQL("ALTER TABLE debt_entry_new RENAME TO debt_entry")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_entry_debtId` ON `debt_entry` (`debtId`)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Weekly/daily EMIs: `frequency` (MONTHLY default keeps every existing loan behaving as
        // before) and `offDaysMask` (weekdays a daily EMI skips; 0 = none). New columns only, with
        // defaults matching what Room derives from the entity's @ColumnInfo(defaultValue = ...).
        db.execSQL("ALTER TABLE emi ADD COLUMN frequency TEXT NOT NULL DEFAULT 'MONTHLY'")
        db.execSQL("ALTER TABLE emi ADD COLUMN offDaysMask INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Custom-interval EMIs ("every N days"): `intervalDays`, only used when frequency = CUSTOM.
        // Default 0 matches the entity's @ColumnInfo(defaultValue = "0"); existing loans are
        // unaffected (they're not CUSTOM).
        db.execSQL("ALTER TABLE emi ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Late-payment fine entered when an installment is marked paid after its due date.
        // Default 0 (paid on time / no penalty) matches the entity's @ColumnInfo(defaultValue = "0").
        db.execSQL("ALTER TABLE emi_payment ADD COLUMN penaltyAmount REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Savings pots: a Saving with an optional targetAmount goal, paid into by ad-hoc
        // SavingContributions. New tables only, no changes to existing data. Column defs match what
        // Room derives from the entities (no SQL DEFAULT clauses — plain Kotlin defaults only).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `saving` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`targetAmount` REAL, " +
                "`note` TEXT NOT NULL, " +
                "`createdDateMillis` INTEGER NOT NULL, " +
                "`isAchieved` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `saving_contribution` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`savingId` INTEGER NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`dateMillis` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "FOREIGN KEY(`savingId`) REFERENCES `saving`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saving_contribution_savingId` ON `saving_contribution` (`savingId`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Debt reshaped from "each debt is a lump sum + repayments" into "each person is one account
        // with a combined ledger". New `debt_entry` holds every line (isCharge = gave / else repaid).
        // Existing data is preserved: each old debt's originalAmount becomes an opening charge entry,
        // and each old debt_payment becomes a repayment entry. Then `debt` loses originalAmount and
        // `debt_payment` is dropped.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt_entry` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`debtId` INTEGER NOT NULL, " +
                "`isCharge` INTEGER NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`dateMillis` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "FOREIGN KEY(`debtId`) REFERENCES `debt`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_entry_debtId` ON `debt_entry` (`debtId`)")

        // Opening charge from each existing debt's originalAmount.
        db.execSQL(
            "INSERT INTO debt_entry (debtId, isCharge, amount, dateMillis, note) " +
                "SELECT id, 1, originalAmount, createdDateMillis, '' FROM debt"
        )
        // Each existing payment becomes a repayment entry.
        db.execSQL(
            "INSERT INTO debt_entry (debtId, isCharge, amount, dateMillis, note) " +
                "SELECT debtId, 0, amount, paidDateMillis, '' FROM debt_payment"
        )

        // Rebuild `debt` without originalAmount.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`personName` TEXT NOT NULL, " +
                "`isOwedToMe` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "`createdDateMillis` INTEGER NOT NULL, " +
                "`dueDateMillis` INTEGER, " +
                "`notificationEnabled` INTEGER NOT NULL, " +
                "`isSettled` INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO debt_new (id, personName, isOwedToMe, note, createdDateMillis, dueDateMillis, notificationEnabled, isSettled) " +
                "SELECT id, personName, isOwedToMe, note, createdDateMillis, dueDateMillis, notificationEnabled, isSettled FROM debt"
        )
        db.execSQL("DROP TABLE debt")
        db.execSQL("ALTER TABLE debt_new RENAME TO debt")
        db.execSQL("DROP TABLE debt_payment")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Personal debt ledger ("who owes whom"): a lump-sum Debt paid down by ad-hoc DebtPayments.
        // New tables only — no changes to existing data. Column definitions must match what Room
        // derives from the entities (no SQL DEFAULT clauses, since the entities use plain Kotlin
        // defaults without @ColumnInfo(defaultValue)).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`personName` TEXT NOT NULL, " +
                "`isOwedToMe` INTEGER NOT NULL, " +
                "`originalAmount` REAL NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "`createdDateMillis` INTEGER NOT NULL, " +
                "`dueDateMillis` INTEGER, " +
                "`notificationEnabled` INTEGER NOT NULL, " +
                "`isSettled` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt_payment` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`debtId` INTEGER NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`paidDateMillis` INTEGER NOT NULL, " +
                "FOREIGN KEY(`debtId`) REFERENCES `debt`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payment_debtId` ON `debt_payment` (`debtId`)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Enforce one payment per (loan, installment). A rapid double-tap on "mark paid" could
        // previously insert the same monthNumber twice and double-count. First drop any existing
        // duplicates (keep the earliest row per group), since a UNIQUE index can't be created
        // over a table that already violates it — then add the index Room now expects.
        db.execSQL(
            """
            DELETE FROM emi_payment
            WHERE id NOT IN (
                SELECT MIN(id) FROM emi_payment GROUP BY emiId, monthNumber
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_emi_payment_emiId_monthNumber` ON `emi_payment` (`emiId`, `monthNumber`)"
        )
    }
}
