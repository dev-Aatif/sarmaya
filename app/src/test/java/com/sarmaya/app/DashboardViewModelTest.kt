package com.sarmaya.app

import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    open class FakeStockDao : StockDao {
        override fun getAllStocks() = flowOf(emptyList<Stock>())
        override fun searchStocks(sq: String) = flowOf(emptyList<Stock>())
        override fun getStocks(syms: List<String>) = flowOf(emptyList<Stock>())
        override suspend fun getStocksSync(syms: List<String>) = emptyList<Stock>()
        override suspend fun updatePrice(sym: String, p: Double, ud: Long) {}
        override suspend fun updateStocks(s: List<Stock>) {}
        override suspend fun insertStocks(s: List<Stock>) {}
        override suspend fun getStocksBySectorSync(sector: String): List<Stock> = emptyList()
    }

    open class FakeTransactionDao(private val holdings: List<ComputedHolding>) : TransactionDao {
        override suspend fun insert(t: com.sarmaya.app.data.Transaction) {}
        override suspend fun update(transaction: com.sarmaya.app.data.Transaction) {}
        override suspend fun delete(t: com.sarmaya.app.data.Transaction) {}
        override fun getAllTransactions() = flowOf(emptyList<com.sarmaya.app.data.Transaction>())
        override fun getTransactionsByPortfolio(portfolioId: Long) = flowOf(emptyList<com.sarmaya.app.data.Transaction>())
        override suspend fun getTransactionsByPortfolioSync(portfolioId: Long) = emptyList<com.sarmaya.app.data.Transaction>()
        override suspend fun getTransactionsForStock(symbol: String) = emptyList<com.sarmaya.app.data.Transaction>()
        override suspend fun getTransactionsForStockInPortfolio(symbol: String, portfolioId: Long) = emptyList<com.sarmaya.app.data.Transaction>()
        override suspend fun getStockQuantity(s: String) = 0
        override suspend fun getStockQuantityInPortfolio(s: String, p: Long) = 0
        override suspend fun getTransactionById(id: Long): com.sarmaya.app.data.Transaction? = null
    }

    private lateinit var stockDao: StockDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val sList = listOf(
            Stock("AAPL", "Apple", "Tech", 150.0, 0L),
            Stock("MSFT", "Microsoft", "Tech", 200.0, 0L),
            Stock("JNJ", "Johnson", "Health", 100.0, 0L)
        )
        stockDao = object: FakeStockDao() {
            override fun getAllStocks() = flowOf(sList)
        }
        
        val tList = listOf(
            com.sarmaya.app.data.Transaction(stockSymbol = "AAPL", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
            com.sarmaya.app.data.Transaction(stockSymbol = "AAPL", type = "DIVIDEND", quantity = 10, pricePerShare = 5.0, date = 2L),
            com.sarmaya.app.data.Transaction(stockSymbol = "MSFT", type = "BUY", quantity = 5, pricePerShare = 160.0, date = 1L),
            com.sarmaya.app.data.Transaction(stockSymbol = "JNJ", type = "BUY", quantity = 20, pricePerShare = 90.0, date = 1L),
            com.sarmaya.app.data.Transaction(stockSymbol = "JNJ", type = "DIVIDEND", quantity = 20, pricePerShare = 5.0, date = 2L)
        )
        
        transactionDao = object: FakeTransactionDao(emptyList()) {
            override fun getAllTransactions() = flowOf(tList)
            override fun getTransactionsByPortfolio(portfolioId: Long) = flowOf(tList)
            override suspend fun getTransactionsForStock(symbol: String) = tList.filter { it.stockSymbol == symbol }.sortedBy { it.date }
        }
        
        val dataStoreManager = mock(DataStoreManager::class.java)
        `when`(dataStoreManager.username).thenReturn(flowOf(""))
        `when`(dataStoreManager.activePortfolioId).thenReturn(flowOf(null))
        
        val portfolioDao = mock(com.sarmaya.app.data.PortfolioDao::class.java)
        `when`(portfolioDao.getAllPortfolios()).thenReturn(flowOf(emptyList()))
        
        viewModel = DashboardViewModel(transactionDao, stockDao, portfolioDao, dataStoreManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `totalPortfolioValue aggregates correctly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.totalPortfolioValue.collect { }
        }
        advanceUntilIdle()
        assertEquals(4500.0, viewModel.totalPortfolioValue.value, 0.001)
    }

    @Test
    fun `totalInvested aggregates correctly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.totalInvested.collect { }
        }
        advanceUntilIdle()
        assertEquals(3600.0, viewModel.totalInvested.value, 0.001)
    }

    @Test
    fun `sectorAllocation groups correctly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sectorAllocation.collect { }
        }
        advanceUntilIdle()
        val allocation = viewModel.sectorAllocation.value
        assertEquals(2, allocation.size)
        assertEquals("Tech", allocation[0].first)
        assertEquals(2500.0, allocation[0].second, 0.001)
        assertEquals("Health", allocation[1].first)
        assertEquals(2000.0, allocation[1].second, 0.001)
    }
}
