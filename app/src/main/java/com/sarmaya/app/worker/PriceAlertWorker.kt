package com.sarmaya.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.PriceAlert
import com.sarmaya.app.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodically checks if any active price alerts have been triggered.
 */
class PriceAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = applicationContext as SarmayaApplication
        val container = application.container
        val alertDao = container.priceAlertDao
        val repository = container.stockDataRepository
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            // 1. Get all active alerts
            val pendingAlerts = alertDao.getActivePendingAlerts()
            if (pendingAlerts.isEmpty()) return@withContext Result.success()

            // 2. Fetch current prices for all unique symbols in alerts
            val uniqueSymbols = pendingAlerts.map { it.stockSymbol }.distinct()
            val pricesMap = mutableMapOf<String, Double>()
            
            // We use the bulk scraper sync logic to get fresh prices efficiently
            repository.syncPsxQuotes()
            
            // Get updated prices from DB/Cache
            uniqueSymbols.forEach { symbol ->
                repository.getQuote(symbol).onSuccess { quote ->
                    pricesMap[symbol] = quote.price
                }
            }

            // 3. Process each alert
            pendingAlerts.forEach { alert ->
                val currentPrice = pricesMap[alert.stockSymbol] ?: return@forEach
                
                val isTriggered = when (alert.alertType) {
                    "ABOVE" -> currentPrice >= alert.targetPrice
                    "BELOW" -> currentPrice <= alert.targetPrice
                    else -> false
                }

                if (isTriggered) {
                    // 4. Trigger notification
                    notificationHelper.showPriceAlert(alert.stockSymbol, currentPrice, alert.alertType)
                    
                    // 5. Mark as triggered in DB
                    alertDao.markAsTriggered(alert.id)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
