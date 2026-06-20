package com.sarmaya.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.PortfolioCalculator
import com.sarmaya.app.data.PortfolioSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Records a snapshot of all portfolios.
 */
class PortfolioSnapshotWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = applicationContext as SarmayaApplication
        val container = application.container
        
        val portfolioDao = container.portfolioDao
        val transactionDao = container.transactionDao
        val stockDao = container.stockDao
        val snapshotDao = container.portfolioSnapshotDao
        
        try {
            val dbStocks = stockDao.getAllStocksSync()
            val cacheStocks = container.stockQuoteCacheDao.getAllCacheSync().associateBy { it.symbol }
            
            val allStocks = dbStocks.map { stock ->
                val cached = cacheStocks[stock.symbol]
                if (cached != null) {
                    stock.copy(currentPrice = cached.price)
                } else {
                    stock
                }
            }
            
            val portfolios = portfolioDao.getAllPortfoliosSync()
            
            portfolios.forEach { portfolio ->
                val transactions = transactionDao.getTransactionsByPortfolioSync(portfolio.id)
                val holdings = PortfolioCalculator.computeSnapshotSynchronous(transactions, allStocks)
                
                val totalValue = holdings.sumOf { it.currentValue }
                val totalInvested = holdings.sumOf { it.totalInvested }
                
                snapshotDao.insert(
                    PortfolioSnapshot(
                        portfolioId = portfolio.id,
                        totalValue = totalValue,
                        investedValue = totalInvested
                    )
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
