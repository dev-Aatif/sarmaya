package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM WatchlistItem ORDER BY addedAt DESC")
    fun getAllItems(): Flow<List<WatchlistItem>>

    @Query("SELECT COUNT(*) FROM WatchlistItem WHERE stockSymbol = :symbol")
    suspend fun isInWatchlist(symbol: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItem)

    @Delete
    suspend fun delete(item: WatchlistItem)

    @Query("DELETE FROM WatchlistItem WHERE stockSymbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)
}
