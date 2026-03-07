package com.sarmaya.app.viewmodel

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import android.content.Intent
import androidx.core.content.FileProvider
import com.sarmaya.app.data.TransactionDao
import com.sarmaya.app.SarmayaApplication
import java.io.File
import java.io.FileWriter

class SettingsViewModel(
    private val context: Context,
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val prefs = context.getSharedPreferences("sarmaya_settings", Context.MODE_PRIVATE)
    
    // null = system default, true = dark, false = light
    private val _isDarkTheme = MutableStateFlow<Boolean?>(
        if (prefs.contains("dark_theme")) prefs.getBoolean("dark_theme", false) else null
    )
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "dark_theme") {
            _isDarkTheme.value = if (sharedPreferences.contains("dark_theme")) sharedPreferences.getBoolean("dark_theme", false) else null
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun setTheme(darkTheme: Boolean?) {
        _isDarkTheme.value = darkTheme
        prefs.edit { 
            if (darkTheme == null) {
                remove("dark_theme")
            } else {
                putBoolean("dark_theme", darkTheme)
            }
        }
    }

    fun exportPortfolioToCsv() {
        viewModelScope.launch {
            try {
                val transactions = transactionDao.getAllTransactions().first()
                val csvHeader = "ID,StockSymbol,Type,Quantity,PricePerShare,Date,Notes\n"
                val sb = java.lang.StringBuilder(csvHeader)
                
                for (t in transactions) {
                    val formattedNotes = t.notes.replace("\"", "\"\"")
                    sb.append("${t.id},${t.stockSymbol},${t.type},${t.quantity},${t.pricePerShare},${t.date},\"${formattedNotes}\"\n")
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
                e.printStackTrace()
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
                return SettingsViewModel(application, application.container.transactionDao) as T
            }
        }
    }
}
