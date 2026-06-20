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
        StockQuoteCache::class,
        Portfolio::class,
        PortfolioSnapshot::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockQuoteCacheDao(): StockQuoteCacheDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
