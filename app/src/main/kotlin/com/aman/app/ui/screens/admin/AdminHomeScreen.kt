package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PendingActions
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
import com.aman.app.ui.components.AmanButton
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.StatusBadge
import com.aman.app.ui.theme.*

/**
 * Screen: الرئيسية لإدارة المنظومة
 * Adheres strictly to AMAN.XZ.txt:
 * - Admin header
 * - Real pending protection requests (or Empty state)
 * - Zero hardcoded fake metrics / statistics
 */
@Composable
fun AdminHomeScreen(
    viewModel: AdminHomeViewModel = viewModel(),
    onBackToClient: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var rejectingRequestId by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToClient) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "العودة لتطبيق العميل", tint = Primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("لوحة إدارة أمان", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                        Text("الإشراف ومراجعة الطلبات", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightTeal)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("إدارة", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
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
                is AdminHomeUiState.ConfigurationPending -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "حالة النظام: الاتصال بالخادم السحابي المشفر قيد المزامنة التلقائية.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                is AdminHomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                is AdminHomeUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PendingActions,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "لا توجد طلبات حماية جديدة قيد المراجعة حالياً",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }

                is AdminHomeUiState.Content -> {
                    Text(
                        text = "طلبات الحماية قيد المراجعة (${state.pendingRequests.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.pendingRequests) { req ->
                            AmanCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            req.customerNumber?.phoneNumber ?: "طلب حماية",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "الباقة: ${req.plan?.name ?: "-"} • القيمة: ${req.protectionValue}",
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    StatusBadge(status = req.status.name)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AmanButton(
                                        text = "مراجعة واعتماد",
                                        onClick = { viewModel.approveRequest(req.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    AmanButton(
                                        text = "رفض",
                                        onClick = {
                                            rejectingRequestId = req.id
                                            rejectionReason = ""
                                        },
                                        modifier = Modifier.weight(1f),
                                        isSecondary = true
                                    )
                                }
                            }
                        }
                    }
                }

                is AdminHomeUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("خطأ أثناء جلب الطلبات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                is AdminHomeUiState.Uninitialized -> {}
            }
        }

        if (rejectingRequestId != null) {
            AlertDialog(
                onDismissRequest = { rejectingRequestId = null },
                title = { Text("تأكيد رفض طلب الحماية", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = {
                    Column {
                        Text("يرجى كتابة سبب الرفض ليتم تسجيله في سجل التدقيق:", fontSize = 13.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            placeholder = { Text("مثال: رقم الحوالة غير مطابق") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
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
