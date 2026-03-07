package com.sarmaya.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.viewmodel.HoldingsViewModel

@Composable
fun HoldingsScreen(
    viewModel: HoldingsViewModel = viewModel(factory = HoldingsViewModel.Factory)
) {
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val activeHoldings = holdings.filter { it.quantity > 0 }
    val closedHoldings = holdings.filter { it.quantity == 0 }

    var selectedManageStock by remember { mutableStateOf<String?>(null) }

    if (selectedManageStock != null) {
        com.sarmaya.app.ui.components.ManagePositionSheet(
            stockSymbol = selectedManageStock!!,
            onDismissRequest = { selectedManageStock = null }
        )
    }

    var showUpdatePricesSheet by remember { mutableStateOf(false) }

    if (showUpdatePricesSheet) {
        com.sarmaya.app.ui.components.UpdatePricesSheet(
            onDismissRequest = { showUpdatePricesSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Holdings", style = MaterialTheme.typography.headlineMedium)
            FilledTonalButton(onClick = { showUpdatePricesSheet = true }) {
                Text("Update Prices")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(Modifier.fillMaxSize()) {
            if (activeHoldings.isNotEmpty()) {
                item { Text("Active Positions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }
                items(activeHoldings) { holding ->
                    HoldingItem(holding, onClick = { selectedManageStock = holding.stockSymbol })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            if (closedHoldings.isNotEmpty()) {
                item { Text("Closed Positions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp)) }
                items(closedHoldings) { holding ->
                    HoldingItem(holding, onClick = { selectedManageStock = holding.stockSymbol })
                }
            }
            if (holdings.isEmpty()) {
                item {
                    Text(
                        "No holdings yet. Add a transaction to get started.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HoldingItem(holding: ComputedHolding, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(holding.stockSymbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("₨ ${String.format("%,.2f", holding.currentValue)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${holding.quantity} shares", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val isProfit = holding.profitLossAmount >= 0
                val plColor = if (isProfit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                val onPlColor = if (isProfit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                
                Surface(
                    color = plColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "${if (isProfit) "+" else ""}₨ ${String.format("%,.2f", holding.profitLossAmount)} (${String.format("%.2f", holding.profitLossPercentage)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = onPlColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Avg: ₨ ${String.format("%,.2f", holding.avgBuyPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Price: ₨ ${String.format("%,.2f", holding.currentPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (holding.totalDividends > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Dividends Earned: ₨ ${String.format("%,.2f", holding.totalDividends)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap to manage (Sell / Dividend)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
