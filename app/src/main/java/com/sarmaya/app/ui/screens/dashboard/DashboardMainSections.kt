package com.sarmaya.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarmaya.app.ui.theme.LossRedLight
import com.sarmaya.app.ui.theme.ProfitGreenLight
import com.sarmaya.app.ui.theme.SarmayaFinanceColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MarketStatusCard(
    lastPriceUpdate: Long?,
    onRefreshClick: () -> Unit
) {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val timeInMinutes = hour * 60 + minute
    val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
    val isMarketOpen = isWeekday && timeInMinutes in (9 * 60 + 30)..(15 * 60 + 30)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "PSX Market",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (isMarketOpen) ProfitGreenLight.copy(alpha = 0.15f) else LossRedLight.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        if (isMarketOpen) "Open" else "Closed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMarketOpen) ProfitGreenLight else LossRedLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                val lastUpdatedStr = if (lastPriceUpdate != null) {
                    dateFormat.format(Date(lastPriceUpdate))
                } else {
                    "Never"
                }
                Text(
                    lastUpdatedStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Refresh,
                        contentDescription = "Refresh Prices",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioValueCard(
    totalValue: Double,
    totalProfitLoss: Double,
    totalInvested: Double,
    holdingsCount: Int,
    financeColors: SarmayaFinanceColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Portfolio Value",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    "₨ ${String.format("%,.2f", totalValue)}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isProfit = totalProfitLoss >= 0
                    Icon(
                        if (isProfit) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isProfit) ProfitGreenLight else LossRedLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${if (isProfit) "+" else ""}₨ ${String.format("%,.2f", totalProfitLoss)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (totalInvested > 0) {
                        val plPercent = (totalProfitLoss / totalInvested) * 100
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(${String.format("%.1f", plPercent)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$holdingsCount active holdings",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}
