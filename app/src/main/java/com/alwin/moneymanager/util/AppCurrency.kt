package com.alwin.moneymanager.util

import java.text.DecimalFormat

private val amountFormat = DecimalFormat("#,##0.00")

/** App-wide currency is fixed to Rupee — no user-facing switcher. */
fun formatCurrency(amount: Double): String = "₹${amountFormat.format(amount)}"
