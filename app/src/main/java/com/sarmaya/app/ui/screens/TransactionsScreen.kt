package com.sarmaya.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.data.*
import com.sarmaya.app.ui.components.PortfolioSelector
import com.sarmaya.app.ui.components.TransactionFlow
import com.sarmaya.app.ui.theme.*
import com.sarmaya.app.viewmodel.TransactionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val allPortfolios by viewModel.allPortfolios.collectAsStateWithLifecycle()
    val activePortfolio by viewModel.activePortfolio.collectAsStateWithLifecycle()
    
    var showTypeSelection by remember { mutableStateOf(false) }
    var showTransactionForm by remember { mutableStateOf<Pair<String, Transaction?>?>(null) }
    var selectedStockForForm by remember { mutableStateOf<String?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf("All", "BUY", "SELL", "DIVIDEND", "BONUS", "SPLIT")
    val filteredTransactions = if (selectedFilter == "All") {
        transactions
    } else {
        transactions.filter { it.type == selectedFilter }
    }

    val financeColors = LocalSarmayaColors.current

    TransactionFlow(
        showTypeSelection = showTypeSelection,
        showTransactionForm = showTransactionForm?.first,
        existingTransaction = showTransactionForm?.second,
        preselectedSymbol = selectedStockForForm,
        onTypeSelected = { type ->
            showTypeSelection = false
            showTransactionForm = type to null
        },
        onDismissTypeSelection = { showTypeSelection = false },
        onDismissForm = {
            showTransactionForm = null
            selectedStockForForm = null
        }
    )

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transactionToDelete!!)
                    transactionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("Ledger", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                PortfolioSelector(
                    activePortfolio = activePortfolio,
                    allPortfolios = allPortfolios,
                    onPortfolioSelected = { viewModel.selectPortfolio(it) },
                    onCreatePortfolio = { viewModel.createPortfolio(it) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Premium Filter Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = selectedFilter == filter,
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTransactionForm = "BUY" to null },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Filled.Add, "Add Transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📓", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (selectedFilter == "All") "No transactions yet" else "No $selectedFilter recorded",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(filteredTransactions) { tx ->
                    TransactionItem(
                        tx = tx,
                        onEdit = { showTransactionForm = tx.type to tx },
                        onDelete = { transactionToDelete = it },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    tx: Transaction,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalSarmayaColors.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(tx.date))
    var expanded by remember { mutableStateOf(false) }

    val (badgeColor, badgeTextColor) = when (tx.type) {
        "BUY" -> financeColors.profitContainer to financeColors.onProfitContainer
        "SELL" -> financeColors.lossContainer to financeColors.onLossContainer
        "DIVIDEND" -> financeColors.dividendContainer to financeColors.dividend
        "BONUS" -> financeColors.warningContainer to financeColors.warning
        "SPLIT" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val totalAmount = tx.quantity * tx.pricePerShare

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type badge
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    tx.type,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        tx.stockSymbol,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${tx.quantity} shares @ ₨ ${String.format("%,.2f", tx.pricePerShare)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (totalAmount > 0) {
                        Text(
                            "₨ ${String.format("%,.0f", totalAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (tx.type) {
                                "BUY" -> financeColors.loss  // money going out
                                "SELL" -> financeColors.profit  // money coming in
                                "DIVIDEND" -> financeColors.dividend
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                if (tx.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        tx.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            // Options menu
            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            expanded = false
                            onEdit(tx)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onDelete(tx)
                        }
                    )
                }
            }
        }
    }
}
