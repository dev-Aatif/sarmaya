package com.sarmaya.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.sarmaya.app.SarmayaApplication
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sarmaya.app.ui.screens.DashboardScreen
import com.sarmaya.app.ui.screens.HoldingsScreen
import com.sarmaya.app.ui.screens.OnboardingScreen
import com.sarmaya.app.ui.screens.StockDetailScreen
import com.sarmaya.app.ui.screens.TransactionsScreen
import com.sarmaya.app.ui.screens.WatchlistScreen
import com.sarmaya.app.ui.screens.MarketScreen
import com.sarmaya.app.ui.screens.PriceAlertsScreen
import com.sarmaya.app.ui.screens.PortfolioSummaryScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object Holdings : Screen("holdings", "Portfolio", Icons.AutoMirrored.Filled.List)
    object Watchlist : Screen("watchlist", "Market", Icons.Filled.Star)
    object Transactions : Screen("transactions", "History", Icons.Filled.ShoppingCart)
    
    // Non-tab screens
    object StockDetail : Screen("stock_detail/{symbol}", "Stock Detail", Icons.Filled.Info) {
        fun createRoute(symbol: String) = "stock_detail/$symbol"
    }
    object PriceAlerts : Screen("price_alerts", "Price Alerts", Icons.Filled.Notifications)
    object PortfolioSummary : Screen("portfolio_summary", "Portfolio Summary", Icons.Filled.Info)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Holdings,
    Screen.Watchlist,
    Screen.Transactions
)

@Composable
fun SarmayaNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as SarmayaApplication
    val dataStore = app.container.dataStoreManager
    val navController = rememberNavController()

    // Check onboarding state
    val hasOnboarded by dataStore.hasOnboarded.collectAsState(initial = null)

    if (hasOnboarded == null) return // Or show a splash/loading

    AnimatedContent(
        targetState = hasOnboarded,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "onboarding_transition"
    ) { onboarded ->
        if (onboarded == false) {
            val scope = rememberCoroutineScope()
            OnboardingScreen(
                onComplete = { username ->
                    scope.launch {
                        dataStore.setUsername(username)
                        dataStore.setOnboarded(true)
                    }
                }
            )
        } else {
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainAppContent(
                        onStockClick = { symbol ->
                            navController.navigate(Screen.StockDetail.createRoute(symbol))
                        },
                        onAlertsClick = {
                            navController.navigate(Screen.PriceAlerts.route)
                        },
                        onTotalValueClick = {
                            navController.navigate(Screen.PortfolioSummary.route)
                        }
                    )
                }
                composable(
                    route = Screen.StockDetail.route,
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                    StockDetailScreen(
                        symbol = symbol,
                        onBack = { navController.popBackStack() },
                        onPeerClick = { peerSymbol ->
                            navController.navigate(Screen.StockDetail.createRoute(peerSymbol))
                        }
                    )
                }
                composable(Screen.PriceAlerts.route) {
                    PriceAlertsScreen(
                        onDismiss = { navController.popBackStack() }
                    )
                }
                composable(Screen.PortfolioSummary.route) {
                    com.sarmaya.app.ui.screens.PortfolioSummaryScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/**
 * Main app content with bottom navigation.
 * Uses Crossfade (via AnimatedContent) instead of HorizontalPager to prevent
 * navigation race conditions where tabs get stuck or open the wrong screen.
 */
@Composable
private fun MainAppContent(
    onStockClick: (String) -> Unit,
    onAlertsClick: () -> Unit,
    onTotalValueClick: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Use Crossfade-style AnimatedContent instead of HorizontalPager
        // This eliminates the dual-LaunchedEffect race condition that caused
        // tabs to get stuck or navigate to the wrong screen.
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "tab_content"
        ) { page ->
            when (page) {
                0 -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    DashboardScreen(
                        onStockClick = onStockClick,
                        onAlertsClick = onAlertsClick,
                        onTotalValueClick = onTotalValueClick,
                        onViewAllTransactions = { selectedTab = 3 },
                        onNewsClick = { article ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(article.link))
                            context.startActivity(intent)
                        }
                    )
                }
                1 -> HoldingsScreen(onStockClick = onStockClick)
                2 -> MarketScreen(onStockClick = onStockClick)
                3 -> TransactionsScreen()
            }
        }
    }
}
