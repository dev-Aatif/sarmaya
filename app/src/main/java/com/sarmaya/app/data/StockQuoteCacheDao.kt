package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StockQuoteCacheDao {

    @Query("SELECT * FROM StockQuoteCache WHERE symbol = :symbol LIMIT 1")
    suspend fun getCache(symbol: String): StockQuoteCache?

    @Query("SELECT * FROM StockQuoteCache")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<StockQuoteCache>>

    @Query("SELECT * FROM StockQuoteCache")
    suspend fun getAllCacheSync(): List<StockQuoteCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: StockQuoteCache)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(caches: List<StockQuoteCache>)

    @Query("DELETE FROM StockQuoteCache WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query("DELETE FROM StockQuoteCache")
    suspend fun clearAll()
}
