package com.sarmaya.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.viewmodel.TransactionsViewModel
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePositionSheet(
    stockSymbol: String,
    onDismissRequest: () -> Unit,
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)
) {
    var selectedType by remember { mutableStateOf("SELL") }
    var quantity by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }
    var pricePerShare by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val types = listOf("SELL", "DIVIDEND", "BONUS")
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
                    "Manage $stockSymbol",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Action type dropdown — full width
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedType,
                        onValueChange = { },
                        label = { Text("Action") },
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
                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Grid row: Quantity + Price side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) quantity = it },
                        label = { Text(if (selectedType == "DIVIDEND") "Shares" else "Qty") },
                        isError = isQuantityInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    if (selectedType != "BONUS") {
                        OutlinedTextField(
                            value = pricePerShare,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) pricePerShare = it },
                            label = { Text(if (selectedType == "DIVIDEND") "Per Share (₨)" else "Price (₨)") },
                            isError = isPriceInvalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Notes — full width
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons — grid row
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
                                val qty = quantity.toIntOrNull() ?: 0
                                val price = pricePerShare.toDoubleOrNull() ?: 0.0
                                errorMessage = null
                                viewModel.addTransaction(
                                    stockSymbol = stockSymbol,
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
                            Text("Confirm", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
