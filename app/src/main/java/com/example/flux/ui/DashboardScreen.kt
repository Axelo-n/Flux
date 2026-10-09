package com.example.flux.ui

import com.example.flux.preferences.translate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import com.example.flux.ui.settings.BudgetSettingsScreen
import com.example.flux.ui.settings.RulesScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flux.ui.analytics.AnalyticsScreen
import com.example.flux.ui.components.FluxBottomNavigation
import com.example.flux.ui.history.HistoryScreen
import com.example.flux.ui.home.HomeScreen
import com.example.flux.ui.settings.SettingsScreen
import com.example.flux.ui.transaction.AddTransactionScreen
import com.example.flux.ui.transaction.EditTransactionScreen
import com.example.flux.ui.theme.UIBackground
import com.example.flux.ui.theme.UITeal
import com.example.flux.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(message) { message?.let { Toast.makeText(context, translate(it), Toast.LENGTH_LONG).show(); viewModel.clearMessage() } }
    if (state.isLoading) { Surface(color = UIBackground, modifier = Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = UITeal) } }; return }
    if (!state.configured) { Surface(color = UIBackground, modifier = Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().safeDrawingPadding()) { BudgetSettingsScreen(viewModel) } }; return }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: FluxRoutes.HOME

    val mainRoutes = listOf(FluxRoutes.HOME, FluxRoutes.ANALYTICS, FluxRoutes.HISTORY, FluxRoutes.WALLET)
    val showBottomComponents = currentRoute in mainRoutes

    val selectedNavIndex = when (currentRoute) {
        FluxRoutes.HOME      -> 0
        FluxRoutes.ANALYTICS -> 1
        FluxRoutes.HISTORY   -> 2
        FluxRoutes.WALLET    -> 3
        else                 -> 0
    }

    Scaffold(
        containerColor = UIBackground,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()).consumeWindowInsets(innerPadding).imePadding()) {
            NavHost(navController = navController, startDestination = FluxRoutes.HOME, modifier = Modifier.fillMaxSize()) {
                composable(FluxRoutes.HOME) {
                    HomeScreen(viewModel = viewModel, onSettings = { navController.navigate(FluxRoutes.WALLET) }, onTransaction = { navController.navigate("${FluxRoutes.EDIT_TRANSACTION}/$it") }, onAdd = { navController.navigate(FluxRoutes.ADD_TRANSACTION) }, onHistory = { navController.navigate(FluxRoutes.HISTORY) })
                }
                composable(FluxRoutes.ANALYTICS) {
                    AnalyticsScreen(viewModel = viewModel)
                }
                composable(FluxRoutes.HISTORY) {
                    HistoryScreen(viewModel = viewModel, navController = navController, onBack = { navController.popBackStack() })
                }
                composable(FluxRoutes.WALLET) {
                    SettingsScreen(viewModel = viewModel, onBudget = { navController.navigate("budget") }, onRules = { navController.navigate("rules") }, onRestart = { navController.navigate("restart") }, onPockets = { navController.navigate("pockets") })
                }
                composable("pockets") { com.example.flux.ui.settings.PocketPlannerScreen(viewModel, { navController.popBackStack() }) }
                composable("budget") { BudgetSettingsScreen(viewModel, { navController.popBackStack() }) }
                composable("rules") { RulesScreen(viewModel, { navController.popBackStack() }) }
                composable("restart") { BudgetSettingsScreen(viewModel, { navController.popBackStack() }, restart = true) }
                composable(FluxRoutes.ADD_TRANSACTION) {
                    AddTransactionScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("${FluxRoutes.EDIT_TRANSACTION}/{txId}") { backStackEntry ->
                    val txId = backStackEntry.arguments?.getString("txId")?.toIntOrNull()
                    if (txId != null) {
                        EditTransactionScreen(viewModel = viewModel, transactionId = txId, onBack = { navController.popBackStack() })
                    }
                }
            }

            if (showBottomComponents) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    FluxBottomNavigation(
                        selectedIndex = selectedNavIndex,
                        onAdd = { navController.navigate(FluxRoutes.ADD_TRANSACTION) },
                        onItemSelected = { index ->
                            val route = when (index) {
                                0 -> FluxRoutes.HOME
                                1 -> FluxRoutes.ANALYTICS
                                2 -> FluxRoutes.HISTORY
                                3 -> FluxRoutes.WALLET
                                else -> FluxRoutes.HOME
                            }
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }
}
