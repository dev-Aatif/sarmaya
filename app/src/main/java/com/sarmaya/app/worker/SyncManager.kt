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
        private const val SNAPSHOT_WORK_NAME = "portfolio_snapshot_work"
        private const val NEWS_WORK_NAME = "news_polling_work"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }



    fun scheduleSnapshotWork() {
        // Daily snapshot
        val snapshotRequest = PeriodicWorkRequestBuilder<PortfolioSnapshotWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS // Flexible interval
        )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SNAPSHOT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            snapshotRequest
        )
    }

    /**
     * One-time snapshot on app start if needed
     */
    fun runImmediateSnapshot() {
        val request = OneTimeWorkRequestBuilder<PortfolioSnapshotWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }


}
