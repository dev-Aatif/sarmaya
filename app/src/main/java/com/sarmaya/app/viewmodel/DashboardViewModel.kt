package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.sarmaya.app.data.PortfolioCalculator

class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val stockDao: StockDao,
    private val portfolioDao: PortfolioDao,
    private val dataStoreManager: DataStoreManager,
    private val sarmayaRepository: com.sarmaya.app.network.StockDataRepository
) : ViewModel() {

    val allPortfolios: StateFlow<List<Portfolio>> = portfolioDao.getAllPortfolios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePortfolioId = dataStoreManager.activePortfolioId
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val activePortfolio: StateFlow<Portfolio?> = _activePortfolioId.flatMapLatest { id: Long? ->
        if (id != null) {
            flow { emit(portfolioDao.getPortfolioById(id)) }
        } else {
            flow { emit(portfolioDao.getDefaultPortfolio()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions = activePortfolio.flatMapLatest { portfolio ->
        if (portfolio != null) {
            transactionDao.getTransactionsByPortfolio(portfolio.id)
        } else {
            flowOf(emptyList())
        }
    }

    val computedHoldings = PortfolioCalculator.getEventSourcedHoldings(
        transactions,
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

    val recentTransactions = transactions
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPriceUpdate = stockDao.getAllStocks()
        .map { stocks -> stocks.mapNotNull { it.priceUpdatedAt }.maxOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val username = dataStoreManager.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refreshPrices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            sarmayaRepository.syncPsxQuotes()
            _isRefreshing.value = false
        }
    }

    fun updatePrices(prices: Map<String, Double>) {
        viewModelScope.launch {
            if (prices.isEmpty()) return@launch
            prices.forEach { (symbol, price) ->
                stockDao.updatePrice(symbol, price)
            }
        }
    }

    fun selectPortfolio(portfolioId: Long) {
        viewModelScope.launch {
            dataStoreManager.setActivePortfolioId(portfolioId)
        }
    }

    fun createPortfolio(name: String) {
        viewModelScope.launch {
            val newPortfolio = Portfolio(name = name)
            val id = portfolioDao.insert(newPortfolio)
            dataStoreManager.setActivePortfolioId(id)
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
                    application.container.portfolioDao,
                    application.container.dataStoreManager,
                    application.container.stockDataRepository
                ) as T
            }
        }
    }
}
