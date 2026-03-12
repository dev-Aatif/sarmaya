package com.sarmaya.app.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Yahoo Finance API v8/v7/v10 endpoints.
 * Base URL: https://query1.finance.yahoo.com
 *
 * PSX stocks use the ".KA" suffix (Karachi exchange).
 * Example: OGDC → OGDC.KA, HBL → HBL.KA
 */
interface YahooFinanceApi {

    /**
     * Fetch historical chart data for a symbol.
     * @param symbol Stock symbol (e.g. "OGDC.KA")
     * @param range Time range: 1d, 5d, 1mo, 3mo, 6mo, 1y, 3y, 5y, max
     * @param interval Data interval: 1m, 5m, 15m, 1h, 1d, 1wk, 1mo
     */
    @GET("/v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "1mo",
        @Query("interval") interval: String = "1d",
        @Query("includePrePost") includePrePost: Boolean = false
    ): YahooChartResponse

    /**
     * Fetch live quotes for one or more symbols.
     * @param symbols Comma-separated list of symbols
     */
    @GET("/v7/finance/quote")
    suspend fun getQuotes(
        @Query("symbols") symbols: String
    ): YahooQuoteResponse

    /**
     * Fetch detailed summary for a symbol (profile, stats, etc.).
     * @param symbol Stock symbol
     * @param modules Comma-separated: assetProfile, defaultKeyStatistics, financialData, summaryDetail, price
     */
    @GET("/v10/finance/quoteSummary/{symbol}")
    suspend fun getQuoteSummary(
        @Path("symbol") symbol: String,
        @Query("modules") modules: String = "assetProfile,defaultKeyStatistics,financialData,summaryDetail,price"
    ): YahooQuoteSummaryResponse

    /**
     * Search for stock symbols.
     */
    @GET("/v1/finance/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("quotesCount") count: Int = 20,
        @Query("newsCount") newsCount: Int = 0
    ): YahooSearchResponse
}

// ─── Response models ───

@JsonClass(generateAdapter = true)
data class YahooChartResponse(
    @Json(name = "chart") val chart: YahooChartResult?
)

@JsonClass(generateAdapter = true)
data class YahooChartResult(
    @Json(name = "result") val result: List<YahooChartData>?,
    @Json(name = "error") val error: YahooError?
)

@JsonClass(generateAdapter = true)
data class YahooChartData(
    @Json(name = "meta") val meta: YahooChartMeta?,
    @Json(name = "timestamp") val timestamp: List<Long>?,
    @Json(name = "indicators") val indicators: YahooIndicators?
)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
    @Json(name = "currency") val currency: String?,
    @Json(name = "symbol") val symbol: String?,
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double?,
    @Json(name = "previousClose") val previousClose: Double?,
    @Json(name = "exchangeName") val exchangeName: String?
)

@JsonClass(generateAdapter = true)
data class YahooIndicators(
    @Json(name = "quote") val quote: List<YahooOHLC>?
)

