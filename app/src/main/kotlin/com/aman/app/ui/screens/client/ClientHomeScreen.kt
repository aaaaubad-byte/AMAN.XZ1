package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.NumberProtectionStatus
import com.aman.app.data.model.Protection
import com.aman.app.data.model.ProtectionDisplayStatus
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*

@Composable
fun ClientHomeScreen(
    viewModel: ClientHomeViewModel = viewModel(),
    customerId: String? = null,
    onMenuClick: () -> Unit,
    onNavigateToAddNumber: () -> Unit,
    onNavigateToMyNumbers: () -> Unit,
    onNavigateToProtections: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToCreateRequest: (String?) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadData(customerId)
    }

    val unreadCount = when (val state = uiState) {
        is ClientHomeUiState.Content -> state.unreadNotificationsCount
        else -> 0
    }

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "AMAN | أمان",
                onMenuClick = onMenuClick,
                unreadNotificationsCount = unreadCount,
                onNotificationsClick = onNavigateToNotifications
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddNumber,
                containerColor = Primary,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة رقم", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundMuted)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ClientHomeUiState.Loading, ClientHomeUiState.Uninitialized -> {
                    AmanLoadingIndicator(message = "جارٍ تحميل لوحة الحماية...")
                }

                is ClientHomeUiState.ConfigurationPending -> {
                    EmptyView(
                        icon = Icons.Default.CloudOff,
                        title = "جاري تهيئة الاتصال",
                        description = "يرجى التحقق من إعدادات الاتصال بقاعدة البيانات",
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { viewModel.loadData(customerId) }
                    )
                }

                is ClientHomeUiState.Empty -> {
                    EmptyView(
                        icon = Icons.Default.Shield,
                        title = "أهلاً بك في أمان",
                        description = "لم تقم بإضافة أي أرقام هواتف بعد. ابدأ بإضافة رقم هاتفك لحمايته من السحب أو الإلغاء.",
                        buttonText = "إضافة رقمك الأول",
                        onButtonClick = onNavigateToAddNumber
                    )
                }

                is ClientHomeUiState.Error -> {
                    EmptyView(
                        icon = Icons.Default.Warning,
                        title = "تعذر تحميل البيانات",
                        description = state.message,
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { viewModel.loadData(customerId) }
                    )
                }

                is ClientHomeUiState.Content -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Welcome Card
                        item {
                            AmanCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "نظام أمان الذكي لحماية الأرقام",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "متابعة دورية وتجديد تلقائي لضمان بقاء خطوطك نشطة ومحمية",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Statistics Grid
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AmanStatCard(
                                        title = "إجمالي الأرقام",
                                        value = state.numbers.size.toString(),
                                        icon = Icons.Default.PhoneIphone,
                                        iconColor = Primary,
                                        modifier = Modifier.weight(1f),
                                        onClick = onNavigateToMyNumbers
                                    )
                                    AmanStatCard(
                                        title = "حمايات نشطة",
                                        value = state.activeProtectionsCount.toString(),
                                        icon = Icons.Default.Shield,
                                        iconColor = Color(0xFF16A34A),
                                        modifier = Modifier.weight(1f),
                                        onClick = onNavigateToProtections
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AmanStatCard(
                                        title = "تحتاج تجديد",
                                        value = state.needsRenewalCount.toString(),
                                        icon = Icons.Default.AccessTime,
                                        iconColor = Color(0xFFD97706),
                                        modifier = Modifier.weight(1f),
                                        onClick = onNavigateToProtections
                                    )
                                    AmanStatCard(
                                        title = "طلبات قيد الانتظار",
                                        value = state.pendingRequestsCount.toString(),
                                        icon = Icons.Default.ReceiptLong,
                                        iconColor = Color(0xFF2563EB),
                                        modifier = Modifier.weight(1f),
                                        onClick = onNavigateToRequests
                                    )
                                }
                            }
                        }

                        // Quick Actions Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToAddNumber,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة رقم", fontSize = 14.sp)
                                }

                                OutlinedButton(
                                    onClick = { onNavigateToCreateRequest(null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("طلب حماية", fontSize = 14.sp)
                                }
                            }
                        }

                        // My Numbers Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "أرقامي المسجلة",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                TextButton(onClick = onNavigateToMyNumbers) {
                                    Text("عرض الكل (${state.numbers.size})", color = Primary, fontSize = 13.sp)
                                }
                            }
                        }

                        // Recent Numbers List
                        if (state.numbers.isEmpty()) {
                            item {
                                AmanCard {
                                    Text(
                                        text = "لا توجد أرقام مسجلة حتى الآن",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )
                                }
                            }
                        } else {
                            items(state.numbers.take(3)) { number ->
                                HomeNumberItem(
                                    number = number,
                                    onRequestProtection = { onNavigateToCreateRequest(number.id) }
                                )
                            }
                        }

                        // Protections Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل الحمايات",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                TextButton(onClick = onNavigateToProtections) {
                                    Text("عرض الكل (${state.protections.size})", color = Primary, fontSize = 13.sp)
                                }
                            }
                        }

                        // Recent Protections
                        if (state.protections.isEmpty()) {
                            item {
                                AmanCard {
                                    Text(
                                        text = "لا توجد حمايات مفعلة بعد",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )
                                }
                            }
                        } else {
                            items(state.protections.take(3)) { protection ->
                                HomeProtectionItem(
                                    protection = protection,
                                    onRenew = { onNavigateToCreateRequest(protection.customerNumberId) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeNumberItem(
    number: CustomerNumber,
    onRequestProtection: () -> Unit
) {
    AmanCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = number.phoneNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProviderBadge(providerName = number.provider?.name ?: "غير محدد")
                }
                Spacer(modifier = Modifier.height(4.dp))
                NumberProtectionBadge(status = number.protectionStatus)
            }

            if (number.protectionStatus != NumberProtectionStatus.PROTECTED) {
                Button(
                    onClick = onRequestProtection,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("حماية الرقم", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeProtectionItem(
    protection: Protection,
    onRenew: () -> Unit
) {
    val displayStatus = protection.calculateDisplayStatus()
    val daysRemaining = protection.daysRemaining()

    AmanCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protection.customerNumber?.phoneNumber ?: "رقم الهاتف",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                ProtectionDisplayBadge(status = displayStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الباقة: ${protection.plan?.name ?: "${protection.durationAtPurchase} يوم"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                    text = "المتبقي: $daysRemaining يوم",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (displayStatus == ProtectionDisplayStatus.NEEDS_RENEWAL) Color(0xFFD97706) else TextPrimary
                    )
                )
            }

            if (displayStatus == ProtectionDisplayStatus.NEEDS_RENEWAL || displayStatus == ProtectionDisplayStatus.EXPIRED) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRenew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تجديد الحماية الآن", fontSize = 13.sp)
                }
            }
        }
    }
}
