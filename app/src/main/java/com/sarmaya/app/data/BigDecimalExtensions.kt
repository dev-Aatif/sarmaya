package com.sarmaya.app.data

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Robust conversion from Double to BigDecimal.
 * Handles NaN and Infinite values by returning BigDecimal.ZERO.
 */
fun Double?.toSafeBigDecimal(): BigDecimal {
    if (this == null || this.isNaN() || this.isInfinite()) {
        return BigDecimal.ZERO
    }
    return try {
        BigDecimal.valueOf(this)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}

/**
 * Robust conversion from String to BigDecimal.
 * Handles empty, malformed, or scientific notation strings safely.
 */
fun String?.toSafeBigDecimal(): BigDecimal {
    if (this.isNullOrBlank()) return BigDecimal.ZERO
    return try {
        BigDecimal(this)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}

/**
 * Safe division that returns ZERO instead of throwing on zero divisor.
 */
fun BigDecimal.safeDivide(divisor: BigDecimal, scale: Int = 8, roundingMode: RoundingMode = RoundingMode.HALF_UP): BigDecimal {
    if (divisor.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
    return this.divide(divisor, scale, roundingMode)
}

/**
 * Format BigDecimal as a currency display string: "1,234.56"
 * IMPORTANT: String.format("%f") does NOT work with BigDecimal on Android.
 * It throws IllegalFormatConversionException. Must convert to Double first.
 */
fun BigDecimal.fmtPkr(): String = "₨ " + String.format("%,.2f", this.toDouble())

/**
 * Format BigDecimal as a percentage display string: "12.34"
 */
fun BigDecimal.fmtPct(decimals: Int = 1): String = String.format("%.${decimals}f", this.toDouble())

/**
 * Format BigDecimal with no decimals: "1,234"
 */
fun BigDecimal.fmtInt(): String = String.format("%,.0f", this.toDouble())
