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
        PriceAlert::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun transactionDao(): TransactionDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun stockQuoteCacheDao(): StockQuoteCacheDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun priceAlertDao(): PriceAlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS WatchlistItem (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        stockSymbol TEXT NOT NULL,
                        addedAt INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to Transaction table
                db.execSQL("ALTER TABLE `Transaction` ADD COLUMN portfolioId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `Transaction` ADD COLUMN commissionType TEXT NOT NULL DEFAULT 'FLAT'")
                db.execSQL("ALTER TABLE `Transaction` ADD COLUMN commissionAmount REAL NOT NULL DEFAULT 0.0")

                // 2. Create StockQuoteCache table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS StockQuoteCache (
                        symbol TEXT NOT NULL PRIMARY KEY,
                        price REAL NOT NULL,
                        change REAL NOT NULL,
                        changePercent REAL NOT NULL,
                        volume INTEGER NOT NULL,
                        high REAL NOT NULL,
                        low REAL NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // 3. Create Portfolio table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS Portfolio (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isDefault INTEGER NOT NULL
                    )
                """.trimIndent())

                // 4. Insert default portfolio
                db.execSQL("INSERT INTO Portfolio (id, name, createdAt, isDefault) VALUES (1, 'My Portfolio', ${System.currentTimeMillis()}, 1)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS PriceAlert (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        stockSymbol TEXT NOT NULL,
                        targetPrice REAL NOT NULL,
                        alertType TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        isTriggered INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sarmaya_database"
                )
                .createFromAsset("database/sarmaya.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
