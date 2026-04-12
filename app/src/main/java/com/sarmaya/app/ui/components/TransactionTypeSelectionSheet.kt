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
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                "Record Transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            TypeItem(
                title = "Buy Stock",
                subtitle = "Add new shares to your portfolio",
                icon = Icons.Default.Add,
                color = financeColors.profit,
                containerColor = financeColors.profitContainer.copy(alpha = 0.4f),
                onClick = { onTypeSelected("BUY") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TypeItem(
                title = "Sell Stock",
                subtitle = "Reduce or exit a position",
                icon = Icons.Default.KeyboardArrowDown,
                color = financeColors.loss,
                containerColor = financeColors.lossContainer.copy(alpha = 0.4f),
                onClick = { onTypeSelected("SELL") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TypeItem(
                title = "Cash Dividend",
                subtitle = "Record payouts from your holdings",
                icon = Icons.Default.KeyboardArrowUp,
                color = financeColors.dividend,
                containerColor = financeColors.dividendContainer.copy(alpha = 0.4f),
                onClick = { onTypeSelected("DIVIDEND") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TypeItem(
                title = "Bonus Issue",
                subtitle = "Record bonus shares received",
                icon = Icons.Default.Add,
                color = financeColors.warning,
                containerColor = financeColors.warningContainer.copy(alpha = 0.4f),
                onClick = { onTypeSelected("BONUS") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
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
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
