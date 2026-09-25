package com.aman.app.ui.screens.client

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
import com.aman.app.data.repository.ProtectionRequestRepository
import com.aman.app.data.repository.ProtectionRequestRepositoryImpl
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * شاشة تفاصيل طلب الحماية المستقلة
 * Defined according to AMAN.XZ.txt:
 * - حالة الطلب
 * - بيانات الرقم والشركة
 * - بيانات الباقة والمبلغ
 * - وسيلة الدفع ورقم الحوالة
 * - سبب الرفض بالتفصيل عند الرفض
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionRequestDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    repository: ProtectionRequestRepository = remember { ProtectionRequestRepositoryImpl() }
) {
    var request by remember { mutableStateOf<ProtectionRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = repository.getRequestDetails(requestId)) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل طلب الحماية",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    AmanLoadingIndicator(message = "جارٍ تحميل تفاصيل الطلب...")
                }
                errorMessage != null -> {
                    EmptyView(
                        icon = Icons.Default.Warning,
                        title = "خطأ في تحميل تفاصيل الطلب",
                        description = errorMessage ?: "",
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { loadData() }
                    )
                }
                request != null -> {
                    val req = request!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Status Card
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = req.customerNumber?.phoneNumber ?: "رقم الهاتف",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ProviderBadge(providerName = req.provider?.name ?: "غير محدد")
                                    }
                                    RequestStatusBadge(status = req.status)
                                }

                                // Rejection reason banner if rejected
                                if (req.status == ProtectionRequestStatus.REJECTED && !req.rejectionReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFEE2E2))
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "سبب الرفض المسجل من الإدارة:",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFDC2626)
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = req.rejectionReason ?: "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color(0xFF991B1B)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Plan & Value Details Card
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "بيانات الباقة والحماية",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DetailRow(label = "الباقة المختارة", value = req.plan?.name ?: req.planNameSnapshot ?: "باقة الحماية")
                                DetailRow(label = "مدة الحماية", value = "${req.planDurationDaysSnapshot} يوم")
                                DetailRow(label = "قيمة الحماية", value = "${req.protectionValue} ${req.plan?.currency ?: "ريال"}")
                            }
                        }

                        // Payment & Voucher Card
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "بيانات السداد والتحويل",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DetailRow(label = "طريقة السداد", value = req.paymentMethod?.name ?: "سند تحويل بنكي")
                                DetailRow(label = "رقم / سند التحويل", value = req.transferData ?: "غير مسجل")
                                DetailRow(label = "تاريخ التقديم", value = req.createdAt?.take(10) ?: "غير مسجل")
                                if (req.reviewedAt != null) {
                                    DetailRow(label = "تاريخ المراجعة", value = req.reviewedAt.take(10))
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
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
    }
}
