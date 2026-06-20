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
import com.sarmaya.app.data.PortfolioDao
import com.sarmaya.app.network.api.GithubApi
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
import java.io.BufferedReader
import java.io.InputStreamReader
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val dataStoreManager: DataStoreManager,
    private val githubApi: GithubApi,
    private val portfolioDao: PortfolioDao
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


    private val _isUpdateAvailable = MutableStateFlow<String?>(null) // Contains the new version name or null
    val isUpdateAvailable: StateFlow<String?> = _isUpdateAvailable.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _updateUrl = MutableStateFlow<String?>(null)
    val updateUrl: StateFlow<String?> = _updateUrl.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val latest = githubApi.getLatestRelease("dev-Aatif", "Sarmaya")
                val currentVersion = BuildConfig.VERSION_NAME
                
                val tag = latest.tagName.removePrefix("v").trim()
                
                fun compareVersions(v1: String, v2: String): Int {
                    val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
                    val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
                    val length = maxOf(parts1.size, parts2.size)
                    for (i in 0 until length) {
                        val p1 = parts1.getOrElse(i) { 0 }
                        val p2 = parts2.getOrElse(i) { 0 }
                        if (p1 != p2) return p1.compareTo(p2)
                    }
                    return 0
                }

                if (compareVersions(tag, currentVersion) > 0) {
                    _isUpdateAvailable.value = tag
                    _updateUrl.value = latest.htmlUrl
                } else {
                    _isUpdateAvailable.value = null
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "Update check failed", e)
            }
        }
    }

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

    suspend fun getPortfolioCsvContent(): String {
        val transactions = transactionDao.getAllTransactions().first()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val csvHeader = "ID,PortfolioID,StockSymbol,Type,Quantity,PricePerShare,Date,CommissionType,CommissionAmount,Notes\n"
        val sb = java.lang.StringBuilder(csvHeader)

        for (t in transactions) {
            val formattedNotes = t.notes.replace("\"", "\"\"")
            val formattedDate = isoFormat.format(Date(t.date))
            sb.append("${t.id},${t.portfolioId},${t.stockSymbol},${t.type},${t.quantity},${t.pricePerShare},${formattedDate},${t.commissionType},${t.commissionAmount},\"${formattedNotes}\"\n")
        }
        return sb.toString()
    }

    fun importPortfolioFromCsv(uri: android.net.Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val header = reader.readLine() // Skip header
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

                    var line: String? = reader.readLine()
                    val transactions = mutableListOf<Transaction>()

                    while (line != null) {
                        try {
                            val parts = parseCsvLine(line)
                            if (parts.size >= 10) {
                                val domainModel = com.sarmaya.app.domain.TransactionDomainModel(
                                    portfolioId = parts[1].toLongOrNull() ?: 1,
                                    stockSymbol = parts[2],
                                    type = parts[3],
                                    quantity = parts[4].toIntOrNull() ?: 1,
                                    pricePerShare = parts[5].toDoubleOrNull() ?: 0.0,
                                    date = isoFormat.parse(parts[6])?.time ?: System.currentTimeMillis(),
                                    commissionType = parts[7],
                                    commissionAmount = parts[8].toDoubleOrNull() ?: 0.0,
                                    notes = parts[9].trim('\"').replace("\"\"", "\"")
                                )
                                val validationError = domainModel.validate()
                                if (validationError == null) {
                                    transactions.add(domainModel.toEntity())
                                } else {
                                    Log.e("SettingsVM", "Validation failed for line: $line. Reason: $validationError")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SettingsVM", "Failed to parse CSV line: $line", e)
                        }
                        line = reader.readLine()
                    }

                    if (transactions.isNotEmpty()) {
                        var isValid = true
                        transactions.groupBy { it.portfolioId to it.stockSymbol }.forEach { (key, txs) ->
                            val existingTxs = transactionDao.getTransactionsForStockInPortfolio(key.second, key.first)
                            val allTxs = existingTxs + txs
                            val sorted = allTxs.sortedBy { it.date }
                            var runningBalance = 0
                            for (txn in sorted) {
                                when (txn.type) {
                                    "BUY", "BONUS" -> runningBalance += txn.quantity
                                    "SELL" -> runningBalance -= txn.quantity
                                    "SPLIT" -> {
                                        val factor = txn.splitRatio ?: 1.0
                                        runningBalance = Math.round(runningBalance * factor).toInt()
                                    }
                                }
                                if (runningBalance < 0) {
                                    isValid = false
                                }
                            }
                        }
                        if (isValid) {
                            val portfolioIds = transactions.map { it.portfolioId }.distinct()
                            for (pid in portfolioIds) {
                                val existingPortfolio = portfolioDao.getPortfolioById(pid)
                                if (existingPortfolio == null) {
                                    portfolioDao.insert(com.sarmaya.app.data.Portfolio(id = pid, name = "Imported Portfolio $pid"))
                                }
                            }
                            transactionDao.insertTransactions(transactions)
                        } else {
                            Log.e("SettingsVM", "Chronological balance validation failed for imported data.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "Import failed", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    currentToken.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentToken.toString())
                currentToken = StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString())
        return result
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
                    application.container.dataStoreManager,
                    application.container.githubApi,
                    application.container.portfolioDao
                ) as T
            }
        }
    }
}
