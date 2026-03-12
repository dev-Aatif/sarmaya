package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.PriceAlert
import com.sarmaya.app.data.PriceAlertDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PriceAlertViewModel(
    private val priceAlertDao: PriceAlertDao
) : ViewModel() {

    val allAlerts: StateFlow<List<PriceAlert>> = priceAlertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlert(symbol: String, targetPrice: Double, type: String) {
        viewModelScope.launch {
            priceAlertDao.insert(
                PriceAlert(
                    stockSymbol = symbol,
                    targetPrice = targetPrice,
                    alertType = type
                )
            )
        }
    }

    fun deleteAlert(alert: PriceAlert) {
        viewModelScope.launch {
            priceAlertDao.delete(alert)
        }
    }

    fun toggleAlert(alert: PriceAlert) {
        viewModelScope.launch {
            priceAlertDao.update(alert.copy(isActive = !alert.isActive))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SarmayaApplication
                return PriceAlertViewModel(
                    application.container.priceAlertDao
                ) as T
            }
        }
    }
}
