package com.sarmaya.app.network

import android.util.Log
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.StockQuoteCache
import com.sarmaya.app.data.StockQuoteCacheDao
import com.sarmaya.app.network.api.PsxApi
import com.sarmaya.app.network.api.PsxTerminalApi
import com.sarmaya.app.network.api.PsxIndex
import com.sarmaya.app.network.api.YahooFinanceApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central data repository implementing multi-tier fallback:
 *   PSX DPS → PSX Terminal → Yahoo Finance → local cache
 *
 * All methods return [Result] so callers can gracefully handle failures.
 * Successfully fetched data is always cached to Room for offline access.
 */
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
        /** Cache is considered fresh if < 5 minutes old */
        private const val CACHE_FRESHNESS_MS = 5 * 60 * 1000L
        /** PSX Yahoo Finance suffix */
        const val PSX_SUFFIX = ".KA"
    }

    /**
     * Converts a PSX local symbol (e.g. "OGDC") to Yahoo Finance format ("OGDC.KA").
     * If already has suffix, returns as-is.
     */
    fun toYahooSymbol(psxSymbol: String): String {
        return if (psxSymbol.endsWith(PSX_SUFFIX)) psxSymbol else "$psxSymbol$PSX_SUFFIX"
    }

    /**
     * Strips the Yahoo suffix to get the local PSX symbol.
     */
    fun toPsxSymbol(yahooSymbol: String): String {
        return yahooSymbol.removeSuffix(PSX_SUFFIX)
    }

    // ─── Quotes ───

    /**
     * Fetch live quote for a single symbol.
     * Strategy: cache first (if fresh) → sync PSX quotes → Yahoo API → stale cache → placeholder.
     */
    suspend fun getQuote(psxSymbol: String): Result<UnifiedQuote> = withContext(Dispatchers.IO) {
        // 1. Check fresh cache
        val cached = quoteCacheDao.getCache(psxSymbol)
        if (cached != null && isCacheFresh(cached.cachedAt)) {
            return@withContext Result.success(cached.toUnifiedQuote())
        }

        // 2. Try PSX data sync (DPS + Terminal fallback)
        if (connectivityChecker.isOnline()) {
            try {
                val psxResult = syncPsxQuotes()
                if (psxResult.isSuccess) {
                    val freshCached = quoteCacheDao.getCache(psxSymbol)
                    if (freshCached != null) {
                        return@withContext Result.success(freshCached.toUnifiedQuote())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "PSX sync failed for $psxSymbol: ${e.message}")
            }
        }

        // 3. Fallback to Yahoo Finance API
        if (connectivityChecker.isOnline()) {
            try {
                val yahooSymbol = toYahooSymbol(psxSymbol)
                val response = yahooApi.getQuotes(yahooSymbol)
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
            } catch (e: Exception) {
                Log.w(TAG, "Yahoo quote failed for $psxSymbol: ${e.message}")
            }
        }

        // 4. Return stale cache if available
        if (cached != null) {
            Log.i(TAG, "Returning stale cache for $psxSymbol (age: ${(System.currentTimeMillis() - cached.cachedAt) / 1000}s)")
            return@withContext Result.success(cached.toUnifiedQuote())
        }

        // 5. Ultimate fallback: last known price from Stock table
        var lastKnownPrice: Double? = null
        try {
            val lastStock = stockDao.getStocksSync(listOf(psxSymbol)).firstOrNull()
            if (lastStock != null) {
                lastKnownPrice = lastStock.currentPrice
                return@withContext Result.success(
                    UnifiedQuote(
                        symbol = psxSymbol,
                        price = lastKnownPrice,
                        change = 0.0,
                        changePercent = 0.0,
                        volume = 0L,
                        dayHigh = 0.0,
                        dayLow = 0.0,
                        open = 0.0,
                        previousClose = 0.0
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to Stock table failed for $psxSymbol: ${e.message}")
        }

        // 6. Placeholder quote (prevents hard error on detail screen)
        Log.w(TAG, "No remote or cached data for $psxSymbol, returning placeholder quote")
        Result.success(
            UnifiedQuote(
                symbol = psxSymbol,
                price = lastKnownPrice ?: 0.0,
                change = 0.0,
                changePercent = 0.0,
                volume = 0L,
                dayHigh = 0.0,
                dayLow = 0.0,
                open = 0.0,
                previousClose = 0.0
            )
        )
    }

    /**
     * Fetch live quotes for multiple symbols in one batch.
     */
    suspend fun getBatchQuotes(psxSymbols: List<String>): Result<List<UnifiedQuote>> = withContext(Dispatchers.IO) {
        if (psxSymbols.isEmpty()) return@withContext Result.success(emptyList())

        // Try PSX sync first (DPS + Terminal fallback)
        if (connectivityChecker.isOnline()) {
            val psxResult = syncPsxQuotes()
            if (psxResult.isSuccess) {
                val cached = psxSymbols.mapNotNull { symbol ->
                    quoteCacheDao.getCache(symbol)?.toUnifiedQuote()
                }
                if (cached.isNotEmpty()) return@withContext Result.success(cached)
            }
        }

        // Try Yahoo batch quote fallback
        if (connectivityChecker.isOnline()) {
            try {
                val yahooSymbols = psxSymbols.joinToString(",") { toYahooSymbol(it) }
                val response = yahooApi.getQuotes(yahooSymbols)
                val results = response.quoteResponse?.result
                if (!results.isNullOrEmpty()) {
                    val quotes = results.mapNotNull { quoteData ->
                        val symbol = quoteData.symbol?.let { toPsxSymbol(it) } ?: return@mapNotNull null
                        val price = quoteData.regularMarketPrice ?: return@mapNotNull null
                        UnifiedQuote(
                            symbol = symbol,
                            price = price,
                            change = quoteData.regularMarketChange ?: 0.0,
                            changePercent = quoteData.regularMarketChangePercent ?: 0.0,
                            volume = quoteData.regularMarketVolume ?: 0L,
                            dayHigh = quoteData.regularMarketDayHigh ?: 0.0,
                            dayLow = quoteData.regularMarketDayLow ?: 0.0,
                            open = quoteData.regularMarketOpen ?: 0.0,
                            previousClose = quoteData.regularMarketPreviousClose ?: 0.0
                        )
                    }
                    quotes.forEach { cacheQuote(it) }
                    quotes.forEach { stockDao.updatePrice(it.symbol, it.price) }
                    return@withContext Result.success(quotes)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Yahoo batch quote failed: ${e.message}")
            }
        }

        // Fallback: return cached data for each symbol
        val cached = psxSymbols.mapNotNull { symbol ->
            quoteCacheDao.getCache(symbol)?.toUnifiedQuote()
        }
        if (cached.isNotEmpty()) {
            return@withContext Result.success(cached)
        }

        Result.failure(Exception("No data available for batch quote"))
    }

    // ─── Historical Chart Data ───

    /**
     * Fetch historical price data for charting.
     * Uses Yahoo Finance (only source for historical charts).
     */
    suspend fun getHistoricalData(
        psxSymbol: String,
        range: ChartRange,
        interval: ChartInterval? = null
    ): Result<List<PricePoint>> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        val effectiveInterval = interval ?: getDefaultInterval(range)

        try {
            val yahooSymbol = toYahooSymbol(psxSymbol)
            val response = yahooApi.getChart(
                symbol = yahooSymbol,
                range = range.yahooParam,
                interval = effectiveInterval.yahooParam
            )

            val chartData = response.chart?.result?.firstOrNull()
            if (chartData != null && !chartData.timestamp.isNullOrEmpty()) {
                val timestamps = chartData.timestamp
                val ohlc = chartData.indicators?.quote?.firstOrNull()

                val points = timestamps.mapIndexedNotNull { index, ts ->
                    PricePoint(
                        timestamp = ts * 1000, // Yahoo returns seconds, we use millis
                        open = ohlc?.open?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        high = ohlc.high?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        low = ohlc.low?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        close = ohlc.close?.getOrNull(index) ?: return@mapIndexedNotNull null,
                        volume = ohlc.volume?.getOrNull(index) ?: 0L
                    )
                }
                return@withContext Result.success(points)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Yahoo chart failed for $psxSymbol: ${e.message}")
        }

        Result.failure(Exception("No chart data available for $psxSymbol"))
    }

    // ─── Company Profile ───

    /**
     * Fetch detailed company profile.
     * Strategy: PSX Terminal → Yahoo Finance → placeholder.
     * Always returns a profile (never fails hard) so the detail screen always renders.
     */
    suspend fun getCompanyProfile(psxSymbol: String): Result<CompanyProfile> = withContext(Dispatchers.IO) {
        // 1. Try PSX Terminal for company data + yields
        if (connectivityChecker.isOnline()) {
            try {
                val company = psxTerminalApi.getCompany(psxSymbol)
                var yields: com.sarmaya.app.network.api.PsxTerminalYields? = null
                try {
                    yields = psxTerminalApi.getYields(psxSymbol)
                } catch (e: Exception) {
                    Log.w(TAG, "PSX Terminal yields failed for $psxSymbol: ${e.message}")
                }

                return@withContext Result.success(
                    CompanyProfile(
                        symbol = psxSymbol,
                        name = company.effectiveName(),
                        description = company.description ?: "",
                        sector = company.sector ?: "",
                        industry = company.industry ?: "",
                        website = company.website ?: "",
                        phone = company.phone ?: "",
                        country = "Pakistan",
                        logoUrl = "",
                        marketCap = company.marketCap ?: yields?.marketCap ?: 0L,
                        peRatio = yields?.pe ?: 0.0,
                        eps = yields?.eps ?: 0.0,
                        beta = yields?.beta ?: 0.0,
                        weekHigh52 = yields?.high52w ?: 0.0,
                        weekLow52 = yields?.low52w ?: 0.0,
                        dividendYield = yields?.dividendYield ?: 0.0,
                        earningsDate = ""
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "PSX Terminal company failed for $psxSymbol: ${e.message}")
            }
        }

        // 2. Try Yahoo Finance for profile
        if (connectivityChecker.isOnline()) {
            try {
                val yahooSymbol = toYahooSymbol(psxSymbol)
                val response = yahooApi.getQuoteSummary(yahooSymbol)
                val modules = response.quoteSummary?.result?.firstOrNull()
                if (modules != null) {
                    val profile = modules.assetProfile
                    val stats = modules.keyStats
                    val summary = modules.summaryDetail
                    val price = modules.price

                    return@withContext Result.success(
                        CompanyProfile(
                            symbol = psxSymbol,
                            name = price?.longName ?: price?.shortName ?: psxSymbol,
                            description = profile?.longBusinessSummary ?: "",
                            sector = profile?.sector ?: "",
                            industry = profile?.industry ?: "",
                            website = profile?.website ?: "",
                            phone = profile?.phone ?: "",
                            country = profile?.country ?: "",
                            logoUrl = "",
                            marketCap = summary?.marketCap?.raw?.toLong() ?: price?.marketCap?.raw?.toLong() ?: 0L,
                            peRatio = summary?.trailingPE?.raw ?: 0.0,
                            eps = price?.eps?.raw ?: 0.0,
                            beta = stats?.beta?.raw ?: 0.0,
                            weekHigh52 = summary?.fiftyTwoWeekHigh?.raw ?: 0.0,
                            weekLow52 = summary?.fiftyTwoWeekLow?.raw ?: 0.0,
                            dividendYield = summary?.dividendYield?.raw ?: 0.0,
                            earningsDate = ""
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Yahoo profile failed for $psxSymbol: ${e.message}")
            }
        }

        // 3. Return placeholder profile (never fail — the detail screen will still render)
        Log.i(TAG, "Returning placeholder profile for $psxSymbol")
        val stock = try {
            stockDao.getStocksSync(listOf(psxSymbol)).firstOrNull()
        } catch (e: Exception) { null }
        
        Result.success(
            CompanyProfile(
                symbol = psxSymbol,
                name = stock?.name ?: psxSymbol,
                description = "Company profile data is currently unavailable. Please check your internet connection and try again.",
                sector = stock?.sector ?: "",
                industry = "",
                website = "",
                phone = "",
                country = "Pakistan",
                logoUrl = "",
                marketCap = 0L,
                peRatio = 0.0,
                eps = 0.0,
                beta = 0.0,
                weekHigh52 = 0.0,
                weekLow52 = 0.0,
                dividendYield = 0.0,
                earningsDate = ""
            )
        )
    }

    // ─── Peers (same-sector stocks from local DB) ───

    /**
     * Get peer companies from the same sector using local DB.
     */
    suspend fun getPeers(psxSymbol: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val stocks = stockDao.getStocksSync(listOf(psxSymbol))
            val stock = stocks.firstOrNull()
                ?: return@withContext Result.failure(Exception("Stock not found"))

            val peers = stockDao.getStocksBySectorSync(stock.sector)
                .filter { it.symbol != psxSymbol }
                .take(10)
                .map { it.symbol }

            Result.success(peers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync all live quotes from PSX.
     * Strategy: PSX DPS → PSX Terminal fallback.
     */
    suspend fun syncPsxQuotes(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        
        // 1. Try PSX DPS (original source)
        try {
            Log.d(TAG, "Attempting PSX DPS sync...")
            val response = psxApi.getLiveQuotes()
            val stocks = response.stocks
            if (!stocks.isNullOrEmpty()) {
                Log.i(TAG, "PSX DPS sync success: ${stocks.size} stocks")
                val quotes = stocks.map { psxStock ->
                    UnifiedQuote(
                        symbol = psxStock.scrip,
                        price = psxStock.current,
                        change = psxStock.change,
                        changePercent = psxStock.changep,
                        volume = psxStock.vol,
                        dayHigh = psxStock.high ?: 0.0,
                        dayLow = psxStock.low ?: 0.0,
                        open = psxStock.open ?: 0.0,
                        previousClose = psxStock.prev ?: 0.0
                    )
                }
                bulkCacheAndUpdate(quotes)
                return@withContext Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.w(TAG, "PSX DPS sync failed: ${e.javaClass.simpleName}: ${e.message}")
        }

        // 2. Try PSX Terminal (free fallback)
        try {
            Log.d(TAG, "Attempting PSX Terminal sync...")
            val terminalStocks = psxTerminalApi.getMarketData()
            if (terminalStocks.isNotEmpty()) {
                Log.i(TAG, "PSX Terminal sync success: ${terminalStocks.size} stocks")
                val quotes = terminalStocks.map { stock ->
                    UnifiedQuote(
                        symbol = stock.symbol,
                        price = stock.effectivePrice(),
                        change = stock.effectiveChange(),
                        changePercent = stock.effectiveChangePercent(),
                        volume = stock.effectiveVolume(),
                        dayHigh = stock.high ?: 0.0,
                        dayLow = stock.low ?: 0.0,
                        open = stock.open ?: 0.0,
                        previousClose = stock.ldcp ?: 0.0
                    )
                }
                bulkCacheAndUpdate(quotes)
                return@withContext Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.w(TAG, "PSX Terminal sync failed: ${e.javaClass.simpleName}: ${e.message}")
        }

        Log.e(TAG, "All PSX sync sources failed")
        Result.failure(Exception("All PSX data sources unavailable"))
    }

    suspend fun getIndices(): Result<List<PsxIndex>> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        
        // 1. Try PSX DPS
        try {
            val response = psxApi.getIndices()
            val indices = response.indices
            if (!indices.isNullOrEmpty()) {
                return@withContext Result.success(indices)
            }
        } catch (e: Exception) {
            Log.w(TAG, "PSX DPS indices failed: ${e.message}")
        }

        // 2. Try PSX Terminal
        try {
            val response = psxTerminalApi.getStats()
            val terminalIndices = response.indices
            if (!terminalIndices.isNullOrEmpty()) {
                val mapped = terminalIndices.map { idx ->
                    PsxIndex(
                        name = idx.name,
                        current = idx.effectiveValue(),
                        change = idx.effectiveChange(),
                        changep = idx.effectiveChangePercent(),
                        high = idx.high ?: idx.effectiveValue(),
                        low = idx.low ?: idx.effectiveValue(),
                        prev = idx.effectivePrev()
                    )
                }
                return@withContext Result.success(mapped)
            }
        } catch (e: Exception) {
            Log.w(TAG, "PSX Terminal indices failed: ${e.message}")
        }

        Result.failure(Exception("No index data available"))
    }

    // ─── Helpers ───

    /**
     * Bulk cache quotes and update Stock table prices.
     */
    private suspend fun bulkCacheAndUpdate(quotes: List<UnifiedQuote>) {
        try {
            val cacheEntities = quotes.map { quote ->
                StockQuoteCache(
                    symbol = quote.symbol,
                    price = quote.price,
                    change = quote.change,
                    changePercent = quote.changePercent,
                    volume = quote.volume,
                    high = quote.dayHigh,
                    low = quote.dayLow,
                    cachedAt = System.currentTimeMillis()
                )
            }
            quoteCacheDao.upsertAll(cacheEntities)

            val priceUpdates = quotes.associate { it.symbol to it.price }
            stockDao.updatePrices(priceUpdates)
        } catch (e: Exception) {
            Log.w(TAG, "Bulk cache/update failed: ${e.message}")
        }
    }

    private suspend fun cacheQuote(quote: UnifiedQuote) {
        try {
            quoteCacheDao.upsert(
                StockQuoteCache(
                    symbol = quote.symbol,
                    price = quote.price,
                    change = quote.change,
                    changePercent = quote.changePercent,
                    volume = quote.volume,
                    high = quote.dayHigh,
                    low = quote.dayLow,
                    cachedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache quote: ${e.message}")
        }
    }

    private fun isCacheFresh(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt < CACHE_FRESHNESS_MS
    }

    private fun getDefaultInterval(range: ChartRange): ChartInterval {
        return when (range) {
            ChartRange.ONE_DAY -> ChartInterval.FIVE_MINUTES
            ChartRange.ONE_WEEK -> ChartInterval.FIFTEEN_MINUTES
            ChartRange.ONE_MONTH -> ChartInterval.ONE_DAY
            ChartRange.THREE_MONTHS -> ChartInterval.ONE_DAY
            ChartRange.SIX_MONTHS -> ChartInterval.ONE_DAY
            ChartRange.ONE_YEAR -> ChartInterval.ONE_WEEK
            ChartRange.THREE_YEARS -> ChartInterval.ONE_WEEK
            ChartRange.FIVE_YEARS -> ChartInterval.ONE_MONTH
        }
    }
}

/** Extension to convert cached quote entity to unified model */
private fun StockQuoteCache.toUnifiedQuote() = UnifiedQuote(
    symbol = symbol,
    price = price,
    change = change,
    changePercent = changePercent,
    volume = volume,
    dayHigh = high,
    dayLow = low,
    open = 0.0,
    previousClose = 0.0,
    timestamp = cachedAt
)
