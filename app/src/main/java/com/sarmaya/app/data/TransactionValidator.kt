package com.sarmaya.app.data

/**
 * Validates transactions before saving to prevent invalid portfolio states.
 */
object TransactionValidator {

    fun validateSell(
        holdings: List<ComputedHolding>,
        symbol: String,
        quantityToSell: Int
    ): ValidationResult {
        val currentHolding = holdings.find { it.stockSymbol == symbol }
        val currentQty = currentHolding?.quantity ?: 0
        
        return if (quantityToSell > currentQty) {
            ValidationResult.Error("Insufficient quantity. You only own $currentQty shares of $symbol.")
        } else {
            ValidationResult.Success
        }
    }

    fun validateBuy(
        symbol: String,
        quantity: Int,
        price: Double
    ): ValidationResult {
        return if (quantity <= 0) {
            ValidationResult.Error("Quantity must be greater than zero.")
        } else if (price <= 0) {
            ValidationResult.Error("Price must be greater than zero.")
        } else if (symbol.isBlank()) {
            ValidationResult.Error("Symbol cannot be empty.")
        } else {
            ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
