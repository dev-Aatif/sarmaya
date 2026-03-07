package com.sarmaya.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var stockDao: StockDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        transactionDao = db.transactionDao()
        stockDao = db.stockDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadTransaction() = runBlocking {
        val stock = Stock("AAPL", "Apple Inc.", "Tech", 150.0, System.currentTimeMillis())
        stockDao.insertStocks(listOf(stock))

        val transaction = Transaction(
            stockSymbol = "AAPL",
            type = "BUY",
            quantity = 10,
            pricePerShare = 150.0,
            date = System.currentTimeMillis(),
            notes = "Initial Buy"
        )
        transactionDao.insert(transaction)

        val transactions = transactionDao.getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals("AAPL", transactions[0].stockSymbol)
        assertEquals(10, transactions[0].quantity)
    }

    @Test
    fun testComputedHoldingsLogic() = runBlocking {
        val stock = Stock("AAPL", "Apple Inc.", "Tech", 150.0, System.currentTimeMillis())
        stockDao.insertStocks(listOf(stock))

        val buy = Transaction(stockSymbol = "AAPL", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 0, notes = "")
        val sell = Transaction(stockSymbol = "AAPL", type = "SELL", quantity = 5, pricePerShare = 150.0, date = 1, notes = "")
        
        transactionDao.insert(buy)
        transactionDao.insert(sell)

        val holdings = transactionDao.getComputedHoldings().first()
        assertEquals(1, holdings.size)
        
        val aaplHolding = holdings[0]
        assertEquals(5, aaplHolding.quantity) // 10 - 5
    }
}
