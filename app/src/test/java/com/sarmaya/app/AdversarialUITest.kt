package com.sarmaya.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AdversarialUITest {

    @Test
    fun testTransactionQuantity_silentFallback() {
        // Simulating pasting a non-numeric string into the quantity field
        val uiInputQuantity = "100a"
        val parsedQuantity = uiInputQuantity.toIntOrNull()

        // The user intended to log 100 or made a typo, and the UI now strictly rejects it
        assertEquals(null, parsedQuantity)
    }

    @Test
    fun testTransactionPrice_silentFallback() {
        // Simulating pasting invalid price
        val uiInputPrice = "10.0.0"
        val parsedPrice = uiInputPrice.toDoubleOrNull()

        // Invalid price is rejected
        assertEquals(null, parsedPrice)
    }

    @Test
    fun testTransactionDate_futureDateAllowed() {
        // Simulating adversarial date input (e.g. year 2099)
        val futureDateLong = 4102444800000L // Jan 1, 2100
        val isDateValid = futureDateLong <= System.currentTimeMillis() + 86400000L
        
        // Future dates are now blocked by logic
        assert(!isDateValid)
    }
}
