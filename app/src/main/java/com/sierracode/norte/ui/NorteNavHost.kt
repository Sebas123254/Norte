package com.sierracode.norte.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sierracode.norte.R
import com.sierracode.norte.feature.dashboard.DashboardScreen
import com.sierracode.norte.feature.debts.DebtsScreen
import com.sierracode.norte.feature.documents.DocumentDetailScreen
import com.sierracode.norte.feature.documents.DocumentsListScreen
import com.sierracode.norte.feature.documents.ScannerDocumentScreen
import com.sierracode.norte.feature.expenses.ExpensesScreen
import com.sierracode.norte.feature.goals.GoalsScreen
import com.sierracode.norte.feature.settings.SettingsScreen

sealed class Route(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Dashboard : Route("dashboard", R.string.nav_dashboard, Icons.Filled.Home)
    data object Documents : Route("documents", R.string.nav_documents, Icons.Filled.Description)
    data object Expenses : Route("expenses", R.string.nav_expenses, Icons.Filled.Payments)
    data object Goals : Route("goals", R.string.nav_goals, Icons.Filled.Flag)
    data object Debts : Route("debts", R.string.nav_debts, Icons.Filled.AccountBalance)
    data object Settings : Route("settings", R.string.nav_settings, Icons.Filled.Settings)
}

private val bottomItems = listOf(
    Route.Dashboard, Route.Documents, Route.Expenses, Route.Goals, Route.Debts
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NorteNavHost() {
    val nav = rememberNavController()
    val backstack by nav.currentBackStackEntryAsState()
    val current = backstack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    val labelText = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = {
                            nav.navigate(item.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = labelText) },
                        label = { Text(labelText) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Route.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Dashboard.route) { DashboardScreen(nav) }
            composable(Route.Documents.route) { DocumentsListScreen(nav) }
            composable(Route.Expenses.route) { ExpensesScreen(nav) }
            composable(Route.Goals.route) { GoalsScreen(nav) }
            composable(Route.Debts.route) { DebtsScreen(nav) }
            composable(Route.Settings.route) { SettingsScreen(nav) }

            composable(
                route = "document/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                DocumentDetailScreen(nav, entry.arguments?.getString("id").orEmpty())
            }
            composable("scanner") { ScannerDocumentScreen(nav) }
        }
    }
}