package com.sarmaya.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

data class PortfolioState(
    val holdings: List<ComputedHolding>,
    val uninvestedCash: Double
)

object PortfolioCalculator {
    fun getEventSourcedHoldings(
        transactionsFlow: Flow<List<Transaction>>,
        stocksFlow: Flow<List<Stock>>,
        quotesFlow: Flow<List<StockQuoteCache>> = kotlinx.coroutines.flow.flowOf(emptyList())
    ): Flow<PortfolioState> {
        return combine(transactionsFlow, stocksFlow, quotesFlow) { transactions, stocks, quotes ->
            val stockMap = stocks.associateBy { it.symbol }
            val quoteMap = quotes.associateBy { it.symbol }
            val holdingsMap = mutableMapOf<String, ComputedHolding>()
            
            var cash = java.math.BigDecimal.ZERO
            
            transactions.sortedBy { it.date }.forEach { tx ->
                val txPrice = tx.pricePerShare.toSafeBigDecimal()
                val txQuantity = java.math.BigDecimal(tx.quantity)
                val txCommission = tx.commissionAmount.toSafeBigDecimal()
                
                when (tx.type) {
                    "DEPOSIT" -> cash = cash.add(txPrice)
                    "WITHDRAWAL" -> cash = cash.subtract(txPrice)
                    "DIVIDEND" -> cash = cash.add(txPrice)
                    "BUY" -> cash = cash.subtract(txPrice.multiply(txQuantity)).subtract(txCommission)
                    "SELL" -> cash = cash.add(txPrice.multiply(txQuantity)).subtract(txCommission)
                }
            }

            transactions.filter { it.type !in listOf("DEPOSIT", "WITHDRAWAL") }.groupBy { it.stockSymbol }.forEach { (symbol, txList) ->
                val stock = stockMap[symbol] ?: return@forEach
                val quote = quoteMap[symbol]
                val currentPrice = quote?.price ?: stock.currentPrice
                
                val holding = calculateHolding(symbol, stock.name, stock.sector, currentPrice, txList)
                if (holding != null) {
                    holdingsMap[symbol] = holding
                }
            }
            PortfolioState(
                holdings = holdingsMap.values.toList().sortedBy { it.stockSymbol },
                uninvestedCash = cash.toDouble()
            )
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Synchronous version for background workers/snapshots
     */
    fun computeSnapshotSynchronous(
        transactions: List<Transaction>,
        stocks: List<Stock>
    ): PortfolioState {
        val stockMap = stocks.associateBy { it.symbol }
        val holdingsMap = mutableMapOf<String, ComputedHolding>()
        
        var cash = java.math.BigDecimal.ZERO
        transactions.sortedBy { it.date }.forEach { tx ->
            val txPrice = tx.pricePerShare.toSafeBigDecimal()
            val txQuantity = java.math.BigDecimal(tx.quantity)
            val txCommission = tx.commissionAmount.toSafeBigDecimal()
            
            when (tx.type) {
                "DEPOSIT" -> cash = cash.add(txPrice)
                "WITHDRAWAL" -> cash = cash.subtract(txPrice)
                "DIVIDEND" -> cash = cash.add(txPrice)
                "BUY" -> cash = cash.subtract(txPrice.multiply(txQuantity)).subtract(txCommission)
                "SELL" -> cash = cash.add(txPrice.multiply(txQuantity)).subtract(txCommission)
            }
        }

        transactions.filter { it.type !in listOf("DEPOSIT", "WITHDRAWAL") }.groupBy { it.stockSymbol }.forEach { (symbol, txList) ->
            val stock = stockMap[symbol] ?: return@forEach
            val holding = calculateHolding(symbol, stock.name, stock.sector, stock.currentPrice, txList)
            if (holding != null) {
                holdingsMap[symbol] = holding
            }
        }
        return PortfolioState(
            holdings = holdingsMap.values.toList().sortedBy { it.stockSymbol },
            uninvestedCash = cash.toDouble()
        )
    }

    private fun calculateHolding(
        symbol: String,
        name: String,
        sector: String,
        currentPrice: Double,
        txList: List<Transaction>
    ): ComputedHolding? {
        var qty = 0
        var invested = java.math.BigDecimal.ZERO
        var divs = java.math.BigDecimal.ZERO
        var realizedPL = java.math.BigDecimal.ZERO

        txList.sortedBy { it.date }.forEach { tx ->
            val txQuantity = java.math.BigDecimal(tx.quantity)
            val txPrice = tx.pricePerShare.toSafeBigDecimal()
            val txCommission = tx.commissionAmount.toSafeBigDecimal()

            when (tx.type) {
                "BUY" -> {
                    qty += tx.quantity
                    val commission = txCommission
                    invested = invested.add(txQuantity.multiply(txPrice)).add(commission)
                }
                "BONUS" -> {
                    qty += tx.quantity
                }
                "SELL" -> {
                    if (qty > 0) {
                        val sellQtyInt = minOf(tx.quantity, qty)
                        val sellQty = java.math.BigDecimal(sellQtyInt)
                        val qtyDecimal = java.math.BigDecimal(qty)
                        
                        val avgCost = invested.safeDivide(qtyDecimal)
                        val commission = txCommission
                        
                        val profitFromShares = txPrice.subtract(avgCost).multiply(sellQty)
                        realizedPL = realizedPL.add(profitFromShares).subtract(commission)
                        
                        qty -= sellQtyInt
                        val costBasisReduction = sellQty.multiply(avgCost)
                        invested = invested.subtract(costBasisReduction).max(java.math.BigDecimal.ZERO)
                    }
                }
                "DIVIDEND" -> {
                    divs = divs.add(txPrice)
                }
                "SPLIT" -> {
                    val factor = tx.splitRatio ?: 1.0
                    qty = Math.round(qty * factor).toInt()
                }
            }
            if (qty <= 0) {
                qty = 0
                invested = java.math.BigDecimal.ZERO
            }
        }

        if (qty > 0 || invested > java.math.BigDecimal.ZERO || divs > java.math.BigDecimal.ZERO || realizedPL.compareTo(java.math.BigDecimal.ZERO) != 0) {
            return ComputedHolding(
                stockSymbol = symbol,
                name = name,
                sector = sector,
                currentPrice = currentPrice,
                quantity = qty,
                totalInvested = invested.toDouble(),
                totalDividends = divs.toDouble(),
                realizedProfitLoss = realizedPL.toDouble()
            )
        }
        return null
    }
}
