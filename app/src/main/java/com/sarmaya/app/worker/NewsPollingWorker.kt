package com.sarmaya.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sarmaya.app.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appContainer: AppContainer
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val result = appContainer.newsRepository.refreshNews()
        if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
