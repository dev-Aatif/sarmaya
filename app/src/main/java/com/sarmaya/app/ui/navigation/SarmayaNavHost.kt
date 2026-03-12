package com.sarmaya.app.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sarmaya.app.ui.screens.DashboardScreen
import com.sarmaya.app.ui.screens.HoldingsScreen
import com.sarmaya.app.ui.screens.SettingsScreen
import com.sarmaya.app.ui.screens.TransactionsScreen
import com.sarmaya.app.ui.screens.WatchlistScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object Holdings : Screen("holdings", "Portfolio", Icons.AutoMirrored.Filled.List)
    object Watchlist : Screen("watchlist", "Watchlist", Icons.Filled.Star)
    object Transactions : Screen("transactions", "History", Icons.Filled.ShoppingCart)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Holdings,
    Screen.Watchlist,
    Screen.Transactions,
    Screen.Settings
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SarmayaNavHost() {
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
                0 -> DashboardScreen()
                1 -> HoldingsScreen()
                2 -> WatchlistScreen()
                3 -> TransactionsScreen()
                4 -> SettingsScreen()
            }
        }
    }
}
