package com.sarmaya.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Stock::class,
        Transaction::class,
        WatchlistItem::class,
        StockQuoteCache::class,
        Portfolio::class,
        PriceAlert::class,
        PortfolioSnapshot::class,
        NewsArticle::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun transactionDao(): TransactionDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun stockQuoteCacheDao(): StockQuoteCacheDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao
    abstract fun newsArticleDao(): NewsArticleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sarmaya_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert default portfolio
                        db.execSQL("INSERT INTO Portfolio (id, name, createdAt, isDefault) VALUES (1, 'My Portfolio', ${System.currentTimeMillis()}, 1)")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
