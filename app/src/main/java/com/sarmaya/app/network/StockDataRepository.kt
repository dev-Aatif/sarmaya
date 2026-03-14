package com.sarmaya.app.network

import android.util.Log
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.StockQuoteCache
import com.sarmaya.app.data.StockQuoteCacheDao
import com.sarmaya.app.network.api.PsxApi
import com.sarmaya.app.network.api.YahooFinanceApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central data repository implementing:
 *   Yahoo Finance API → PSX website scraper (future) → local cache fallback
 *
 * All methods return [Result] so callers can gracefully handle failures.
 * Successfully fetched data is always cached to Room for offline access.
 */
class StockDataRepository(
    private val yahooApi: YahooFinanceApi,
    private val psxApi: PsxApi,
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
     * Strategy: cache first (if fresh) → Yahoo API → return stale cache on failure.
     */
    suspend fun getQuote(psxSymbol: String): Result<UnifiedQuote> = withContext(Dispatchers.IO) {
        // 1. Check fresh cache
        val cached = quoteCacheDao.getCache(psxSymbol)
        if (cached != null && isCacheFresh(cached.cachedAt)) {
            return@withContext Result.success(cached.toUnifiedQuote())
        }

        // 2. Try direct PSX Scraper (Faster & more accurate)
        if (connectivityChecker.isOnline()) {
            try {
                // To avoid fetching all stocks for one quote, we could have a single scrip endpoint,
                // but usually live.json is the way. Let's try batching or bulk sync first.
                val psxResult = syncPsxQuotes()
                if (psxResult.isSuccess) {
                    val freshCached = quoteCacheDao.getCache(psxSymbol)
                    if (freshCached != null) {
                        return@withContext Result.success(freshCached.toUnifiedQuote())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "PSX scraper failed for $psxSymbol: ${e.message}")
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
                    // Cache it
                    cacheQuote(quote)
                    // Also update Stock table price
                    stockDao.updatePrice(psxSymbol, quote.price)
                    return@withContext Result.success(quote)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Yahoo quote failed for $psxSymbol: ${e.message}")
            }
        }

        // 3. Return stale cache if available
        if (cached != null) {
            return@withContext Result.success(cached.toUnifiedQuote())
        }

        // 4. Ultimate fallback: last known price from Stock table
        try {
            val lastStock = stockDao.getStocksSync(listOf(psxSymbol)).firstOrNull()
            if (lastStock != null) {
                return@withContext Result.success(UnifiedQuote(
                    symbol = psxSymbol,
                    price = lastStock.currentPrice,
                    change = 0.0,
                    changePercent = 0.0,
                    volume = 0L,
                    dayHigh = 0.0,
                    dayLow = 0.0,
                    open = 0.0,
                    previousClose = 0.0
                ))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to Stock table failed for $psxSymbol: ${e.message}")
        }

        Result.failure(Exception("No data available for $psxSymbol"))
    }

    /**
     * Fetch live quotes for multiple symbols in one batch.
     */
    suspend fun getBatchQuotes(psxSymbols: List<String>): Result<List<UnifiedQuote>> = withContext(Dispatchers.IO) {
        if (psxSymbols.isEmpty()) return@withContext Result.success(emptyList())

        // Try PSX Scraper first (Bulk sync)
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
                    // Cache all
                    quotes.forEach { cacheQuote(it) }
                    // Update Stock table prices
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
     * Fetch detailed company profile (description, stats, etc.).
     */
    suspend fun getCompanyProfile(psxSymbol: String): Result<CompanyProfile> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

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
                        logoUrl = "", // Not available from Yahoo, use placeholder
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

        Result.failure(Exception("No profile data available for $psxSymbol"))
    }

    // ─── Peers (same-sector stocks from local DB) ───

    /**
     * Get peer companies from the same sector using local DB.
     * Since Finnhub peers endpoint is US-only, we derive from sector.
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
     * Fetch all live quotes from PSX Data Portal and update local DB.
     * This is the "Scraper" implementation.
     */
    suspend fun syncPsxQuotes(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        
        try {
            val response = psxApi.getLiveQuotes()
            val stocks = response.stocks ?: return@withContext Result.failure(Exception("No stocks in PSX response"))
            
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
            
            // Bulk cache
            quotes.forEach { cacheQuote(it) }
            // Bulk update Stock table
            quotes.forEach { stockDao.updatePrice(it.symbol, it.price) }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "syncPsxQuotes failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getIndices(): Result<List<com.sarmaya.app.network.api.PsxIndex>> = withContext(Dispatchers.IO) {
        if (!connectivityChecker.isOnline()) return@withContext Result.failure(Exception("No internet"))
        try {
            val response = psxApi.getIndices()
            Result.success(response.indices ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Helpers ───

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
