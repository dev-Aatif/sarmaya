package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PriceAlert")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stockSymbol: String,
    val targetPrice: Double,
    val alertType: String, // "ABOVE" or "BELOW"
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
