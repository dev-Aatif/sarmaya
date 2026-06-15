package com.sarmaya.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.network.ChartInterval
import com.sarmaya.app.network.ChartRange
import com.sarmaya.app.network.CompanyProfile
import com.sarmaya.app.network.PricePoint
import com.sarmaya.app.network.StockDataRepository
import com.sarmaya.app.network.UnifiedQuote
import com.sarmaya.app.data.*
import com.sarmaya.app.network.websocket.TickUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class StockDetailUiState {
    object Loading : StockDetailUiState()
    data class Success(
        val quote: UnifiedQuote,
        val profile: CompanyProfile,
        val chartData: List<PricePoint>,
        val peers: List<String>
    ) : StockDetailUiState()
    data class Error(val message: String) : StockDetailUiState()
}

class StockDetailViewModel(
    private val repository: StockDataRepository,
    private val wsManager: com.sarmaya.app.network.websocket.PsxWebSocketManager,
    private val symbol: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<StockDetailUiState>(StockDetailUiState.Loading)
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private val _chartRange = MutableStateFlow(ChartRange.ONE_DAY)
    val chartRange: StateFlow<ChartRange> = _chartRange.asStateFlow()

    init {
        refreshAll()
        
        // WebSocket logic: subscribe to this specific symbol
        wsManager.subscribe("tick:REG:$symbol")
        
        viewModelScope.launch {
            wsManager.tickUpdates
                .filter { it.symbol == symbol }
                .collect { update: TickUpdate ->
                    val currentState = _uiState.value
                    if (currentState is StockDetailUiState.Success) {
                        _uiState.value = currentState.copy(
                            quote = currentState.quote.copy(
                                price = update.price,
                                change = update.change,
                                changePercent = update.changePercent,
                                volume = update.volume,
                                trades = update.trades,
                                value = update.value,
                                marketState = update.marketState
                            )
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.unsubscribe("tick:REG:$symbol")
    }



    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = StockDetailUiState.Loading
            
            val quoteResult = repository.getQuote(symbol)
            val profileResult = repository.getCompanyProfile(symbol)
            val chartResult = repository.getHistoricalData(symbol, _chartRange.value)

            if (quoteResult.isSuccess) {
                _uiState.value = StockDetailUiState.Success(
                    quote = quoteResult.getOrThrow(),
                    profile = profileResult.getOrElse { emptyProfile(symbol) },
                    chartData = chartResult.getOrDefault(emptyList()),
                    peers = emptyList() // Peers logic temporarily disabled in v2
                )
            } else {
                val errorMsg = quoteResult.exceptionOrNull()?.message ?: "Failed to load"
                _uiState.value = StockDetailUiState.Error(errorMsg)
            }
        }
    }

    fun setChartRange(range: ChartRange) {
        if (_chartRange.value == range) return
        _chartRange.value = range
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is StockDetailUiState.Success) {
                val chartResult = repository.getHistoricalData(symbol, range)
                if (chartResult.isSuccess) {
                    _uiState.value = currentState.copy(chartData = chartResult.getOrThrow())
                }
            }
        }
    }

    private fun emptyProfile(symbol: String) = CompanyProfile(
        symbol = symbol,
        name = symbol,
        description = "Profile missing",
        sector = "",
        industry = "",
        website = "",
        phone = "",
        country = "PK",
        logoUrl = "",
        marketCap = 0L,
        peRatio = 0.0,
        eps = 0.0,
        beta = 0.0,
        weekHigh52 = 0.0,
        weekLow52 = 0.0,
        dividendYield = 0.0,
        earningsDate = ""
    )

    class Factory(private val symbol: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SarmayaApplication
            return StockDetailViewModel(
                application.container.stockDataRepository,
                application.container.psxWebSocketManager,
                symbol
            ) as T
        }
    }
}
