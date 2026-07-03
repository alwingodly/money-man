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
