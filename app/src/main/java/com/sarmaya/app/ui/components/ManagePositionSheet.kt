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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Manage $stockSymbol", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

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
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = quantity,
                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) quantity = it },
                label = { Text(if (selectedType == "DIVIDEND") "Shares Paying Dividend" else "Quantity") },
                isError = isQuantityInvalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedType != "BONUS") {
                OutlinedTextField(
                    value = pricePerShare,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) pricePerShare = it },
                    label = { Text(if (selectedType == "DIVIDEND") "Dividend Per Share (₨)" else "Sell Price Per Share (₨)") },
                    isError = isPriceInvalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = quantity.isNotBlank() && !isQuantityInvalid && !isPriceInvalid && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm $selectedType", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
