package com.sarmaya.app.network.websocket

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WsSubscribeRequest(
    val action: String = "subscribe",
    val channels: List<String>
)

@JsonClass(generateAdapter = true)
data class WsUnsubscribeRequest(
    val action: String = "unsubscribe",
    val channels: List<String>
)

@JsonClass(generateAdapter = true)
data class WsMessage(
    val type: String, // "welcome", "tick", "stats", "kline", "ping", "pong"
    val channel: String? = null,
    val data: Any? = null // Handled dynamically in parsing
)

data class TickUpdate(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val trades: Long,
    val value: Long,
    val high: Double,
    val low: Double,
    val marketState: String
)

data class KlineUpdate(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
