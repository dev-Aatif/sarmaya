package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.data.DataStoreManager
import com.sarmaya.app.data.Portfolio
import com.sarmaya.app.data.PortfolioDao
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.data.TransactionDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import androidx.room.withTransaction
import kotlinx.coroutines.sync.withLock
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.AppDatabase

class TransactionsViewModel(
    private val transactionDao: TransactionDao,
    private val stockDao: StockDao,
    private val portfolioDao: PortfolioDao,
    private val dataStoreManager: DataStoreManager,
    private val dbTransactionRunner: suspend (suspend () -> Unit) -> Unit = { it() }
) : ViewModel() {

    private val mutex = Mutex()

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
    val transactions = activePortfolio.flatMapLatest { portfolio ->
        if (portfolio != null) {
            transactionDao.getTransactionsByPortfolio(portfolio.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Stock picker context
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            stockDao.getAllStocks()
        } else {
            stockDao.searchStocks(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTransaction(
        stockSymbol: String, 
        type: String, 
        quantity: Int, 
        pricePerShare: Double, 
        date: Long, 
        notes: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            mutex.withLock {
                val currentPortfolioId = activePortfolio.value?.id ?: 1L
                val domainModel = com.sarmaya.app.domain.TransactionDomainModel(
                    portfolioId = currentPortfolioId,
                    stockSymbol = stockSymbol,
                    type = type,
                    quantity = quantity,
                    pricePerShare = pricePerShare,
                    date = date,
                    notes = notes
                )
                val validationError = domainModel.validate()
                if (validationError != null) {
                    onError(validationError)
                    return@launch
                }
                val t = domainModel.toEntity()

                if (type != "DIVIDEND") {
                    val allTxs = transactionDao.getTransactionsForStockInPortfolio(stockSymbol, currentPortfolioId).toMutableList()
                    allTxs.add(t)
                    allTxs.sortBy { it.date }
                    var runningBalance = 0
                    for (txn in allTxs) {
                        when (txn.type) {
                            "BUY", "BONUS" -> runningBalance += txn.quantity
                            "SELL" -> runningBalance -= txn.quantity
                        }
                        if (runningBalance < 0) {
                            onError("Transaction drops chronological holding below zero at point-in-time.")
                            return@launch
                        }
                    }
                }

                dbTransactionRunner {
                    val existingStocks = stockDao.getStocksSync(listOf(stockSymbol))
                    if (existingStocks.isEmpty()) {
                        stockDao.insertStocks(listOf(Stock(
                            symbol = stockSymbol,
                            name = stockSymbol,
                            sector = "Other",
                            currentPrice = pricePerShare,
                            priceUpdatedAt = System.currentTimeMillis()
                        )))
                    } else {
                        val stock = existingStocks.first()
                        if (stock.currentPrice <= 0.0 || type == "BUY") {
                            stockDao.updatePrice(stockSymbol, pricePerShare)
                        }
                    }
                    transactionDao.insert(t)
                }
                onSuccess()
            }
        }
    }

    fun updateTransaction(
        transactionId: Long,
        stockSymbol: String, 
        type: String, 
        quantity: Int, 
        pricePerShare: Double, 
        date: Long, 
        notes: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            mutex.withLock {
                val oldTx = transactionDao.getTransactionById(transactionId)
                if (oldTx == null) {
                    onError("Transaction not found")
                    return@launch
                }

                val domainModel = com.sarmaya.app.domain.TransactionDomainModel(
                    id = transactionId,
                    stockSymbol = stockSymbol,
                    type = type,
                    quantity = quantity,
                    pricePerShare = pricePerShare,
                    date = date,
                    notes = notes
                )
                val validationError = domainModel.validate()
                if (validationError != null) {
                    onError(validationError)
                    return@launch
                }
                val t = domainModel.toEntity()

                if (type != "DIVIDEND" || oldTx.type != "DIVIDEND") {
                    val allTxs = transactionDao.getTransactionsForStockInPortfolio(stockSymbol, oldTx.portfolioId).toMutableList()
                    val index = allTxs.indexOfFirst { it.id == transactionId }
                    if (index != -1) {
                        allTxs[index] = t
                    } else {
                        allTxs.add(t)
                    }
                    // We sort and calculate running balance to ensure NO chronological 
                    // point in time goes negative, preventing time travel paradoxes.
                    allTxs.sortBy { it.date }
                    var runningBalance = 0
                    for (txn in allTxs) {
                        when (txn.type) {
                            "BUY", "BONUS" -> runningBalance += txn.quantity
                            "SELL" -> runningBalance -= txn.quantity
                        }
                        if (runningBalance < 0) {
                            onError("Transaction edit drops chronological holding below zero at point-in-time.")
                            return@launch
                        }
                    }
                }
                
                dbTransactionRunner {
                    transactionDao.update(t)
                    // When editing a BUY, also update the stock's currentPrice
                    // to match the new buy price (same behavior as addTransaction)
                    if (type == "BUY") {
                        stockDao.updatePrice(stockSymbol, pricePerShare)
                    }
                }
                onSuccess()
            }
        }
    }

    fun deleteTransaction(
        transaction: Transaction,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            mutex.withLock {
                if (transaction.type != "DIVIDEND") {
                    val allTxs = transactionDao.getTransactionsForStockInPortfolio(transaction.stockSymbol, transaction.portfolioId).toMutableList()
                    allTxs.removeIf { it.id == transaction.id }
                    // We sort and recalculate running balance to prevent deleting 
                    // a BUY that subsequent SELLs depend on, avoiding state corruption.
                    allTxs.sortBy { it.date }
                    var runningBalance = 0
                    for (txn in allTxs) {
                        when (txn.type) {
                            "BUY", "BONUS" -> runningBalance += txn.quantity
                            "SELL" -> runningBalance -= txn.quantity
                        }
                        if (runningBalance < 0) {
                            onError("Deleting this transaction drops chronological holding below zero.")
                            return@launch
                        }
                    }
                }
                
                dbTransactionRunner {
                    transactionDao.delete(transaction)
                }
                onSuccess()
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
                return TransactionsViewModel(
                    application.container.transactionDao,
                    application.container.stockDao,
                    application.container.portfolioDao,
                    application.container.dataStoreManager,
                    { block: suspend () -> Unit -> application.container.database.withTransaction { block() } }
                ) as T
            }
        }
    }
}
