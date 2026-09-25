package com.aman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import com.aman.app.data.model.NumberProtectionStatus
import com.aman.app.data.model.ProtectionDisplayStatus
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
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSecondary) SurfaceWhite else Primary,
            contentColor = if (isSecondary) Primary else SurfaceWhite,
            disabledContainerColor = Color(0xFFE3EDEB),
            disabledContentColor = TextDisabled
        ),
        border = if (isSecondary) ButtonDefaults.outlinedButtonBorder else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
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
        "approved", "مقبول" -> Triple(StatusApprovedBg, StatusApprovedText, "مقبول ومفعل")
        "rejected", "مرفوض" -> Triple(StatusRejectedBg, StatusRejectedText, "مرفوض")
        "upcoming", "مجدول" -> Triple(StatusPendingBg, StatusPendingText, "مجدول قادماً")
        "due", "مستحق" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "مستحق اليوم")
        "due_soon", "يستحق قريباً" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "يستحق قريباً")
        "overdue", "متأخر" -> Triple(StatusRejectedBg, StatusRejectedText, "متأخر السداد")
        "completed", "مكتمل" -> Triple(StatusProtectedBg, StatusProtectedText, "مكتمل")
        "cancelled", "ملغي" -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "ملغي")
        "active", "نشط" -> Triple(StatusProtectedBg, StatusProtectedText, "نشط وسارٍ")
        "expired", "منتهي" -> Triple(StatusRejectedBg, StatusRejectedText, "منتهي الصلاحية")
        "unprotected", "غير محمي" -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "غير محمي")
        "suspended", "معلق" -> Triple(StatusRejectedBg, StatusRejectedText, "معلق مؤقتاً")
        "needs_renewal", "بحاجة لتجديد" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "بحاجة لتجديد")
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
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AmanLoadingIndicator(
    message: String = "جارٍ التحميل...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun EmptyView(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AmanCard(modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                if (buttonText != null && onButtonClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onButtonClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
fun AmanStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = clickableModifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ProviderBadge(
    providerName: String,
    modifier: Modifier = Modifier
) {
    val text = providerName.ifBlank { "غير محدد" }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Primary.copy(alpha = 0.09f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun NumberProtectionBadge(
    status: NumberProtectionStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        NumberProtectionStatus.UNPROTECTED -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "غير محمي")
        NumberProtectionStatus.PENDING -> Triple(StatusPendingBg, StatusPendingText, "قيد المراجعة")
        NumberProtectionStatus.PROTECTED -> Triple(StatusProtectedBg, StatusProtectedText, "محمي")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProtectionDisplayBadge(
    status: ProtectionDisplayStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        ProtectionDisplayStatus.ACTIVE -> Triple(StatusProtectedBg, StatusProtectedText, "نشط")
        ProtectionDisplayStatus.NEEDS_RENEWAL -> Triple(StatusPendingBg, StatusPendingText, "بحاجة لتجديد")
        ProtectionDisplayStatus.EXPIRED -> Triple(StatusRejectedBg, StatusRejectedText, "منتهي")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

