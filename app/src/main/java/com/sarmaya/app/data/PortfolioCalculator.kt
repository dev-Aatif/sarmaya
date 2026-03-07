package com.sarmaya.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

object PortfolioCalculator {
    fun getEventSourcedHoldings(
        transactionsFlow: Flow<List<Transaction>>,
        stocksFlow: Flow<List<Stock>>
    ): Flow<List<ComputedHolding>> {
        return combine(transactionsFlow, stocksFlow) { transactions, stocks ->
            val stockMap = stocks.associateBy { it.symbol }
            val holdingsMap = mutableMapOf<String, ComputedHolding>()
            
            transactions.groupBy { it.stockSymbol }.forEach { (symbol, txList) ->
                val stock = stockMap[symbol] ?: return@forEach
                var qty = 0
                var invested = 0.0
                var divs = 0.0
                
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
                        "SPLIT" -> {
                            qty *= tx.quantity
                        }
                        "SELL" -> {
                            if (qty > 0) {
                                val avgCost = invested / qty
                                qty -= tx.quantity
                                // Proportional reduction of cost basis, floored at 0
                                invested = maxOf(0.0, invested - (tx.quantity * avgCost))
                            } else {
                                qty -= tx.quantity
                            }
                        }
                        "DIVIDEND" -> {
                            // Dividend payout is based on chronological owned quantity, not transaction input quantity
                            divs += (qty * tx.pricePerShare)
                        }
                    }
                    if (qty <= 0) {
                        qty = 0
                        invested = 0.0
                    }
                }
                
                if (qty > 0 || invested > 0.0 || divs > 0.0) {
                    holdingsMap[symbol] = ComputedHolding(
                        stockSymbol = symbol,
                        name = stock.name,
                        sector = stock.sector,
                        currentPrice = stock.currentPrice,
                        quantity = qty,
                        totalInvested = invested,
                        totalDividends = divs
                    )
                }
            }
            holdingsMap.values.toList().sortedBy { it.stockSymbol }
        }
    }
}
