package com.sarmaya.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.viewmodel.TransactionsViewModel
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    type: String, // "BUY", "SELL", "DIVIDEND", "BONUS"
    onDismissRequest: () -> Unit,
    existingTransaction: Transaction? = null,
    preselectedSymbol: String? = null,
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)
) {
    var selectedStock by remember { mutableStateOf<Stock?>(null) }
    var showStockPicker by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    var quantity by remember { mutableStateOf(existingTransaction?.quantity?.toString() ?: "") }
    var pricePerShare by remember { mutableStateOf(existingTransaction?.pricePerShare?.toString() ?: "") }
    var date by remember { mutableStateOf(existingTransaction?.date ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(existingTransaction?.notes ?: "") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val searchResults by viewModel.searchResults.collectAsState()
    
    // Header and Labels logic
    val (title, qtyLabel, priceLabel, actionLabel) = when (type) {
        "BUY" -> listOf("Buy Stock", "Quantity", "Purchase Price (₨)", "Record Buy")
        "SELL" -> listOf("Sell Stock", "Quantity", "Sale Price (₨)", "Record Sell")
        "DIVIDEND" -> listOf("Add Dividend", "Shares Qualified", "Per Share Amount (₨)", "Record Dividend")
        "BONUS" -> listOf("Add Bonus", "Bonus Shares", "Cost Basis (Optional)", "Record Bonus")
        else -> listOf("Transaction", "Quantity", "Price", "Save")
    }

    LaunchedEffect(preselectedSymbol ?: existingTransaction?.stockSymbol) {
        val sym = preselectedSymbol ?: existingTransaction?.stockSymbol
        if (sym != null) {
            viewModel.updateSearchQuery(sym)
        }
    }
    
    LaunchedEffect(searchResults, preselectedSymbol ?: existingTransaction?.stockSymbol) {
        val sym = preselectedSymbol ?: existingTransaction?.stockSymbol
        if (sym != null && selectedStock == null) {
            val stock = searchResults.find { it.symbol == sym }
            if (stock != null) {
                selectedStock = stock
                if (pricePerShare.isEmpty() && stock.currentPrice > 0 && existingTransaction == null) {
                    pricePerShare = stock.currentPrice.toString()
                }
            }
        }
    }
    
    val isQuantityInvalid = quantity.isNotEmpty() && (quantity.toIntOrNull() ?: 0) <= 0
    val isPriceInvalid = pricePerShare.isNotEmpty() && (pricePerShare.toDoubleOrNull() ?: -1.0) < 0.0

    if (showStockPicker && existingTransaction == null) {
        StockPickerSheet(
            onDismissRequest = { showStockPicker = false },
            onStockSelected = {
                selectedStock = it
                showStockPicker = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Stock Selector
                OutlinedButton(
                    onClick = { if (existingTransaction == null) showStockPicker = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = existingTransaction == null
                ) {
                    Text(
                        selectedStock?.let { "${it.symbol} — ${it.name}" } 
                            ?: existingTransaction?.stockSymbol 
                            ?: "Select Stock",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Grid row: Quantity + Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) quantity = it },
                        label = { Text(qtyLabel) },
                        isError = isQuantityInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pricePerShare,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) pricePerShare = it },
                        label = { Text(priceLabel) },
                        isError = isPriceInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                if (type == "DIVIDEND" && quantity.isNotEmpty() && pricePerShare.isNotEmpty() && !isQuantityInvalid && !isPriceInvalid) {
                    val total = (quantity.toIntOrNull() ?: 0) * (pricePerShare.toDoubleOrNull() ?: 0.0)
                    Text(
                        "Total Amount: \u20A8 ${String.format("%,.2f", total)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Date display (simplified)
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date)),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime < 500) return@Button
                            lastClickTime = now

                            val sym = selectedStock?.symbol ?: existingTransaction?.stockSymbol
                            if (sym != null && quantity.isNotBlank() && !isQuantityInvalid && !isPriceInvalid) {
                                isProcessing = true
                                val qty = quantity.toIntOrNull()
                                val price = if (pricePerShare.isEmpty() && type == "BONUS") 0.0 else pricePerShare.toDoubleOrNull()
                                
                                if (qty == null || price == null) {
                                    errorMessage = "Invalid input values"
                                    isProcessing = false
                                    return@Button
                                }
                                
                                errorMessage = null
                                if (existingTransaction == null) {
                                    viewModel.addTransaction(
                                        stockSymbol = sym,
                                        type = type,
                                        quantity = qty,
                                        pricePerShare = price,
                                        date = date,
                                        notes = notes,
                                        onSuccess = { 
                                            isProcessing = false
                                            onDismissRequest() 
                                        },
                                        onError = { 
                                            isProcessing = false
                                            errorMessage = it 
                                        }
                                    )
                                } else {
                                    viewModel.updateTransaction(
                                        transactionId = existingTransaction.id,
                                        stockSymbol = sym,
                                        type = type,
                                        quantity = qty,
                                        pricePerShare = price,
                                        date = date,
                                        notes = notes,
                                        onSuccess = {
                                            isProcessing = false
                                            onDismissRequest()
                                        },
                                        onError = {
                                            isProcessing = false
                                            errorMessage = it
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (selectedStock != null || existingTransaction != null) && quantity.isNotBlank() && !isQuantityInvalid && !isPriceInvalid && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (existingTransaction != null) "Update" else actionLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
