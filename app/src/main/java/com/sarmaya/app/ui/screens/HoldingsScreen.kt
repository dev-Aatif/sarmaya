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
    val activePortfolio by viewModel.activePortfolio.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val activeHoldings = holdings.filter { it.quantity > 0 }
    val closedHoldings = holdings.filter { it.quantity == 0 }

    var showUpdatePricesSheet by remember { mutableStateOf(false) }
    var showTypeSelection by remember { mutableStateOf(false) }
    var showTransactionForm by remember { mutableStateOf<String?>(null) }
    var selectedStockForForm by remember { mutableStateOf<String?>(null) }

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



    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTypeSelection = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Options")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ─── Header ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Portfolio",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    PortfolioSelector(
                        activePortfolio = activePortfolio,
                        allPortfolios = allPortfolios,
                        onPortfolioSelected = { viewModel.selectPortfolio(it) },
                        onCreatePortfolio = { viewModel.createPortfolio(it) }
                    )
                }
                Button(
                    onClick = { showUpdatePricesSheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync", fontWeight = FontWeight.Bold)
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                com.sarmaya.app.ui.components.ShimmerCard(height = 120.dp)
                Spacer(modifier = Modifier.height(16.dp))
                com.sarmaya.app.ui.components.ShimmerCard(height = 80.dp)
                Spacer(modifier = Modifier.height(16.dp))
                com.sarmaya.app.ui.components.ShimmerCard(height = 80.dp)
            } else {
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(Modifier.fillMaxSize()) {
                if (activeHoldings.isNotEmpty()) {
                    item {
                        Text(
                            "Active Positions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(activeHoldings) { holding ->
                        HoldingItem(
                            holding = holding,
                            financeColors = financeColors,
                            onClick = { 
                                selectedStockForForm = holding.stockSymbol
                                showTypeSelection = true
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
                if (closedHoldings.isNotEmpty()) {
                    item {
                        Text(
                            "Closed Positions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(closedHoldings) { holding ->
                        HoldingItem(
                            holding = holding,
                            financeColors = financeColors,
                            onClick = { 
                                selectedStockForForm = holding.stockSymbol
                                showTypeSelection = true
                            }
                        )
                    }
                }
                if (holdings.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📈", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No holdings yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Your portfolio will appear here once you add transactions.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
}

@Composable
fun HoldingItem(holding: ComputedHolding, financeColors: SarmayaFinanceColors, onClick: () -> Unit) {
    val isProfit = holding.profitLossAmount >= 0
    val isClosed = holding.quantity == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Symbol + current value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        holding.stockSymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        holding.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isClosed) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "₨ ${String.format("%,.2f", holding.currentValue)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Surface(
                                color = if (isProfit) financeColors.profitContainer else financeColors.lossContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${if (isProfit) "+" else ""}${String.format("%.1f", holding.profitLossPercentage)}%  ${if (isProfit) "▲" else "▼"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfit) financeColors.onProfitContainer else financeColors.onLossContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Details
            if (!isClosed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailLabel(label = "Shares", value = "${holding.quantity}")
                    DetailLabel(label = "Avg Cost", value = "₨ ${String.format("%,.2f", holding.avgBuyPrice)}")
                    DetailLabel(label = "Price", value = "₨ ${String.format("%,.2f", holding.currentPrice)}")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailLabel(
                        label = "Unrealized P/L",
                        value = "${if (isProfit) "+" else ""}₨ ${String.format("%,.2f", holding.profitLossAmount)}",
                        valueColor = if (isProfit) financeColors.profit else financeColors.loss
                    )
                    if (holding.realizedProfitLoss != 0.0) {
                        val realizedIsProfit = holding.realizedProfitLoss >= 0
                        DetailLabel(
                            label = "Realized P/L",
                            value = "${if (realizedIsProfit) "+" else ""}₨ ${String.format("%,.2f", holding.realizedProfitLoss)}",
                            valueColor = if (realizedIsProfit) financeColors.profit else financeColors.loss
                        )
                    }
                    if (holding.totalDividends > 0) {
                        DetailLabel(
                            label = "Dividends",
                            value = "₨ ${String.format("%,.2f", holding.totalDividends)}",
                            valueColor = financeColors.dividend
                        )
                    }
                }
            } else {
                // Closed holdings show realized P/L and dividends
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (holding.realizedProfitLoss != 0.0) {
                        val realizedIsProfit = holding.realizedProfitLoss >= 0
                        DetailLabel(
                            label = "Realized P/L",
                            value = "${if (realizedIsProfit) "+" else ""}₨ ${String.format("%,.2f", holding.realizedProfitLoss)}",
                            valueColor = if (realizedIsProfit) financeColors.profit else financeColors.loss
                        )
                    }
                    if (holding.totalDividends > 0) {
                        DetailLabel(
                            label = "Dividends",
                            value = "₨ ${String.format("%,.2f", holding.totalDividends)}",
                            valueColor = financeColors.dividend
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (isClosed) "Tap to view history" else "Tap to manage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
