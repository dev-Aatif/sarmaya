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
import androidx.compose.material.icons.filled.StarBorder
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
    
    val financeColors = LocalSarmayaColors.current

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("Market", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search scrips or company name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                
                // Sector Filter
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = isOnlyWatchlist,
                            onClick = { viewModel.toggleOnlyWatchlist() },
                            label = { Text("Watchlist") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSector == null && !isOnlyWatchlist,
                            onClick = { viewModel.selectSector(null) },
                            label = { Text("All Sectors") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(sectors) { sector ->
                        FilterChip(
                            selected = selectedSector == sector,
                            onClick = { viewModel.selectSector(sector) },
                            label = { Text(sector) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Indices Section
            if (indices.isNotEmpty() && searchQuery.isBlank() && selectedSector == null && !isOnlyWatchlist) {
                item {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            "Market Indices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Top Gainers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(topMovers.first) { mover ->
                                MoverMiniCard(
                                    mover = mover, 
                                    financeColors = financeColors, 
                                    onStockClick = onStockClick,
                                    onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            "Top Losers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(marketStocks) { marketStock ->
                MarketStockItem(
                    marketStock = marketStock, 
                    financeColors = financeColors, 
                    onStockClick = onStockClick,
                    onToggleWatchlist = { viewModel.toggleWatchlist(it) }
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(index.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                String.format("%,.0f", index.current),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (isProfit) "+" else ""}${String.format("%.2f", index.change)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isProfit) financeColors.profit else financeColors.loss
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "(${String.format("%.2f", index.changep)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isProfit) financeColors.profit else financeColors.loss
                )
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
                        imageVector = if (mover.isWatched) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Watchlist",
                        tint = if (mover.isWatched) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
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
            Text(
                "₨ ${String.format("%.2f", mover.quote?.price ?: 0.0)}",
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
    onToggleWatchlist: (String) -> Unit
) {
    val quote = marketStock.quote
    val isProfit = (quote?.change ?: 0.0) >= 0

    Row(
        modifier = Modifier
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
                imageVector = if (marketStock.isWatched) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Watchlist",
                tint = if (marketStock.isWatched) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(
                "₨ ${String.format("%.2f", quote?.price ?: 0.0)}",
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
