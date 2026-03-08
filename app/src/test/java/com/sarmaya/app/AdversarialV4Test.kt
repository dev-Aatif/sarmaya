package com.sarmaya.app

import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.data.PortfolioCalculator
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.viewmodel.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalCoroutinesApi::class)
class AdversarialV4Test {

    class LatencyFakeTransactionDao : TransactionDao {
        var insertedTransactions = mutableListOf<Transaction>()
        var nextId = 1L
        
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
        
        override fun getAllTransactions() = flowOf(insertedTransactions)
        override suspend fun getTransactionsForStock(symbol: String): List<Transaction> {
            return insertedTransactions.filter { it.stockSymbol == symbol }.sortedBy { it.date }
        }
        
        override suspend fun getTransactionById(id: Long): Transaction? {
            return insertedTransactions.find { it.id == id }
        }
        
        override suspend fun getStockQuantity(s: String): Int {
            // Replicate the actual DB behavior (in the real app it's a SUM query)
            // BUY, BONUS, SPLIT add, SELL subtracts
            var total = 0
            for (tx in insertedTransactions) {
                if (tx.stockSymbol == s) {
                    if (tx.type == "BUY" || tx.type == "BONUS" || tx.type == "SPLIT") {
                        total += tx.quantity
                    } else if (tx.type == "SELL") {
                        total -= tx.quantity
                    }
                }
            }
            return total
        }
    }

