package com.example.flux.ui

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
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
        floatingActionButton = {
            AnimatedVisibility(visible = showBottomComponents, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    onClick = { navController.navigate(FluxRoutes.ADD_TRANSACTION) },
                    containerColor = UITeal,
                    contentColor = UIBackground,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 100.dp).size(56.dp).shadow(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            NavHost(navController = navController, startDestination = FluxRoutes.HOME, modifier = Modifier.fillMaxSize()) {
                composable(FluxRoutes.HOME) {
                    HomeScreen(viewModel = viewModel)
                }
                composable(FluxRoutes.ANALYTICS) {
                    AnalyticsScreen(viewModel = viewModel)
                }
                composable(FluxRoutes.HISTORY) {
                    HistoryScreen(viewModel = viewModel, navController = navController, onBack = { navController.popBackStack() })
                }
                composable(FluxRoutes.WALLET) {
                    SettingsScreen(viewModel = viewModel)
                }
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
