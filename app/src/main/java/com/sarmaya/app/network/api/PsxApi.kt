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

    /**
     * Fetch live market data for all stocks.
     */
    @GET("api/market-data")
    suspend fun getMarketData(): List<PsxTerminalStock>

    /**
     * Fetch market statistics/indices.
     */
    @GET("api/stats")
    suspend fun getStats(): PsxTerminalStatsResponse

    /**
     * Fetch company profile/details.
     */
    @GET("api/company/{symbol}")
    suspend fun getCompany(@Path("symbol") symbol: String): PsxTerminalCompany

    /**
     * Fetch fundamental data (PE, yields, etc.)
     */
    @GET("api/yields/{symbol}")
    suspend fun getYields(@Path("symbol") symbol: String): PsxTerminalYields
}

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

@JsonClass(generateAdapter = true)
data class PsxTerminalStock(
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "name") val name: String? = null,
    @Json(name = "company") val company: String? = null,
    @Json(name = "ldcp") val ldcp: Double? = null,       // Last Day Close Price
    @Json(name = "open") val open: Double? = null,
    @Json(name = "high") val high: Double? = null,
    @Json(name = "low") val low: Double? = null,
    @Json(name = "current") val current: Double? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "change") val change: Double? = null,
    @Json(name = "change_p") val changePercent: Double? = null,
    @Json(name = "volume") val volume: Long? = null,
    @Json(name = "vol") val vol: Long? = null,
    @Json(name = "sector") val sector: String? = null
) {
    /** Resolve the effective price from whichever field is populated */
    fun effectivePrice(): Double = current ?: price ?: 0.0
    fun effectiveVolume(): Long = volume ?: vol ?: 0L
    fun effectiveName(): String = name ?: company ?: symbol
    fun effectiveChange(): Double = change ?: 0.0
    fun effectiveChangePercent(): Double = changePercent ?: 0.0
}

@JsonClass(generateAdapter = true)
data class PsxTerminalStatsResponse(
    @Json(name = "indices") val indices: List<PsxTerminalIndex>? = null,
    @Json(name = "stats") val stats: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class PsxTerminalIndex(
    @Json(name = "name") val name: String = "",
    @Json(name = "current") val current: Double? = null,
    @Json(name = "value") val value: Double? = null,
    @Json(name = "change") val change: Double? = null,
    @Json(name = "change_p") val changePercent: Double? = null,
    @Json(name = "high") val high: Double? = null,
    @Json(name = "low") val low: Double? = null,
    @Json(name = "prev") val prev: Double? = null,
    @Json(name = "previous") val previous: Double? = null
) {
    fun effectiveValue(): Double = current ?: value ?: 0.0
    fun effectiveChange(): Double = change ?: 0.0
    fun effectiveChangePercent(): Double = changePercent ?: 0.0
    fun effectivePrev(): Double = prev ?: previous ?: 0.0
}

@JsonClass(generateAdapter = true)
data class PsxTerminalCompany(
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "company") val company: String? = null,
    @Json(name = "sector") val sector: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "website") val website: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "industry") val industry: String? = null,
    @Json(name = "market_cap") val marketCap: Long? = null,
    @Json(name = "shares") val shares: Long? = null,
    @Json(name = "free_float") val freeFloat: Double? = null,
    @Json(name = "face_value") val faceValue: Double? = null
) {
    fun effectiveName(): String = name ?: company ?: symbol ?: ""
}

@JsonClass(generateAdapter = true)
data class PsxTerminalYields(
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "pe") val pe: Double? = null,
    @Json(name = "eps") val eps: Double? = null,
    @Json(name = "dividend_yield") val dividendYield: Double? = null,
    @Json(name = "book_value") val bookValue: Double? = null,
    @Json(name = "high_52w") val high52w: Double? = null,
    @Json(name = "low_52w") val low52w: Double? = null,
    @Json(name = "beta") val beta: Double? = null,
    @Json(name = "market_cap") val marketCap: Long? = null
)
