package com.sarmaya.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.sarmaya.app.network.ChartRange
import com.sarmaya.app.network.CompanyProfile
import com.sarmaya.app.network.UnifiedQuote
import com.sarmaya.app.ui.theme.LocalSarmayaColors
import com.sarmaya.app.ui.theme.SarmayaFinanceColors
import com.sarmaya.app.viewmodel.PriceAlertViewModel
import com.sarmaya.app.viewmodel.StockDetailUiState
import com.sarmaya.app.viewmodel.StockDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    onPeerClick: (String) -> Unit = {}
) {
    val viewModel: StockDetailViewModel = viewModel(
        key = symbol,
        factory = StockDetailViewModel.Factory(symbol)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chartRange by viewModel.chartRange.collectAsStateWithLifecycle()
    val financeColors = LocalSarmayaColors.current
    
    val alertViewModel: PriceAlertViewModel = viewModel(factory = PriceAlertViewModel.Factory)
    var showAddAlertSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState is StockDetailUiState.Success) {
                            Text(
                                (uiState as StockDetailUiState.Success).profile.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddAlertSheet = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Add Price Alert")
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is StockDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StockDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.refreshAll() }) {
                            Text("Retry")
                        }
                    }
                }
                is StockDetailUiState.Success -> {
                    StockDetailContent(
                        quote = state.quote,
                        profile = state.profile,
                        chartData = state.chartData,
                        peers = state.peers,
                        currentRange = chartRange,
                        onRangeSelected = { viewModel.setChartRange(it) },
                        financeColors = financeColors,
                        onPeerClick = onPeerClick
                    )
                }
    if (showAddAlertSheet) {
        AddPriceAlertSheet(
            onDismiss = { showAddAlertSheet = false },
            onAdd = { sym, price, type ->
                alertViewModel.addAlert(sym, price, type)
                showAddAlertSheet = false
            }
        )
    }
}

@Composable
private fun StockDetailContent(
    quote: UnifiedQuote,
    profile: CompanyProfile,
    chartData: List<com.sarmaya.app.network.PricePoint>,
    peers: List<String>,
    currentRange: ChartRange,
    onRangeSelected: (ChartRange) -> Unit,
    financeColors: SarmayaFinanceColors,
    onPeerClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val isProfit = quote.change >= 0
    var showManageSheet by remember { mutableStateOf(false) }

    if (showManageSheet) {
        com.sarmaya.app.ui.components.ManagePositionSheet(
            stockSymbol = quote.symbol,
            onDismissRequest = { showManageSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Price Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "₨ ${String.format("%,.2f", quote.price)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${if (isProfit) "+" else ""}${String.format("%.2f", quote.change)} (${String.format("%.2f", quote.changePercent)}%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfit) financeColors.profit else financeColors.loss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isProfit) "▲" else "▼",
                        color = if (isProfit) financeColors.profit else financeColors.loss
                    )
                }
            }

            Button(
                onClick = { showManageSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Manage", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Chart Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (chartData.isNotEmpty()) {
                    val model = entryModelOf(chartData.map { it.close.toFloat() })
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = if (isProfit) financeColors.profit.toArgb() else financeColors.loss.toArgb(),
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(
                                            (if (isProfit) financeColors.profit else financeColors.loss).copy(alpha = 0.4f).toArgb(),
                                            (if (isProfit) financeColors.profit else financeColors.loss).copy(alpha = 0f).toArgb()
                                        )
                                    )
                                )
                            ),
                            axisValuesOverrider = AxisValuesOverrider.adaptiveYValues(1.05f, round = true)
                        ),
                        model = model,
                        startAxis = rememberStartAxis(
                            label = null,
                            tick = null,
                            axis = null,
                            guideline = null
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = null,
                            tick = null,
                            axis = null,
                            guideline = null
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No chart data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Range Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ChartRange.values().forEach { range ->
                        val selected = currentRange == range
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onRangeSelected(range) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                range.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Grid
        Text(
            "Market Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), label = "Open", value = "₨ ${String.format("%,.2f", quote.open)}")
            StatCard(modifier = Modifier.weight(1f), label = "Prev Close", value = "₨ ${String.format("%,.2f", quote.previousClose)}")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), label = "Day High", value = "₨ ${String.format("%,.2f", quote.dayHigh)}")
            StatCard(modifier = Modifier.weight(1f), label = "Day Low", value = "₨ ${String.format("%,.2f", quote.dayLow)}")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), label = "52W High", value = "₨ ${String.format("%,.2f", profile.weekHigh52)}")
            StatCard(modifier = Modifier.weight(1f), label = "52W Low", value = "₨ ${String.format("%,.2f", profile.weekLow52)}")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), label = "PE Ratio", value = String.format("%.2f", profile.peRatio))
            StatCard(modifier = Modifier.weight(1f), label = "Div Yield", value = "${String.format("%.2f", profile.dividendYield * 100)}%")
        }
        Spacer(modifier = Modifier.height(12.dp))
        StatCard(modifier = Modifier.fillMaxWidth(), label = "Market Cap", value = "₨ ${formatLargeNumber(profile.marketCap)}")

        Spacer(modifier = Modifier.height(24.dp))

        // About Section
        Text(
            "About ${profile.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            profile.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        if (peers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Related Stocks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                peers.forEach { peer ->
                    Surface(
                        modifier = Modifier.clickable { onPeerClick(peer) },
                        shape = RoundedCornerShape(12.dp),
                        color = financeColors.cardSurface,
                        border = RowDefaults.cardBorder()
                    ) {
                        Text(
                            peer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String) {
    val financeColors = LocalSarmayaColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatLargeNumber(number: Long): String {
    return when {
        number >= 1_000_000_000_000L -> String.format("%.2fT", number / 1_000_000_000_000.0)
        number >= 1_000_000_000L -> String.format("%.2fB", number / 1_000_000_000.0)
        number >= 1_000_000L -> String.format("%.2fM", number / 1_000_000.0)
        else -> String.format("%,d", number)
    }
}

object RowDefaults {
    @Composable
    fun cardBorder() = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
