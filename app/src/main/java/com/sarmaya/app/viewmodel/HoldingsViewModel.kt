package com.sarmaya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.StockDao
import com.sarmaya.app.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import com.sarmaya.app.data.PortfolioCalculator

class HoldingsViewModel(
    private val transactionDao: TransactionDao,
    private val stockDao: StockDao
) : ViewModel() {

    // Sorted by descending current value by default
    val holdings = PortfolioCalculator.getEventSourcedHoldings(
        transactionDao.getAllTransactions(),
        stockDao.getAllStocks()
    ).map { list ->
        list.sortedByDescending { it.currentValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SarmayaApplication
                return HoldingsViewModel(
                    application.container.transactionDao,
                    application.container.stockDao
                ) as T
            }
        }
    }
}
