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
            portfolioDao.getPortfolioByIdFlow(id)
        } else {
            portfolioDao.getDefaultPortfolioFlow()
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
        commissionAmount: Double = 0.0,
        splitRatio: Double? = null,
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
                    notes = notes,
                    commissionAmount = commissionAmount,
                    splitRatio = splitRatio
                )
                val validationError = domainModel.validate()
                if (validationError != null) {
                    onError(validationError)
                    return@launch
                }
                val t = domainModel.toEntity()

                var success = false
                dbTransactionRunner {
                    if (type !in listOf("DIVIDEND", "DEPOSIT", "WITHDRAWAL")) {
                        val allTxs = transactionDao.getTransactionsForStockInPortfolio(stockSymbol, currentPortfolioId).toMutableList()
                        allTxs.add(t)
                        val error = validateChronologicalBalance(allTxs)
                        if (error != null) {
                            onError("Transaction drops chronological holding below zero at point-in-time.")
                            return@dbTransactionRunner
                        }
                    }

                    if (type !in listOf("DEPOSIT", "WITHDRAWAL")) {
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
                            if (stock.currentPrice <= 0.0) {
                                stockDao.updatePrice(stockSymbol, pricePerShare)
                            }
                        }
                    }
                    transactionDao.insert(t)
                    success = true
                }
                if (success) {
                    onSuccess()
                }
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
        commissionAmount: Double = 0.0,
        splitRatio: Double? = null,
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
                    notes = notes,
                    commissionAmount = commissionAmount,
                    splitRatio = splitRatio
                )
                val validationError = domainModel.validate()
                if (validationError != null) {
                    onError(validationError)
                    return@launch
                }
                val t = domainModel.toEntity()

                var success = false
                dbTransactionRunner {
                    if (type !in listOf("DIVIDEND", "DEPOSIT", "WITHDRAWAL") || oldTx.type !in listOf("DIVIDEND", "DEPOSIT", "WITHDRAWAL")) {
                        val allTxs = transactionDao.getTransactionsForStockInPortfolio(stockSymbol, oldTx.portfolioId).toMutableList()
                        val index = allTxs.indexOfFirst { it.id == transactionId }
                        if (index != -1) {
                            allTxs[index] = t
                        } else {
                            allTxs.add(t)
                        }
                        // We sort and calculate running balance to ensure NO chronological 
                        // point in time goes negative, preventing time travel paradoxes.
                        val error = validateChronologicalBalance(allTxs)
                        if (error != null) {
                            onError("Transaction edit drops chronological holding below zero at point-in-time.")
                            return@dbTransactionRunner
                        }
                    }
                
                    transactionDao.update(t)
                    // We DO NOT blindly update currentPrice on edit of a BUY because 
                    // this could be a historical transaction edit.
                    success = true
                }
                if (success) {
                    onSuccess()
                }
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
                var success = false
                dbTransactionRunner {
                    if (transaction.type !in listOf("DIVIDEND", "DEPOSIT", "WITHDRAWAL")) {
                        val allTxs = transactionDao.getTransactionsForStockInPortfolio(transaction.stockSymbol, transaction.portfolioId).toMutableList()
                        allTxs.removeIf { it.id == transaction.id }
                        // We sort and recalculate running balance to prevent deleting 
                        // a BUY that subsequent SELLs depend on, avoiding state corruption.
                        val error = validateChronologicalBalance(allTxs)
                        if (error != null) {
                            onError("Deleting this transaction drops chronological holding below zero.")
                            return@dbTransactionRunner
                        }
                    }
                
                    transactionDao.delete(transaction)
                    success = true
                }
                if (success) {
                    onSuccess()
                }
            }
        }
    }

    private fun validateChronologicalBalance(transactions: List<Transaction>): String? {
        val sorted = transactions.sortedBy { it.date }
        var runningBalance = 0
        for (txn in sorted) {
            when (txn.type) {
                "BUY", "BONUS" -> runningBalance += txn.quantity
                "SELL" -> runningBalance -= txn.quantity
                "SPLIT" -> {
                    val factor = txn.splitRatio ?: 1.0
                    runningBalance = Math.round(runningBalance * factor).toInt()
                }
            }
            if (runningBalance < 0) {
                return "Balance drops below zero."
            }
        }
        return null
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
