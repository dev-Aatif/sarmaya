package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PortfolioSnapshot")
data class PortfolioSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val portfolioId: Long, // 0 for "Total" or specific ID
    val totalValue: Double,
    val investedValue: Double,
    val timestamp: Long = System.currentTimeMillis()
)
