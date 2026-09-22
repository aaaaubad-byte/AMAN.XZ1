package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.ui.components.*
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state for Rejection
    var rejectingRequestId by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                currentRoute = Screen.AdminDashboard.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "لوحة إدارة أمان",
                    subtitle = "نظرة تشغيلية عامة ومؤشرات النظام",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Primary)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is AdminDashboardUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminDashboardUiState.ConfigurationPending -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AmanCard {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "حالة الخادم السحابي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "جاري تهيئة المزامنة السحابية الآمنة مع قاعدة بيانات النظام المعتمدة.",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    is AdminDashboardUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AmanCard {
                                Text(
                                    text = "خطأ في الاتصال بقاعدة البيانات",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message, color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                AmanButton(text = "إعادة المحاولة", onClick = { viewModel.loadData() })
                            }
                        }
                    }
                    is AdminDashboardUiState.Content -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Operational Metrics Grid
                            item {
                                Text(
                                    text = "المؤشرات التشغيلية الحقيقية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AdminMetricCard(
                                            title = "العملاء",
                                            count = state.metrics.customerCount,
                                            icon = Icons.Default.People,
                                            color = Primary,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onNavigate(Screen.AdminCustomers.route) }
                                        )
                                        AdminMetricCard(
                                            title = "أرقام الهواتف",
                                            count = state.metrics.phoneNumberCount,
                                            icon = Icons.Default.PhoneIphone,
                                            color = Color(0xFF0D9488),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AdminMetricCard(
                                            title = "طلبات معلقة",
                                            count = state.metrics.pendingRequestsCount,
                                            icon = Icons.Default.PendingActions,
                                            color = if (state.metrics.pendingRequestsCount > 0) Color(0xFFD97706) else TextSecondary,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onNavigate(Screen.AdminProtectionRequests.route) }
                                        )
                                        AdminMetricCard(
                                            title = "حمايات نشطة",
                                            count = state.metrics.activeProtectionsCount,
                                            icon = Icons.Default.Shield,
                                            color = StatusProtectedText,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onNavigate(Screen.AdminProtections.route) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AdminMetricCard(
                                            title = "مهام مستحقة",
                                            count = state.metrics.dueTasksCount + state.metrics.overdueTasksCount,
                                            icon = Icons.Default.WarningAmber,
                                            color = if (state.metrics.overdueTasksCount > 0) MaterialTheme.colorScheme.error else Color(0xFFEAB308),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onNavigate(Screen.AdminPaymentTasks.route) }
                                        )
                                        AdminMetricCard(
                                            title = "مهام مكتملة",
                                            count = state.metrics.completedTasksCount,
                                            icon = Icons.Default.CheckCircle,
                                            color = Primary,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onNavigate(Screen.AdminPaymentTasks.route) }
                                        )
                                    }
                                }
                            }

                            // Quick Links Section
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "إجراءات وتكوينات سريعة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickActionChip(
                                        label = "شركات الاتصالات",
                                        icon = Icons.Default.CellTower,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onNavigate(Screen.AdminTelecomProviders.route) }
                                    )
                                    QuickActionChip(
                                        label = "باقات الحماية",
                                        icon = Icons.Default.CardGiftcard,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onNavigate(Screen.AdminProtectionPlans.route) }
                                    )
                                    QuickActionChip(
                                        label = "طرق الدفع",
                                        icon = Icons.Default.AccountBalanceWallet,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onNavigate(Screen.AdminPaymentMethods.route) }
                                    )
                                }
                            }

                            // Pending Protection Requests Feed
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "طلبات الحماية المعلقة (${state.pendingRequests.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "عرض الكل",
                                        color = Primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onNavigate(Screen.AdminProtectionRequests.route) }
                                    )
                                }
                            }

                            if (state.pendingRequests.isEmpty()) {
                                item {
                                    AmanCard {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircleOutline,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "لا توجد طلبات حماية جديدة معلقة حالياً",
                                                color = TextSecondary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(state.pendingRequests) { req ->
                                    ProtectionRequestCard(
                                        request = req,
                                        onApprove = { viewModel.approveRequest(req.id) },
                                        onReject = {
                                            rejectingRequestId = req.id
                                            rejectionReason = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rejection Reason Dialog
            if (rejectingRequestId != null) {
                AlertDialog(
                    onDismissRequest = { rejectingRequestId = null },
                    title = {
                        Text(
                            text = "تأكيد رفض طلب الحماية",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "يجب تحديد سبب الرفض ليتم تسجيله في سجل التدقيق وإشعار العميل به:",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = rejectionReason,
                                onValueChange = { rejectionReason = it },
                                placeholder = { Text("مثال: رقم الحوالة غير مطابق، الرصيد غير مكتمل...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = rejectingRequestId
                                if (id != null && rejectionReason.isNotBlank()) {
                                    viewModel.rejectRequest(id, rejectionReason)
                                    rejectingRequestId = null
                                }
                            },
                            enabled = rejectionReason.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("تأكيد الرفض", color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { rejectingRequestId = null }) {
                            Text("إلغاء", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun ProtectionRequestCard(
    request: ProtectionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AmanCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = request.customerNumber?.phoneNumber ?: "طلب حماية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "القيمة: ${request.protectionValue} ريال",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            StatusBadge(status = request.status.name)
        }

        if (!request.transferData.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundLight)
                    .padding(8.dp)
            ) {
                Text(
                    text = "بيانات التحويل: ${request.transferData}",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AmanButton(
                text = "اعتماد وتفعيل",
                onClick = onApprove,
                modifier = Modifier.weight(1f)
            )
            AmanButton(
                text = "رفض",
                onClick = onReject,
                modifier = Modifier.weight(1f),
                isSecondary = true
            )
        }
    }
}
