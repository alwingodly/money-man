package com.alwin.moneymanager.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.DecimalFormat

enum class CurrencyType(val symbol: String, val label: String) {
    RUPEE("₹", "Rupee"),
    DOLLAR("$", "Dollar"),
    DINAR("IQD", "Dinar"),
}

private val amountFormat = DecimalFormat("#,##0.00")

/**
 * Backing store for the app-wide currency symbol, synced once at the root composable (see
 * MainActivity) from [com.alwin.moneymanager.data.repository.CurrencyRepository]'s DataStore
 * value. Reading a `mutableStateOf` like this from a plain (non-@Composable) function still
 * participates in Compose's recomposition tracking as long as the read happens during
 * composition, so every existing `formatCurrency()` call site updates without being threaded
 * through as a parameter. This only swaps the printed symbol — no conversion between currencies
 * is performed, matching the app's single fixed-currency data model.
 */
var currentCurrency: CurrencyType by mutableStateOf(CurrencyType.RUPEE)

fun formatCurrency(amount: Double): String = "${currentCurrency.symbol}${amountFormat.format(amount)}"

fun currencyTypeFromName(name: String?): CurrencyType =
    CurrencyType.entries.firstOrNull { it.name == name } ?: CurrencyType.RUPEE
