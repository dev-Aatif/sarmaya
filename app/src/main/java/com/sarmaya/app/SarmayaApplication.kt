package com.sarmaya.app

import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import com.sarmaya.app.data.Stock

class SarmayaApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        
        // Start portfolio snapshots for historical charts
        container.syncManager.scheduleSnapshotWork()
        container.syncManager.runImmediateSnapshot()

        // Populate offline stock database if empty
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (container.stockDao.getStocksCount() == 0) {
                    val inputStream = assets.open("psx_stocks.json")
                    val size = inputStream.available()
                    val buffer = ByteArray(size)
                    inputStream.read(buffer)
                    inputStream.close()
                    val json = String(buffer, Charsets.UTF_8)
                    val jsonArray = JSONArray(json)
                    val stocks = mutableListOf<Stock>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        stocks.add(
                            Stock(
                                symbol = obj.getString("symbol"),
                                name = obj.getString("name"),
                                sector = obj.getString("sector"),
                                currentPrice = 0.0,
                                priceUpdatedAt = 0L
                            )
                        )
                    }
                    if (stocks.isNotEmpty()) {
                        container.stockDao.insertStocks(stocks)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
