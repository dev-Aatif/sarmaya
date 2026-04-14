package com.sarmaya.app.network

import android.util.Log
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.StockQuoteCache
import com.sarmaya.app.data.StockQuoteCacheDao
import com.sarmaya.app.network.api.PsxApi
import com.sarmaya.app.network.api.PsxTerminalApi
import com.sarmaya.app.network.api.PsxIndex
import com.sarmaya.app.network.api.PsxTerminalTick
import com.sarmaya.app.network.api.YahooFinanceApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockDataRepository(
    private val yahooApi: YahooFinanceApi,
    private val psxApi: PsxApi,
    private val psxTerminalApi: PsxTerminalApi,
    private val stockDao: StockDao,
    private val quoteCacheDao: StockQuoteCacheDao,
    private val connectivityChecker: ConnectivityChecker
) {
    companion object {
        private const val TAG = "StockDataRepo"
        private const val CACHE_FRESHNESS_MS = 5 * 60 * 1000L
        const val PSX_SUFFIX = ".KA"
    }

    private fun toYahooSymbol(psxSymbol: String): String =
        if (psxSymbol.endsWith(PSX_SUFFIX)) psxSymbol else "$psxSymbol$PSX_SUFFIX"

    private fun toPsxSymbol(yahooSymbol: String): String =
        yahooSymbol.removeSuffix(PSX_SUFFIX)

    suspend fun getSymbols(): Result<List<SymbolSearchResult>> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        try {
            val response = psxTerminalApi.getSymbols()
            val symbols = response.data
            // Side effect: Populate local Stock table while we have the full list
            if (symbols.isNotEmpty()) {
                val stockEntities = symbols.map { symbol ->
                    com.sarmaya.app.data.Stock(
                        symbol = symbol,
                        name = symbol, // Fallback as new API doesn't provide name here
                        sector = "Other",
                        currentPrice = 0.0,
                        priceUpdatedAt = System.currentTimeMillis()
                    )
                }
                stockDao.insertStocks(stockEntities)
            }
            Result.success(symbols.map { 
                SymbolSearchResult(it, it, "PSX", "REG")
            })
        } catch (e: Exception) {
            Log.e(TAG, "getSymbols failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncAllSymbols(): Result<Unit> = withContext(Dispatchers.IO) {
        getSymbols().map { Unit }
    }

    suspend fun getQuote(psxSymbol: String): Result<UnifiedQuote> = withContext(Dispatchers.IO) {
        val cached = quoteCacheDao.getCache(psxSymbol)
        if (cached != null && isCacheFresh(cached.cachedAt)) {
            return@withContext Result.success(cached.toUnifiedQuote())
        }

        if (connectivityChecker.isOnline()) {
            try {
                val response = psxTerminalApi.getStockTick(psxSymbol)
                val tick = response.data
                val quote = UnifiedQuote(
                    symbol = tick.symbol,
                    price = tick.price,
                    change = tick.change,
                    changePercent = tick.changePercent,
                    volume = tick.volume,
                    dayHigh = tick.high,
                    dayLow = tick.low,
                    open = 0.0,
                    previousClose = tick.price - tick.change,
                    marketState = tick.state,
                    trades = tick.trades,
                    value = tick.value.toLong()
                )
                cacheQuote(quote)
                stockDao.updatePrice(psxSymbol, quote.price)
                return@withContext Result.success(quote)
            } catch (e: Exception) {
                Log.w(TAG, "PSX Terminal tick failed for $psxSymbol: ${e.message}, trying fallback")
            }
        }

        // Try Yahoo Fallback
        if (connectivityChecker.isOnline()) {
            try {
                val response = yahooApi.getQuotes(toYahooSymbol(psxSymbol))
                val quoteData = response.quoteResponse?.result?.firstOrNull()
                if (quoteData != null && quoteData.regularMarketPrice != null) {
                    val quote = UnifiedQuote(
                        symbol = psxSymbol,
                        price = quoteData.regularMarketPrice,
                        change = quoteData.regularMarketChange ?: 0.0,
                        changePercent = quoteData.regularMarketChangePercent ?: 0.0,
                        volume = quoteData.regularMarketVolume ?: 0L,
                        dayHigh = quoteData.regularMarketDayHigh ?: 0.0,
                        dayLow = quoteData.regularMarketDayLow ?: 0.0,
                        open = quoteData.regularMarketOpen ?: 0.0,
                        previousClose = quoteData.regularMarketPreviousClose ?: 0.0
                    )
                    cacheQuote(quote)
                    stockDao.updatePrice(psxSymbol, quote.price)
                    return@withContext Result.success(quote)
                }
            } catch (e: Exception) { }
        }

        if (cached != null) return@withContext Result.success(cached.toUnifiedQuote())
        
        Result.failure(Exception("No data available"))
    }

    suspend fun getBatchQuotes(psxSymbols: List<String>): Result<List<UnifiedQuote>> = withContext(Dispatchers.IO) {
        if (psxSymbols.isEmpty()) return@withContext Result.success(emptyList())

        if (connectivityChecker.isOnline()) {
            val psxResult = syncPsxQuotes() // Reuses the sync function that fetches all live quotes
            if (psxResult.isSuccess) {
                val cached = psxSymbols.mapNotNull { symbol ->
                    quoteCacheDao.getCache(symbol)?.toUnifiedQuote()
                }
                if (cached.isNotEmpty()) return@withContext Result.success(cached)
            }
        }

        val cached = psxSymbols.mapNotNull { symbol ->
            quoteCacheDao.getCache(symbol)?.toUnifiedQuote()
        }
        if (cached.isNotEmpty()) return@withContext Result.success(cached)
        Result.failure(Exception("No data available for batch quote"))
    }

    suspend fun syncPsxQuotes(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        
        // 1. Terminal App (Stats for gainers/losers/active)
        try {
            val response = psxTerminalApi.getMarketStats()
            val stats = response.data
            val allTicks = mutableListOf<UnifiedQuote>()
            
            val convert = { tick: PsxTerminalTick ->
                UnifiedQuote(
                    symbol = tick.symbol, price = tick.price, change = tick.change,
                    changePercent = tick.changePercent, volume = tick.volume, dayHigh = tick.high,
                    dayLow = tick.low, open = 0.0, previousClose = tick.price - tick.change,
                    trades = tick.trades, value = tick.value.toLong(), marketState = tick.state
                )
            }
            
            allTicks.addAll(stats.topGainers.map(convert))
            allTicks.addAll(stats.topLosers.map(convert))
            allTicks.addAll(stats.volumeLeaders.map(convert))
            
            if (allTicks.isNotEmpty()) {
                bulkCacheAndUpdate(allTicks)
                return@withContext Result.success(Unit)
            }
        } catch (e: Exception) {}

        // Fallback to DPS
        try {
            val response = psxApi.getLiveQuotes()
            val stocks = response.stocks
            if (!stocks.isNullOrEmpty()) {
                val quotes = stocks.map { psxStock ->
                    UnifiedQuote(
                        symbol = psxStock.scrip, price = psxStock.current, change = psxStock.change,
                        changePercent = psxStock.changep, volume = psxStock.vol, dayHigh = psxStock.high ?: 0.0,
                        dayLow = psxStock.low ?: 0.0, open = psxStock.open ?: 0.0, previousClose = psxStock.prev ?: 0.0
                    )
                }
                bulkCacheAndUpdate(quotes)
                return@withContext Result.success(Unit)
            }
        } catch (e: Exception) {}

        Result.failure(Exception("All PSX data sources unavailable"))
    }

    suspend fun getHistoricalData(psxSymbol: String, range: ChartRange, interval: ChartInterval? = null): Result<List<PricePoint>> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))

        try {
            // Attempt V2 Terminal Klines
            val tf = interval?.apiParam ?: "1d"
            val response = psxTerminalApi.getKlines(psxSymbol, tf)
            val klines = response.data
            val points = klines.map { candle -> 
                PricePoint(
                    timestamp = candle[0].toLong() * 1000,
                    open = candle[1],
                    high = candle[2],
                    low = candle[3],
                    close = candle[4],
                    volume = candle[5].toLong()
                )
            }
            if (points.isNotEmpty()) return@withContext Result.success(points)
        } catch (e: Exception) { }

        // Fallback to Yahoo
        try {
            val response = yahooApi.getChart(toYahooSymbol(psxSymbol), range.yahooParam, interval?.apiParam ?: ChartInterval.ONE_DAY.apiParam)
            val chartData = response.chart?.result?.firstOrNull()
            if (chartData != null && !chartData.timestamp.isNullOrEmpty()) {
                val points = chartData.timestamp.mapIndexedNotNull { index, ts ->
                    val ohlc = chartData.indicators?.quote?.firstOrNull()
                    PricePoint(
                        timestamp = ts * 1000,
                        open = ohlc?.open?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        high = ohlc.high?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        low = ohlc.low?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        close = ohlc.close?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        volume = ohlc.volume?.getOrNull(index) ?: 0L
                    )
                }
                return@withContext Result.success(points)
            }
        } catch (e: Exception) {}

        Result.failure(Exception("No chart data available for $psxSymbol"))
    }

    suspend fun getCompanyProfile(psxSymbol: String): Result<CompanyProfile> = withContext(Dispatchers.IO) {
        if (connectivityChecker.isOnline()) {
            try {
                val companyRes = psxTerminalApi.getCompany(psxSymbol)
                val company = companyRes.data
                var fundas: com.sarmaya.app.network.api.PsxTerminalFundamentals? = null
                try { 
                    val fundasRes = psxTerminalApi.getFundamentals(psxSymbol)
                    fundas = fundasRes.data
                } catch (e: Exception) {}
                
                return@withContext Result.success(
                    CompanyProfile(
                        symbol = psxSymbol, name = company.name, description = company.description,
                        sector = company.sector, industry = company.industry, website = "", phone = "",
                        country = "Pakistan", logoUrl = company.profileImage,
                        marketCap = fundas?.marketCap ?: 0L, peRatio = fundas?.peRatio ?: 0.0,
                        eps = fundas?.eps ?: 0.0, beta = 0.0, weekHigh52 = 0.0, weekLow52 = 0.0,
                        dividendYield = fundas?.dividendYield ?: 0.0, earningsDate = ""
                    )
                )
            } catch (e: Exception) { }

            try {
                val response = yahooApi.getQuoteSummary(toYahooSymbol(psxSymbol))
                val modules = response.quoteSummary?.result?.firstOrNull()
                if (modules != null) {
                    val profile = modules.assetProfile
                    val summary = modules.summaryDetail
                    val price = modules.price
                    return@withContext Result.success(
                        CompanyProfile(
                            symbol = psxSymbol, name = price?.longName ?: psxSymbol, description = profile?.longBusinessSummary ?: "",
                            sector = profile?.sector ?: "", industry = profile?.industry ?: "", website = profile?.website ?: "",
                            phone = profile?.phone ?: "", country = profile?.country ?: "", logoUrl = "",
                            marketCap = summary?.marketCap?.raw?.toLong() ?: 0L, peRatio = summary?.trailingPE?.raw ?: 0.0,
                            eps = price?.eps?.raw ?: 0.0, beta = modules.keyStats?.beta?.raw ?: 0.0,
                            weekHigh52 = summary?.fiftyTwoWeekHigh?.raw ?: 0.0, weekLow52 = summary?.fiftyTwoWeekLow?.raw ?: 0.0,
                            dividendYield = summary?.dividendYield?.raw ?: 0.0, earningsDate = ""
                        )
                    )
                }
            } catch (e: Exception) {}
        }
        
        Result.success(
            CompanyProfile(psxSymbol, psxSymbol, "Unavailable", "", "", "", "", "Pakistan", "", 0L, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "")
        )
    }

    suspend fun getIndices(): Result<List<PsxIndex>> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        
        // Try DPS first (Legacy)
        try {
            val response = psxApi.getIndices()
            val indices = response.indices
            if (!indices.isNullOrEmpty()) return@withContext Result.success(indices)
        } catch (e: Exception) { }

        // Fallback to Terminal API for major indices
        try {
            val majorIndices = listOf("KSE100", "KSE30", "KMI30", "ALLSHR")
            val indices = majorIndices.map { symbol ->
                val tick = psxTerminalApi.getIndexTick(symbol).data
                PsxIndex(
                    name = symbol,
                    current = tick.price,
                    change = tick.change,
                    changep = tick.changePercent * 100, // API returns decimal, model expects percentage
                    high = tick.high,
                    low = tick.low,
                    prev = tick.price - tick.change
                )
            }
            if (indices.isNotEmpty()) return@withContext Result.success(indices)
        } catch (e: Exception) {
            Log.e(TAG, "getIndices Fallback failed: ${e.message}")
        }
        
        Result.failure(Exception("No index data available"))
    }

    suspend fun getMarketStatus(): Result<String> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        try {
            val response = psxTerminalApi.getStatus()
            Result.success(response.marketState ?: com.sarmaya.app.util.MarketHoursUtil.getMarketState())
        } catch (e: Exception) {
            Result.success(com.sarmaya.app.util.MarketHoursUtil.getMarketState())
        }
    }
    
    // Additional Terminal API queries exposed directly
    suspend fun getMarketStats() = runCatching { psxTerminalApi.getMarketStats() }
    suspend fun getMarketBreadth() = runCatching { psxTerminalApi.getMarketBreadth() }
    suspend fun getSectorStats() = runCatching { psxTerminalApi.getSectorStats() }
    suspend fun getDividends(symbol: String) = runCatching { psxTerminalApi.getDividends(symbol) }
    suspend fun getToWatch() = runCatching { psxTerminalApi.getToWatch() }

    private suspend fun bulkCacheAndUpdate(quotes: List<UnifiedQuote>) {
        try {
            val now = System.currentTimeMillis()
            val cacheEntities = quotes.map { quote ->
                StockQuoteCache(
                    symbol = quote.symbol, price = quote.price, change = quote.change,
                    changePercent = quote.changePercent, volume = quote.volume,
                    high = quote.dayHigh, low = quote.dayLow, cachedAt = now
                )
            }
            quoteCacheDao.upsertAll(cacheEntities)
            
            // Check for missing stocks and insert them as well
            val symbols = quotes.map { it.symbol }
            val existing = stockDao.getStocksSync(symbols).map { it.symbol }.toSet()
            val missing = quotes.filter { it.symbol !in existing }
            
            if (missing.isNotEmpty()) {
                val newStocks = missing.map { 
                    com.sarmaya.app.data.Stock(
                        symbol = it.symbol,
                        name = it.symbol, // We don't have the full name here, but better than nothing
                        sector = "Other",
                        currentPrice = it.price,
                        priceUpdatedAt = now
                    )
                }
                stockDao.insertStocks(newStocks)
            }

            val priceUpdates = quotes.associate { it.symbol to it.price }
            stockDao.updatePrices(priceUpdates)
        } catch (e: Exception) {
            Log.e(TAG, "bulkCacheAndUpdate failed: ${e.message}")
        }
    }

    private suspend fun cacheQuote(quote: UnifiedQuote) {
        try {
            quoteCacheDao.upsert(StockQuoteCache(
                symbol = quote.symbol, price = quote.price, change = quote.change,
                changePercent = quote.changePercent, volume = quote.volume,
                high = quote.dayHigh, low = quote.dayLow, cachedAt = System.currentTimeMillis()
            ))
        } catch (e: Exception) {}
    }

    private fun isCacheFresh(cachedAt: Long): Boolean = 
        System.currentTimeMillis() - cachedAt < CACHE_FRESHNESS_MS

    suspend fun getPeers(psxSymbol: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val stocks = stockDao.getStocksSync(listOf(psxSymbol))
            val stock = stocks.firstOrNull() ?: return@withContext Result.failure(Exception("Stock not found"))
            val peers = stockDao.getStocksBySectorSync(stock.sector)
                .filter { it.symbol != psxSymbol }.take(10).map { it.symbol }
            Result.success(peers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun StockQuoteCache.toUnifiedQuote() = UnifiedQuote(
    symbol = symbol, price = price, change = change, changePercent = changePercent,
    volume = volume, dayHigh = high, dayLow = low, open = 0.0, previousClose = 0.0, timestamp = cachedAt
)
