package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Transaction")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val portfolioId: Long = 1, // default portfolio for backwards compatibility
    val stockSymbol: String,
    val type: String,  // "BUY", "SELL", "DIVIDEND", "BONUS", "SPLIT"
    val quantity: Int,
    val pricePerShare: Double,
    val date: Long,
    val notes: String = "",
    val commissionType: String = "FLAT",  // "FLAT" or "PER_SHARE"
    val commissionAmount: Double = 0.0
)
