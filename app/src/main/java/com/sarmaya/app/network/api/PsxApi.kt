package com.sarmaya.app.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface for PSX Data Portal (DPS) — primary source
 * Endpoints are mostly cached JSON files.
 * Base URL: https://dps.psx.com.pk/
 */
interface PsxApi {
    
    /**
     * Fetch live data summary for all stocks.
     * Maps to https://dps.psx.com.pk/cache/live.json
     */
    @GET("cache/live.json")
    suspend fun getLiveQuotes(): PsxLiveResponse

    /**
     * Fetch all indices summary.
     * Maps to https://dps.psx.com.pk/cache/indices.json
     */
    @GET("cache/indices.json")
    suspend fun getIndices(): PsxIndicesResponse
}

/**
 * Interface for PSX Terminal — free, no-auth fallback source
 * Base URL: https://psxterminal.com/
 */
interface PsxTerminalApi {

    @GET("api/status")
    suspend fun getStatus(): PsxTerminalStatus

    @GET("api/symbols")
    suspend fun getSymbols(): PsxTerminalResponse<List<String>>

    @GET("api/ticks/REG/{symbol}")
    suspend fun getStockTick(@Path("symbol") symbol: String): PsxTerminalResponse<PsxTerminalTick>

    @GET("api/ticks/IDX/{symbol}")
    suspend fun getIndexTick(@Path("symbol") symbol: String): PsxTerminalResponse<PsxTerminalTick>

    @GET("api/stats/REG")
    suspend fun getMarketStats(): PsxTerminalResponse<PsxTerminalStats>

    @GET("api/stats/IDX")
    suspend fun getIndexStats(): PsxTerminalResponse<PsxTerminalStats>

    @GET("api/stats/breadth")
    suspend fun getMarketBreadth(): PsxTerminalResponse<PsxTerminalBreadth>

    @GET("api/stats/sectors")
    suspend fun getSectorStats(): PsxTerminalResponse<Map<String, PsxTerminalSectorStat>>

    @GET("api/companies/{symbol}")
    suspend fun getCompany(@Path("symbol") symbol: String): PsxTerminalResponse<PsxTerminalCompanyInfo>

    @GET("api/fundamentals/{symbol}")
    suspend fun getFundamentals(@Path("symbol") symbol: String): PsxTerminalResponse<PsxTerminalFundamentals>

    @GET("api/dividends/{symbol}")
    suspend fun getDividends(@Path("symbol") symbol: String): PsxTerminalResponse<List<PsxTerminalDividend>>

    @GET("api/klines/{symbol}/{tf}")
    suspend fun getKlines(@Path("symbol") symbol: String, @Path("tf") timeframe: String): PsxTerminalResponse<List<PsxTerminalKline>>

    @GET("api/towatch")
    suspend fun getToWatch(): PsxTerminalResponse<Map<String, List<PsxTerminalTick>>>
}

@JsonClass(generateAdapter = true)
data class PsxTerminalResponse<T>(
    val success: Boolean,
    val data: T
)

// ─── PSX DPS Models ───

data class PsxLiveResponse(
    val stats: List<PsxMarketStat>? = null,
    val stocks: List<PsxStockQuote>? = null,
    val update_time: String? = null
)

data class PsxStockQuote(
    val scrip: String,      // Symbol (e.g., OGDC)
    val name: String,       // Full Name
    val current: Double,    // Last Traded Price
    val change: Double,     // Price Change
    val changep: Double,    // Price Change Percentage
    val vol: Long,          // Volume
    val high: Double? = null,
    val low: Double? = null,
    val open: Double? = null,
    val prev: Double? = null,
    val sector: String? = null
)

data class PsxMarketStat(
    val label: String,
    val value: String
)

data class PsxIndicesResponse(
    val indices: List<PsxIndex>? = null
)

data class PsxIndex(
    val name: String,
    val current: Double,
    val change: Double,
    val changep: Double,
    val high: Double,
    val low: Double,
    val prev: Double
)

// ─── PSX Terminal Models ───

// ─── PSX Terminal Models (v2) ───

@JsonClass(generateAdapter = true)
data class PsxTerminalStatus(
    val status: String,
    val timestamp: String? = null,
    val uptime: Double? = null,
    val marketState: String? = null
)

@JsonClass(generateAdapter = true)
data class PsxTerminalSymbol(
    val symbol: String,
    val name: String,
    val sector: String,
    val market: String
)

@JsonClass(generateAdapter = true)
data class PsxTerminalTick(
    val symbol: String,
    val price: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0L,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val value: Double = 0.0, // Changed to Double as it can be large/decimal
    val trades: Long = 0L,
    @Json(name = "st") val state: String = "OFFLINE"
)

@JsonClass(generateAdapter = true)
data class PsxTerminalStats(
    val topGainers: List<PsxTerminalTick> = emptyList(),
    val topLosers: List<PsxTerminalTick> = emptyList(),
    val volumeLeaders: List<PsxTerminalTick> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PsxTerminalBreadth(
    val total: Int = 0,
    val advanced: Int = 0,
    val declined: Int = 0,
    val unchanged: Int = 0
)

@JsonClass(generateAdapter = true)
data class PsxTerminalSectorStat(
    val totalVolume: Long = 0L,
    val totalValue: Double = 0.0,
    val totalTrades: Int = 0,
    val gainers: Int = 0,
    val losers: Int = 0,
    val unchanged: Int = 0,
    val avgChange: Double = 0.0,
    val avgChangePercent: Double = 0.0,
    val symbols: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PsxTerminalCompanyInfo(
    val symbol: String,
    val name: String,
    val description: String = "",
    val sector: String = "",
    val industry: String = "",
    val profileImage: String = ""
)

@JsonClass(generateAdapter = true)
data class PsxTerminalFundamentals(
    val symbol: String,
    val marketCap: String = "",
    val peRatio: Double = 0.0,
    val eps: Double = 0.0,
    val dividendYield: Double = 0.0,
    val pbRatio: Double = 0.0,
    val roe: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class PsxTerminalDividend(
    val exDate: Long,
    val paymentDate: Long,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class PsxTerminalKline(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
