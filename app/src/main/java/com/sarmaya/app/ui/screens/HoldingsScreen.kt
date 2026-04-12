package com.sarmaya.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.data.*
import com.sarmaya.app.ui.components.PortfolioSelector
import com.sarmaya.app.ui.components.TransactionFlow
import com.sarmaya.app.ui.theme.*
import com.sarmaya.app.viewmodel.HoldingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsScreen(
    onStockClick: (String) -> Unit,
    viewModel: HoldingsViewModel = viewModel(factory = HoldingsViewModel.Factory)
) {
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val allPortfolios by viewModel.allPortfolios.collectAsStateWithLifecycle()
    val activePortfolio by viewModel.activePortfolio.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val activeHoldings = holdings.filter { it.quantity > 0 }
    val closedHoldings = holdings.filter { it.quantity == 0 }

    var showTypeSelection by remember { mutableStateOf(false) }
    var showTransactionForm by remember { mutableStateOf<String?>(null) }
    var selectedStockForForm by remember { mutableStateOf<String?>(null) }

    val financeColors = LocalSarmayaColors.current

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

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("Portfolio", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // Filter / Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PortfolioSelector(
                        activePortfolio = activePortfolio,
                        allPortfolios = allPortfolios,
                        onPortfolioSelected = { viewModel.selectPortfolio(it) },
                        onCreatePortfolio = { viewModel.createPortfolio(it) }
                    )
                    
                    Text(
                        "${activeHoldings.size} Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypeSelection = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, "Add Transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            if (isLoading) {
                items(3) {
                    com.sarmaya.app.ui.components.ShimmerCard(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        height = 120.dp
                    )
                }
            } else {
                if (activeHoldings.isNotEmpty()) {
                    item {
                        Text(
                            "Positions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }
                    items(activeHoldings) { holding ->
                        HoldingItem(
                            holding = holding,
                            financeColors = financeColors,
                            onClick = { onStockClick(holding.stockSymbol) },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                }

                if (closedHoldings.isNotEmpty()) {
                    item {
                        Text(
                            "Closed History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(closedHoldings) { holding ->
                        HoldingItem(
                            holding = holding,
                            financeColors = financeColors,
                            onClick = { onStockClick(holding.stockSymbol) },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                }

                if (holdings.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📊", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No holdings yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Your investment journey begins by adding your first trade.",
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

@Composable
fun HoldingItem(
    holding: ComputedHolding, 
    financeColors: SarmayaFinanceColors, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProfit = holding.profitLossAmount >= 0
    val isClosed = holding.quantity == 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        holding.stockSymbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        holding.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                if (!isClosed) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "₨ ${String.format("%,.0f", holding.currentValue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = (if (isProfit) financeColors.profit else financeColors.loss).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${if (isProfit) "+" else ""}${String.format("%.2f", holding.profitLossPercentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfit) financeColors.profit else financeColors.loss,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "CLOSED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isClosed) {
                    DetailLabel(label = "Shares", value = "${holding.quantity}")
                    DetailLabel(label = "Avg Cost", value = "₨ ${String.format("%,.1f", holding.avgBuyPrice)}")
                    DetailLabel(
                        label = "P/L", 
                        value = "${if (isProfit) "+" else ""}₨ ${String.format("%,.0f", holding.profitLossAmount)}",
                        valueColor = if (isProfit) financeColors.profit else financeColors.loss
                    )
                } else {
                    val realizedProfit = holding.realizedProfitLoss >= 0
                    DetailLabel(
                        label = "Realized P/L", 
                        value = "${if (realizedProfit) "+" else ""}₨ ${String.format("%,.0f", holding.realizedProfitLoss)}",
                        valueColor = if (realizedProfit) financeColors.profit else financeColors.loss
                    )
                    if (holding.totalDividends > 0) {
                        DetailLabel(
                            label = "Dividends", 
                            value = "₨ ${String.format("%,.0f", holding.totalDividends)}",
                            valueColor = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailLabel(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
