package com.sarmaya.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class StockDaoTest {
    private lateinit var stockDao: StockDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        stockDao = db.stockDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeStockAndReadInList() = runBlocking {
        val stock = Stock(symbol = "LUCK", name = "Lucky Cement", sector = "Cement")
        stockDao.insertStocks(listOf(stock))
        val allStocks = stockDao.getAllStocks().first()
        assertEquals(allStocks[0].symbol, "LUCK")
    }

    @Test
    @Throws(Exception::class)
    fun searchStocksBySymbol() = runBlocking {
        val stock1 = Stock(symbol = "LUCK", name = "Lucky Cement", sector = "Cement")
        val stock2 = Stock(symbol = "EPCL", name = "Engro Polymer", sector = "Chemical")
        stockDao.insertStocks(listOf(stock1, stock2))
        
        val searchResults = stockDao.searchStocks("LUCK").first()
        assertEquals(1, searchResults.size)
        assertEquals("LUCK", searchResults[0].symbol)
    }

    @Test
    fun updateStockPrice() = runBlocking {
        val stock = Stock(symbol = "LUCK", name = "Lucky Cement", sector = "Cement", currentPrice = 100.0)
        stockDao.insertStocks(listOf(stock))
        
        stockDao.updatePrice("LUCK", 120.0)
        
        val updatedStock = stockDao.getStocksSync(listOf("LUCK")).first()
        assertEquals(120.0, updatedStock.currentPrice, 0.001)
    }
}
