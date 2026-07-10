package com.alwin.moneymanager.data.local.entity

/**
 * How often an EMI installment falls due. The amount/count columns on [Emi] (historically named
 * `monthlyAmount`/`totalMonths`) are reinterpreted per-installment: for [WEEKLY] they mean the
 * weekly amount and total number of weeks, for [DAILY] the daily amount and total number of days.
 */
enum class EmiFrequency {
    MONTHLY,
    WEEKLY,
    DAILY,

    /**
     * A fixed interval of [Emi.intervalDays] days between installments (e.g. every 28 days). Unlike
     * [MONTHLY] it doesn't track the calendar date, and unlike [WEEKLY] the interval isn't tied to
     * 7 — it just rolls forward N days each time, so "every 28 days" starting on a Tuesday always
     * lands on a Tuesday.
     */
    CUSTOM,
}
