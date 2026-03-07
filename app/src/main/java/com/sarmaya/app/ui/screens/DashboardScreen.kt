package com.sarmaya.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val totalValue by viewModel.totalPortfolioValue.collectAsStateWithLifecycle()
    val totalProfitLoss by viewModel.totalProfitLoss.collectAsStateWithLifecycle()
    val sectorAllocation by viewModel.sectorAllocation.collectAsStateWithLifecycle()
    var showUpdatePricesSheet by remember { mutableStateOf(false) }

    var showAddTransactionSheet by remember { mutableStateOf(false) }

    if (showUpdatePricesSheet) {
        com.sarmaya.app.ui.components.UpdatePricesSheet(
            onDismissRequest = { showUpdatePricesSheet = false }
        )
    }

    if (showAddTransactionSheet) {
        com.sarmaya.app.ui.components.AddTransactionSheet(
            onDismissRequest = { showAddTransactionSheet = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTransactionSheet = true }) {
                Icon(Icons.Filled.Add, "Track New Stock")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Portfolio Flow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = { showUpdatePricesSheet = true }) {
                Text("Update Prices")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Total Value", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(
                    "₨ ${String.format("%,.2f", totalValue)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Total P/L", style = MaterialTheme.typography.labelMedium)
                val plColor = if (totalProfitLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    "${if (totalProfitLoss >= 0) "+" else ""}₨ ${String.format("%,.2f", totalProfitLoss)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = plColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Sector Allocation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(Modifier.fillMaxSize()) {
            items(sectorAllocation) { (sector, value) ->
                val percentage = if (totalValue > 0) (value / totalValue).toFloat() else 0f
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(sector, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("₨ ${String.format("%,.2f", value)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
            }
        }
    }
}
