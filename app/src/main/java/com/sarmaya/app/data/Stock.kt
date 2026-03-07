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
    val priceUpdatedAt: Long? = null
)
