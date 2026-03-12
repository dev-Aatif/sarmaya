package com.sarmaya.app.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Orchestrates background workers for price updates and alerts.
 */
class SyncManager(private val context: Context) {

    companion object {
        private const val SYNC_WORK_NAME = "price_alert_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<PriceAlertWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flexible interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing to avoid resetting the timer
            syncRequest
        )
    }
    
    fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }
}
