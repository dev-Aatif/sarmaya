package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Stock")
data class Stock(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val sector: String,
    val currentPrice: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0L,
    val trades: Long = 0L,
    val value: Long = 0L,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val market: String = "REG",
    val state: String = "OFFLINE", // PRE, OPN, SUS, CLS, OFFLINE
    val priceUpdatedAt: Long? = null
)
