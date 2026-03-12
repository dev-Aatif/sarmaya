package com.sarmaya.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Stock::class, Transaction::class, WatchlistItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun transactionDao(): TransactionDao
    abstract fun watchlistDao(): WatchlistDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sarmaya_database"
                )
                .createFromAsset("database/sarmaya.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
