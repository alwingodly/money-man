package com.alwin.moneymanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object EmiList : Destination("emi_list")
    data object EmiDetail : Destination("emi_detail/{emiId}") {
        const val ARG_EMI_ID = "emiId"
        fun createRoute(emiId: Long) = "emi_detail/$emiId"
    }
    data object EmiClosedList : Destination("emi_closed_list")
    data object Expenses : Destination("expenses")
    data object ExpenseDayDetail : Destination("expense_day/{dateMillis}") {
        const val ARG_DATE_MILLIS = "dateMillis"
        fun createRoute(dateMillis: Long) = "expense_day/$dateMillis"
    }
    data object Settings : Destination("settings")
    data object Profile : Destination("profile")
}

data class TopLevelDestination(
    val destination: Destination,
    val label: String,
    val icon: ImageVector,
)

val topLevelDestinations = listOf(
    TopLevelDestination(Destination.Home, "Home", Icons.Filled.Home),
    TopLevelDestination(Destination.EmiList, "EMIs", Icons.Filled.CreditCard),
    TopLevelDestination(Destination.Expenses, "Expenses", Icons.Filled.Receipt),
)
