package com.sarmaya.app.util

import java.util.Calendar
import java.util.TimeZone

object MarketHoursUtil {
    private const val MARKET_TIMEZONE = "Asia/Karachi"

    fun isMarketOpen(): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(MARKET_TIMEZONE))
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Closed on weekends
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val currentMinutes = hour * 60 + minute
        val openMinutes = 9 * 60 + 30  // 9:30 AM
        val closeMinutes = 15 * 60 + 30 // 3:30 PM
        
        return currentMinutes in openMinutes..closeMinutes
    }

    fun getMarketState(): String {
        if (!isMarketOpen()) {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone(MARKET_TIMEZONE))
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                return "OFFLINE"
            }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val currentMinutes = hour * 60 + minute
            val preOpenMinutes = 9 * 60 + 15
            val openMinutes = 9 * 60 + 30
            if (currentMinutes in preOpenMinutes until openMinutes) return "PRE"
            return "CLS"
        }
        return "OPN"
    }
}
