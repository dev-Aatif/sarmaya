package com.sarmaya.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll

import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.ui.components.PortfolioSelector
import com.sarmaya.app.ui.components.TransactionFlow
import com.sarmaya.app.ui.screens.dashboard.*
import com.sarmaya.app.ui.theme.*
import com.sarmaya.app.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStockClick: (String) -> Unit,
    onTotalValueClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val totalValue by viewModel.totalPortfolioValue.collectAsStateWithLifecycle()
    val totalInvested by viewModel.totalInvested.collectAsStateWithLifecycle()
    val totalProfitLoss by viewModel.totalProfitLoss.collectAsStateWithLifecycle()
    val totalRealizedPL by viewModel.totalRealizedPL.collectAsStateWithLifecycle()
    val totalDividends by viewModel.totalDividends.collectAsStateWithLifecycle()
    val totalReturn by viewModel.totalReturn.collectAsStateWithLifecycle()
    val holdingsCount by viewModel.holdingsCount.collectAsStateWithLifecycle()
    val sectorAllocation by viewModel.sectorAllocation.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val allPortfolios by viewModel.allPortfolios.collectAsStateWithLifecycle()
    val activePortfolio by viewModel.activePortfolio.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()


    var showTransactionForm by remember { mutableStateOf<String?>(null) } 
    var selectedStockForForm by remember { mutableStateOf<String?>(null) }

    val financeColors = LocalSarmayaColors.current

    TransactionFlow(
        showTypeSelection = false,
        showTransactionForm = showTransactionForm,
        preselectedSymbol = selectedStockForForm,
        onTypeSelected = { },
        onDismissTypeSelection = { },
        onDismissForm = {
            showTransactionForm = null
            selectedStockForForm = null
        }
    )


    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (username.isNotBlank()) "Salam, $username" else "Sarmaya",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        PortfolioSelector(
                            activePortfolio = activePortfolio,
                            allPortfolios = allPortfolios,
                            onPortfolioSelected = { viewModel.selectPortfolio(it) },
                            onCreatePortfolio = { viewModel.createPortfolio(it) }
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { viewModel.refreshPrices() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Prices")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTransactionForm = "BUY" },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                    hoveredElevation = 10.dp
                )
            ) {
                Icon(
                    Icons.Filled.Add, 
                    contentDescription = "Add Transaction", 
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Trade",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    ) { paddingValues ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPrices() },
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (isLoading) {
                item { 
                    com.sarmaya.app.ui.components.ShimmerCard(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        height = 180.dp 
                    ) 
                }
            } else {
                // ─── Main Portfolio Card ───
                item {
                    PortfolioValueCard(
                        totalValue = totalValue,
                        totalProfitLoss = totalProfitLoss,
                        totalInvested = totalInvested,
                        holdingsCount = holdingsCount,
                        financeColors = financeColors,
                        onClick = onTotalValueClick,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // ─── Quick Stats Row ───
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            QuickStatCard(
                                label = "Direct Invested",
                                value = "₨ ${String.format("%,.0f", totalInvested)}",
                                containerColor = financeColors.cardSurface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        item {
                            QuickStatCard(
                                label = "Realized P/L",
                                value = "${if (totalRealizedPL >= 0) "+" else ""}₨ ${String.format("%,.0f", totalRealizedPL)}",
                                containerColor = if (totalRealizedPL >= 0) financeColors.profitContainer else financeColors.lossContainer,
                                contentColor = if (totalRealizedPL >= 0) financeColors.onProfitContainer else financeColors.onLossContainer
                            )
                        }
                        item {
                            QuickStatCard(
                                label = "Dividends",
                                value = "₨ ${String.format("%,.0f", totalDividends)}",
                                containerColor = financeColors.dividendContainer,
                                contentColor = Color(0xFF1976D2)
                            )
                        }
                    }
                }



                // ─── Recent Activity ───
                if (recentTransactions.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Recent Activity",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onViewAllTransactions) {
                                    Text("View All", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    recentTransactions.forEachIndexed { index, tx ->
                                        RecentTransactionRow(tx = tx, financeColors = financeColors, onClick = { onStockClick(tx.stockSymbol) })
                                        if (index < recentTransactions.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 12.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Empty State ───
                if (holdingsCount == 0 && recentTransactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Start Your Journey",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Add your first transaction to see portfolio insights.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

