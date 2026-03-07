package com.sarmaya.app.data

import androidx.room.ColumnInfo

data class ComputedHolding(
    @ColumnInfo(name = "stockSymbol") val stockSymbol: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sector") val sector: String,
    @ColumnInfo(name = "currentPrice") val currentPrice: Double,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "totalInvested") val totalInvested: Double,
    @ColumnInfo(name = "totalDividends") val totalDividends: Double
) {
    val avgBuyPrice: Double
        get() = if (quantity > 0) totalInvested / quantity else 0.0
    
    val currentValue: Double
        get() = quantity * currentPrice
        
    val profitLossAmount: Double
        get() = currentValue - totalInvested
        
    val profitLossPercentage: Double
        get() = if (totalInvested > 0) (profitLossAmount / totalInvested) * 100 else 0.0
}
