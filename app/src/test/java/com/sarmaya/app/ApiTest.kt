package com.sarmaya.app

import com.sarmaya.app.network.api.PsxTerminalApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ApiTest {
    @Test
    fun testApi() = runBlocking {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder().build()
        val api = Retrofit.Builder()
            .baseUrl("https://psxterminal.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PsxTerminalApi::class.java)

        try {
            val res = api.getStockTick("ENGRO")
            println("Tick: ${res.data.price}")
        } catch (e: Exception) {
            println("Tick error: ${e.message}")
        }
        
        try {
            val stats = api.getMarketStats()
            println("Gainers: ${stats.data.topGainers.size}")
        } catch(e: Exception) {
            println("Stats error: ${e.message}")
        }
    }
}
