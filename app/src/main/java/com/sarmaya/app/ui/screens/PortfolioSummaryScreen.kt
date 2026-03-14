package com.sarmaya.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.ui.theme.LocalSarmayaColors
import com.sarmaya.app.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioSummaryScreen(
    onBack: () -> Unit,
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
    
    val financeColors = LocalSarmayaColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portfolio Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SummaryCard(
                    label = "Total Portfolio Value",
                    value = "₨ ${String.format("%,.2f", totalValue)}",
                    subValue = "Invested: ₨ ${String.format("%,.2f", totalInvested)}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox(
                        label = "Unrealized P/L",
                        value = "${if (totalProfitLoss >= 0) "+" else ""}₨ ${String.format("%,.2f", totalProfitLoss)}",
                        color = if (totalProfitLoss >= 0) financeColors.profit else financeColors.loss,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Realized P/L",
                        value = "${if (totalRealizedPL >= 0) "+" else ""}₨ ${String.format("%,.2f", totalRealizedPL)}",
                        color = if (totalRealizedPL >= 0) financeColors.profit else financeColors.loss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox(
                        label = "Total Dividends",
                        value = "₨ ${String.format("%,.2f", totalDividends)}",
                        color = financeColors.dividend,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Total Return",
                        value = "${if (totalReturn >= 0) "+" else ""}₨ ${String.format("%,.2f", totalReturn)}",
                        color = if (totalReturn >= 0) financeColors.profit else financeColors.loss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionTitle("General Statistics")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatRow("Active Holdings", holdingsCount.toString())
                        StatRow("All-time Return %", if (totalInvested > 0) String.format("%.2f%%", (totalReturn / totalInvested) * 100) else "0%")
                        StatRow("Profitability Ratio", if (totalInvested > 0) String.format("%.2f%%", (totalProfitLoss / totalInvested) * 100) else "0%")
                    }
                }
            }

            if (sectorAllocation.isNotEmpty()) {
                item {
                    SectionTitle("Sector Allocation")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            sectorAllocation.forEach { (sector, value) ->
                                val percentage = if (totalValue > 0) (value / totalValue).toFloat() else 0f
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sector, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("${String.format("%.1f", percentage * 100)}%", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        "₨ ${String.format("%,.2f", value)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SummaryCard(
    label: String,
    value: String,
    subValue: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = contentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    subValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
