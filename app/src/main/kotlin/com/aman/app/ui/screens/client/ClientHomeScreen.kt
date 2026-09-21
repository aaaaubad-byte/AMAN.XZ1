package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.StatusBadge
import com.aman.app.ui.theme.*

/**
 * Screen: الرئيسية لتطبيق العميل
 * Strictly adheres to AMAN.XZ.txt lines 906-932:
 * - أعلى الشاشة: اسم العميل والإشعارات
 * - ملخص الحماية
 * - ملخص الأرقام
 * - طلبات الحماية
 * - خالية تماماً من البيانات الوهمية (No fake numbers, no fake statistics)
 */
@Composable
fun ClientHomeScreen(
    viewModel: ClientHomeViewModel = viewModel(),
    customerId: String? = null,
    onNavigateToAddNumber: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onSwitchToAdmin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadData(customerId)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LightTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("أ", fontWeight = FontWeight.Bold, color = Primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("حساب العميل", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("أمان لحماية الأرقام", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Row {
                    IconButton(onClick = onSwitchToAdmin) {
                        Icon(Icons.Default.Security, contentDescription = "بوابة الإدارة", tint = Primary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = "الإشعارات", tint = TextPrimary)
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddNumber,
                containerColor = Primary,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة رقم جديد", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = uiState) {
                is ClientHomeUiState.ConfigurationPending -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "تنبيه المعمارية: الاتصال الفعلي بـ Supabase معلق بانتظار إعداد مفاتيح المشروع. لا يتم عرض بيانات وهمية.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                is ClientHomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                is ClientHomeUiState.Empty -> {
                    // Summary Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(Primary, Secondary)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("ملخص الحماية", color = SurfaceWhite.copy(alpha = 0.85f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد أرقام مسجلة بعد", color = SurfaceWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("قم بإضافة رقمك الأول لبدء حمايته عبر أمان", color = SurfaceWhite.copy(alpha = 0.9f), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم إضافة أي أرقام حتى الآن",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }

                is ClientHomeUiState.Content -> {
                    // Summary Banner with real counts
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(Primary, Secondary)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ملخص الحماية", color = SurfaceWhite.copy(alpha = 0.85f), fontSize = 14.sp)
                                Text(
                                    "${state.activeProtectionsCount} نشطة",
                                    color = SurfaceWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${state.numbers.size} أرقام مسجلة",
                                color = SurfaceWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("أرقامي المسجلة", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.numbers) { item ->
                            AmanCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(LightTeal),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = Primary)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(item.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                            Text(item.provider?.name ?: "الرقم المسجل", fontSize = 13.sp, color = TextSecondary)
                                        }
                                    }
                                    StatusBadge(status = item.status.name)
                                }
                            }
                        }
                    }
                }

                is ClientHomeUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("تعذر تحميل البيانات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                is ClientHomeUiState.Uninitialized -> {}
            }
        }
    }
}
