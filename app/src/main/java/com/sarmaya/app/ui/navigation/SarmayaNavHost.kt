package com.sarmaya.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.ui.screens.DashboardScreen
import com.sarmaya.app.ui.screens.HoldingsScreen
import com.sarmaya.app.ui.screens.MarketScreen
import com.sarmaya.app.ui.screens.OnboardingScreen
import com.sarmaya.app.ui.screens.PriceAlertsScreen
import com.sarmaya.app.ui.screens.TransactionsScreen
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
    val hasOnboarded by dataStore.hasOnboarded.collectAsState(
        initial = runBlocking { dataStore.hasOnboarded.first() }
    )

    AnimatedContent(
        targetState = hasOnboarded,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "onboarding_transition"
    ) { onboarded ->
        if (!onboarded) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainAppContent(
    onStockClick: (String) -> Unit,
    onAlertsClick: () -> Unit,
    onTotalValueClick: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0) { bottomNavItems.size }

    // Sync bottom bar tap → pager
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    // Sync pager swipe → bottom bar
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTab = page
        }
    }

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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            beyondBoundsPageCount = 1
        ) { page ->
            when (page) {
                0 -> DashboardScreen(
                    onStockClick = onStockClick,
                    onAlertsClick = onAlertsClick,
                    onTotalValueClick = onTotalValueClick,
                    onViewAllTransactions = { selectedTab = 3 }
                )
                1 -> HoldingsScreen(onStockClick = onStockClick)
                2 -> MarketScreen(onStockClick = onStockClick)
                3 -> TransactionsScreen()
            }
        }
    }
}
