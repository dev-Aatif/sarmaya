package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM Stock ORDER BY symbol ASC")
    fun getAllStocks(): Flow<List<Stock>>

    @Query("SELECT * FROM Stock WHERE symbol LIKE '%' || :searchQuery || '%' OR name LIKE '%' || :searchQuery || '%' ORDER BY symbol ASC")
    fun searchStocks(searchQuery: String): Flow<List<Stock>>

    @Query("SELECT * FROM Stock WHERE symbol IN (:symbols)")
    fun getStocks(symbols: List<String>): Flow<List<Stock>>

    @Query("SELECT * FROM Stock WHERE symbol IN (:symbols)")
    suspend fun getStocksSync(symbols: List<String>): List<Stock>

    @Query("UPDATE Stock SET currentPrice = :price, priceUpdatedAt = :updatedAt WHERE symbol = :symbol")
    suspend fun updatePrice(symbol: String, price: Double, updatedAt: Long = System.currentTimeMillis())

    @androidx.room.Update
    suspend fun updateStocks(stocks: List<Stock>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<Stock>)
}
