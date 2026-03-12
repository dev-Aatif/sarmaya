package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.WatchlistDao
import com.sarmaya.app.data.WatchlistItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WatchlistStock(
    val item: WatchlistItem,
    val stock: Stock
)

class WatchlistViewModel(
    private val watchlistDao: WatchlistDao,
    private val stockDao: StockDao
) : ViewModel() {

    private val watchlistItems = watchlistDao.getAllItems()
    private val allStocks = stockDao.getAllStocks()

    val watchlistStocks: StateFlow<List<WatchlistStock>> = combine(watchlistItems, allStocks) { items, stocks ->
        val stockMap = stocks.associateBy { it.symbol }
        items.mapNotNull { item ->
            stockMap[item.stockSymbol]?.let { stock ->
                WatchlistStock(item, stock)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isEmpty: StateFlow<Boolean> = watchlistStocks.map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Search for adding stocks
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<Stock>> = combine(_searchQuery, allStocks, watchlistItems) { query, stocks, items ->
        val watchedSymbols = items.map { it.stockSymbol }.toSet()
        if (query.isBlank()) {
            emptyList()
        } else {
            stocks.filter { stock ->
                stock.symbol !in watchedSymbols &&
                (stock.symbol.contains(query, ignoreCase = true) || stock.name.contains(query, ignoreCase = true))
            }.take(20)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToWatchlist(symbol: String) {
        viewModelScope.launch {
            watchlistDao.insert(WatchlistItem(stockSymbol = symbol))
            _searchQuery.value = ""
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            watchlistDao.deleteBySymbol(symbol)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SarmayaApplication
                return WatchlistViewModel(
                    application.container.watchlistDao,
                    application.container.stockDao
                ) as T
            }
        }
    }
}
