package com.sarmaya.app.data

/**
 * Calculates Capital Gains Tax (CGT) and Brokerage fees for PSX transactions.
 */
object TaxCalculator {

    // PSX/FBR rules simplified for v2
    fun calculateSellFees(
        quantity: Int,
        sellPrice: Double,
        avgCost: Double,
        isFiler: Boolean,
        brokerageRate: Double // e.g., 0.0015
    ): TaxBreakdown {
        val grossValue = quantity * sellPrice
        val profit = (sellPrice - avgCost) * quantity
        
        // 1. Brokerage Commission
        val commission = grossValue * brokerageRate
        
        // 2. SST / FBR on Commission (approx 13% of commission)
        val sst = commission * 0.13
        
        // 3. PSX/CDC/NCCPL Fees (approx 0.03% of gross value)
        val exchangeFees = grossValue * 0.0003
        
        // 4. CGT (Simplified: 15% for Filers, 30% for Non-Filers on profit)
        val cgtRate = if (isFiler) 0.15 else 0.30
        val cgt = if (profit > 0) profit * cgtRate else 0.0
        
        val totalFees = commission + sst + exchangeFees + cgt
        val netProceeds = grossValue - totalFees
        
        return TaxBreakdown(
            grossValue = grossValue,
            commission = commission,
            sst = sst,
            exchangeFees = exchangeFees,
            cgt = cgt,
            totalFees = totalFees,
            netProceeds = netProceeds
        )
    }

    fun calculateBuyFees(
        quantity: Int,
        buyPrice: Double,
        brokerageRate: Double
    ): Double {
        val grossValue = quantity * buyPrice
        val commission = grossValue * brokerageRate
        val sst = commission * 0.13
        val exchangeFees = grossValue * 0.0003
        return commission + sst + exchangeFees
    }
}

data class TaxBreakdown(
    val grossValue: Double,
    val commission: Double,
    val sst: Double,
    val exchangeFees: Double,
    val cgt: Double,
    val totalFees: Double,
    val netProceeds: Double
)
