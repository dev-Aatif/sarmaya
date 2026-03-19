package com.sarmaya.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.PriceAlert
import com.sarmaya.app.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodically checks if any active price alerts have been triggered.
 * Uses multi-tier fallback (PSX DPS → PSX Terminal → Yahoo) for price data.
 */
class PriceAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PriceAlertWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = applicationContext as SarmayaApplication
        val container = application.container
        val alertDao = container.priceAlertDao
        val repository = container.stockDataRepository
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            // 1. Get all active alerts
            val pendingAlerts = alertDao.getActivePendingAlerts()
            if (pendingAlerts.isEmpty()) {
                Log.d(TAG, "No pending alerts, skipping sync")
                return@withContext Result.success()
            }

            Log.d(TAG, "Processing ${pendingAlerts.size} pending alerts")

            // 2. Sync prices (uses multi-tier fallback internally)
            val syncResult = repository.syncPsxQuotes()
            if (syncResult.isFailure) {
                Log.w(TAG, "Price sync failed: ${syncResult.exceptionOrNull()?.message}. Will retry.")
                return@withContext Result.retry()  // WorkManager will retry with exponential backoff
            }

            // 3. Fetch current prices for all unique symbols in alerts
            val uniqueSymbols = pendingAlerts.map { it.stockSymbol }.distinct()
            val pricesMap = mutableMapOf<String, Double>()
            
            uniqueSymbols.forEach { symbol ->
                repository.getQuote(symbol).onSuccess { quote ->
                    pricesMap[symbol] = quote.price
                }
            }

            if (pricesMap.isEmpty()) {
                Log.w(TAG, "Could not fetch any prices for alert symbols")
                return@withContext Result.retry()
            }

            // 4. Process each alert
            var triggeredCount = 0
            pendingAlerts.forEach { alert ->
                val currentPrice = pricesMap[alert.stockSymbol] ?: return@forEach
                
                val isTriggered = when (alert.alertType) {
                    "ABOVE" -> currentPrice >= alert.targetPrice
                    "BELOW" -> currentPrice <= alert.targetPrice
                    else -> false
                }

                if (isTriggered) {
                    notificationHelper.showPriceAlert(alert.stockSymbol, currentPrice, alert.alertType)
                    alertDao.markAsTriggered(alert.id)
                    triggeredCount++
                    Log.i(TAG, "Alert triggered: ${alert.stockSymbol} ${alert.alertType} ${alert.targetPrice} (current: $currentPrice)")
                }
            }

            Log.i(TAG, "Sync complete. $triggeredCount alerts triggered out of ${pendingAlerts.size}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed: ${e.javaClass.simpleName}: ${e.message}")
            Result.retry()  // Retry on unexpected errors instead of hard failure
        }
    }
}
