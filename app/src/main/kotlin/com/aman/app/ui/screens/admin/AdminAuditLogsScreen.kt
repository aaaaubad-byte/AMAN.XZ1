package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.AuditLog
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminAuditLogsScreen(
    viewModel: AdminAuditLogsViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadLogs()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                currentRoute = Screen.AdminAuditLogs.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "سجل العمليات والتدقيق",
                    subtitle = "سجل الإجراءات غير القابل للتعديل للتوثيق والرقابة",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadLogs() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Primary)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when (val state = uiState) {
                    is AdminAuditLogsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminAuditLogsUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن بسجل العمليات السحابي. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminAuditLogsUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب سجل العمليات", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminAuditLogsUiState.Content -> {
                        if (state.logs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("لا توجد سجلات عمليات مدونة بعد", color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.logs) { log ->
                                    AuditLogCard(log = log)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLog) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val actionArabic = when (log.action.uppercase()) {
                    "INSERT" -> "إضافة سجل جديد"
                    "UPDATE" -> "تعديل وتحديث"
                    "DELETE" -> "حذف سجل"
                    "APPROVE" -> "موافقة واعتماد"
                    "REJECT" -> "رفض الطلب"
                    "COMPLETE" -> "إكمال وسداد"
                    "RESCHEDULE" -> "إعادة جدولة"
                    "CANCEL" -> "إلغاء العملية"
                    else -> log.action
                }
                Text(
                    text = actionArabic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Primary
                )
                Text(
                    text = log.createdAt?.take(16)?.replace("T", " ") ?: "",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val tableArabic = when (log.tableName.lowercase()) {
                "customer_numbers" -> "أرقام العملاء"
                "protection_requests" -> "طلبات الحماية"
                "protections" -> "سجل الحمايات"
                "payment_tasks" -> "مهام السداد"
                "payment_methods" -> "طرق الدفع"
                "protection_plans" -> "باقات الحماية"
                "telecom_providers" -> "شركات الاتصالات"
                "telecom_prefixes" -> "بادئات الأرقام"
                "users" -> "المستخدمين"
                "system_settings" -> "إعدادات النظام"
                "task_settings" -> "إعدادات المهام"
                "notifications" -> "مركز الإشعارات"
                "audit_logs" -> "سجل العمليات"
                else -> log.tableName
            }
            Text(
                text = "القسم: $tableArabic • رقم السجل: #${log.recordId?.take(8)?.uppercase() ?: "-"}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            if (!log.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = log.details,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        }
    }
}
