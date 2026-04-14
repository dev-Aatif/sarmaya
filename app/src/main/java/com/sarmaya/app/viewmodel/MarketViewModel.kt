package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.StockQuoteCache
import com.sarmaya.app.data.StockQuoteCacheDao
import com.sarmaya.app.data.WatchlistDao
import com.sarmaya.app.data.WatchlistItem
import com.sarmaya.app.network.StockDataRepository
import com.sarmaya.app.network.api.PsxIndex
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MarketStock(
    val stock: Stock,
    val quote: StockQuoteCache?,
    val isWatched: Boolean = false
)

data class MarketUiState(
    val indices: List<PsxIndex> = emptyList(),
    val topGainers: List<MarketStock> = emptyList(),
    val topLosers: List<MarketStock> = emptyList(),
    val allStocks: List<MarketStock> = emptyList(),
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val selectedSector: String? = null
)

class MarketViewModel(
    private val repository: StockDataRepository,
    private val stockDao: StockDao,
    private val quoteCacheDao: StockQuoteCacheDao,
    private val watchlistDao: WatchlistDao,
    private val wsManager: com.sarmaya.app.network.websocket.PsxWebSocketManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSector = MutableStateFlow<String?>(null)
    val selectedSector: StateFlow<String?> = _selectedSector.asStateFlow()

    private val _isOnlyWatchlist = MutableStateFlow(false)
    val isOnlyWatchlist: StateFlow<Boolean> = _isOnlyWatchlist.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _indices = MutableStateFlow<List<PsxIndex>>(emptyList())
    val indices: StateFlow<List<PsxIndex>> = _indices.asStateFlow()

    private val _marketStatus = MutableStateFlow(com.sarmaya.app.util.MarketHoursUtil.getMarketState())
    val marketStatus: StateFlow<String> = _marketStatus.asStateFlow()

    private val allStocksFlow = stockDao.getAllStocks()
    private val watchlistFlow = watchlistDao.getAllItems()
    private val quoteFlow = quoteCacheDao.getAll()
    
    // Connect websocket tick updates to cache
    init {
        viewModelScope.launch {
            wsManager.tickUpdates.collect { update ->
                // Update local cache for immediate UI reflection
                quoteCacheDao.upsert(StockQuoteCache(
                    symbol = update.symbol,
                    price = update.price,
                    change = update.change,
                    changePercent = update.changePercent,
                    volume = update.volume,
                    high = update.high,
                    low = update.low,
                    cachedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    private val filterFlow = combine(_searchQuery, _selectedSector, _isOnlyWatchlist) { query, sector, onlyWatchlist ->
        Triple(query, sector, onlyWatchlist)
    }

    val marketStocks: StateFlow<List<MarketStock>> = combine(allStocksFlow, watchlistFlow, quoteFlow, filterFlow) { stocks, watchlist, caches, filter ->
        val (query, sector, onlyWatchlist) = filter
        val cacheMap = caches.associateBy { it.symbol }
        val watchedSymbols = watchlist.map { it.stockSymbol }.toSet()
        
        stocks
            .filter { stock ->
                (query.isBlank() || stock.symbol.contains(query, ignoreCase = true) || stock.name.contains(query, ignoreCase = true)) &&
                (sector == null || stock.sector == sector) &&
                (!onlyWatchlist || stock.symbol in watchedSymbols)
            }
            .map { MarketStock(it, cacheMap[it.symbol], it.symbol in watchedSymbols) }
            .sortedBy { it.stock.symbol }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topMovers: StateFlow<Pair<List<MarketStock>, List<MarketStock>>> = marketStocks.map { stocks ->
        val withChange = stocks.filter { it.quote != null }
            .sortedByDescending { it.quote?.changePercent ?: 0.0 }
        
        val gainers = withChange.take(10)
        val losers = withChange.reversed().take(10).filter { (it.quote?.changePercent ?: 0.0) < 0 }
        
        gainers to losers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<MarketStock>() to emptyList<MarketStock>())

    val sectors: StateFlow<List<String>> = allStocksFlow.map { stocks ->
         stocks.map { it.sector }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshMarket()
        // Subscribe to all stocks in viewport (simplified for MVP: all active stocks)
        wsManager.subscribe("marketData:REG")
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.unsubscribe("marketData:REG")
    }

    fun refreshMarket() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Sync full symbol list first to ensure Master data is available
            repository.getSymbols()
            
            repository.syncPsxQuotes() 
            repository.getIndices().onSuccess {
                _indices.value = it
            }
            repository.getMarketStatus().onSuccess {
                _marketStatus.value = it
            }
            _isRefreshing.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSector(sector: String?) {
        _selectedSector.value = sector
    }

    fun toggleOnlyWatchlist() {
        _isOnlyWatchlist.value = !_isOnlyWatchlist.value
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch {
            if (watchlistDao.isInWatchlist(symbol) > 0) {
                watchlistDao.deleteBySymbol(symbol)
            } else {
                watchlistDao.insert(WatchlistItem(stockSymbol = symbol))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SarmayaApplication
                return MarketViewModel(
                    application.container.stockDataRepository,
                    application.container.stockDao,
                    application.container.stockQuoteCacheDao,
                    application.container.watchlistDao,
                    application.container.psxWebSocketManager
                ) as T
            }
        }
    }
}
