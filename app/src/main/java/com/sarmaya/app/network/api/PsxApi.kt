package com.sarmaya.app.network.api

import retrofit2.http.GET

/**
 * Interface for PSX Data Portal (DPS)
 * Endpoints are mostly cached JSON files.
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
