package com.sarmaya.app.network

/**
 * Provider-agnostic data models consumed by the rest of the app.
 * All API-specific response models are mapped to these before reaching ViewModels.
 */

/** Live quote data for a single stock */
data class UnifiedQuote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val dayHigh: Double,
    val dayLow: Double,
    val open: Double,
    val previousClose: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/** Company profile / overview */
data class CompanyProfile(
    val symbol: String,
    val name: String,
    val description: String,
    val sector: String,
    val industry: String,
    val website: String,
    val phone: String,
    val country: String,
    val logoUrl: String,
    val marketCap: Long,
    val peRatio: Double,
    val eps: Double,
    val beta: Double,
    val weekHigh52: Double,
    val weekLow52: Double,
    val dividendYield: Double,
    val earningsDate: String
)

/** A single price data point for charting */
data class PricePoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

/** Symbol search result */
data class SymbolSearchResult(
    val symbol: String,
    val name: String,
    val exchange: String,
    val type: String
)

/** Timeframes for historical charts */
enum class ChartRange(val yahooParam: String, val displayName: String) {
    ONE_DAY("1d", "1D"),
    ONE_WEEK("5d", "1W"),
    ONE_MONTH("1mo", "1M"),
    THREE_MONTHS("3mo", "3M"),
    SIX_MONTHS("6mo", "6M"),
    ONE_YEAR("1y", "1Y"),
    THREE_YEARS("3y", "3Y"),
    FIVE_YEARS("5y", "5Y")
}

/** Intervals for chart data granularity */
enum class ChartInterval(val yahooParam: String) {
    ONE_MINUTE("1m"),
    FIVE_MINUTES("5m"),
    FIFTEEN_MINUTES("15m"),
    ONE_HOUR("1h"),
    ONE_DAY("1d"),
    ONE_WEEK("1wk"),
    ONE_MONTH("1mo")
}

/** GitHub release info for update notifications */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String?
)