@JsonClass(generateAdapter = true)
data class YahooOHLC(
    @Json(name = "open") val open: List<Double?>?,
    @Json(name = "high") val high: List<Double?>?,
    @Json(name = "low") val low: List<Double?>?,
    @Json(name = "close") val close: List<Double?>?,
    @Json(name = "volume") val volume: List<Long?>?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteResponse(
    @Json(name = "quoteResponse") val quoteResponse: YahooQuoteResult?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteResult(
    @Json(name = "result") val result: List<YahooQuoteData>?,
    @Json(name = "error") val error: YahooError?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteData(
    @Json(name = "symbol") val symbol: String?,
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double?,
    @Json(name = "regularMarketChange") val regularMarketChange: Double?,
    @Json(name = "regularMarketChangePercent") val regularMarketChangePercent: Double?,
    @Json(name = "regularMarketVolume") val regularMarketVolume: Long?,
    @Json(name = "regularMarketDayHigh") val regularMarketDayHigh: Double?,
    @Json(name = "regularMarketDayLow") val regularMarketDayLow: Double?,
    @Json(name = "regularMarketOpen") val regularMarketOpen: Double?,
    @Json(name = "regularMarketPreviousClose") val regularMarketPreviousClose: Double?,
    @Json(name = "shortName") val shortName: String?,
    @Json(name = "longName") val longName: String?,
    @Json(name = "marketCap") val marketCap: Long?,
    @Json(name = "fiftyTwoWeekHigh") val fiftyTwoWeekHigh: Double?,
    @Json(name = "fiftyTwoWeekLow") val fiftyTwoWeekLow: Double?,
    @Json(name = "trailingPE") val trailingPE: Double?,
    @Json(name = "epsTrailingTwelveMonths") val epsTrailingTwelveMonths: Double?,
    @Json(name = "dividendYield") val dividendYield: Double?
)

// ─── Quote Summary (v10) ───

@JsonClass(generateAdapter = true)
data class YahooQuoteSummaryResponse(
    @Json(name = "quoteSummary") val quoteSummary: YahooQuoteSummaryResult?
)

@JsonClass(generateAdapter = true)
data class YahooQuoteSummaryResult(
    @Json(name = "result") val result: List<YahooSummaryModules>?,
    @Json(name = "error") val error: YahooError?
)

@JsonClass(generateAdapter = true)
data class YahooSummaryModules(
    @Json(name = "assetProfile") val assetProfile: YahooAssetProfile?,
    @Json(name = "defaultKeyStatistics") val keyStats: YahooKeyStats?,
    @Json(name = "summaryDetail") val summaryDetail: YahooSummaryDetail?,
    @Json(name = "price") val price: YahooPrice?
)

@JsonClass(generateAdapter = true)
data class YahooAssetProfile(
    @Json(name = "longBusinessSummary") val longBusinessSummary: String?,
    @Json(name = "sector") val sector: String?,
    @Json(name = "industry") val industry: String?,
    @Json(name = "website") val website: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "city") val city: String?
)

@JsonClass(generateAdapter = true)
data class YahooKeyStats(
    @Json(name = "beta") val beta: YahooRawFmt?,
    @Json(name = "enterpriseValue") val enterpriseValue: YahooRawFmt?,
    @Json(name = "forwardPE") val forwardPE: YahooRawFmt?,
    @Json(name = "earningsQuarterlyGrowth") val earningsGrowth: YahooRawFmt?
)

@JsonClass(generateAdapter = true)
data class YahooSummaryDetail(
    @Json(name = "marketCap") val marketCap: YahooRawFmt?,
    @Json(name = "trailingPE") val trailingPE: YahooRawFmt?,
    @Json(name = "dividendYield") val dividendYield: YahooRawFmt?,
    @Json(name = "fiftyTwoWeekHigh") val fiftyTwoWeekHigh: YahooRawFmt?,
    @Json(name = "fiftyTwoWeekLow") val fiftyTwoWeekLow: YahooRawFmt?
)

@JsonClass(generateAdapter = true)
data class YahooPrice(
    @Json(name = "regularMarketPrice") val regularMarketPrice: YahooRawFmt?,
    @Json(name = "regularMarketChange") val regularMarketChange: YahooRawFmt?,
    @Json(name = "regularMarketChangePercent") val regularMarketChangePercent: YahooRawFmt?,
    @Json(name = "regularMarketVolume") val regularMarketVolume: YahooRawFmt?,
    @Json(name = "regularMarketDayHigh") val regularMarketDayHigh: YahooRawFmt?,
    @Json(name = "regularMarketDayLow") val regularMarketDayLow: YahooRawFmt?,
    @Json(name = "regularMarketOpen") val regularMarketOpen: YahooRawFmt?,
    @Json(name = "regularMarketPreviousClose") val regularMarketPreviousClose: YahooRawFmt?,
    @Json(name = "shortName") val shortName: String?,
    @Json(name = "longName") val longName: String?,
    @Json(name = "marketCap") val marketCap: YahooRawFmt?,
    @Json(name = "epsTrailingTwelveMonths") val eps: YahooRawFmt?
)

@JsonClass(generateAdapter = true)
data class YahooRawFmt(
    @Json(name = "raw") val raw: Double?,
    @Json(name = "fmt") val fmt: String?
)

// ─── Search ───

@JsonClass(generateAdapter = true)
data class YahooSearchResponse(
    @Json(name = "quotes") val quotes: List<YahooSearchQuote>?
)

@JsonClass(generateAdapter = true)
data class YahooSearchQuote(
    @Json(name = "symbol") val symbol: String?,
    @Json(name = "shortname") val shortName: String?,
    @Json(name = "longname") val longName: String?,
    @Json(name = "exchange") val exchange: String?,
    @Json(name = "quoteType") val quoteType: String?
)

// ─── Error ───

@JsonClass(generateAdapter = true)
data class YahooError(
    @Json(name = "code") val code: String?,
    @Json(name = "description") val description: String?
)
