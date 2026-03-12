package com.sarmaya.app

import android.content.Context
import com.sarmaya.app.data.AppDatabase
import com.sarmaya.app.data.DataStoreManager
import com.sarmaya.app.data.PortfolioDao
import com.sarmaya.app.data.PriceAlertDao
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.StockQuoteCacheDao
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.data.WatchlistDao
import com.sarmaya.app.network.ConnectivityChecker
import com.sarmaya.app.network.StockDataRepository
import com.sarmaya.app.network.api.GitHubApi
import com.sarmaya.app.network.api.PsxApi
import com.sarmaya.app.network.api.YahooFinanceApi
import com.sarmaya.app.worker.SyncManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    // ─── Database ───
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }
    val stockDao: StockDao by lazy { database.stockDao() }
    val transactionDao: TransactionDao by lazy { database.transactionDao() }
    val watchlistDao: WatchlistDao by lazy { database.watchlistDao() }
    val stockQuoteCacheDao: StockQuoteCacheDao by lazy { database.stockQuoteCacheDao() }
    val portfolioDao: PortfolioDao by lazy { database.portfolioDao() }
    val priceAlertDao: PriceAlertDao by lazy { database.priceAlertDao() }

    // ─── DataStore ───
    val dataStoreManager: DataStoreManager by lazy { DataStoreManager(context) }

    // ─── Networking ───
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    val yahooFinanceApi: YahooFinanceApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YahooFinanceApi::class.java)
    }

    val psxApi: PsxApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dps.psx.com.pk/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PsxApi::class.java)
    }

    val gitHubApi: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    val connectivityChecker: ConnectivityChecker by lazy {
        ConnectivityChecker(context)
    }

    val stockDataRepository: StockDataRepository by lazy {
        StockDataRepository(
            yahooApi = yahooFinanceApi,
            psxApi = psxApi,
            stockDao = stockDao,
            quoteCacheDao = stockQuoteCacheDao,
            connectivityChecker = connectivityChecker
        )
    }

    // ─── Workers ───
    val syncManager: SyncManager by lazy { SyncManager(context) }
}
