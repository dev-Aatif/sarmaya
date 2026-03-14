package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.DataStoreManager
import com.sarmaya.app.data.Portfolio
import com.sarmaya.app.data.PortfolioDao
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.TransactionDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.sarmaya.app.data.PortfolioCalculator

class HoldingsViewModel(
    private val transactionDao: TransactionDao,
    private val stockDao: StockDao,
    private val portfolioDao: PortfolioDao,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    val allPortfolios: StateFlow<List<Portfolio>> = portfolioDao.getAllPortfolios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePortfolioId = dataStoreManager.activePortfolioId

    @OptIn(ExperimentalCoroutinesApi::class)
    val activePortfolio: StateFlow<Portfolio?> = _activePortfolioId.flatMapLatest { id ->
        if (id != null) {
            flow { emit(portfolioDao.getPortfolioById(id)) }
        } else {
            flow { emit(portfolioDao.getDefaultPortfolio()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val transactions = activePortfolio.flatMapLatest { portfolio ->
        if (portfolio != null) {
            transactionDao.getTransactionsByPortfolio(portfolio.id)
        } else {
            flowOf(emptyList())
        }
    }

    // Sorted by descending current value by default
    val holdings = PortfolioCalculator.getEventSourcedHoldings(
        transactions,
        stockDao.getAllStocks()
    ).map { list ->
        list.sortedByDescending { it.currentValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _isLoading.value = false
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
                return HoldingsViewModel(
                    application.container.transactionDao,
                    application.container.stockDao,
                    application.container.portfolioDao,
                    application.container.dataStoreManager
                ) as T
            }
        }
    }
}
