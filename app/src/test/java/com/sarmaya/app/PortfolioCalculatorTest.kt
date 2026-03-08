package com.sarmaya.app

import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.data.PortfolioCalculator
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive pure-logic tests for PortfolioCalculator.getEventSourcedHoldings().
 * These cover BUY, SELL, DIVIDEND, BONUS, SPLIT, and combined edge-case scenarios.
 */
class PortfolioCalculatorTest {

    private fun compute(
        transactions: List<Transaction>,
        stocks: List<Stock>
    ) = PortfolioCalculator.getEventSourcedHoldings(
        flowOf(transactions),
        flowOf(stocks)
    )

    // ────────────────── BUY ──────────────────

    @Test
    fun `single BUY sets quantity, invested, and avgPrice`() = runTest {
        val result = compute(
            listOf(Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L)),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 120.0))
        ).first()

        assertEquals(1, result.size)
        val h = result[0]
        assertEquals("A", h.stockSymbol)
        assertEquals(10, h.quantity)
        assertEquals(1000.0, h.totalInvested, 0.001) // 10 * 100
        assertEquals(100.0, h.avgBuyPrice, 0.001)
        assertEquals(1200.0, h.currentValue, 0.001)  // 10 * 120
        assertEquals(200.0, h.profitLossAmount, 0.001)
        assertEquals(20.0, h.profitLossPercentage, 0.001) // (200/1000)*100
        assertEquals(0.0, h.realizedProfitLoss, 0.001) // No sells yet
    }

    @Test
    fun `multiple BUYs accumulate quantity and cost`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "BUY", quantity = 5, pricePerShare = 200.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 150.0))
        ).first()

        val h = result[0]
        assertEquals(15, h.quantity)
        assertEquals(2000.0, h.totalInvested, 0.001) // (10*100) + (5*200)
        assertEquals(133.33, h.avgBuyPrice, 0.01)     // 2000 / 15
        assertEquals(2250.0, h.currentValue, 0.001)   // 15 * 150
        assertEquals(250.0, h.profitLossAmount, 0.001)
        assertEquals(0.0, h.realizedProfitLoss, 0.001)
    }

    // ────────────────── SELL ──────────────────

    @Test
    fun `partial SELL reduces quantity and proportionally reduces cost basis`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 4, pricePerShare = 150.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 120.0))
        ).first()

        val h = result[0]
        assertEquals(6, h.quantity)
        // avgCost was 100; selling 4 removes 4*100=400 from invested
        assertEquals(600.0, h.totalInvested, 0.001)   // 1000 - 400
        assertEquals(100.0, h.avgBuyPrice, 0.001)      // 600 / 6
        assertEquals(720.0, h.currentValue, 0.001)     // 6 * 120
        assertEquals(120.0, h.profitLossAmount, 0.001)  // 720 - 600
        // Realized P/L: (150 - 100) * 4 = 200
        assertEquals(200.0, h.realizedProfitLoss, 0.001)
    }

    @Test
    fun `partial SELL at loss tracks negative realized PL`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 4, pricePerShare = 80.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        val h = result[0]
        assertEquals(6, h.quantity)
        assertEquals(600.0, h.totalInvested, 0.001)
        // Realized P/L: (80 - 100) * 4 = -80
        assertEquals(-80.0, h.realizedProfitLoss, 0.001)
    }

    @Test
    fun `full SELL closes position but retains realized PL`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 10, pricePerShare = 150.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 120.0))
        ).first()

        // Position closed, but realized P/L should be tracked
        assertEquals(1, result.size)
        val h = result[0]
        assertEquals(0, h.quantity)
        assertEquals(0.0, h.totalInvested, 0.001)
        // Realized P/L: (150 - 100) * 10 = 500
        assertEquals(500.0, h.realizedProfitLoss, 0.001)
    }

    @Test
    fun `sell-all then rebuy starts fresh cost basis with accumulated realized PL`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 10, pricePerShare = 150.0, date = 2L),
                Transaction(stockSymbol = "A", type = "BUY", quantity = 5, pricePerShare = 200.0, date = 3L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 220.0))
        ).first()

        val h = result[0]
        assertEquals(5, h.quantity)
        assertEquals(1000.0, h.totalInvested, 0.001) // fresh: 5 * 200
        assertEquals(200.0, h.avgBuyPrice, 0.001)
        assertEquals(1100.0, h.currentValue, 0.001)  // 5 * 220
        assertEquals(100.0, h.profitLossAmount, 0.001)
        // Realized from first sell: (150 - 100) * 10 = 500
        assertEquals(500.0, h.realizedProfitLoss, 0.001)
    }

    @Test
    fun `multiple sells accumulate realized PL`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 20, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 5, pricePerShare = 150.0, date = 2L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 5, pricePerShare = 120.0, date = 3L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        val h = result[0]
        assertEquals(10, h.quantity)
        assertEquals(1000.0, h.totalInvested, 0.001) // 2000 - 500 - 500
        // Realized: (150-100)*5 + (120-100)*5 = 250 + 100 = 350
        assertEquals(350.0, h.realizedProfitLoss, 0.001)
    }

    // ────────────────── DIVIDEND ──────────────────

    @Test
    fun `dividend uses tx quantity and pricePerShare`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "DIVIDEND", quantity = 10, pricePerShare = 5.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        val h = result[0]
        assertEquals(10, h.quantity)
        assertEquals(50.0, h.totalDividends, 0.001) // 10 shares * ₨5 per share
        // Dividends should NOT affect invested or quantity
        assertEquals(1000.0, h.totalInvested, 0.001)
    }

    @Test
    fun `dividend on partial shares only counts those shares`() = runTest {
        // User owns 20 shares but only 10 get the dividend
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 20, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "DIVIDEND", quantity = 10, pricePerShare = 5.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        val h = result[0]
        assertEquals(20, h.quantity)
        // Only 10 shares get the ₨5 dividend, NOT all 20
        assertEquals(50.0, h.totalDividends, 0.001) // 10 * 5, not 20 * 5
    }

    @Test
    fun `multiple dividends accumulate correctly`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "DIVIDEND", quantity = 10, pricePerShare = 5.0, date = 2L),
                Transaction(stockSymbol = "A", type = "DIVIDEND", quantity = 10, pricePerShare = 3.0, date = 3L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        assertEquals(80.0, result[0].totalDividends, 0.001) // (10*5) + (10*3) = 80
    }

    // ────────────────── BONUS ──────────────────

    @Test
    fun `bonus increases shares at zero cost, diluting avgPrice`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "BONUS", quantity = 5, pricePerShare = 0.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        val h = result[0]
        assertEquals(15, h.quantity)
        assertEquals(1000.0, h.totalInvested, 0.001) // unchanged
        assertEquals(66.67, h.avgBuyPrice, 0.01)      // 1000 / 15
        assertEquals(1500.0, h.currentValue, 0.001)   // 15 * 100
    }

    // ────────────────── SPLIT ──────────────────

    @Test
    fun `split multiplies shares, invested stays same`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 200.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SPLIT", quantity = 2, pricePerShare = 0.0, date = 2L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        val h = result[0]
        assertEquals(20, h.quantity)                   // 10 * 2
        assertEquals(2000.0, h.totalInvested, 0.001)   // unchanged
        assertEquals(100.0, h.avgBuyPrice, 0.001)       // 2000 / 20
        assertEquals(2000.0, h.currentValue, 0.001)     // 20 * 100
    }

    // ────────────────── COMBINED SCENARIOS ──────────────────

    @Test
    fun `buy-sell-dividend-bonus full lifecycle`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 20, pricePerShare = 50.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 5, pricePerShare = 70.0, date = 2L),
                Transaction(stockSymbol = "A", type = "DIVIDEND", quantity = 15, pricePerShare = 2.0, date = 3L),
                Transaction(stockSymbol = "A", type = "BONUS", quantity = 3, pricePerShare = 0.0, date = 4L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 60.0))
        ).first()

        val h = result[0]
        // After BUY: qty=20, invested=1000
        // After SELL 5: avgCost=50, invested=1000-250=750, qty=15
        // After DIVIDEND: qty stays 15, divs=15*2=30
        // After BONUS +3: qty=18, invested=750
        assertEquals(18, h.quantity)
        assertEquals(750.0, h.totalInvested, 0.001)
        assertEquals(30.0, h.totalDividends, 0.001)
        assertEquals(41.67, h.avgBuyPrice, 0.01) // 750/18
        assertEquals(1080.0, h.currentValue, 0.001) // 18 * 60
        assertEquals(330.0, h.profitLossAmount, 0.001) // 1080 - 750
        // Realized P/L: (70 - 50) * 5 = 100
        assertEquals(100.0, h.realizedProfitLoss, 0.001)
        // Total return: 330 + 100 + 30 = 460
        assertEquals(460.0, h.totalReturn, 0.001)
    }

    @Test
    fun `multiple stocks are computed independently`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "B", type = "BUY", quantity = 5, pricePerShare = 200.0, date = 1L)
            ),
            listOf(
                Stock("A", "Stock A", "Tech", currentPrice = 120.0),
                Stock("B", "Stock B", "Finance", currentPrice = 180.0)
            )
        ).first()

        assertEquals(2, result.size)
        val a = result.find { it.stockSymbol == "A" }!!
        val b = result.find { it.stockSymbol == "B" }!!

        assertEquals(10, a.quantity)
        assertEquals(1000.0, a.totalInvested, 0.001)
        assertEquals(200.0, a.profitLossAmount, 0.001) // (10*120) - 1000

        assertEquals(5, b.quantity)
        assertEquals(1000.0, b.totalInvested, 0.001)
        assertEquals(-100.0, b.profitLossAmount, 0.001) // (5*180) - 1000
    }

    @Test
    fun `edit scenario - BUY Q10 P250 then conceptually edited to P285`() = runTest {
        // This simulates the user's scenario: after editing the BUY to P285,
        // the transaction list should only have the edited version.
        // If currentPrice was also updated to 285, P/L should be 0.
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 285.0, date = 1L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 285.0))
        ).first()

        val h = result[0]
        assertEquals(10, h.quantity)
        assertEquals(2850.0, h.totalInvested, 0.001)
        assertEquals(285.0, h.avgBuyPrice, 0.001)
        assertEquals(2850.0, h.currentValue, 0.001)
        assertEquals(0.0, h.profitLossAmount, 0.001) // No loss!
        assertEquals(0.0, h.profitLossPercentage, 0.001)
        assertEquals(0.0, h.realizedProfitLoss, 0.001)
    }

    @Test
    fun `no transactions produces empty list`() = runTest {
        val result = compute(
            emptyList(),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 100.0))
        ).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `transaction for unknown stock is skipped`() = runTest {
        val result = compute(
            listOf(Transaction(stockSymbol = "UNKNOWN", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L)),
            emptyList() // No stocks in DB
        ).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `totalReturn combines unrealized realized and dividends`() = runTest {
        val result = compute(
            listOf(
                Transaction(stockSymbol = "A", type = "BUY", quantity = 10, pricePerShare = 100.0, date = 1L),
                Transaction(stockSymbol = "A", type = "SELL", quantity = 5, pricePerShare = 130.0, date = 2L),
                Transaction(stockSymbol = "A", type = "DIVIDEND", quantity = 5, pricePerShare = 10.0, date = 3L)
            ),
            listOf(Stock("A", "Stock A", "Tech", currentPrice = 120.0))
        ).first()

        val h = result[0]
        assertEquals(5, h.quantity)
        assertEquals(500.0, h.totalInvested, 0.001)
        assertEquals(600.0, h.currentValue, 0.001) // 5 * 120
        assertEquals(100.0, h.profitLossAmount, 0.001) // 600 - 500
        assertEquals(150.0, h.realizedProfitLoss, 0.001) // (130-100)*5
        assertEquals(50.0, h.totalDividends, 0.001) // 5*10
        // totalReturn = 100 + 150 + 50 = 300
        assertEquals(300.0, h.totalReturn, 0.001)
    }
}
