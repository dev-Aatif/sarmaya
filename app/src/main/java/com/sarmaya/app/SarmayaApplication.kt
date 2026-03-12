package com.sarmaya.app

import android.app.Application

class SarmayaApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        
        // Start background sync for price alerts
        container.syncManager.scheduleBackgroundSync()
    }
}
