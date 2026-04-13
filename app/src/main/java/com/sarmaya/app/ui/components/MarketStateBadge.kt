package com.sarmaya.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarmaya.app.ui.theme.LocalSarmayaColors

@Composable
fun MarketStateBadge(
    state: String,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalSarmayaColors.current
    
    val (label, containerColor, contentColor) = when (state.uppercase()) {
        "OPN", "OPEN" -> Triple("OPEN", financeColors.profit.copy(alpha = 0.1f), financeColors.profit)
        "CLS", "CLOSED" -> Triple("CLOSED", financeColors.loss.copy(alpha = 0.1f), financeColors.loss)
        "SUS", "SUSPENDED" -> Triple("SUSPENDED", Color(0xFFFFA000).copy(alpha = 0.1f), Color(0xFFFFA000))
        "PRE", "PRE-OPEN" -> Triple("PRE-OPEN", Color(0xFF1976D2).copy(alpha = 0.1f), Color(0xFF1976D2))
        else -> Triple("OFFLINE", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = contentColor
        )
    }
}
