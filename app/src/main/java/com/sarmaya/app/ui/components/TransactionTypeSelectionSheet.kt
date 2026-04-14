package com.sarmaya.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                "Record Transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Choose the type of movement to track",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                TypeCard(
                    title = "Buy",
                    subtitle = "Add to portfolio",
                    icon = Icons.Default.Add,
                    color = financeColors.profit,
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected("BUY") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                TypeCard(
                    title = "Sell",
                    subtitle = "Exit position",
                    icon = Icons.AutoMirrored.Filled.Send,
                    color = financeColors.loss,
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected("SELL") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TypeCard(
                    title = "Dividend",
                    subtitle = "Cash payouts",
                    icon = Icons.Default.KeyboardArrowUp,
                    color = financeColors.dividend,
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected("DIVIDEND") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                TypeCard(
                    title = "Bonus",
                    subtitle = "Extra shares",
                    icon = Icons.Default.Star,
                    color = financeColors.warning,
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected("BONUS") }
                )
            }
        }
    }
}

@Composable
private fun TypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier.height(110.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
