package com.sarmaya.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.ui.theme.*
import com.sarmaya.app.viewmodel.HoldingsViewModel

@Composable
fun HoldingsScreen(
    viewModel: HoldingsViewModel = viewModel(factory = HoldingsViewModel.Factory)
) {
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val activeHoldings = holdings.filter { it.quantity > 0 }
    val closedHoldings = holdings.filter { it.quantity == 0 }

    var selectedManageStock by remember { mutableStateOf<String?>(null) }
    var showUpdatePricesSheet by remember { mutableStateOf(false) }

    val financeColors = LocalSarmayaColors.current

    if (selectedManageStock != null) {
        com.sarmaya.app.ui.components.ManagePositionSheet(
            stockSymbol = selectedManageStock!!,
            onDismissRequest = { selectedManageStock = null }
        )
    }
    if (showUpdatePricesSheet) {
        com.sarmaya.app.ui.components.UpdatePricesSheet(
            onDismissRequest = { showUpdatePricesSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                if (activeHoldings.isNotEmpty()) {
                    val totalActiveValue = activeHoldings.sumOf { it.currentValue }
                    Text(
                        "₨ ${String.format("%,.0f", totalActiveValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = { showUpdatePricesSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Update Prices", style = MaterialTheme.typography.labelLarge)
            }
        }
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
                        onClick = { selectedManageStock = holding.stockSymbol }
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
                        onClick = { selectedManageStock = holding.stockSymbol }
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
            item { Spacer(modifier = Modifier.height(24.dp)) }
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
                Column {
                    Text(
                        holding.stockSymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        holding.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isClosed) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "₨ ${String.format("%,.2f", holding.currentValue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // P/L Badge
                        Surface(
                            color = if (isProfit) financeColors.profitContainer else financeColors.lossContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${if (isProfit) "+" else ""}${String.format("%.1f", holding.profitLossPercentage)}%  ${if (isProfit) "▲" else "▼"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfit) financeColors.onProfitContainer else financeColors.onLossContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
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
                        value = "${if (isProfit) "+" else ""}₨ ${String.format("%,.0f", holding.profitLossAmount)}",
                        valueColor = if (isProfit) financeColors.profit else financeColors.loss
                    )
                    if (holding.realizedProfitLoss != 0.0) {
                        val realizedIsProfit = holding.realizedProfitLoss >= 0
                        DetailLabel(
                            label = "Realized P/L",
                            value = "${if (realizedIsProfit) "+" else ""}₨ ${String.format("%,.0f", holding.realizedProfitLoss)}",
                            valueColor = if (realizedIsProfit) financeColors.profit else financeColors.loss
                        )
                    }
                    if (holding.totalDividends > 0) {
                        DetailLabel(
                            label = "Dividends",
                            value = "₨ ${String.format("%,.0f", holding.totalDividends)}",
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
                            value = "${if (realizedIsProfit) "+" else ""}₨ ${String.format("%,.0f", holding.realizedProfitLoss)}",
                            valueColor = if (realizedIsProfit) financeColors.profit else financeColors.loss
                        )
                    }
                    if (holding.totalDividends > 0) {
                        DetailLabel(
                            label = "Dividends",
                            value = "₨ ${String.format("%,.0f", holding.totalDividends)}",
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
