package com.sarmaya.app

import android.content.Context
import com.sarmaya.app.data.AppDatabase
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.data.WatchlistDao

class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }
    
    val stockDao: StockDao by lazy { database.stockDao() }
    val transactionDao: TransactionDao by lazy { database.transactionDao() }
    val watchlistDao: WatchlistDao by lazy { database.watchlistDao() }
}
