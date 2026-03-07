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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePricesSheet(
    onDismissRequest: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val holdings by viewModel.computedHoldings.collectAsState()
    // Just keeping local state for the editable fields
    val priceMap = remember { mutableStateMapOf<String, String>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(holdings) {
        holdings.forEach { h ->
            if (!priceMap.containsKey(h.stockSymbol)) {
                priceMap[h.stockSymbol] = h.currentPrice.toString()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Update Current Prices", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(holdings) { holding ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(holding.stockSymbol, fontWeight = FontWeight.Bold)
                            Text(holding.name, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        OutlinedTextField(
                            value = priceMap[holding.stockSymbol] ?: "",
                            onValueChange = { priceMap[holding.stockSymbol] = it },
                            modifier = Modifier.width(120.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                    Divider()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val updates = priceMap.mapNotNull { (symbol, priceStr) ->
                        // Standardize string mapping: replace commas with dots, and strip all non-digit/dot characters
                        val cleanStr = priceStr.replace(',', '.').replace(Regex("[^\\d.]"), "")
                        
                        // Handle multiple dots by keeping only the first one
                        val safeStr = buildString {
                            var dotSeen = false
                            for (c in cleanStr) {
                                if (c == '.') {
                                    if (!dotSeen) {
                                        append(c)
                                        dotSeen = true
                                    }
                                } else {
                                    append(c)
                                }
                            }
                        }
                        
                        val parsed = safeStr.toDoubleOrNull()
                        if (parsed != null) symbol to parsed else null
                    }.toMap()
                    viewModel.updatePrices(updates)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Prices", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
