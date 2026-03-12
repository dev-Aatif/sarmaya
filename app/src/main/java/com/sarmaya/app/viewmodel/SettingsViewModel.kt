package com.sarmaya.app.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.DataStoreManager
import com.sarmaya.app.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    // null = system default, true = dark, false = light
    val isDarkTheme: StateFlow<Boolean?> = dataStoreManager.darkThemePreference
        .map { pref ->
            when (pref) {
                "true" -> true
                "false" -> false
                else -> null // "system"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val username: StateFlow<String> = dataStoreManager.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val notificationsPortfolio: StateFlow<Boolean> = dataStoreManager.notificationsPortfolio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsMarket: StateFlow<Boolean> = dataStoreManager.notificationsMarket
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsUpdates: StateFlow<Boolean> = dataStoreManager.notificationsUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTheme(darkTheme: Boolean?) {
        viewModelScope.launch {
            val pref = when (darkTheme) {
                true -> "true"
                false -> "false"
                null -> "system"
            }
            dataStoreManager.setDarkTheme(pref)
        }
    }

    fun setUsername(name: String) {
        viewModelScope.launch {
            dataStoreManager.setUsername(name)
        }
    }

    fun setNotificationPortfolio(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNotificationPreference(DataStoreManager.KEY_NOTIFICATIONS_PORTFOLIO, enabled)
        }
    }

    fun setNotificationMarket(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNotificationPreference(DataStoreManager.KEY_NOTIFICATIONS_MARKET, enabled)
        }
    }

    fun setNotificationUpdates(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNotificationPreference(DataStoreManager.KEY_NOTIFICATIONS_UPDATES, enabled)
        }
    }

    fun exportPortfolioToCsv() {
        viewModelScope.launch {
            try {
                val transactions = transactionDao.getAllTransactions().first()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val csvHeader = "ID,PortfolioID,StockSymbol,Type,Quantity,PricePerShare,Date,CommissionType,CommissionAmount,Notes\n"
                val sb = java.lang.StringBuilder(csvHeader)

                for (t in transactions) {
                    val formattedNotes = t.notes.replace("\"", "\"\"")
                    val formattedDate = isoFormat.format(Date(t.date))
                    sb.append("${t.id},${t.portfolioId},${t.stockSymbol},${t.type},${t.quantity},${t.pricePerShare},${formattedDate},${t.commissionType},${t.commissionAmount},\"${formattedNotes}\"\n")
                }

                val exportDir = File(context.cacheDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()

                val file = File(exportDir, "sarmaya_portfolio_export.csv")
                val writer = FileWriter(file)
                writer.append(sb.toString())
                writer.flush()
                writer.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Sarmaya Portfolio Export")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Export Portfolio")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } catch (e: Exception) {
                Log.e("SettingsVM", "Export failed", e)
            }
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
                return SettingsViewModel(
                    application,
                    application.container.transactionDao,
                    application.container.dataStoreManager
                ) as T
            }
        }
    }
}
