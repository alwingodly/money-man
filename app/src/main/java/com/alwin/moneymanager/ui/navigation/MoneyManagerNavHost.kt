package com.alwin.moneymanager.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alwin.moneymanager.ui.emi.EmiClosedListScreen
import com.alwin.moneymanager.ui.emi.EmiDetailScreen
import com.alwin.moneymanager.ui.emi.EmiListScreen
import com.alwin.moneymanager.ui.expense.DayDetailScreen
import com.alwin.moneymanager.ui.expense.ExpenseScreen
import com.alwin.moneymanager.ui.expense.ExpenseSearchScreen
import com.alwin.moneymanager.ui.home.HomeScreen
import com.alwin.moneymanager.ui.profile.ProfileScreen
import com.alwin.moneymanager.ui.settings.SettingsScreen

@Composable
fun MoneyManagerNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        // No topBar here — each screen has its own TopAppBar which already pads for the
        // status bar. Without this, the default contentWindowInsets (safeDrawing) leaks the
        // status bar height into innerPadding and every screen ends up padded for it twice.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { topLevel ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == topLevel.destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(topLevel.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(topLevel.icon, contentDescription = topLevel.label) },
                        label = { Text(topLevel.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onSettingsClick = { navController.navigate(Destination.Settings.route) },
                    onProfileClick = { navController.navigate(Destination.Profile.route) },
                    onViewAllEmisClick = { navController.navigate(Destination.EmiList.route) },
                    onViewAllExpensesClick = { navController.navigate(Destination.Expenses.route) },
                    onEmiClick = { emiId ->
                        navController.navigate(Destination.EmiDetail.createRoute(emiId))
                    },
                    onExpenseDateClick = { dateMillis ->
                        navController.navigate(Destination.ExpenseDayDetail.createRoute(dateMillis))
                    },
                )
            }
            composable(Destination.EmiList.route) {
                EmiListScreen(
                    onEmiClick = { emiId ->
                        navController.navigate(Destination.EmiDetail.createRoute(emiId))
                    },
                    onClosedLoansClick = { navController.navigate(Destination.EmiClosedList.route) },
                )
            }
            composable(Destination.EmiClosedList.route) {
                EmiClosedListScreen(
                    onBack = { navController.popBackStack() },
                    onEmiClick = { emiId ->
                        navController.navigate(Destination.EmiDetail.createRoute(emiId))
                    },
                )
            }
            composable(
                route = Destination.EmiDetail.route,
                arguments = listOf(
                    navArgument(Destination.EmiDetail.ARG_EMI_ID) { type = NavType.LongType }
                ),
            ) { backStack ->
                val emiId = backStack.arguments?.getLong(Destination.EmiDetail.ARG_EMI_ID)
                    ?: return@composable
                EmiDetailScreen(
                    emiId = emiId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Expenses.route) {
                ExpenseScreen(
                    onDateSelected = { dateMillis ->
                        navController.navigate(Destination.ExpenseDayDetail.createRoute(dateMillis))
                    },
                    onSearchClick = { navController.navigate(Destination.ExpenseSearch.route) },
                )
            }
            composable(Destination.ExpenseSearch.route) {
                ExpenseSearchScreen(
                    onBack = { navController.popBackStack() },
                    onExpenseClick = { dateMillis ->
                        navController.navigate(Destination.ExpenseDayDetail.createRoute(dateMillis))
                    },
                )
            }
            composable(
                route = Destination.ExpenseDayDetail.route,
                arguments = listOf(
                    navArgument(Destination.ExpenseDayDetail.ARG_DATE_MILLIS) { type = NavType.LongType }
                ),
            ) {
                DayDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.Profile.route) {
                ProfileScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
