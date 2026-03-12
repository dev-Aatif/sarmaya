package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    @Query("SELECT * FROM Portfolio ORDER BY isDefault DESC, createdAt ASC")
    fun getAllPortfolios(): Flow<List<Portfolio>>

    @Query("SELECT * FROM Portfolio")
    suspend fun getAllPortfoliosSync(): List<Portfolio>

    @Query("SELECT * FROM Portfolio WHERE id = :id LIMIT 1")
    suspend fun getPortfolioById(id: Long): Portfolio?

    @Query("SELECT * FROM Portfolio WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPortfolio(): Portfolio?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(portfolio: Portfolio): Long

    @Update
    suspend fun update(portfolio: Portfolio)

    @Delete
    suspend fun delete(portfolio: Portfolio)

    @Query("SELECT COUNT(*) FROM Portfolio")
    suspend fun getCount(): Int
}
