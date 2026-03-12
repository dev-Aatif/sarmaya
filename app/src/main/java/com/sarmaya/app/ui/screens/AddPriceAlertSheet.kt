package com.sarmaya.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sarmaya.app.data.Stock
import com.sarmaya.app.ui.components.StockPickerSheet
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AddPriceAlertSheet(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String) -> Unit
) {
    var selectedStock by remember { mutableStateOf<Stock?>(null) }
    var targetPrice by remember { mutableStateOf("") }
    var alertType by remember { mutableStateOf("ABOVE") } // ABOVE or BELOW
    var showStockPicker by remember { mutableStateOf(false) }

    if (showStockPicker) {
        StockPickerSheet(
            onDismissRequest = { showStockPicker = false },
            onStockSelected = {
                selectedStock = it
                showStockPicker = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Create Price Alert",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Stock Picker
                OutlinedButton(
                    onClick = { showStockPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(selectedStock?.symbol ?: "Select Stock")
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Price Input
                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) targetPrice = it },
                    label = { Text("Target Price (₨)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Trigger Type
                Text(
                    "Notify me when price goes:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = alertType == "ABOVE",
                        onClick = { alertType = "ABOVE" },
                        label = { Text("ABOVE") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = alertType == "BELOW",
                        onClick = { alertType = "BELOW" },
                        label = { Text("BELOW") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val price = targetPrice.toDoubleOrNull()
                            if (selectedStock != null && price != null) {
                                onAdd(selectedStock!!.symbol, price, alertType)
                            }
                        },
                        enabled = selectedStock != null && targetPrice.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Alert")
                    }
                }
            }
        }
    }
}
