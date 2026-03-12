package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceAlertDao {

    @Query("SELECT * FROM PriceAlert ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM PriceAlert WHERE isActive = 1 AND isTriggered = 0")
    suspend fun getActivePendingAlerts(): List<PriceAlert>

    @Query("SELECT * FROM PriceAlert WHERE stockSymbol = :symbol")
    fun getAlertsForStock(symbol: String): Flow<List<PriceAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: PriceAlert): Long

    @Update
    suspend fun update(alert: PriceAlert)

    @Delete
    suspend fun delete(alert: PriceAlert)

    @Query("UPDATE PriceAlert SET isTriggered = 1, isActive = 0 WHERE id = :id")
    suspend fun markAsTriggered(id: Long)
}
