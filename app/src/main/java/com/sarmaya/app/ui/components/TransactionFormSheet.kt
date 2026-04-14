package com.sarmaya.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.Stock
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.ui.theme.LocalSarmayaColors
import com.sarmaya.app.viewmodel.TransactionsViewModel
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
    val financeColors = LocalSarmayaColors.current
    
    val primaryColor = MaterialTheme.colorScheme.primary
    // UI Config based on type
    val config = remember(type, primaryColor, financeColors) {
        when (type) {
            "BUY" -> TransactionUIConfig("Buy Stock", "Quantity", "Price per share", "Record Purchase", Icons.Default.Add, financeColors.profit)
            "SELL" -> TransactionUIConfig("Sell Stock", "Quantity", "Sale Price", "Record Sale", Icons.AutoMirrored.Filled.Send, financeColors.loss)
            "DIVIDEND" -> TransactionUIConfig("Add Dividend", "Shares held", "Dividend/Share", "Record Dividend", Icons.Default.KeyboardArrowUp, financeColors.dividend)
            "BONUS" -> TransactionUIConfig("Add Bonus", "Bonus Shares", "Cost Basis", "Record Bonus", Icons.Default.Star, financeColors.warning)
            else -> TransactionUIConfig("Transaction", "Quantity", "Price", "Save", Icons.Default.Info, primaryColor)
        }
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
    
    val qtyVal = quantity.toIntOrNull() ?: 0
    val priceVal = pricePerShare.toDoubleOrNull() ?: 0.0
    val totalAmount = qtyVal * priceVal

    val isQuantityInvalid = quantity.isNotEmpty() && qtyVal <= 0
    val isPriceInvalid = pricePerShare.isNotEmpty() && priceVal < 0.0

    if (showStockPicker && existingTransaction == null) {
        StockPickerSheet(
            onDismissRequest = { showStockPicker = false },
            onStockSelected = {
                selectedStock = it
                showStockPicker = false
                if (pricePerShare.isEmpty() && (it.currentPrice) > 0) {
                    pricePerShare = it.currentPrice.toString()
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        config.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Enter the details of your transaction",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = config.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        config.icon,
                        contentDescription = null,
                        tint = config.color,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Asset Selection Card
            Surface(
                onClick = { if (existingTransaction == null) showStockPicker = true },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (selectedStock?.symbol?.take(1) ?: existingTransaction?.stockSymbol?.take(1) ?: "?").uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedStock?.symbol ?: existingTransaction?.stockSymbol ?: "Select Asset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedStock?.name ?: "Tap to choose a stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (existingTransaction == null) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Section (Modern Touch)
            AnimatedVisibility(
                visible = totalAmount > 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = config.color.copy(alpha = 0.05f),
                    modifier = Modifier.padding(bottom = 24.dp).fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (type == "DIVIDEND") "Total Dividend" else "Total Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = config.color
                        )
                        Text(
                            "₨ ${String.format("%,.2f", totalAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = config.color
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModernTextField(
                    value = quantity,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) quantity = it },
                    label = config.qtyLabel,
                    isError = isQuantityInvalid,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                ModernTextField(
                    value = pricePerShare,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) pricePerShare = it },
                    label = config.priceLabel,
                    isError = isPriceInvalid,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ModernTextField(
                value = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(date)),
                onValueChange = {},
                label = "Transaction Date",
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.outline) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            ModernTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes (Optional)",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Button
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                enabled = (selectedStock != null || existingTransaction != null) && quantity.isNotBlank() && !isQuantityInvalid && !isPriceInvalid && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = config.color,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = if (existingTransaction != null) "Update Transaction" else config.actionLabel, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            readOnly = readOnly,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = if (enabled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                unfocusedContainerColor = if (enabled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
            trailingIcon = trailingIcon
        )
    }
}

data class TransactionUIConfig(
    val title: String,
    val qtyLabel: String,
    val priceLabel: String,
    val actionLabel: String,
    val icon: ImageVector,
    val color: Color
)
