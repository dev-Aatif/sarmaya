package com.sarmaya.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.ui.theme.LocalSarmayaColors
import com.sarmaya.app.ui.theme.SarmayaFinanceColors
import com.sarmaya.app.viewmodel.MarketStock
import com.sarmaya.app.viewmodel.MarketViewModel
import com.sarmaya.app.ui.components.MarketStateBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onStockClick: (String) -> Unit,
    viewModel: MarketViewModel = viewModel(factory = MarketViewModel.Factory)
) {
    val marketStocks by viewModel.marketStocks.collectAsStateWithLifecycle()
    val topMovers by viewModel.topMovers.collectAsStateWithLifecycle()
    val sectors by viewModel.sectors.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSector by viewModel.selectedSector.collectAsStateWithLifecycle()
    val isOnlyWatchlist by viewModel.isOnlyWatchlist.collectAsStateWithLifecycle()
    val indices by viewModel.indices.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val marketStatus by viewModel.marketStatus.collectAsStateWithLifecycle()
    
    val financeColors = LocalSarmayaColors.current

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Market Explorer", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            MarketStateBadge(state = marketStatus)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                // Premium Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    placeholder = { Text("Search symbols or companies...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
                
                // Filters Row: Watchlist Toggle + Sector Dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isOnlyWatchlist,
                        onClick = { viewModel.toggleOnlyWatchlist() },
                        label = { Text("Watchlist") },
                        leadingIcon = { 
                            Icon(
                                if (isOnlyWatchlist) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp)
                            ) 
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = Color(0xFFFFD700)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selected = isOnlyWatchlist,
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = Color.Transparent
                        )
                    )

                    var sectorExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = sectorExpanded,
                        onExpandedChange = { sectorExpanded = !sectorExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSector ?: "All Sectors",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectorExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = sectorExpanded,
                            onDismissRequest = { sectorExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sectors") },
                                onClick = {
                                    viewModel.selectSector(null)
                                    sectorExpanded = false
                                }
                            )
                            sectors.forEach { sector ->
                                DropdownMenuItem(
                                    text = { Text(sector) },
                                    onClick = {
                                        viewModel.selectSector(sector)
                                        sectorExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Indices Section
            if (indices.isNotEmpty() && searchQuery.isBlank() && selectedSector == null && !isOnlyWatchlist) {
                item {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            "Market Indices",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(indices) { index ->
                                IndexCard(index, financeColors)
                            }
                        }
                    }
                }
            }

            // Movers Section (only when not searching/filtering)
            if (searchQuery.isBlank() && selectedSector == null && !isOnlyWatchlist) {
                item {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            "Top Gainers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(topMovers.first) { mover ->
                                MoverMiniCard(
                                    mover = mover, 
                                    financeColors = financeColors, 
                                    onStockClick = onStockClick,
                                    onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            "Top Losers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(topMovers.second) { mover ->
                                MoverMiniCard(
                                    mover = mover, 
                                    financeColors = financeColors, 
                                    onStockClick = onStockClick,
                                    onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                                )
                            }
                        }
                    }
                }
            }

            // All Stocks Section
            item {
                val title = titleForList(searchQuery, selectedSector, isOnlyWatchlist)
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            items(marketStocks) { marketStock ->
                MarketStockItem(
                    marketStock = marketStock, 
                    financeColors = financeColors, 
                    onStockClick = onStockClick,
                    onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun IndexCard(index: com.sarmaya.app.network.api.PsxIndex, financeColors: SarmayaFinanceColors) {
    val isProfit = index.change >= 0
    Card(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = index.name, 
                style = MaterialTheme.typography.labelMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("%,.0f", index.current),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = (if (isProfit) financeColors.profit else financeColors.loss).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format("%.2f", index.changep)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfit) financeColors.profit else financeColors.loss
                    )
                }
            }
        }
    }
}

@Composable
fun MoverMiniCard(
    mover: MarketStock,
    financeColors: SarmayaFinanceColors,
    onStockClick: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit
) {
    val isProfit = (mover.quote?.change ?: 0.0) >= 0
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onStockClick(mover.stock.symbol) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    mover.stock.symbol,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onToggleWatchlist(mover.stock.symbol) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (mover.isWatched) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = "Watchlist",
                        tint = if (mover.isWatched) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                mover.stock.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            val displayPrice = if (mover.quote != null && mover.quote.price > 0) mover.quote.price else mover.stock.currentPrice
            Text(
                "₨ ${String.format("%.2f", displayPrice)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${if (isProfit) "+" else ""}${String.format("%.2f", mover.quote?.changePercent ?: 0.0)}%",
                color = if (isProfit) financeColors.profit else financeColors.loss,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MarketStockItem(
    marketStock: MarketStock,
    financeColors: SarmayaFinanceColors,
    onStockClick: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quote = marketStock.quote
    val isProfit = (quote?.change ?: 0.0) >= 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onStockClick(marketStock.stock.symbol) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onToggleWatchlist(marketStock.stock.symbol) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (marketStock.isWatched) Icons.Default.Star else Icons.Outlined.Star,
                contentDescription = "Watchlist",
                tint = if (marketStock.isWatched) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                marketStock.stock.symbol,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                marketStock.stock.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val displayPrice = if (quote != null && quote.price > 0) quote.price else marketStock.stock.currentPrice
            Text(
                "₨ ${String.format("%.2f", displayPrice)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isProfit) financeColors.profit.copy(alpha = 0.1f) 
                        else financeColors.loss.copy(alpha = 0.1f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "${if (isProfit) "+" else ""}${String.format("%.2f", quote?.changePercent ?: 0.0)}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isProfit) financeColors.profit else financeColors.loss
                )
            }
        }
    }
}

private fun titleForList(query: String, sector: String?, onlyWatchlist: Boolean): String {
    return when {
        query.isNotBlank() -> "Search Results"
        onlyWatchlist -> "My Watchlist"
        sector != null -> "Sector: $sector"
        else -> "All Scrips"
    }
}
