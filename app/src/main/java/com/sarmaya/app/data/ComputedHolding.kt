package com.sarmaya.app.data

import androidx.room.ColumnInfo

data class ComputedHolding(
    val stockSymbol: String,
    val name: String,
    val sector: String,
    val currentPrice: Double,
    val quantity: Int,
    val totalInvested: Double,
    val totalDividends: Double,
    val realizedProfitLoss: Double = 0.0
) {
    val avgBuyPrice: Double
        get() = if (quantity > 0) totalInvested / quantity else 0.0
    
    val currentValue: Double
        get() = quantity * currentPrice
        
    val profitLossAmount: Double
        get() = currentValue - totalInvested
        
    val profitLossPercentage: Double
        get() = if (totalInvested > 0) (profitLossAmount / totalInvested) * 100 else 0.0

    /** Total return = unrealized P/L + realized P/L from sells + dividends */
    val totalReturn: Double
        get() = profitLossAmount + realizedProfitLoss + totalDividends
}
