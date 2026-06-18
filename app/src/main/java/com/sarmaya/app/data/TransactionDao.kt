package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Insert
    suspend fun insertTransactions(transactions: List<Transaction>)

    @androidx.room.Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
    
    @Query("SELECT * FROM `Transaction` WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM `Transaction` ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM `Transaction` WHERE portfolioId = :portfolioId ORDER BY date DESC")
    fun getTransactionsByPortfolio(portfolioId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM `Transaction` WHERE portfolioId = :portfolioId")
    suspend fun getTransactionsByPortfolioSync(portfolioId: Long): List<Transaction>

    @Query("SELECT * FROM `Transaction` WHERE stockSymbol = :symbol ORDER BY date ASC, id ASC")
    suspend fun getTransactionsForStock(symbol: String): List<Transaction>

    @Query("SELECT * FROM `Transaction` WHERE stockSymbol = :symbol AND portfolioId = :portfolioId ORDER BY date ASC, id ASC")
    suspend fun getTransactionsForStockInPortfolio(symbol: String, portfolioId: Long): List<Transaction>



}
