package com.sarmaya.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioCalculatorTest {

    private val testStock = Stock(
        symbol = "LUCK",
        name = "Lucky Cement",
        sector = "Cement",
        currentPrice = 800.0
    )

    @Test
    fun `computeSnapshotSynchronous correctly handles multiple BUY transactions`() {
        val transactions = listOf(
            Transaction(stockSymbol = "LUCK", type = "BUY", quantity = 10, pricePerShare = 700.0, date = 1000L),
            Transaction(stockSymbol = "LUCK", type = "BUY", quantity = 10, pricePerShare = 900.0, date = 2000L)
        )
        
        val holdings = PortfolioCalculator.computeSnapshotSynchronous(transactions, listOf(testStock))
        
        assertEquals(1, holdings.size)
        val holding = holdings[0]
        assertEquals(20, holding.quantity)
        assertEquals(16000.0, holding.totalInvested, 0.001) // (10*700) + (10*900)
    }

    @Test
    fun `computeSnapshotSynchronous correctly calculates Realized PL on SELL`() {
        val transactions = listOf(
            Transaction(stockSymbol = "LUCK", type = "BUY", quantity = 20, pricePerShare = 100.0, date = 1000L),
            Transaction(stockSymbol = "LUCK", type = "SELL", quantity = 10, pricePerShare = 150.0, date = 2000L)
        )
        // Avg Cost = 100.0
        // Profit on 10 shares = (150-100)*10 = 500.0
        
        val holdings = PortfolioCalculator.computeSnapshotSynchronous(transactions, listOf(testStock))
        
        val holding = holdings[0]
        assertEquals(10, holding.quantity)
        assertEquals(1000.0, holding.totalInvested, 0.001)
        assertEquals(500.0, holding.realizedProfitLoss, 0.001)
    }

    @Test
    fun `computeSnapshotSynchronous handles DIVIDEND transactions`() {
        val transactions = listOf(
            Transaction(stockSymbol = "LUCK", type = "BUY", quantity = 100, pricePerShare = 100.0, date = 1000L),
            Transaction(stockSymbol = "LUCK", type = "DIVIDEND", quantity = 100, pricePerShare = 5.0, date = 2000L)
        )
        
        val holdings = PortfolioCalculator.computeSnapshotSynchronous(transactions, listOf(testStock))
        
        val holding = holdings[0]
        assertEquals(500.0, holding.totalDividends, 0.001)
    }

    @Test
    fun `computeSnapshotSynchronous resets on full liquidaton`() {
        val transactions = listOf(
            Transaction(stockSymbol = "LUCK", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1000L),
            Transaction(stockSymbol = "LUCK", type = "SELL", quantity = 10, pricePerShare = 120.0, date = 2000L)
        )
        // Profit = 200. realized. Qty = 0.
        
        val holdings = PortfolioCalculator.computeSnapshotSynchronous(transactions, listOf(testStock))
        
        val holding = holdings[0]
        assertEquals(0, holding.quantity)
        assertEquals(0.0, holding.totalInvested, 0.001)
        assertEquals(200.0, holding.realizedProfitLoss, 0.001)
    }
}
