package com.sarmaya.app

import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.viewmodel.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import com.sarmaya.app.data.PortfolioDao
import com.sarmaya.app.data.DataStoreManager
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class AdversarialV3Test {

    class LatencyFakeTransactionDao : TransactionDao {
        var insertedTransactions = mutableListOf<Transaction>()
        var nextId = 1L
        var mockQuantity = 0
        override suspend fun insert(t: Transaction) {
            val txnToInsert = if (t.id == 0L) t.copy(id = nextId++) else t
            insertedTransactions.add(txnToInsert)
        }

        override suspend fun update(transaction: Transaction) {
            val index = insertedTransactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                insertedTransactions[index] = transaction
            }
        }

        override suspend fun delete(t: Transaction) {
            insertedTransactions.removeIf { it.id == t.id }
        }
        override fun getAllTransactions() = flowOf(emptyList<Transaction>())
        override suspend fun getTransactionsForStock(symbol: String): List<Transaction> {
            return insertedTransactions.filter { it.stockSymbol == symbol }.sortedBy { it.date }
        }
        override suspend fun getTransactionById(id: Long): Transaction? {
            return insertedTransactions.find { it.id == id }
        }
        override suspend fun getStockQuantity(s: String): Int? {
            return mockQuantity
        }
        override fun getTransactionsByPortfolio(portfolioId: Long) = flowOf(insertedTransactions.filter { it.portfolioId == portfolioId })
        override suspend fun getTransactionsByPortfolioSync(portfolioId: Long) = insertedTransactions.filter { it.portfolioId == portfolioId }
        override suspend fun getTransactionsForStockInPortfolio(symbol: String, portfolioId: Long) = insertedTransactions.filter { it.stockSymbol == symbol && it.portfolioId == portfolioId }.sortedBy { it.date }
        override suspend fun getStockQuantityInPortfolio(s: String, p: Long): Int? {
            return insertedTransactions.filter { it.stockSymbol == s && it.portfolioId == p }.sumOf { 
                when (it.type) {
                    "BUY", "BONUS", "SPLIT" -> it.quantity
                    "SELL" -> -it.quantity
                    else -> 0
                }
            }
        }
    }

    class FakeStockDao : StockDao {
        override fun getAllStocks() = flowOf(emptyList<Stock>())
        override fun searchStocks(sq: String) = flowOf(emptyList<Stock>())
        override fun getStocks(syms: List<String>) = flowOf(emptyList<Stock>())
        override suspend fun getStocksSync(syms: List<String>) = emptyList<Stock>()
        override suspend fun updatePrice(sym: String, p: Double, ud: Long) {}
        override suspend fun updateStocks(s: List<Stock>) {}
        override suspend fun insertStocks(s: List<Stock>) {}
        override suspend fun getStocksBySectorSync(sector: String): List<Stock> = emptyList()
    }

    private lateinit var stockDao: FakeStockDao
    private lateinit var transactionDao: LatencyFakeTransactionDao
    private lateinit var viewModel: TransactionsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stockDao = FakeStockDao()
        transactionDao = LatencyFakeTransactionDao()
        
        val defaultPortfolio = com.sarmaya.app.data.Portfolio(id = 1L, name = "Default", isDefault = true)
        val portfolioDao = mock(PortfolioDao::class.java)
        `when`(portfolioDao.getAllPortfolios()).thenReturn(flowOf(listOf(defaultPortfolio)))
        runBlocking {
            `when`(portfolioDao.getDefaultPortfolio()).thenReturn(defaultPortfolio)
        }

        val dataStoreManager = mock(DataStoreManager::class.java)
        `when`(dataStoreManager.activePortfolioId).thenReturn(flowOf(1L))

        viewModel = TransactionsViewModel(transactionDao, stockDao, portfolioDao, dataStoreManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEditBuyTransactionBypass_NegativeBalanceCorruption() = runTest {
        // Step 1: User buys 100 shares of AAPL
        val initialBuy = Transaction(id = 1L, stockSymbol = "AAPL", type = "BUY", quantity = 100, pricePerShare = 150.0, date = 0L, portfolioId = 1L)
        transactionDao.insertedTransactions.add(initialBuy)
        
        // Step 2: User sells 90 shares of AAPL
        val subsequentSell = Transaction(id = 2L, stockSymbol = "AAPL", type = "SELL", quantity = 90, pricePerShare = 160.0, date = 1L, portfolioId = 1L)
        transactionDao.insertedTransactions.add(subsequentSell)
        
        // Set mock quantity to 10 (100 - 90)
        transactionDao.mockQuantity = 10

        // Step 3: User edits the BUY transaction and changes quantity to 1
        var success = false
        var errorMsg = ""
        viewModel.updateTransaction(
            transactionId = 1L,
            stockSymbol = "AAPL",
            type = "BUY",
            quantity = 1,
            pricePerShare = 150.0,
            date = 0L,
            onSuccess = { success = true },
            onError = { errorMsg = it }
        )
        advanceUntilIdle()

        // Because of the Phase 4 patch, the viewmodel enforces
        // chronological point-in-time constraints.
        assertFalse("Edit was blocked, preventing negative balance", success)
    }

    @Test
    fun testExtremeSmallDoublePrecision() = runTest {
        var success = false
        // User inputs a ridiculously small but positive price: 1e-300
        viewModel.addTransaction(
            stockSymbol = "AAPL",
            type = "BUY",
            quantity = 1,
            pricePerShare = 1e-300,
            date = 0L,
            onSuccess = { success = true }
        )
        advanceUntilIdle()
        
        // The validation check now uses `price < 0.01` so it correctly blocks microscopic doubles.
        assertFalse("Microscopic doubles should be blocked", success)
    }
}