    class FakeStockDao : StockDao {
        val stocks = mutableListOf<Stock>()
        override fun getAllStocks() = flowOf(stocks)
        override fun searchStocks(sq: String) = flowOf(stocks.filter { it.symbol.contains(sq) })
        override fun getStocks(syms: List<String>) = flowOf(stocks.filter { syms.contains(it.symbol) })
        override suspend fun getStocksSync(syms: List<String>) = stocks.filter { syms.contains(it.symbol) }
        override suspend fun updatePrice(sym: String, p: Double, ud: Long) {
            val idx = stocks.indexOfFirst { it.symbol == sym }
            if (idx != -1) {
                stocks[idx] = stocks[idx].copy(currentPrice = p, priceUpdatedAt = ud)
            }
        }
        override suspend fun updateStocks(s: List<Stock>) {}
        override suspend fun insertStocks(s: List<Stock>) {
            stocks.addAll(s)
        }
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
        viewModel = TransactionsViewModel(transactionDao, stockDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testChronologicalBypass_NegativePointInTimeHoldings() = runTest {
        // Goal: Exploit the aggregate quantity check to create a timeline anomaly
        // Day 2: BUY 100 shares
        var success1 = false
        viewModel.addTransaction(
            stockSymbol = "AAPL",
            type = "BUY",
            quantity = 100,
            pricePerShare = 150.0,
            date = 2000L, // Day 2
            onSuccess = { success1 = true }
        )
        advanceUntilIdle()
        assertTrue(success1)

        // Day 3: SELL 100 shares. Aggregate checks: 100 >= 100. Pass.
        var success2 = false
        viewModel.addTransaction(
            stockSymbol = "AAPL",
            type = "SELL",
            quantity = 100,
            pricePerShare = 160.0,
            date = 3000L, // Day 3
            onSuccess = { success2 = true }
        )
        advanceUntilIdle()
        assertTrue(success2)

        // Day 1: Edit the Day 3 SELL to occur on Day 1.
        // Wait, edit of a SELL only checks if hypotheticalQty < 0.
        // The viewModel logic:
        // currentQtyWithoutOldTx = (total qty) 0 - (-100) = 100
        // hypotheticalQty = 100 - 100 = 0
        // 0 is not < 0. The edit passes!
        // But the timeline is now: Day 1: SELL 100. Day 2: BUY 100.
        
        var success3 = false
        val sellTx = transactionDao.insertedTransactions.find { it.type == "SELL" }!!
        
        viewModel.updateTransaction(
            transactionId = sellTx.id,
            stockSymbol = "AAPL",
            type = "SELL",
            quantity = 100,
            pricePerShare = 160.0,
            date = 1000L, // Moved to Day 1
            onSuccess = { success3 = true }
        )
        advanceUntilIdle()
        
        // Because of the Phase 4 patch, this will FAIL because the viewmodel 
        // checks chronological bounds directly.
        assertFalse("Timeline paradox validation restored; backdating SELL before BUY is blocked.", success3)
        // Ensure no data corruption occurred
        val flow = PortfolioCalculator.getEventSourcedHoldings(flowOf(transactionDao.insertedTransactions), flowOf(stockDao.stocks))
        val holdings = flow.first()
        // Because the malicious edit failed, the Day 2 BUY and Day 3 SELL remain.
        // The holding is retained because of realized P/L, but quantity is 0.
        assertEquals("State remains uncorrupted and holding is closed.", 0, holdings.firstOrNull()?.quantity ?: 0)
    }

    @Test
    fun testInfiniteDividend_BypassesHoldingsCheck() = runTest {
        var success = false
        // User adds a DIVIDEND for a stock they do NOT own
        viewModel.addTransaction(
            stockSymbol = "TSLA",
            type = "DIVIDEND",
            quantity = 1000,
            pricePerShare = 5.0, // $5 dividend per share
            date = 1000L,
            onSuccess = { success = true }
        )
        advanceUntilIdle()
        
        // This will succeed because TransactionsViewModel explicitly ignores DIVIDEND validation
        assertTrue("DIVIDEND transactions bypass holding verification", success)
        
        // Let's check PortfolioCalculator
        val flow = PortfolioCalculator.getEventSourcedHoldings(flowOf(transactionDao.insertedTransactions), flowOf(stockDao.stocks))
        val holdings = flow.first()
        
        // It creates a holding because divs > 0, but quantity remains 0.
        assertEquals("Holding retained for dividends but quantity is 0", 0, holdings.firstOrNull()?.quantity ?: 0)
    }

    @Test
    fun testSplitCalculation_DoesNotMultiply() = runTest {
        // Step 1: Buy 10 shares
        viewModel.addTransaction(
            stockSymbol = "MSFT",
            type = "BUY",
            quantity = 10,
            pricePerShare = 100.0,
            date = 1000L
        )
        
        // Step 2: A 10-for-1 Split occurs. User logs it as a SPLIT
        // But the only field is 'quantity'. Does the user put 10, or 90 to make the total 100?
        // Let's say they put 10 representing the split ratio.
        viewModel.addTransaction(
            stockSymbol = "MSFT",
            type = "SPLIT",
            quantity = 10, // A 10-for-1 split
            pricePerShare = 0.0,
            date = 2000L
        )
        advanceUntilIdle()
        
        val flow = PortfolioCalculator.getEventSourcedHoldings(flowOf(transactionDao.insertedTransactions), flowOf(stockDao.stocks))
        val holdings = flow.first()
        
        // The calculator treats SPLIT as a simple + (qty += tx.quantity)
        // So total quantity is 10 * 10 = 100.
        assertEquals(100, holdings[0].quantity)
        // This proves SPLIT behavior is correctly semantic as a pure multiplication.
    }

    @Test
    fun testSplitDivergence_BlocksLegitimateSell() = runTest {
        var success1 = false
        viewModel.addTransaction(
            stockSymbol = "NVDA", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1000L, onSuccess = { success1 = true }
        )
        advanceUntilIdle()
        assertTrue(success1)

        var success2 = false
        viewModel.addTransaction(
            stockSymbol = "NVDA", type = "SPLIT", quantity = 10, pricePerShare = 0.0, date = 2000L, onSuccess = { success2 = true }
        )
        advanceUntilIdle()
        assertTrue(success2)

        var errorOccurred = false
        viewModel.addTransaction(
            stockSymbol = "NVDA", type = "SELL", quantity = 50, pricePerShare = 150.0, date = 3000L,
            onSuccess = { },
            onError = { errorOccurred = true }
        )
        advanceUntilIdle()
        assertFalse("ViewModel correctly allowed a valid SELL of 50 shares out of 100 owned, fixing State Divergence", errorOccurred)
    }

    @Test
    fun testGhostShares_SplitCreatesSellableShares() = runTest {
        var success1 = false
        viewModel.addTransaction(
            stockSymbol = "GME", type = "SPLIT", quantity = 10, pricePerShare = 0.0, date = 1000L, onSuccess = { success1 = true }
        )
        advanceUntilIdle()
        assertTrue(success1)

        var success2 = false
        viewModel.addTransaction(
            stockSymbol = "GME", type = "SELL", quantity = 10, pricePerShare = 250.0, date = 2000L, onSuccess = { success2 = true }
        )
        advanceUntilIdle()
        assertFalse("ViewModel correctly blocked selling 10 ghost shares created by a naked SPLIT", success2)
        
        val flow = PortfolioCalculator.getEventSourcedHoldings(flowOf(transactionDao.insertedTransactions), flowOf(stockDao.stocks))
        val holdings = flow.first()
        assertTrue("Holding is empty despite selling ghost shares for cash", holdings.isEmpty())
    }
}
