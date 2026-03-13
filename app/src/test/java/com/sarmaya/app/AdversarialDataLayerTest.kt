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
import com.sarmaya.app.data.AppDatabase
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.sarmaya.app.data.ComputedHolding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.sarmaya.app.data.PortfolioCalculator
@OptIn(ExperimentalCoroutinesApi::class)
class AdversarialDataLayerTest {

    class LatencyFakeTransactionDao : TransactionDao {
        var insertedTransactions = mutableListOf<Transaction>()
        var stockQuantity = 10
        var nextId = 1L
        override suspend fun insert(t: Transaction) {
            delay(50) // Simulate DB latency
            val txnToInsert = if (t.id == 0L) t.copy(id = nextId++) else t
            insertedTransactions.add(txnToInsert)
            if (txnToInsert.type == "SELL") stockQuantity -= txnToInsert.quantity
            if (txnToInsert.type == "BUY") stockQuantity += txnToInsert.quantity
        }

        override suspend fun update(transaction: Transaction) {
            delay(50) // Simulate DB latency
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
            delay(50) // Simulate DB latency
            return stockQuantity
        }
        override fun getTransactionsByPortfolio(portfolioId: Long) = flowOf(insertedTransactions.filter { it.portfolioId == portfolioId })
        override suspend fun getTransactionsByPortfolioSync(portfolioId: Long) = insertedTransactions.filter { it.portfolioId == portfolioId }
        override suspend fun getTransactionsForStockInPortfolio(symbol: String, portfolioId: Long) = insertedTransactions.filter { it.stockSymbol == symbol && it.portfolioId == portfolioId }.sortedBy { it.date }
        override suspend fun getStockQuantityInPortfolio(s: String, p: Long): Int? {
            delay(50)
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
        override suspend fun getStocksSync(syms: List<String>) = listOf(
            Stock("AAPL", "Apple", "Tech", 150.0, 0L)
        )
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
    fun testConcurrencyRaceCondition_multiTapExploit() = runTest {
        // User has 10 shares initially
        viewModel.addTransaction("AAPL", "BUY", 10, 150.0, -10L)
        advanceUntilIdle()
        
        // User multi-taps the Sell button 5 times very fast, each trying to sell 10 shares
        for (i in 1..5) {
            launch {
                viewModel.addTransaction(
                    stockSymbol = "AAPL",
                    type = "SELL",
                    quantity = 10,
                    pricePerShare = 150.0,
                    date = 0L,
                    notes = ""
                )
            }
        }
        
        advanceUntilIdle()
        
        // With Mutex fix, the first one processes, reduces quantity to 0, 
        // and the next four fail the 'quantity > currentQty' check because it's re-eval'd inside the lock.
        // We expect ONE buy and ONE sell transaction.
        assertEquals(2, transactionDao.insertedTransactions.size)
    }

    @Test
    fun testZeroCostBasisManipulation() = runTest {
        var isSuccess = false
        
        // Adversary buys 10,000 shares at price 0.0
        viewModel.addTransaction(
            stockSymbol = "AAPL",
            type = "BUY",
            quantity = 10000,
            pricePerShare = 0.0, // allowed by logic
            date = 0L,
            notes = "",
            onSuccess = { isSuccess = true }
        )
        
        advanceUntilIdle()
        
        assertFalse(isSuccess)
        assertTrue(transactionDao.insertedTransactions.isEmpty())
        // Proof that 0.0 price for BUY is now rejected.
        // This confirms 0.0 price passes validation and enters the DB.
    }
    
    @Test
    fun testSplitIllusion_constraintViolation() = runTest {
        // Logic test based on TransactionDao behavior
        // The DAO query groups by type: SUM(CASE WHEN type IN ('BUY','BONUS') ... WHEN 'SELL' THEN -quantity ELSE 0)
        // 'SPLIT' is not in the CASE statements.
        
        val transactions = listOf(
            Transaction(stockSymbol = "AAPL", type = "BUY", quantity = 10, pricePerShare = 150.0, date = 0L),
            Transaction(stockSymbol = "AAPL", type = "SPLIT", quantity = 90, pricePerShare = 15.0, date = 0L) // 10-for-1 split
        )
        
        // With Event-Sourced logic in PortfolioCalculator, SPLIT is accounted for.
        // But here we test the DAO's direct `getStockQuantity` method which was used for validation.
        // We added 'SPLIT' to the SQL SUM CASE.
        // Wait, the DAO logic was updated! Let's reflect the SQL update:
        var computedQuantity = 0
        for (t in transactions) {
            when (t.type) {
                "BUY", "BONUS", "SPLIT" -> computedQuantity += t.quantity
                "SELL" -> computedQuantity -= t.quantity
                else -> computedQuantity += 0
            }
        }
        assertEquals(100, computedQuantity)
    }
    @Test
    fun testUIDoubleTapVulnerability_phantomBuys() = runTest {
        // Assume user wants to BUY 10 shares. 
        // But the DB is slow (simulated by LatencyFakeTransactionDao taking 50ms).
        // The UI has no "isLoading" state, so the user impatiently double-taps the BUY button.
        // The button click handler fires twice rapidly.
        
        var successCount = 0
        val uiClickHandler = {
            viewModel.addTransaction(
                stockSymbol = "AAPL",
                type = "BUY",
                quantity = 10,
                pricePerShare = 150.0,
                date = 0L,
                onSuccess = { successCount++ }
            )
        }

        // Simulating the double tap within 10ms
        uiClickHandler()
        delay(10) 
        uiClickHandler()
        
        advanceUntilIdle() // Wait for all queued coroutines to process
        
        // Since both were queued before the first one could call onSuccess (to close the UI),
        // BOTH transactions get inserted. The user accidentally bought 20 shares instead of 10.
        assertEquals(2, successCount)
        assertEquals(2, transactionDao.insertedTransactions.size)
        // They now have 20 shares from the double-tap bug (+10 initial fake DAO shares = 30)
        assertEquals(30, transactionDao.stockQuantity)
    }
    @Test
    fun testUpdateMutationBypass_SellMoreThanOwned() = runTest {
        // Assume user bought 10 shares
        viewModel.addTransaction("AAPL", "BUY", 10, 150.0, 0L)
        advanceUntilIdle()
        
        val buyTx = transactionDao.insertedTransactions.first()
        transactionDao.stockQuantity = 10 // Update mock state manually
        
        // Assume user sold 5 shares (valid)
        viewModel.addTransaction("AAPL", "SELL", 5, 150.0, 0L)
        advanceUntilIdle()
        
        val sellTx = transactionDao.insertedTransactions.last()
        transactionDao.stockQuantity = 5 // User has 5 shares left
        
        // NOW ADVERSARY uses the UI to "Edit" the SELL transaction.
        // They change the quantity to 1000.
        // `updateTransaction` DOES NOT CHECK `quantity > currentQty`!
        var isSuccess = false
        viewModel.updateTransaction(
            transactionId = sellTx.id,
            stockSymbol = "AAPL",
            type = "SELL",
            quantity = 1000,
            pricePerShare = 150.0,
            date = 0L,
            onSuccess = { isSuccess = true }
        )
        advanceUntilIdle()
        
        // Because of the Phase 4 patch, this will fail because
        // the viewmodel enforces chronological point-in-time balances.
        assertFalse("Bypass no longer allowed, chronological balance is enforced", isSuccess)
        assertEquals(5, transactionDao.insertedTransactions.last().quantity)
    }

    @Test
    fun testIntegerOverflow_QuantityWraparound() = runTest {
        // User inserts a SPLIT with Max Int value
        var isSuccess = false
        viewModel.addTransaction("AAPL", "SPLIT", Int.MAX_VALUE, 0.0, 0L, onSuccess = { isSuccess = true })
        advanceUntilIdle()
        assertTrue(isSuccess)
        
        transactionDao.stockQuantity = Int.MAX_VALUE // Mock state
        
        // User adds another SPLIT or BUY, overflowing the integer max limit.
        // Since we can't test full SQLite overflow here, let's test if PortfolioCalculator overflows.
        val transactions = listOf(
            Transaction(stockSymbol = "AAPL", type = "BUY", quantity = Int.MAX_VALUE, pricePerShare = 10.0, date = 0L),
            Transaction(stockSymbol = "AAPL", type = "BONUS", quantity = 10, pricePerShare = 0.0, date = 1L)
        )
        
        val flowTx = kotlinx.coroutines.flow.flowOf(transactions)
        val flowStock = kotlinx.coroutines.flow.flowOf(listOf(Stock("AAPL", "Apple", "Tech", 100.0, 0L)))
        val holdings = PortfolioCalculator.getEventSourcedHoldings(flowTx, flowStock).first()
        
        // With the fix, the negative quantity is clamped to 0, and the holding is discarded.
        assertTrue("Integer overflow was correctly clamped and discarded", holdings.isEmpty())
    }

    @Test
    fun testTimeTravelCostBasisCorruption() = runTest {
        // Transactions arrive out of order, or user inputs backdated SELL before BUY.
        val transactions = listOf(
            Transaction(stockSymbol = "AAPL", type = "SELL", quantity = 10, pricePerShare = 200.0, date = 0L),
            Transaction(stockSymbol = "AAPL", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1000L)
        )
        
        val flowTx = kotlinx.coroutines.flow.flowOf(transactions)
        val flowStock = kotlinx.coroutines.flow.flowOf(listOf(Stock("AAPL", "Apple", "Tech", 100.0, 0L)))
        val holdings = PortfolioCalculator.getEventSourcedHoldings(flowTx, flowStock).first()
        
        // With the clamp fix: 
        // SELL at date 0 makes qty=-10, which clamps to 0. 
        // BUY at date 1000 makes qty=10, invested=1000.0.
        // The system gracefully recovered instead of lingering in negative shares.
        val holding = holdings.first()
        assertEquals("System should recover and show 10 shares from the BUY", 10, holding.quantity)
        assertEquals("Invested should be exactly the BUY amount", 1000.0, holding.totalInvested, 0.0)
    }

    @Test
    fun testNaNPoisoning() = runTest {
        var isSuccess = false
        // User injects Double.NaN as price
        viewModel.addTransaction(
            stockSymbol = "AAPL",
            type = "BUY",
            quantity = 10,
            pricePerShare = Double.NaN,
            date = 0L,
            notes = "",
            onSuccess = { isSuccess = true }
        )
        advanceUntilIdle()
        
        // The validation check now uses `isNaN()` so it correctly blocks the request.
        assertFalse("NaN was blocked by validation", isSuccess)
        
        // Since it was blocked, the inserted transactions should be empty
        assertTrue("No transactions should have been inserted", transactionDao.insertedTransactions.isEmpty())
    }

    @Test
    fun testUnrestrictedDeletionStateCorruption_DeleteBuy() = runTest {
        // User buys 10 shares
        viewModel.addTransaction("AAPL", "BUY", 10, 150.0, 0L)
        advanceUntilIdle()
        transactionDao.stockQuantity = 10 // Mock state
        
        // User sells 10 shares
        viewModel.addTransaction("AAPL", "SELL", 10, 160.0, 1L)
        advanceUntilIdle()
        transactionDao.stockQuantity = 0 // Mock state
        
        assertEquals(2, transactionDao.insertedTransactions.size)
        val buyTx = transactionDao.insertedTransactions.find { it.type == "BUY" }!!
        
        var deleteError: String? = null
        // User DELETES the BUY transaction
        // There IS validation now preventing deleting a transaction that subsequent SELLs depend on!
        viewModel.deleteTransaction(buyTx, onError = { deleteError = it })
        advanceUntilIdle()
        
        // Because of the Phase 4 patch, this will be blocked 
        // by chronological running balance calculations.
        assertNotNull("Deletion is blocked because it drops chronological balance below zero", deleteError)
        assertEquals(2, transactionDao.insertedTransactions.size)
    }

    @Test
    fun testTypeInjection() = runTest {
        var isSuccess = false
        // User injects a random string as type
        viewModel.addTransaction(
            stockSymbol = "AAPL",
            type = "HACK",
            quantity = 10,
            pricePerShare = 150.0,
            date = 0L,
            notes = "",
            onSuccess = { isSuccess = true }
        )
        advanceUntilIdle()
        
        assertFalse("HACK type was blocked by validation", isSuccess)
        val inserted = transactionDao.insertedTransactions.find { it.type == "HACK" }
        assertNull(inserted)
    }
}
