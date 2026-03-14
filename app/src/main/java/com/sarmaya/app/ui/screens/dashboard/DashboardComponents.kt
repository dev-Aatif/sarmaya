package com.sarmaya.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarmaya.app.data.ComputedHolding
import com.sarmaya.app.data.Transaction
import com.sarmaya.app.ui.theme.SarmayaFinanceColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QuickStatCard(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MoverChip(
    holding: ComputedHolding,
    isGainer: Boolean,
    financeColors: SarmayaFinanceColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGainer) financeColors.profitContainer else financeColors.lossContainer
        )
    ) {
        Column(
            modifier = Modifier
                .width(140.dp)
                .padding(14.dp)
        ) {
            Text(
                holding.stockSymbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isGainer) financeColors.onProfitContainer else financeColors.onLossContainer
            )
            Text(
                "${if (isGainer) "+" else ""}${String.format("%.2f", holding.profitLossPercentage)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isGainer) financeColors.profit else financeColors.loss
            )
            Text(
                "₨ ${String.format("%.2f", holding.currentPrice)}",
                style = MaterialTheme.typography.labelSmall,
                color = (if (isGainer) financeColors.onProfitContainer else financeColors.onLossContainer).copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RecentTransactionRow(
    tx: Transaction,
    financeColors: SarmayaFinanceColors,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    val dateStr = try {
        sdf.format(Date(tx.timestamp))
    } catch (e: Exception) {
        ""
    }

    val (badgeColor, badgeTextColor) = when (tx.type) {
        "BUY" -> financeColors.profitContainer to financeColors.onProfitContainer
        "SELL" -> financeColors.lossContainer to financeColors.onLossContainer
        "DIVIDEND" -> financeColors.dividendContainer to financeColors.dividend
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    tx.type,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    tx.stockSymbol,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${tx.quantity} shares @ ₨ ${String.format("%,.2f", tx.pricePerShare)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
