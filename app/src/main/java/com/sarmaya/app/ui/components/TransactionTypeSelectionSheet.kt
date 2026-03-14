package com.sarmaya.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarmaya.app.ui.theme.LocalSarmayaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeSelectionSheet(
    onDismissRequest: () -> Unit,
    onTypeSelected: (String) -> Unit
) {
    val financeColors = LocalSarmayaColors.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Select Transaction Type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            TypeItem(
                title = "Buy",
                subtitle = "Record a new stock purchase",
                icon = Icons.Default.Add,
                color = financeColors.profit,
                containerColor = financeColors.profitContainer.copy(alpha = 0.5f),
                onClick = { onTypeSelected("BUY") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TypeItem(
                title = "Sell",
                subtitle = "Record a stock sale",
                icon = Icons.Default.KeyboardArrowDown,
                color = financeColors.loss,
                containerColor = financeColors.lossContainer.copy(alpha = 0.5f),
                onClick = { onTypeSelected("SELL") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TypeItem(
                title = "Dividend",
                subtitle = "Record income from dividends",
                icon = Icons.Default.KeyboardArrowUp,
                color = financeColors.dividend,
                containerColor = financeColors.dividendContainer.copy(alpha = 0.5f),
                onClick = { onTypeSelected("DIVIDEND") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TypeItem(
                title = "Bonus",
                subtitle = "Record bonus shares received",
                icon = Icons.Default.Add,
                color = financeColors.warning,
                containerColor = financeColors.warningContainer.copy(alpha = 0.5f),
                onClick = { onTypeSelected("BONUS") }
            )
        }
    }
}

@Composable
private fun TypeItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
