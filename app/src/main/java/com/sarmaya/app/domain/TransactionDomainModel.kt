package com.sarmaya.app.domain

import com.sarmaya.app.data.Transaction

/**
 * Encapsulates strict business rules and validation for a Transaction.
 * This Domain Model ensures no invalid data can reach the repository layer.
 */
data class TransactionDomainModel(
    val id: Long = 0,
    val stockSymbol: String,
    val type: String,
    val quantity: Int,
    val pricePerShare: Double,
    val date: Long,
    val notes: String = ""
) {
    companion object {
        val VALID_TYPES = listOf("BUY", "SELL", "DIVIDEND", "BONUS", "SPLIT")
    }

    /**
     * Validates the transaction constraints.
     * @return null if valid, otherwise returns the specific error message.
     */
    fun validate(): String? {
        if (type !in VALID_TYPES) {
            return "Invalid transaction type: $type"
        }
        if (pricePerShare.isNaN() || pricePerShare.isInfinite()) {
            return "Price cannot be NaN or Infinite"
        }
        if (quantity <= 0) {
            return "Quantity must be strictly positive"
        }
        if (date > System.currentTimeMillis() + 86400000L) { // Allow up to 1 day in the future just in case of timezone delays
            return "Transaction date cannot be in the future"
        }
        if (pricePerShare < 0.01 && (type == "BUY" || type == "SELL")) {
            return "Price must be at least 0.01 for BUY/SELL"
        }
        if (pricePerShare < 0.0 && type != "BONUS") {
            return "Price must be non-negative"
        }
        if (notes.length > 500) {
            return "Notes cannot exceed 500 characters"
        }
        return null
    }

    fun toEntity(): Transaction {
        return Transaction(
            id = id,
            stockSymbol = stockSymbol,
            type = type,
            quantity = quantity,
            pricePerShare = pricePerShare,
            date = date,
            notes = notes
        )
    }
}
