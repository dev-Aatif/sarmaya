package com.sarmaya.app

import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.viewmodel.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking
import com.sarmaya.app.data.ComputedHolding
import kotlinx.coroutines.flow.Flow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import com.sarmaya.app.data.PortfolioDao
import com.sarmaya.app.data.DataStoreManager

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    open class FakeStockDao : StockDao {
        var insertedStocks = mutableListOf<Stock>()
        override fun getAllStocks() = flowOf(emptyList<Stock>())
        override fun searchStocks(sq: String) = flowOf(emptyList<Stock>())
        override fun getStocks(syms: List<String>) = flowOf(emptyList<Stock>())
        override suspend fun getStocksSync(syms: List<String>) = emptyList<Stock>()
        override suspend fun updatePrice(sym: String, p: Double, ud: Long) {}
        override suspend fun updateStocks(s: List<Stock>) {}
        override suspend fun insertStocks(s: List<Stock>) {
            insertedStocks.addAll(s)
        }
        override suspend fun getStocksBySectorSync(sector: String): List<Stock> = emptyList()
    }

    class FakeTransactionDao : TransactionDao {
        var insertedTransactions = mutableListOf<Transaction>()
        override suspend fun insert(t: Transaction) {
            insertedTransactions.add(t)
        }
        override suspend fun update(transaction: Transaction) {}
        override suspend fun delete(t: Transaction) {}
        override fun getAllTransactions() = flowOf(emptyList<Transaction>())
        override fun getTransactionsByPortfolio(portfolioId: Long) = flowOf(emptyList<Transaction>())
        override suspend fun getTransactionsByPortfolioSync(portfolioId: Long) = emptyList<Transaction>()
        override suspend fun getTransactionsForStock(symbol: String): List<Transaction> {
            return insertedTransactions.filter { it.stockSymbol == symbol }.sortedBy { it.date }
        }
        override suspend fun getTransactionsForStockInPortfolio(symbol: String, portfolioId: Long) = emptyList<Transaction>()
        override suspend fun getStockQuantity(s: String): Int? = 0
        override suspend fun getStockQuantityInPortfolio(s: String, p: Long): Int? = 0
        override suspend fun getTransactionById(id: Long): Transaction? = insertedTransactions.find { it.id == id }
    }

    private lateinit var stockDao: FakeStockDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var viewModel: TransactionsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stockDao = FakeStockDao()
        transactionDao = FakeTransactionDao()
        
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
    fun `addTransaction creates Stock if it does not exist`() = runTest {
        var isSuccess = false
        
        viewModel.addTransaction(
            stockSymbol = "TSLA",
            type = "BUY",
            quantity = 10,
            pricePerShare = 200.0,
            date = 0L,
            notes = "",
            onSuccess = { isSuccess = true },
            onError = { fail("Should not fail") }
        )
        
        advanceUntilIdle()
        
        assertTrue(isSuccess)
        assertTrue(stockDao.insertedStocks.any { it.symbol == "TSLA" })
        assertTrue(transactionDao.insertedTransactions.any { it.stockSymbol == "TSLA" })
    }
    
    @Test
    fun `addTransaction fails on negative price for BUY`() = runTest {
        var errorMsg: String? = null
        
        viewModel.addTransaction(
            stockSymbol = "AAPL",
            type = "BUY",
            quantity = 10,
            pricePerShare = -50.0,
            date = 0L,
            notes = "",
            onSuccess = { fail("Should not succeed") },
            onError = { errorMsg = it }
        )
        
        advanceUntilIdle()
        assertEquals("Price must be at least 0.01 for BUY/SELL", errorMsg)
        assertTrue(stockDao.insertedStocks.isEmpty())
        assertTrue(transactionDao.insertedTransactions.isEmpty())
    }

    @Test
    fun `updateTransaction for BUY updates stock currentPrice`() = runTest {
        // First add a BUY
        var addSuccess = false
        viewModel.addTransaction(
            stockSymbol = "TSLA",
            type = "BUY",
            quantity = 10,
            pricePerShare = 250.0,
            date = 1L,
            notes = "",
            onSuccess = { addSuccess = true },
            onError = { fail("Add should not fail: $it") }
        )
        advanceUntilIdle()
        assertTrue(addSuccess)

        // The inserted transaction has an id from the fake DAO (defaults to 0)
        val insertedTx = transactionDao.insertedTransactions.first()

        // Now edit the BUY to a new price
        var updateSuccess = false
        var priceUpdated = false
        // Override stockDao to track price updates
        val trackingStockDao = object : FakeStockDao() {
            override suspend fun getStocksSync(syms: List<String>) = stockDao.insertedStocks.filter { it.symbol in syms }
            override suspend fun updatePrice(sym: String, p: Double, ud: Long) {
                if (sym == "TSLA" && p == 285.0) priceUpdated = true
            }
        }
        val defaultPortfolio = com.sarmaya.app.data.Portfolio(id = 1L, name = "Default", isDefault = true)
        val portfolioDao = mock(PortfolioDao::class.java)
        `when`(portfolioDao.getAllPortfolios()).thenReturn(flowOf(listOf(defaultPortfolio)))
        runBlocking {
            `when`(portfolioDao.getDefaultPortfolio()).thenReturn(defaultPortfolio)
        }

        val dataStoreManager = mock(DataStoreManager::class.java)
        `when`(dataStoreManager.activePortfolioId).thenReturn(flowOf(1L))

        val viewModel2 = TransactionsViewModel(transactionDao, trackingStockDao, portfolioDao, dataStoreManager)

        viewModel2.updateTransaction(
            transactionId = insertedTx.id,
            stockSymbol = "TSLA",
            type = "BUY",
            quantity = 10,
            pricePerShare = 285.0,
            date = 1L,
            notes = "",
            onSuccess = { updateSuccess = true },
            onError = { fail("Update should not fail: $it") }
        )
        advanceUntilIdle()
        assertTrue("Update should succeed", updateSuccess)
        assertTrue("Stock price should be updated to 285 on BUY edit", priceUpdated)
    }
}
