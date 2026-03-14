package com.sarmaya.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.ui.theme.*
import com.sarmaya.app.viewmodel.DashboardViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePricesSheet(
    onDismissRequest: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val holdings by viewModel.computedHoldings.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val financeColors = LocalSarmayaColors.current
    
    // Use TextFieldValue for proper cursor/selection control
    val priceFields = remember { mutableStateMapOf<String, TextFieldValue>() }

    LaunchedEffect(holdings) {
        holdings.forEach { h ->
            if (!priceFields.containsKey(h.stockSymbol)) {
                val text = String.format("%.2f", h.currentPrice)
                priceFields[h.stockSymbol] = TextFieldValue(text)
            }
        }
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
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Column(
                    modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Update Prices",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${holdings.filter { it.quantity > 0 }.size} active stocks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.refreshPrices() },
                            enabled = !isRefreshing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auto Sync", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Scrollable stock list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(holdings.filter { it.quantity > 0 }) { holding ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = financeColors.cardSurface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            holding.stockSymbol,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            holding.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    val isProfit = holding.profitLossPercentage >= 0
                                    Surface(
                                        color = if (isProfit) financeColors.profitContainer else financeColors.lossContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "${if (isProfit) "+" else ""}${String.format("%.1f", holding.profitLossPercentage)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProfit) financeColors.onProfitContainer else financeColors.onLossContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                OutlinedTextField(
                                    value = priceFields[holding.stockSymbol] ?: TextFieldValue(""),
                                    onValueChange = { newValue ->
                                        // Filter to allow only digits and one decimal point
                                        val filtered = newValue.text.replace(',', '.')
                                        val dotCount = filtered.count { it == '.' }
                                        if (filtered.isEmpty() || (filtered.all { it.isDigit() || it == '.' } && dotCount <= 1)) {
                                            priceFields[holding.stockSymbol] = newValue.copy(text = filtered)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                // Select all text on focus for easy replacement
                                                val current = priceFields[holding.stockSymbol]
                                                if (current != null) {
                                                    priceFields[holding.stockSymbol] = current.copy(
                                                        selection = TextRange(0, current.text.length)
                                                    )
                                                }
                                            }
                                        },
                                    label = { Text("Price (₨)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // Sticky footer with action buttons
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 12.dp, 16.dp, 16.dp),
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
                            val updates = priceFields.mapNotNull { (symbol, fieldValue) ->
                                val cleanStr = fieldValue.text.replace(',', '.').replace(Regex("[^\\d.]"), "")
                                val safeStr = buildString {
                                    var dotSeen = false
                                    for (c in cleanStr) {
                                        if (c == '.') {
                                            if (!dotSeen) { append(c); dotSeen = true }
                                        } else { append(c) }
                                    }
                                }
                                val parsed = safeStr.toDoubleOrNull()
                                if (parsed != null) symbol to parsed else null
                            }.toMap()
                            viewModel.updatePrices(updates)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Prices", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
