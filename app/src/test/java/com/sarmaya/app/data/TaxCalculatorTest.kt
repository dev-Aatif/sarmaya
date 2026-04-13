package com.sarmaya.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TaxCalculatorTest {

    @Test
    fun `calculateBuyFees returns correct sum of commission, sst and exchange fees`() {
        val qty = 100
        val price = 50.0
        val rate = 0.0015 // 0.15%
        
        // gross = 5000
        // commission = 5000 * 0.0015 = 7.5
        // sst = 7.5 * 0.13 = 0.975
        // exchange = 5000 * 0.0003 = 1.5
        // total = 7.5 + 0.975 + 1.5 = 9.975
        
        val fees = TaxCalculator.calculateBuyFees(qty, price, rate)
        assertEquals(9.975, fees, 0.001)
    }

    @Test
    fun `calculateSellFees for Filer with profit accounts for CGT correctly`() {
        val qty = 500
        val sellPrice = 200.0
        val avgCost = 150.0 // Profit = (200-150)*500 = 25,000
        val isFiler = true
        val rate = 0.0015
        
        // gross = 100,000
        // commission = 150.0
        // sst = 150 * 0.13 = 19.5
        // exchange = 100,000 * 0.0003 = 30.0
        // cgt = 25,000 * 0.15 = 3,750.0
        // totalFees = 150 + 19.5 + 30 + 3750 = 3949.5
        // netProceeds = 100,000 - 3949.5 = 96050.5
        
        val breakdown = TaxCalculator.calculateSellFees(qty, sellPrice, avgCost, isFiler, rate)
        
        assertEquals(100000.0, breakdown.grossValue, 0.001)
        assertEquals(150.0, breakdown.commission, 0.001)
        assertEquals(19.5, breakdown.sst, 0.001)
        assertEquals(30.0, breakdown.exchangeFees, 0.001)
        assertEquals(3750.0, breakdown.cgt, 0.001)
        assertEquals(3949.5, breakdown.totalFees, 0.001)
        assertEquals(96050.5, breakdown.netProceeds, 0.001)
    }

    @Test
    fun `calculateSellFees for Non-Filer with profit accounts for higher CGT`() {
        val qty = 100
        val sellPrice = 100.0
        val avgCost = 80.0 // Profit = 2,000
        val isFiler = false
        val rate = 0.0015
        
        // cgt = 2000 * 0.30 = 600.0
        val breakdown = TaxCalculator.calculateSellFees(qty, sellPrice, avgCost, isFiler, rate)
        assertEquals(600.0, breakdown.cgt, 0.001)
    }

    @Test
    fun `calculateSellFees with loss results in zero CGT`() {
        val qty = 100
        val sellPrice = 80.0
        val avgCost = 100.0 // Loss = 2,000
        val isFiler = true
        val rate = 0.0015
        
        val breakdown = TaxCalculator.calculateSellFees(qty, sellPrice, avgCost, isFiler, rate)
        assertEquals(0.0, breakdown.cgt, 0.001)
    }
}
