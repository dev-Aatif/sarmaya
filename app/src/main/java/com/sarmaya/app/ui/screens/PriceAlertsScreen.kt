package com.sarmaya.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarmaya.app.data.PriceAlert
import com.sarmaya.app.ui.theme.LocalSarmayaColors
import com.sarmaya.app.viewmodel.PriceAlertViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsScreen(
    onDismiss: () -> Unit,
    viewModel: PriceAlertViewModel = viewModel(factory = PriceAlertViewModel.Factory)
) {
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    var showAddAlertSheet by remember { mutableStateOf(false) }

    val financeColors = LocalSarmayaColors.current

    if (showAddAlertSheet) {
        // We'll reuse the stock picker logic or a simple sheet
        AddPriceAlertSheet(
            onDismiss = { showAddAlertSheet = false },
            onAdd = { symbol, price, type ->
                viewModel.addAlert(symbol, price, type)
                showAddAlertSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Price Alerts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(androidx.compose.material.icons.Icons.Default.Notifications, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddAlertSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Alert")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(
                    "Set target prices for your favorite stocks and get notified when they are reached.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (alerts.isEmpty()) {
                item {
                    EmptyAlertsState()
                }
            } else {
                items(alerts) { alert ->
                    AlertItem(
                        alert = alert,
                        onToggle = { viewModel.toggleAlert(alert) },
                        onDelete = { viewModel.deleteAlert(alert) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertItem(
    alert: PriceAlert,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val financeColors = LocalSarmayaColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = financeColors.cardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.stockSymbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${if (alert.alertType == "ABOVE") "Crosses Above" else "Drops Below"} ₨ ${String.format("%,.2f", alert.targetPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (alert.isTriggered) {
                    Text(
                        "Triggered",
                        style = MaterialTheme.typography.labelSmall,
                        color = financeColors.profit,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alert.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    enabled = !alert.isTriggered
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlertsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No alerts set",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Tap + to create your first price alert",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
