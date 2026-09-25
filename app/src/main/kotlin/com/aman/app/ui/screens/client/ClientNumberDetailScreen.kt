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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.NumberProtectionStatus
import com.aman.app.data.repository.CustomerNumberRepository
import com.aman.app.data.repository.CustomerNumberRepositoryImpl
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * شاشة تفاصيل الرقم المستقلة للعميل
 * Defined according to AMAN.XZ.txt:
 * - رقم الهاتف ومزود الخدمة
 * - حالة الحماية
 * - تاريخ الإضافة
 * - ممنوع: زر «طلب حماية» داخل تفاصيل الرقم
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientNumberDetailScreen(
    numberId: String,
    onBack: () -> Unit,
    repository: CustomerNumberRepository = remember { CustomerNumberRepositoryImpl() }
) {
    var number by remember { mutableStateOf<CustomerNumber?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = repository.getNumberDetails(numberId)) {
                is AmanResult.Success -> {
                    number = res.data
                    isLoading = false
                }
                is AmanResult.Error -> {
                    errorMessage = res.error.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(numberId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل الرقم",
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
                    AmanLoadingIndicator(message = "جارٍ تحميل تفاصيل الرقم...")
                }
                errorMessage != null -> {
                    EmptyView(
                        icon = Icons.Default.Warning,
                        title = "خطأ في تحميل الرقم",
                        description = errorMessage ?: "",
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { loadData() }
                    )
                }
                number != null -> {
                    val num = number!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Main Number Card
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = num.phoneNumber,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ProviderBadge(providerName = num.provider?.name ?: "غير محدد")
                                    }
                                    NumberProtectionBadge(status = num.protectionStatus)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = BorderColor)
                                Spacer(modifier = Modifier.height(16.dp))

                                DetailRow(label = "معرّف الرقم في النظام", value = num.id.take(8) + "...")
                                DetailRow(label = "تاريخ الإضافة", value = num.createdAt?.take(10) ?: "غير مسجل")
                                DetailRow(label = "حالة الرقم", value = if (num.status.name == "ACTIVE") "نشط" else "معلق")
                            }
                        }

                        // Protection Status Explanation Card
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "حالة الأمان والحماية",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                when (num.protectionStatus) {
                                    NumberProtectionStatus.PROTECTED -> {
                                        Text(
                                            text = "هذا الرقم مشمول بحماية نشطة ويتم سداد الرسوم الدورية لضمان استمراريته وعدم فصله.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Success
                                        )
                                    }
                                    NumberProtectionStatus.PENDING -> {
                                        Text(
                                            text = "يوجد طلب حماية قيد المراجعة حالياً من قبل الإدارة. ستصلك رسالة إشعار فور اعتماد الطلب وبدء الحماية.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Warning
                                        )
                                    }
                                    NumberProtectionStatus.UNPROTECTED -> {
                                        Text(
                                            text = "هذا الرقم غير محمي حالياً. يمكنك تفعيل الحماية واختيار الباقة وطريقة الدفع المناسبة لتأمين الرقم من السحب.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
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
