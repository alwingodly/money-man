package com.alwin.moneymanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
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
    data object ExpenseSearch : Destination("expense_search")
    data object ExpenseSummary : Destination("expense_summary")
    data object ExpenseDayDetail : Destination("expense_day/{dateMillis}") {
        const val ARG_DATE_MILLIS = "dateMillis"
        fun createRoute(dateMillis: Long) = "expense_day/$dateMillis"
    }
    data object Debts : Destination("debts")
    data object DebtHistory : Destination("debt_history")
    data object DebtDetail : Destination("debt_detail/{debtId}") {
        const val ARG_DEBT_ID = "debtId"
        fun createRoute(debtId: Long) = "debt_detail/$debtId"
    }
    data object Savings : Destination("savings")
    data object SavingDetail : Destination("saving_detail/{savingId}") {
        const val ARG_SAVING_ID = "savingId"
        fun createRoute(savingId: Long) = "saving_detail/$savingId"
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
    TopLevelDestination(Destination.Debts, "Debts", Icons.Filled.SwapHoriz),
    TopLevelDestination(Destination.Savings, "Savings", Icons.Filled.Savings),
)
