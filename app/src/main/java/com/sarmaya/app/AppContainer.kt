package com.sarmaya.app

import android.content.Context
import com.sarmaya.app.data.AppDatabase
import com.sarmaya.app.data.DataStoreManager
import com.sarmaya.app.data.PortfolioDao
import com.sarmaya.app.data.PortfolioSnapshotDao
import com.sarmaya.app.data.PriceAlertDao
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.StockQuoteCacheDao
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.data.WatchlistDao
import com.sarmaya.app.network.ConnectivityChecker
import com.sarmaya.app.network.StockDataRepository
import com.sarmaya.app.network.api.PsxApi
import com.sarmaya.app.network.api.PsxTerminalApi
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
    val portfolioSnapshotDao: PortfolioSnapshotDao by lazy { database.portfolioSnapshotDao() }
    val newsArticleDao: com.sarmaya.app.data.NewsArticleDao by lazy { database.newsArticleDao() }

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

    private val psxOkHttpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val yahooFinanceApi: YahooFinanceApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://query2.finance.yahoo.com")  // query2 is the community-maintained mirror
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YahooFinanceApi::class.java)
    }

    val psxApi: PsxApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dps.psx.com.pk/")
            .client(psxOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PsxApi::class.java)
    }

    val psxTerminalApi: PsxTerminalApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://psxterminal.com/") // Reverted to main domain to match /api/ paths
            .client(psxOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PsxTerminalApi::class.java)
    }

    val connectivityChecker: ConnectivityChecker by lazy {
        ConnectivityChecker(context)
    }

    val stockDataRepository: StockDataRepository by lazy {
        StockDataRepository(
            yahooApi = yahooFinanceApi,
            psxApi = psxApi,
            psxTerminalApi = psxTerminalApi,
            stockDao = stockDao,
            quoteCacheDao = stockQuoteCacheDao,
            connectivityChecker = connectivityChecker
        )
    }

    val psxWebSocketManager: com.sarmaya.app.network.websocket.PsxWebSocketManager by lazy {
        com.sarmaya.app.network.websocket.PsxWebSocketManager(
            connectivityChecker = connectivityChecker,
            moshi = moshi
        )
    }

    private val googleRssParser: com.sarmaya.app.network.rss.GoogleRssParser by lazy {
        com.sarmaya.app.network.rss.GoogleRssParser()
    }

    val newsRepository: com.sarmaya.app.network.repository.NewsRepository by lazy {
        com.sarmaya.app.network.repository.NewsRepository(
            newsDao = newsArticleDao,
            rssParser = googleRssParser,
            connectivityChecker = connectivityChecker
        )
    }

    // ─── Workers ───
    val syncManager: SyncManager by lazy { SyncManager(context) }
}
