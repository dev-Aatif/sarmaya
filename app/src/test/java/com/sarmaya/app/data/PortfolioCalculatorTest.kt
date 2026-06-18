package com.sarmaya.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PortfolioCalculatorTest {

    @Test
    fun `test single buy transaction`() {
        val tx = Transaction(
            portfolioId = 1,
            stockSymbol = "TEST",
            type = "BUY",
            quantity = 100,
            pricePerShare = 10.0,
            date = 1000L,
            commissionType = "FLAT",
            commissionAmount = 5.0,
            notes = ""
        )
        val stock = Stock("TEST", "Test Co", "Sector", 12.0)

        val holdings = PortfolioCalculator.computeSnapshotSynchronous(listOf(tx), listOf(stock))
        
        assertEquals(1, holdings.size)
        val holding = holdings.first()
        assertEquals(100, holding.quantity)
        assertEquals(1005.0, holding.totalInvested, 0.001) // 100 * 10 + 5
        assertEquals(0.0, holding.realizedProfitLoss, 0.001)
    }

    @Test
    fun `test buy and sell partial quantity`() {
        val txBuy = Transaction(
            portfolioId = 1, stockSymbol = "TEST", type = "BUY", quantity = 100,
            pricePerShare = 10.0, date = 1000L, commissionType = "FLAT", commissionAmount = 5.0, notes = ""
        )
        val txSell = Transaction(
            portfolioId = 1, stockSymbol = "TEST", type = "SELL", quantity = 50,
            pricePerShare = 15.0, date = 2000L, commissionType = "FLAT", commissionAmount = 10.0, notes = ""
        )
        val stock = Stock("TEST", "Test Co", "Sector", 15.0)

        val holdings = PortfolioCalculator.computeSnapshotSynchronous(listOf(txBuy, txSell), listOf(stock))
        
        assertEquals(1, holdings.size)
        val holding = holdings.first()
        assertEquals(50, holding.quantity)
        
        // Initial invested: 1005.0
        // Avg Cost: 1005.0 / 100 = 10.05
        // Sell 50 -> Cost basis reduction: 50 * 10.05 = 502.5
        // Remaining Invested: 1005.0 - 502.5 = 502.5
        assertEquals(502.5, holding.totalInvested, 0.001)
        
        // Realized PL = (15.0 - 10.05) * 50 - 10 = (4.95 * 50) - 10 = 247.5 - 10 = 237.5
        assertEquals(237.5, holding.realizedProfitLoss, 0.001)
    }

    @Test
    fun `test dividend`() {
        val txDiv = Transaction(
            portfolioId = 1, stockSymbol = "TEST", type = "DIVIDEND", quantity = 0,
            pricePerShare = 50.0, date = 1000L, commissionType = "FLAT", commissionAmount = 0.0, notes = ""
        )
        val stock = Stock("TEST", "Test Co", "Sector", 15.0)

        val holdings = PortfolioCalculator.computeSnapshotSynchronous(listOf(txDiv), listOf(stock))
        
        assertEquals(1, holdings.size)
        val holding = holdings.first()
        assertEquals(0, holding.quantity)
        assertEquals(0.0, holding.totalInvested, 0.001)
        assertEquals(50.0, holding.totalDividends, 0.001)
        assertEquals(0.0, holding.realizedProfitLoss, 0.001)
    }

    @Test
    fun `test split`() {
        val txBuy = Transaction(
            portfolioId = 1, stockSymbol = "TEST", type = "BUY", quantity = 10,
            pricePerShare = 100.0, date = 1000L, commissionType = "FLAT", commissionAmount = 0.0, notes = ""
        )
        val txSplit = Transaction(
            portfolioId = 1, stockSymbol = "TEST", type = "SPLIT", quantity = 0,
            pricePerShare = 0.0, date = 2000L, commissionType = "FLAT", commissionAmount = 0.0,
            splitRatio = 1.5, notes = ""
        )
        val stock = Stock("TEST", "Test Co", "Sector", 50.0)

        val holdings = PortfolioCalculator.computeSnapshotSynchronous(listOf(txBuy, txSplit), listOf(stock))
        
        assertEquals(1, holdings.size)
        val holding = holdings.first()
        // 10 shares * 1.5 = 15 shares
        assertEquals(15, holding.quantity)
        assertEquals(1000.0, holding.totalInvested, 0.001)
    }
}
