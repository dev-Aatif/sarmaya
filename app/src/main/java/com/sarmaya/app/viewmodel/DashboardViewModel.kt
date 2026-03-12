package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.DataStoreManager
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.sarmaya.app.data.PortfolioCalculator

class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val stockDao: StockDao,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    val computedHoldings = PortfolioCalculator.getEventSourcedHoldings(
        transactionDao.getAllTransactions(),
        stockDao.getAllStocks()
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val totalPortfolioValue = computedHoldings.map { holdings ->
        holdings.sumOf { it.currentValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalInvested = computedHoldings.map { holdings ->
        holdings.sumOf { it.totalInvested }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    val totalProfitLoss = computedHoldings.map { holdings ->
        holdings.sumOf { it.profitLossAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalRealizedPL = computedHoldings.map { holdings ->
        holdings.sumOf { it.realizedProfitLoss }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDividends = computedHoldings.map { holdings ->
        holdings.sumOf { it.totalDividends }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalReturn = computedHoldings.map { holdings ->
        holdings.sumOf { it.totalReturn }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val holdingsCount = computedHoldings.map { holdings ->
        holdings.count { it.quantity > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val sectorAllocation = computedHoldings.map { holdings ->
        holdings.filter { it.quantity > 0 }
            .groupBy { it.sector }
            .mapValues { entry -> entry.value.sumOf { it.currentValue } }
            .toList()
            .sortedByDescending { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topGainers = computedHoldings.map { holdings ->
        holdings.filter { it.quantity > 0 && it.profitLossPercentage > 0 }
            .sortedByDescending { it.profitLossPercentage }
            .take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topLosers = computedHoldings.map { holdings ->
        holdings.filter { it.quantity > 0 && it.profitLossPercentage < 0 }
            .sortedBy { it.profitLossPercentage }
            .take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions = transactionDao.getAllTransactions()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPriceUpdate = stockDao.getAllStocks()
        .map { stocks -> stocks.mapNotNull { it.priceUpdatedAt }.maxOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val username = dataStoreManager.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun updatePrices(prices: Map<String, Double>) {
        viewModelScope.launch {
            if (prices.isEmpty()) return@launch
            prices.forEach { (symbol, price) ->
                stockDao.updatePrice(symbol, price)
            }
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
                return DashboardViewModel(
                    application.container.transactionDao,
                    application.container.stockDao,
                    application.container.dataStoreManager
                ) as T
            }
        }
    }
}
