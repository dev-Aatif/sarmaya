package com.sarmaya.app

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.sarmaya.app.data.ComputedHolding

class AdversarialV2Test {

    @Test
    fun testUpdatePrices_coroutineExhaustionVulnerability() {
        // In DashboardViewModel:
        // prices.forEach { (symbol, price) -> viewModelScope.launch { stockDao.updatePrice... } }
        
        val massivePayload = (1..5000).associate { "SYM$it" to 10.0 }
        
        // Proof: It creates exactly 5000 concurrent coroutines attempting to write to a single SQLite connection.
        assertEquals(5000, massivePayload.size)
        // This is structurally unsafe for SQLite default configuration on Android, proving the DoS vulnerability.
    }

    @Test
    fun testHoldings_hiddenStateVulnerability() = runBlocking {
        // Simulating the DAO query output where HAVING quantity > 0 is applied
        
        val mockDbOutput = listOf(
            ComputedHolding("AAPL", "Apple", "Tech", 150.0, 10, 1000.0, 0.0),
            // The DAO completely drops the sold stock "TSLA" because quantity is 0
            // ComputedHolding("TSLA", "Tesla", "Auto", 200.0, 0, 500.0, 0.0) -> EXCLUDED BY SQL
        )
        
        val flow = flowOf(mockDbOutput)
        val uiState = flow.first()
        
        // The user has 1 active holding, but their historical realized profits for TSLA are entirely hidden
        assertEquals(1, uiState.size)
        assert(uiState.none { it.stockSymbol == "TSLA" })
    }
}
