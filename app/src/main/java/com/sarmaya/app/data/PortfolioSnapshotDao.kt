package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioSnapshotDao {

    @Insert
    suspend fun insert(snapshot: PortfolioSnapshot)

    @Query("SELECT * FROM PortfolioSnapshot WHERE portfolioId = :portfolioId ORDER BY timestamp ASC")
    fun getSnapshotsForPortfolio(portfolioId: Long): Flow<List<PortfolioSnapshot>>

    @Query("SELECT * FROM PortfolioSnapshot ORDER BY timestamp ASC")
    fun getAllSnapshots(): Flow<List<PortfolioSnapshot>>

    @Query("SELECT * FROM PortfolioSnapshot WHERE portfolioId = :portfolioId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSnapshot(portfolioId: Long): PortfolioSnapshot?
}
