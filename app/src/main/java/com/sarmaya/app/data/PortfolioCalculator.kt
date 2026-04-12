package com.sarmaya.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

object PortfolioCalculator {
    fun getEventSourcedHoldings(
        transactionsFlow: Flow<List<Transaction>>,
        stocksFlow: Flow<List<Stock>>,
        quotesFlow: Flow<List<StockQuoteCache>> = kotlinx.coroutines.flow.flowOf(emptyList())
    ): Flow<List<ComputedHolding>> {
        return combine(transactionsFlow, stocksFlow, quotesFlow) { transactions, stocks, quotes ->
            val stockMap = stocks.associateBy { it.symbol }
            val quoteMap = quotes.associateBy { it.symbol }
            val holdingsMap = mutableMapOf<String, ComputedHolding>()
            
            transactions.groupBy { it.stockSymbol }.forEach { (symbol, txList) ->
                val stock = stockMap[symbol] ?: return@forEach
                val quote = quoteMap[symbol]
                val currentPrice = quote?.price ?: stock.currentPrice
                var qty = 0
                var invested = 0.0
                var divs = 0.0
                var realizedPL = 0.0
                
                // Chronological replay to properly account for re-entry and cost-drag
                txList.sortedBy { it.date }.forEach { tx ->
                    when (tx.type) {
                        "BUY" -> {
                            qty += tx.quantity
                            invested += (tx.quantity * tx.pricePerShare)
                        }
                        "BONUS" -> {
                            qty += tx.quantity
                        }
                        "SELL" -> {
                            if (qty > 0) {
                                val sellQty = minOf(tx.quantity, qty)
                                val avgCost = invested / qty
                                // Realized P/L = (sell price - avg cost) * quantity sold
                                realizedPL += (tx.pricePerShare - avgCost) * sellQty
                                qty -= sellQty
                                // Proportional reduction of cost basis, floored at 0
                                invested = maxOf(0.0, invested - (sellQty * avgCost))
                            }
                        }
                        "DIVIDEND" -> {
                            // Dividend payout uses the user-entered quantity of shares paying the dividend
                            divs += (tx.quantity * tx.pricePerShare)
                        }
                        "SPLIT" -> {
                            // pricePerShare stores the split factor (e.g. 2 for 2:1, 0.5 for 1:2)
                            val factor = if (tx.pricePerShare <= 0.0) 1.0 else tx.pricePerShare
                            qty = (qty * factor).toInt()
                            // Invested value (cost basis) remains the same, but price per share reduces
                        }
                    }
                    if (qty <= 0) {
                        qty = 0
                        invested = 0.0
                    }
                }
                
                if (qty > 0 || invested > 0.0 || divs > 0.0 || realizedPL != 0.0) {
                    holdingsMap[symbol] = ComputedHolding(
                        stockSymbol = symbol,
                        name = stock.name,
                        sector = stock.sector,
                        currentPrice = currentPrice,
                        quantity = qty,
                        totalInvested = invested,
                        totalDividends = divs,
                        realizedProfitLoss = realizedPL
                    )
                }
            }
            holdingsMap.values.toList().sortedBy { it.stockSymbol }
        }
    }

    /**
     * Synchronous version for background workers/snapshots
     */
    fun computeSnapshotSynchronous(
        transactions: List<Transaction>,
        stocks: List<Stock>
    ): List<ComputedHolding> {
        val stockMap = stocks.associateBy { it.symbol }
        val holdingsMap = mutableMapOf<String, ComputedHolding>()
        
        transactions.groupBy { it.stockSymbol }.forEach { (symbol, txList) ->
            val stock = stockMap[symbol] ?: return@forEach
            var qty = 0
            var invested = 0.0
            var divs = 0.0
            var realizedPL = 0.0
            
            txList.sortedBy { it.date }.forEach { tx ->
                when (tx.type) {
                    "BUY" -> {
                        qty += tx.quantity
                        invested += (tx.quantity * tx.pricePerShare)
                    }
                    "BONUS" -> qty += tx.quantity
                    "SELL" -> {
                        if (qty > 0) {
                            val avgCost = invested / qty
                            val sellQty = minOf(tx.quantity, qty)
                            realizedPL += (tx.pricePerShare - avgCost) * sellQty
                            qty -= sellQty
                            invested = maxOf(0.0, invested - (sellQty * avgCost))
                        }
                    }
                    "DIVIDEND" -> divs += (tx.quantity * tx.pricePerShare)
                    "SPLIT" -> {
                        val factor = if (tx.pricePerShare <= 0.0) 1.0 else tx.pricePerShare
                        qty = (qty * factor).toInt()
                    }
                }
            }
            
            if (qty > 0 || invested > 0.0 || divs > 0.0 || realizedPL != 0.0) {
                holdingsMap[symbol] = ComputedHolding(
                    stockSymbol = symbol,
                    name = stock.name,
                    sector = stock.sector,
                    currentPrice = stock.currentPrice,
                    quantity = qty,
                    totalInvested = invested,
                    totalDividends = divs,
                    realizedProfitLoss = realizedPL
                )
            }
        }
        return holdingsMap.values.toList()
    }
}
