package com.sarmaya.app.ui.components

import androidx.compose.runtime.*
import com.sarmaya.app.data.Transaction

/**
 * A reusable component that handles the multi-step transaction flow:
 * Select Type -> Fill Form.
 */
@Composable
fun TransactionFlow(
    showTypeSelection: Boolean,
    showTransactionForm: String?, // "BUY", "SELL", etc.
    preselectedSymbol: String? = null,
    existingTransaction: Transaction? = null,
    onTypeSelected: (String) -> Unit,
    onDismissTypeSelection: () -> Unit,
    onDismissForm: () -> Unit
) {
    if (showTypeSelection) {
        TransactionTypeSelectionSheet(
            onDismissRequest = onDismissTypeSelection,
            onTypeSelected = onTypeSelected
        )
    }

    if (showTransactionForm != null) {
        TransactionFormSheet(
            type = showTransactionForm,
            preselectedSymbol = preselectedSymbol,
            existingTransaction = existingTransaction,
            onDismissRequest = onDismissForm
        )
    }
}
