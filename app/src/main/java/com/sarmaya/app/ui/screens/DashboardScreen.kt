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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.ui.components.PortfolioSelector
import com.sarmaya.app.ui.components.TransactionFlow
import com.sarmaya.app.ui.components.UpdateBanner
import com.sarmaya.app.ui.screens.dashboard.*
import com.sarmaya.app.ui.theme.*
import com.sarmaya.app.viewmodel.DashboardViewModel
import com.sarmaya.app.viewmodel.UpdateViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStockClick: (String) -> Unit,
    onAlertsClick: () -> Unit,
    onTotalValueClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
    updateViewModel: UpdateViewModel = viewModel(factory = UpdateViewModel.Factory)
) {
    val totalValue by viewModel.totalPortfolioValue.collectAsStateWithLifecycle()
    val totalInvested by viewModel.totalInvested.collectAsStateWithLifecycle()
    val totalProfitLoss by viewModel.totalProfitLoss.collectAsStateWithLifecycle()
    val totalRealizedPL by viewModel.totalRealizedPL.collectAsStateWithLifecycle()
    val totalDividends by viewModel.totalDividends.collectAsStateWithLifecycle()
    val totalReturn by viewModel.totalReturn.collectAsStateWithLifecycle()
    val holdingsCount by viewModel.holdingsCount.collectAsStateWithLifecycle()
    val sectorAllocation by viewModel.sectorAllocation.collectAsStateWithLifecycle()
    val topGainers by viewModel.topGainers.collectAsStateWithLifecycle()
    val topLosers by viewModel.topLosers.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val lastPriceUpdate by viewModel.lastPriceUpdate.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val allPortfolios by viewModel.allPortfolios.collectAsStateWithLifecycle()
    val activePortfolio by viewModel.activePortfolio.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()


    // Update notifier
    val latestRelease by updateViewModel.latestRelease.collectAsStateWithLifecycle()
    val showUpdateBanner by updateViewModel.showUpdateBanner.collectAsStateWithLifecycle()

    var showUpdatePricesSheet by remember { mutableStateOf(false) }
    var showTypeSelection by remember { mutableStateOf(false) }
    var showTransactionForm by remember { mutableStateOf<String?>(null) } // "BUY", "SELL", etc.
    var selectedStockForForm by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }


    val financeColors = LocalSarmayaColors.current

    if (showUpdatePricesSheet) {
        com.sarmaya.app.ui.components.UpdatePricesSheet(
            onDismissRequest = { showUpdatePricesSheet = false }
        )
    }
    TransactionFlow(
        showTypeSelection = showTypeSelection,
        showTransactionForm = showTransactionForm,
        preselectedSymbol = selectedStockForForm,
        onTypeSelected = { type ->
            showTypeSelection = false
            showTransactionForm = type
        },
        onDismissTypeSelection = { showTypeSelection = false },
        onDismissForm = {
            showTransactionForm = null
            selectedStockForForm = null
        }
    )

    if (showSettingsSheet) {
        SettingsScreen(
            onDismiss = { showSettingsSheet = false }
        )
    }


    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypeSelection = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, "Add Options")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Header ───
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (username.isNotBlank()) "Welcome back, $username" else "Sarmaya",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PortfolioSelector(
                            activePortfolio = activePortfolio,
                            allPortfolios = allPortfolios,
                            onPortfolioSelected = { viewModel.selectPortfolio(it) },
                            onCreatePortfolio = { viewModel.createPortfolio(it) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onAlertsClick) {
                            Icon(androidx.compose.material.icons.Icons.Default.Notifications, "Alerts", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (isLoading) {
                item { com.sarmaya.app.ui.components.ShimmerCard(height = 160.dp) }
                item { com.sarmaya.app.ui.components.ShimmerCard(height = 100.dp) }
                item { com.sarmaya.app.ui.components.ShimmerCard(height = 200.dp) }
            } else {
                // ─── Update Banner ───
                val release = latestRelease
                if (release != null) {
                    item {
                        UpdateBanner(
                            release = release,
                            visible = showUpdateBanner,
                            onDismiss = { updateViewModel.dismissUpdate() }
                        )
                    }
                }

                // ─── Market Status ───
                item {
                    MarketStatusCard(
                        lastPriceUpdate = lastPriceUpdate,
                        onRefreshClick = { showUpdatePricesSheet = true }
                    )
                }

                // ─── Portfolio Value Card ───
                item {
                    PortfolioValueCard(
                        totalValue = totalValue,
                        totalProfitLoss = totalProfitLoss,
                        totalInvested = totalInvested,
                        holdingsCount = holdingsCount,
                        financeColors = financeColors,
                        onClick = onTotalValueClick
                    )
                }

                // ─── Quick Stats ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickStatCard(
                            label = "Invested",
                            value = "₨ ${String.format("%,.2f", totalInvested)}",
                            containerColor = financeColors.cardSurface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatCard(
                            label = "Realized P/L",
                            value = "${if (totalRealizedPL >= 0) "+" else ""}₨ ${String.format("%,.2f", totalRealizedPL)}",
                            containerColor = if (totalRealizedPL >= 0) financeColors.profitContainer else financeColors.lossContainer,
                            contentColor = if (totalRealizedPL >= 0) financeColors.onProfitContainer else financeColors.onLossContainer,
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatCard(
                            label = "Dividends",
                            value = "₨ ${String.format("%,.2f", totalDividends)}",
                            containerColor = financeColors.dividendContainer,
                            contentColor = DividendBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ─── Total Return Card ───
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (totalReturn >= 0) financeColors.profitContainer else financeColors.lossContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Total Return",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (totalReturn >= 0) financeColors.onProfitContainer else financeColors.onLossContainer
                                )
                                Text(
                                    "Unrealized + Realized + Dividends",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = (if (totalReturn >= 0) financeColors.onProfitContainer else financeColors.onLossContainer).copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                "${if (totalReturn >= 0) "+" else ""}₨ ${String.format("%,.2f", totalReturn)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (totalReturn >= 0) financeColors.onProfitContainer else financeColors.onLossContainer
                            )
                        }
                    }
                }

                // ─── Top Movers ───
                if (topGainers.isNotEmpty() || topLosers.isNotEmpty()) {
                    item {
                        Text(
                            "Top Movers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(topGainers) { holding ->
                                MoverChip(holding = holding, isGainer = true, financeColors = financeColors, onClick = { onStockClick(holding.stockSymbol) })
                            }
                            items(topLosers) { holding ->
                                MoverChip(holding = holding, isGainer = false, financeColors = financeColors, onClick = { onStockClick(holding.stockSymbol) })
                            }
                        }
                    }
                }

                // ─── Quick Actions (Buy / Sell) ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showTransactionForm = "BUY" },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = financeColors.profitContainer,
                                contentColor = financeColors.onProfitContainer
                            )
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showTypeSelection = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = financeColors.lossContainer,
                                contentColor = financeColors.onLossContainer
                            )
                        ) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ─── Sector Allocation ───
                if (sectorAllocation.isNotEmpty()) {
                    item {
                        Text(
                            "Sector Allocation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    item {
                        SectorAllocationCard(
                            sectorAllocation = sectorAllocation,
                            totalValue = totalValue,
                            financeColors = financeColors
                        )
                    }
                }

                // ─── Recent Activity ───
                if (recentTransactions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Activity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = onViewAllTransactions) {
                                Text("View All")
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                recentTransactions.forEachIndexed { index, tx ->
                                    RecentTransactionRow(tx = tx, financeColors = financeColors, onClick = { onStockClick(tx.stockSymbol) })
                                    if (index < recentTransactions.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Empty State ───
                if (holdingsCount == 0 && recentTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "📊",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Start tracking your portfolio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tap + to add your first stock transaction",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacer for FAB clearance
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

