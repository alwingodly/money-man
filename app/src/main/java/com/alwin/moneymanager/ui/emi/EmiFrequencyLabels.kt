package com.alwin.moneymanager.ui.emi

import com.alwin.moneymanager.data.local.entity.EmiFrequency

/** Singular unit word for an installment period: "month" / "week" / "day" / "payment". */
val EmiFrequency.unitSingular: String
    get() = when (this) {
        EmiFrequency.MONTHLY -> "month"
        EmiFrequency.WEEKLY -> "week"
        EmiFrequency.DAILY -> "day"
        EmiFrequency.CUSTOM -> "payment"
    }

/** Label for the per-installment amount field/stat: "Monthly amount" / "Weekly amount" / etc. */
val EmiFrequency.amountLabel: String
    get() = when (this) {
        EmiFrequency.MONTHLY -> "Monthly amount"
        EmiFrequency.WEEKLY -> "Weekly amount"
        EmiFrequency.DAILY -> "Daily amount"
        EmiFrequency.CUSTOM -> "Amount per payment"
    }
