package com.sarmaya.app.network.websocket

import android.util.Log
import com.sarmaya.app.network.ConnectivityChecker
import com.sarmaya.app.util.MarketHoursUtil
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit
import okio.ByteString

class PsxWebSocketManager(
    private val connectivityChecker: ConnectivityChecker,
    private val moshi: Moshi
) {
    companion object {
        private const val TAG = "PsxWebSocket"
        private const val WS_URL = "wss://api.psxterminal.com/ws"
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable timeout for websockets
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null

    private val subscribedChannels = mutableSetOf<String>()
    private var clientId: String? = null
    private var isConnected = false
    private var reconnectDelay = 1000L

    private val _tickUpdates = MutableSharedFlow<TickUpdate>(replay = 10)
    val tickUpdates = _tickUpdates.asSharedFlow()

    private val _klineUpdates = MutableSharedFlow<KlineUpdate>(replay = 5)
    val klineUpdates = _klineUpdates.asSharedFlow()

    init {
        // Automatically connect if during market hours
        if (MarketHoursUtil.isMarketOpen() && connectivityChecker.isOnline()) {
            connect()
        }
    }

    fun connect() {
        if (isConnected || connectionJob?.isActive == true) return

        connectionJob = scope.launch {
            if (!connectivityChecker.isOnline()) return@launch

            val request = Request.Builder()
                .url(WS_URL)
                .build()

            webSocket = client.newWebSocket(request, createWebSocketListener())
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        webSocket?.close(NORMAL_CLOSURE_STATUS, "User disconnected")
        webSocket = null
        isConnected = false
        subscribedChannels.clear()
        clientId = null
        reconnectDelay = 1000L
    }

    fun subscribe(channel: String) {
        subscribedChannels.add(channel)
        if (isConnected) {
            sendSubscriptionRequest(listOf(channel))
        } else {
            connect()
        }
    }

    fun unsubscribe(channel: String) {
        subscribedChannels.remove(channel)
        if (isConnected) {
            val req = WsUnsubscribeRequest(channels = listOf(channel))
            try {
                val json = moshi.adapter(WsUnsubscribeRequest::class.java).toJson(req)
                webSocket?.send(json)
            } catch (e: Exception) {
                Log.e(TAG, "Unsubscribe error: ${e.message}")
            }
        }
    }

    private fun sendSubscriptionRequest(channels: List<String>) {
        if (channels.isEmpty()) return
        val req = WsSubscribeRequest(channels = channels)
        try {
            val json = moshi.adapter(WsSubscribeRequest::class.java).toJson(req)
            webSocket?.send(json)
        } catch (e: Exception) {
            Log.e(TAG, "Subscribe error: ${e.message}")
        }
    }

    private fun handleMessage(json: String) {
        try {
            // Simplified parsing for MVP purposes
            // In a real implementation this would deserialize cleanly with Moshi
            if (json.contains("\"type\":\"welcome\"")) {
                isConnected = true
                reconnectDelay = 1000L // Reset delay
                if (subscribedChannels.isNotEmpty()) {
                    sendSubscriptionRequest(subscribedChannels.toList())
                }
            } else if (json.contains("\"type\":\"tick\"")) {
                parseAndEmitTick(json)
            } else if (json.contains("\"type\":\"kline\"")) {
                parseAndEmitKline(json)
            }
            // Add ping pong as necessary if okHttp ping isn't enough
        } catch (e: Exception) {
            Log.e(TAG, "Message parse error: ${e.message}")
        }
    }
    
    private fun parseAndEmitTick(json: String) {
        // Very basic stub parsing, assuming server sends JSON like:
        // {"type":"tick","channel":"marketData:REG","data":{"symbol":"OGDC","price":120.5,...}}
        // Handled dynamically to avoid complex polymorphic deserialization overhead
        scope.launch {
            try {
                // Temporary stub logic - use actual Moshi parsing in reality
                // We'll just emit a dummy update for testing the flow
                val tick = TickUpdate(
                    symbol = "TEST", price = 0.0, change = 0.0, changePercent = 0.0,
                    volume = 0L, trades = 0L, value = 0L, high = 0.0, low = 0.0, marketState = "OPN"
                )
                // _tickUpdates.emit(actualParsedTick)
            } catch (e: Exception) {}
        }
    }

    private fun parseAndEmitKline(json: String) {}

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.v(TAG, "Received message: $text")
            handleMessage(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
            isConnected = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            isConnected = false
            
            // Reconnect with exponential backoff if in market hours
            if (MarketHoursUtil.isMarketOpen()) {
                scope.launch {
                    delay(reconnectDelay)
                    reconnectDelay = (reconnectDelay * 2).coerceAtMost(30000L)
                    connect()
                }
            }
        }
    }
}
