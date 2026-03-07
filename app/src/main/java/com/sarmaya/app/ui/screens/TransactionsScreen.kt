package com.sarmaya.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.viewmodel.TransactionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    if (showAddSheet) {
        com.sarmaya.app.ui.components.AddTransactionSheet(
            onDismissRequest = { showAddSheet = false }
        )
    }
    
    if (transactionToEdit != null) {
        com.sarmaya.app.ui.components.EditTransactionSheet(
            transaction = transactionToEdit!!,
            onDismissRequest = { transactionToEdit = null }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transactionToDelete!!)
                    transactionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Transaction History", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(transactions) { tx ->
                    TransactionItem(
                        tx = tx,
                        onEdit = { transactionToEdit = it },
                        onDelete = { transactionToDelete = it }
                    )
                    Divider()
                }
                if (transactions.isEmpty()) {
                    item {
                        Text("No transactions logged.", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: Transaction, onEdit: (Transaction) -> Unit, onDelete: (Transaction) -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(tx.date))
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${tx.type} ${tx.stockSymbol}", fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall)
                }
                Text("${tx.quantity} shares @ ₨ ${String.format("%,.2f", tx.pricePerShare)}")
                if (tx.notes.isNotBlank()) {
                    Text("Note: ${tx.notes}", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
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
