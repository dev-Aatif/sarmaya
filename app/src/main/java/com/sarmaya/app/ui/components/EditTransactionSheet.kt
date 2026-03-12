package com.sarmaya.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.viewmodel.TransactionsViewModel
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: Transaction,
    onDismissRequest: () -> Unit,
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)
) {
    var isProcessing by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    var selectedType by remember { mutableStateOf(transaction.type) }
    var quantity by remember { mutableStateOf(transaction.quantity.toString()) }
    var pricePerShare by remember { mutableStateOf(transaction.pricePerShare.toString()) }
    var date by remember { mutableStateOf(transaction.date) }
    var notes by remember { mutableStateOf(transaction.notes) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val types = listOf("BUY", "SELL", "DIVIDEND", "BONUS", "SPLIT")
    var expanded by remember { mutableStateOf(false) }

    val isQuantityInvalid = quantity.isNotEmpty() && (quantity.toIntOrNull() ?: 0) <= 0
    val isPriceInvalid = pricePerShare.isNotEmpty() && (pricePerShare.toDoubleOrNull() ?: -1.0) < 0.0

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
                    "Edit Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Grid row: Stock Symbol + Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = transaction.stockSymbol,
                        onValueChange = {},
                        label = { Text("Stock") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedType,
                            onValueChange = { },
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            types.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        selectedType = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

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
                        label = { Text("Qty") },
                        isError = isQuantityInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pricePerShare,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) pricePerShare = it },
                        label = { Text("Price (₨)") },
                        isError = isPriceInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

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

                            if (quantity.isNotBlank() && !isQuantityInvalid && !isPriceInvalid) {
                                isProcessing = true
                                val qty = quantity.toIntOrNull()
                                val price = pricePerShare.toDoubleOrNull()
                                if (qty == null || price == null) {
                                    errorMessage = "Invalid input values"
                                    isProcessing = false
                                    return@Button
                                }
                                errorMessage = null
                                viewModel.updateTransaction(
                                    transactionId = transaction.id,
                                    stockSymbol = transaction.stockSymbol,
                                    type = selectedType,
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
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = quantity.isNotBlank() && !isQuantityInvalid && !isPriceInvalid && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
