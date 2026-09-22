package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.data.model.ProtectionRequestStatus
import com.aman.app.ui.components.*
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminProtectionRequestsScreen(
    viewModel: AdminProtectionRequestsViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var rejectingRequestId by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadRequests()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                currentRoute = Screen.AdminProtectionRequests.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "طلبات الحماية",
                    subtitle = "مراجعة واعتماد أو رفض طلبات اشتراك الحماية",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadRequests() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Primary)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when (val state = uiState) {
                    is AdminProtectionRequestsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminProtectionRequestsUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن بسجل طلبات الحماية السحابي. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminProtectionRequestsUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب طلبات الحماية", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminProtectionRequestsUiState.Content -> {
                        // Status Filter Chips
                        val filters = listOf(
                            null to "الكل (${state.allRequests.size})",
                            ProtectionRequestStatus.PENDING to "قيد المراجعة (${state.allRequests.count { it.status == ProtectionRequestStatus.PENDING }})",
                            ProtectionRequestStatus.APPROVED to "مقبولة (${state.allRequests.count { it.status == ProtectionRequestStatus.APPROVED }})",
                            ProtectionRequestStatus.REJECTED to "مرفوضة (${state.allRequests.count { it.status == ProtectionRequestStatus.REJECTED }})"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filters) { (status, label) ->
                                val isSelected = state.selectedStatus == status
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Primary else SurfaceWhite)
                                        .clickable { viewModel.setFilter(status) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) SurfaceWhite else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (state.filteredRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد طلبات حماية في هذا التبويب",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.filteredRequests) { req ->
                                    AdminProtectionRequestItemCard(
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
                                placeholder = { Text("مثال: رقم الحوالة غير مطابق، أو بيانات الإيداع غير واضحة...") },
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
fun AdminProtectionRequestItemCard(
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
                    text = "رقم الطلب: #REQ-${request.id.take(8).uppercase()} • القيمة: ${request.protectionValue} ريال",
                    fontSize = 12.sp,
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

        if (request.status == ProtectionRequestStatus.REJECTED && !request.rejectionReason.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "سبب الرفض: ${request.rejectionReason}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (request.status == ProtectionRequestStatus.PENDING) {
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
}
