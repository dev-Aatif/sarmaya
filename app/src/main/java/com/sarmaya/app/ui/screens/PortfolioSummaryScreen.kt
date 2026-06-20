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
            CenterAlignedTopAppBar(
                title = { Text("Performance Audit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 48.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SummaryCard(
                    label = "Current Portfolio Worth",
                    value = "₨ ${String.format("%,.0f", totalValue)}",
                    subValue = "Cost Basis: ₨ ${String.format("%,.0f", totalInvested)}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatBox(
                        label = "Unrealized P/L",
                        value = "${if (totalProfitLoss >= 0) "+" else ""}₨ ${String.format("%,.0f", totalProfitLoss)}",
                        color = if (totalProfitLoss >= 0) financeColors.profit else financeColors.loss,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Realized P/L",
                        value = "${if (totalRealizedPL >= 0) "+" else ""}₨ ${String.format("%,.0f", totalRealizedPL)}",
                        color = if (totalRealizedPL >= 0) financeColors.profit else financeColors.loss,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatBox(
                        label = "Dividends",
                        value = "₨ ${String.format("%,.0f", totalDividends)}",
                        color = financeColors.dividend,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Total Return",
                        value = "${if (totalReturn >= 0) "+" else ""}₨ ${String.format("%,.0f", totalReturn)}",
                        color = if (totalReturn >= 0) financeColors.profit else financeColors.loss,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionTitle("Engagement Metrics")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        StatRow("Active Holdings", holdingsCount.toString())
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        StatRow("Total ROI %", if (totalInvested > 0) String.format("%.2f%%", (totalReturn / totalInvested) * 100) else "0%")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        StatRow("Market Exposure", if (totalInvested > 0) String.format("%.2f%%", (totalProfitLoss / totalInvested) * 100) else "0%")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (sectorAllocation.isNotEmpty()) {
                item {
                    SectionTitle("Diversification")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            sectorAllocation.toList().sortedByDescending { it.second }.forEach { (sector, value) ->
                                val percentage = if (totalValue > 0) (value / totalValue).toFloat() else 0f
                                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sector, 
                                            style = MaterialTheme.typography.bodyMedium, 
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${String.format("%.1f", percentage * 100)}%", 
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    )
                                    Text(
                                        text = "₨ ${String.format("%,.0f", value)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = contentColor)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = contentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    subValue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
    val financeColors = LocalSarmayaColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
    )
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
