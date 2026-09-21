package com.aman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import com.aman.app.ui.theme.*

@Composable
fun AmanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSecondary) SurfaceWhite else Primary,
            contentColor = if (isSecondary) Primary else SurfaceWhite,
            disabledContainerColor = Color(0xFFDCE9E6),
            disabledContentColor = TextDisabled
        ),
        border = if (isSecondary) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (isSecondary) Primary else SurfaceWhite,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, textAr) = when (status.lowercase()) {
        "protected", "محمي" -> Triple(StatusProtectedBg, StatusProtectedText, "محمي")
        "pending", "قيد المراجعة" -> Triple(StatusPendingBg, StatusPendingText, "قيد المراجعة")
        "approved", "مقبول" -> Triple(StatusApprovedBg, StatusApprovedText, "مقبول")
        "rejected", "مرفوض" -> Triple(StatusRejectedBg, StatusRejectedText, "مرفوض")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), status)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = textAr,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AmanCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
