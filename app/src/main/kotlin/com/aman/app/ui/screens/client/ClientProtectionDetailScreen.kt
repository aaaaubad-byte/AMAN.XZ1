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
import com.aman.app.data.model.Protection
import com.aman.app.data.model.ProtectionDisplayStatus
import com.aman.app.data.repository.ProtectionRepository
import com.aman.app.data.repository.ProtectionRepositoryImpl
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * شاشة تفاصيل الحماية المستقلة للعميل
 * Defined according to AMAN.XZ.txt:
 * - حالة الحماية وحساب الأيام المتبقية
 * - بيانات الباقة والمزود
 * - تواريخ البداية والنهاية
 * - زر التجديد المباشر لنفس الرقم
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProtectionDetailScreen(
    protectionId: String,
    onBack: () -> Unit,
    onRenewProtection: (String) -> Unit,
    repository: ProtectionRepository = remember { ProtectionRepositoryImpl() }
) {
    var protection by remember { mutableStateOf<Protection?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = repository.getProtectionDetails(protectionId)) {
                is AmanResult.Success -> {
                    protection = res.data
                    isLoading = false
                }
                is AmanResult.Error -> {
                    errorMessage = res.error.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(protectionId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل الحماية",
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
                    AmanLoadingIndicator(message = "جارٍ تحميل بيانات الحماية...")
                }
                errorMessage != null -> {
                    EmptyView(
                        icon = Icons.Default.Warning,
                        title = "خطأ في تحميل بيانات الحماية",
                        description = errorMessage ?: "",
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { loadData() }
                    )
                }
                protection != null -> {
                    val prot = protection!!
                    val displayStatus = prot.calculateDisplayStatus()
                    val daysLeft = prot.daysRemaining()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Main Protection Header
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = prot.customerNumber?.phoneNumber ?: "رقم الهاتف",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ProviderBadge(providerName = prot.provider?.name ?: prot.providerNameSnapshot ?: "غير محدد")
                                    }
                                    ProtectionStatusBadge(displayStatus = displayStatus)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Days Remaining Highlight Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when (displayStatus) {
                                                ProtectionDisplayStatus.ACTIVE -> Success.copy(alpha = 0.1f)
                                                ProtectionDisplayStatus.NEEDS_RENEWAL -> Warning.copy(alpha = 0.12f)
                                                ProtectionDisplayStatus.EXPIRED -> ErrorColor.copy(alpha = 0.1f)
                                            }
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (daysLeft > 0) "$daysLeft يوم" else "انتهت فترة الحماية",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = when (displayStatus) {
                                                    ProtectionDisplayStatus.ACTIVE -> Success
                                                    ProtectionDisplayStatus.NEEDS_RENEWAL -> Warning
                                                    ProtectionDisplayStatus.EXPIRED -> ErrorColor
                                                }
                                            )
                                        )
                                        Text(
                                            text = if (daysLeft > 0) "المتبقي حتى تاريخ انتهاء الحماية" else "الرقم غير محمي حالياً",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }
                                }
                            }
                        }

                        // Dates & Details Card
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "بيانات التغطية والمواعيد",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DetailRow(label = "الباقة المفعلة", value = prot.plan?.name ?: prot.planNameSnapshot ?: "باقة الحماية")
                                DetailRow(label = "تاريخ بدء الحماية", value = prot.startDate)
                                DetailRow(label = "تاريخ نهاية الحماية", value = prot.endDate)
                                DetailRow(label = "إجمالي مدة الحماية", value = "${prot.protectionDurationDays} يوم")
                                DetailRow(label = "قيمة الاشتراك", value = "${prot.protectionValue} ${prot.plan?.currency ?: "ريال"}")
                            }
                        }

                        // Renewal Action
                        if (displayStatus == ProtectionDisplayStatus.NEEDS_RENEWAL || displayStatus == ProtectionDisplayStatus.EXPIRED) {
                            AmanButton(
                                text = "تجديد الحماية لهذا الرقم الآن",
                                onClick = { onRenewProtection(prot.customerNumberId) }
                            )
                        } else {
                            OutlinedButton(
                                onClick = { onRenewProtection(prot.customerNumberId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("طلب تمديد مبكر للحماية", fontWeight = FontWeight.Bold)
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
