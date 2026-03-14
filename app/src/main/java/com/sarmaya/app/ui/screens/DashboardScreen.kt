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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.ui.components.UpdateBanner
import com.sarmaya.app.ui.theme.*
import com.sarmaya.app.viewmodel.DashboardViewModel
import com.sarmaya.app.viewmodel.UpdateViewModel
import java.text.SimpleDateFormat
import java.util.*

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


    // Update notifier
    val latestRelease by updateViewModel.latestRelease.collectAsStateWithLifecycle()
    val showUpdateBanner by updateViewModel.showUpdateBanner.collectAsStateWithLifecycle()

    var showUpdatePricesSheet by remember { mutableStateOf(false) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showStockPickerForBuy by remember { mutableStateOf(false) }
    var showStockPickerForManage by remember { mutableStateOf(false) }
    var selectedStockForManage by remember { mutableStateOf<String?>(null) }
    var showManagePositionSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPortfolioMenu by remember { mutableStateOf(false) }
    var showCreatePortfolioDialog by remember { mutableStateOf(false) }
    var newPortfolioName by remember { mutableStateOf("") }
    var showFabChoiceSheet by remember { mutableStateOf(false) }
    var showAddDividendSheet by remember { mutableStateOf(false) }
    var selectedStockForBuy by remember { mutableStateOf<String?>(null) }


    val financeColors = LocalSarmayaColors.current

    if (showUpdatePricesSheet) {
        com.sarmaya.app.ui.components.UpdatePricesSheet(
            onDismissRequest = { showUpdatePricesSheet = false }
        )
    }
    if (showAddTransactionSheet) {
        com.sarmaya.app.ui.components.AddTransactionSheet(
            preselectedSymbol = selectedStockForBuy,
            onDismissRequest = { 
                showAddTransactionSheet = false
                selectedStockForBuy = null
            }
        )
    }
    if (showSettingsSheet) {
        SettingsScreen(
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showStockPickerForBuy) {
        com.sarmaya.app.ui.components.StockPickerSheet(
            onDismissRequest = { showStockPickerForBuy = false },
            onStockSelected = { stock ->
                showStockPickerForBuy = false
                selectedStockForBuy = stock.symbol
                showAddTransactionSheet = true
            }
        )
    }

    if (showStockPickerForManage) {
        com.sarmaya.app.ui.components.StockPickerSheet(
            onDismissRequest = { showStockPickerForManage = false },
            onStockSelected = { stock ->
                showStockPickerForManage = false
                selectedStockForManage = stock.symbol
                showManagePositionSheet = true
            }
        )
    }

    if (showManagePositionSheet && selectedStockForManage != null) {
        com.sarmaya.app.ui.components.ManagePositionSheet(
            stockSymbol = selectedStockForManage!!,
            onDismissRequest = { 
                showManagePositionSheet = false
                selectedStockForManage = null
            }
        )
    }

    if (showFabChoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFabChoiceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Add New Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                Surface(
                    onClick = {
                        showFabChoiceSheet = false
                        showStockPickerForBuy = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = financeColors.profitContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = financeColors.profit,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Add Buy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Record a new stock purchase", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    onClick = {
                        showFabChoiceSheet = false
                        showStockPickerForManage = true // This will open ManagePositionSheet which has Dividend
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = financeColors.dividendContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DividendBlue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp, // Or some other icon for dividend
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Add Dividend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Record income from dividends", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showCreatePortfolioDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePortfolioDialog = false },
            title = { Text("Create Portfolio") },
            text = {
                OutlinedTextField(
                    value = newPortfolioName,
                    onValueChange = { newPortfolioName = it },
                    label = { Text("Portfolio Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPortfolioName.isNotBlank()) {
                            viewModel.createPortfolio(newPortfolioName)
                            newPortfolioName = ""
                            showCreatePortfolioDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePortfolioDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFabChoiceSheet = true },
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
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showPortfolioMenu = true }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    activePortfolio?.name ?: "All Portfolios",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showPortfolioMenu,
                                onDismissRequest = { showPortfolioMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                allPortfolios.forEach { portfolio ->
                                    DropdownMenuItem(
                                        text = { Text(portfolio.name) },
                                        onClick = {
                                            viewModel.selectPortfolio(portfolio.id)
                                            showPortfolioMenu = false
                                        },
                                        trailingIcon = {
                                            if (portfolio.id == activePortfolio?.id) {
                                                Icon(
                                                    Icons.Default.Check, // Changed from Add to Check for selection
                                                    contentDescription = "Selected",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("Create New Portfolio", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showPortfolioMenu = false
                                        showCreatePortfolioDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onAlertsClick
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Price Alerts",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true }
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

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
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val timeInMinutes = hour * 60 + minute
                val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
                val isMarketOpen = isWeekday && timeInMinutes in (9 * 60 + 30)..(15 * 60 + 30)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "PSX Market",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (isMarketOpen) ProfitGreenLight.copy(alpha = 0.15f) else LossRedLight.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    if (isMarketOpen) "Open" else "Closed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMarketOpen) ProfitGreenLight else LossRedLight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            val lastUpdatedStr = if (lastPriceUpdate != null) {
                                dateFormat.format(Date(lastPriceUpdate!!))
                            } else {
                                "Never"
                            }
                            Text(
                                lastUpdatedStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            IconButton(
                                onClick = { showUpdatePricesSheet = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Refresh Prices",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // ─── Portfolio Value Card (Gradient) ───
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTotalValueClick() },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(GradientEmeraldStart, GradientEmeraldEnd)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                "Total Portfolio Value",
                                style = MaterialTheme.typography.labelLarge,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "₨ ${String.format("%,.2f", totalValue)}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isProfit = totalProfitLoss >= 0
                                Icon(
                                    if (isProfit) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isProfit) ProfitGreenLight else LossRedLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    "${if (isProfit) "+" else ""}₨ ${String.format("%,.2f", totalProfitLoss)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfit) ProfitGreenLight else LossRedLight
                                )
                                if (totalInvested > 0) {
                                    val plPercent = (totalProfitLoss / totalInvested) * 100
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "(${String.format("%.1f", plPercent)}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${holdingsCount} active holdings",
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            // ─── Quick Stats Row ───
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickStatCard(
                        label = "Invested",
                        value = "₨ ${String.format("%,.2f", totalInvested)}",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                        onClick = { showStockPickerForBuy = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = financeColors.profitContainer,
                            contentColor = financeColors.onProfitContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showStockPickerForManage = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = financeColors.lossContainer,
                            contentColor = financeColors.onLossContainer
                        )
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sell / More", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            sectorAllocation.forEach { (sector, value) ->
                                val percentage = if (totalValue > 0) (value / totalValue).toFloat() else 0f
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        sector,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${String.format("%.1f", percentage * 100)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        "₨ ${String.format("%,.2f", value)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ─── Recent Transactions ───
            if (recentTransactions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = onViewAllTransactions) {
                            Text("View all")
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

            // Bottom spacer for FAB clearance
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun QuickStatCard(
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MoverChip(
    holding: ComputedHolding,
    isGainer: Boolean,
    financeColors: SarmayaFinanceColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGainer) financeColors.profitContainer else financeColors.lossContainer
        )
    ) {
        Column(
            modifier = Modifier
                .width(140.dp)
                .padding(14.dp)
        ) {
            Text(
                holding.stockSymbol,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGainer) financeColors.onProfitContainer else financeColors.onLossContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "₨ ${String.format("%,.2f", holding.currentPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = (if (isGainer) financeColors.onProfitContainer else financeColors.onLossContainer).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${if (isGainer) "▲" else "▼"} ${String.format("%.1f", holding.profitLossPercentage)}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isGainer) financeColors.profit else financeColors.loss
            )
        }
    }
}

@Composable
fun RecentTransactionRow(
    tx: Transaction,
    financeColors: SarmayaFinanceColors,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val dateStr = dateFormat.format(Date(tx.date))

    val (badgeColor, badgeTextColor) = when (tx.type) {
        "BUY" -> financeColors.profitContainer to financeColors.onProfitContainer
        "SELL" -> financeColors.lossContainer to financeColors.onLossContainer
        "DIVIDEND" -> financeColors.dividendContainer to financeColors.dividend
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    tx.type,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    tx.stockSymbol,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${tx.quantity} shares @ ₨ ${String.format("%,.2f", tx.pricePerShare)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
