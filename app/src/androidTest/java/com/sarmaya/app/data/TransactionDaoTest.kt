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
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var transactionDao: TransactionDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        transactionDao = db.transactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveTransaction() = runBlocking {
        val tx = Transaction(
            stockSymbol = "LUCK",
            type = "BUY",
            quantity = 100,
            pricePerShare = 500.0,
            date = System.currentTimeMillis()
        )
        transactionDao.insert(tx)
        
        val transactions = transactionDao.getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals("LUCK", transactions[0].stockSymbol)
        assertEquals(100, transactions[0].quantity)
    }

    @Test
    fun getStockQuantityCalculation() = runBlocking {
        val buy = Transaction(stockSymbol = "ENGRO", type = "BUY", quantity = 100, pricePerShare = 300.0, date = 1000L)
        val sell = Transaction(stockSymbol = "ENGRO", type = "SELL", quantity = 40, pricePerShare = 350.0, date = 2000L)
        
        transactionDao.insert(buy)
        transactionDao.insert(sell)
        
        val qty = transactionDao.getStockQuantity("ENGRO")
        assertEquals(60, qty)
    }

    @Test
    fun updateAndRetrieveById() = runBlocking {
        val tx = Transaction(id = 1, stockSymbol = "SYS", type = "BUY", quantity = 10, pricePerShare = 400.0, date = 1000L)
        transactionDao.insert(tx)
        
        // Retrieve the auto-generated ID or use a fixed one for testing if not auto-gen
        val insertedTx = transactionDao.getAllTransactions().first()[0]
        val updatedTx = insertedTx.copy(quantity = 20)
        transactionDao.update(updatedTx)
        
        val result = transactionDao.getTransactionById(insertedTx.id)
        assertEquals(20, result?.quantity)
    }
}
