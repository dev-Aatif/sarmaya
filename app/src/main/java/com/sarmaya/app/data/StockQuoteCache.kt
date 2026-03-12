package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached live quote data for offline access.
 * Updated every time a fresh quote is fetched from Yahoo Finance.
 */
@Entity(tableName = "StockQuoteCache")
data class StockQuoteCache(
    @PrimaryKey
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val high: Double,
    val low: Double,
    val cachedAt: Long = System.currentTimeMillis()
)
