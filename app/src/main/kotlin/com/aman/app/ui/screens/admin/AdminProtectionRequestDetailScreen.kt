package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.data.model.ProtectionRequestStatus
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.StatusBadge
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProtectionRequestDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    repository: AdminRepository = remember { AdminRepositoryImpl() }
) {
    var request by remember { mutableStateOf<ProtectionRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isOperating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = repository.getProtectionRequest(requestId)) {
                is AmanResult.Success -> {
                    request = res.data
                    isLoading = false
                }
                is AmanResult.Error -> {
                    errorMessage = res.error.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(requestId) {
        loadData()
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            actionMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تفاصيل طلب الحماية",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        request?.let {
                            Text(
                                text = "طلب رقم: ${it.id.take(8)}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Danger, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = errorMessage ?: "تعذر جلب تفاصيل الطلب", color = Danger, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadData() }) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                request != null -> {
                    val req = request!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. حالة الطلب
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
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when (req.status) {
                                                    ProtectionRequestStatus.APPROVED -> StatusProtectedBg
                                                    ProtectionRequestStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                                                    ProtectionRequestStatus.PENDING -> WarningBg
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (req.status) {
                                                ProtectionRequestStatus.APPROVED -> Icons.Default.CheckCircle
                                                ProtectionRequestStatus.REJECTED -> Icons.Default.Cancel
                                                ProtectionRequestStatus.PENDING -> Icons.Default.HourglassEmpty
                                            },
                                            contentDescription = null,
                                            tint = when (req.status) {
                                                ProtectionRequestStatus.APPROVED -> StatusProtectedText
                                                ProtectionRequestStatus.REJECTED -> MaterialTheme.colorScheme.error
                                                ProtectionRequestStatus.PENDING -> WarningText
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = when (req.status) {
                                                ProtectionRequestStatus.APPROVED -> "طلب مقبول ومعتمد"
                                                ProtectionRequestStatus.REJECTED -> "طلب مرفوض"
                                                ProtectionRequestStatus.PENDING -> "قيد المراجعة والتدقيق"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "تاريخ التقديم: ${req.createdAt?.take(19)?.replace("T", " ") ?: "—"}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                StatusBadge(status = req.status.name)
                            }

                            if (!req.rejectionReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "سبب الرفض المسجل:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = req.rejectionReason ?: "",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        // 2. بيانات العميل والرقم
                        AmanCard {
                            Text(
                                text = "بيانات العميل والرقم المطلوب حمايته",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(label = "اسم العميل", value = req.actorName ?: "العميل")
                            DetailRow(
                                label = "رقم الهاتف",
                                value = req.customerNumber?.phoneNumber ?: "—"
                            )
                            DetailRow(
                                label = "شركة الاتصالات",
                                value = req.provider?.name ?: "—"
                            )
                        }

                        // 3. بيانات باقة الحماية والمبلغ
                        AmanCard {
                            Text(
                                text = "تفاصيل باقة الحماية والمبلغ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(
                                label = "اسم الباقة",
                                value = req.planNameSnapshot ?: req.plan?.name ?: "باقة حماية"
                            )
                            req.plan?.let { pl ->
                                DetailRow(label = "مدة الحماية", value = "${pl.durationDays} يوم")
                            }
                            DetailRow(
                                label = "المبلغ الإجمالي",
                                value = "${req.amount} ريال يمني",
                                isHighlight = true
                            )
                        }

                        // 4. بيانات وسيلة الدفع والتحويل
                        AmanCard {
                            Text(
                                text = "معلومات التحويل والدفع",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(
                                label = "وسيلة الدفع",
                                value = req.paymentMethodNameSnapshot ?: req.paymentMethod?.name ?: "—"
                            )
                            req.paymentMethod?.let { pm ->
                                DetailRow(label = "رقم الحساب", value = pm.accountNumber)
                                DetailRow(label = "اسم المستفيد", value = pm.accountOwnerName)
                            }
                            DetailRow(
                                label = "رقم الحوالة / العملية",
                                value = req.paymentReference ?: "غير مدون"
                            )
                            if (!req.paymentNotes.isNullOrBlank()) {
                                DetailRow(label = "ملاحظات الدفع", value = req.paymentNotes ?: "")
                            }
                        }

                        // 5. إجراءات الإدارة (فقط للطلبات قيد المراجعة)
                        if (req.status == ProtectionRequestStatus.PENDING) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isOperating = true
                                        scope.launch {
                                            when (val approveRes = repository.approveProtectionRequest(req.id)) {
                                                is AmanResult.Success -> {
                                                    actionMessage = "تم اعتماد الطلب وتفعيل الحماية وإصدار مهمة الدفع بنجاح"
                                                    loadData()
                                                }
                                                is AmanResult.Error -> {
                                                    actionMessage = approveRes.error.message
                                                }
                                            }
                                            isOperating = false
                                        }
                                    },
                                    enabled = !isOperating,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اعتماد وتفعيل", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        rejectionReason = ""
                                        showRejectDialog = true
                                    },
                                    enabled = !isOperating,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("رفض الطلب", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Dialog for rejection reason
            if (showRejectDialog) {
                AlertDialog(
                    onDismissRequest = { showRejectDialog = false },
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
                                text = "يجب تسجيل سبب الرفض ليتم إشعار العميل به وحفظه في سجل التدقيق:",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = rejectionReason,
                                onValueChange = { rejectionReason = it },
                                placeholder = { Text("مثال: رقم الحوالة غير مطابق، أو بيانات الإيداع غير صحيحة...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (rejectionReason.isNotBlank() && request != null) {
                                    showRejectDialog = false
                                    isOperating = true
                                    scope.launch {
                                        when (val rejRes = repository.rejectProtectionRequest(request!!.id, rejectionReason.trim())) {
                                            is AmanResult.Success -> {
                                                actionMessage = "تم رفض الطلب وتسجيل السبب وإشعار العميل"
                                                loadData()
                                            }
                                            is AmanResult.Error -> {
                                                actionMessage = rejRes.error.message
                                            }
                                        }
                                        isOperating = false
                                    }
                                }
                            },
                            enabled = rejectionReason.isNotBlank() && !isOperating,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("تأكيد الرفض", color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRejectDialog = false }) {
                            Text("إلغاء", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) Primary else TextPrimary
        )
    }
}
