package com.sarmaya.app.network.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PsxApiParsingTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `parse PsxLiveResponse correctly`() {
        val json = """
            {
              "stats": [
                {"label": "KSE100", "value": "70,000"}
              ],
              "stocks": [
                {
                  "scrip": "OGDC",
                  "name": "Oil & Gas Development",
                  "current": 120.5,
                  "change": 1.5,
                  "changep": 1.2,
                  "vol": 1000000
                }
              ],
              "update_time": "2024-04-14 10:00:00"
            }
        """.trimIndent()

        val adapter = moshi.adapter(PsxLiveResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(1, response?.stocks?.size)
        assertEquals("OGDC", response?.stocks?.get(0)?.scrip)
        assertEquals(120.5, response?.stocks?.get(0)?.current ?: 0.0, 0.001)
    }

    @Test
    fun `parse PsxTerminalStatus correctly`() {
        val json = """
            {
              "status": "ok",
              "marketState": "OPN"
            }
        """.trimIndent()

        val adapter = moshi.adapter(PsxTerminalStatus::class.java)
        val status = adapter.fromJson(json)

        assertNotNull(status)
        assertEquals("ok", status?.status)
        assertEquals("OPN", status?.marketState)
    }

    @Test
    fun `parse PsxTerminalTick correctly`() {
        val json = """
            {
              "symbol": "LUCK",
              "price": 850.0,
              "change": 10.0,
              "changePercent": 1.18,
              "volume": 50000,
              "high": 860.0,
              "low": 840.0,
              "value": 42500000,
              "trades": 500,
              "state": "OPN"
            }
        """.trimIndent()

        val adapter = moshi.adapter(PsxTerminalTick::class.java)
        val tick = adapter.fromJson(json)

        assertNotNull(tick)
        assertEquals("LUCK", tick?.symbol)
        assertEquals(850.0, tick?.price ?: 0.0, 0.001)
        assertEquals("OPN", tick?.state)
    }
}
