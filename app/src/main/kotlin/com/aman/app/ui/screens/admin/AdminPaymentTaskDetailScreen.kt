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
import com.aman.app.data.model.PaymentTask
import com.aman.app.data.model.TaskStatus
import com.aman.app.data.repository.*
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * شاشة تفاصيل المهمة المالية للإدارة
 * Defined according to AMAN.XZ.txt:
 * - تفاصيل المهمة (الرقم، المزود، المبلغ، تاريخ الاستحقاق)
 * - حالة المهمة
 * - إجراءات الإدارة: إكمال المهمة، إعادة الجدولة، أو الإلغاء
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentTaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    taskRepo: PaymentTaskRepository = remember { PaymentTaskRepositoryImpl() },
    adminRepo: AdminRepository = remember { AdminRepositoryImpl() }
) {
    var task by remember { mutableStateOf<PaymentTask?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = taskRepo.getTaskDetails(taskId)) {
                is AmanResult.Success -> {
                    task = res.data
                    isLoading = false
                }
                is AmanResult.Error -> {
                    errorMessage = res.error.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(taskId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل المهمة المالية",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    AmanLoadingIndicator(message = "جارٍ تحميل بيانات المهمة...")
                }
                errorMessage != null -> {
                    EmptyView(
                        icon = Icons.Default.Warning,
                        title = "خطأ في تحميل المهمة",
                        description = errorMessage ?: "",
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { loadData() }
                    )
                }
                task != null -> {
                    val t = task!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Main Task Header Card
                        AmanCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = t.customerNumber?.phoneNumber ?: "رقم الهاتف",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ProviderBadge(providerName = t.provider?.name ?: "غير محدد")
                                    }
                                    TaskStatusBadge(status = t.status)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = BorderColor)
                                Spacer(modifier = Modifier.height(16.dp))

                                DetailRow(label = "معرّف المهمة", value = t.id.take(8) + "...")
                                DetailRow(label = "نوع المهمة", value = if (t.taskType.name == "FIRST") "تفعيل أولي" else "سداد دوري")
                                DetailRow(label = "مبلغ السداد المطلوب", value = "${t.amount} ريال")
                                DetailRow(label = "تاريخ الاستحقاق", value = t.dueDate)
                                if (t.completedAt != null) {
                                    DetailRow(label = "تاريخ الإكمال", value = t.completedAt.take(10))
                                }
                            }
                        }

                        // Administrative Actions Card
                        if (t.status == TaskStatus.PENDING || t.status == TaskStatus.OVERDUE) {
                            AmanCard {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "إجراءات إدارة المهمة",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )

                                    AmanButton(
                                        text = "إكمال المهمة وتأكيد السداد",
                                        onClick = {
                                            scope.launch {
                                                when (val res = adminRepo.completePaymentTask(t.id)) {
                                                    is AmanResult.Success -> {
                                                        snackbarHostState.showSnackbar("تم إكمال المهمة بنجاح وجدولة المهمة الدورية التالية")
                                                        loadData()
                                                    }
                                                    is AmanResult.Error -> {
                                                        snackbarHostState.showSnackbar("فشل إكمال المهمة: ${res.error.message}")
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    OutlinedButton(
                                        onClick = { showCancelDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("إلغاء هذه المهمة", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel Task Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("تأكيد إلغاء المهمة", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("يرجى كتابة سبب إلغاء هذه المهمة المالية:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("سبب الإلغاء...") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            showCancelDialog = false
                            when (val res = adminRepo.cancelPaymentTask(taskId, cancelReason)) {
                                is AmanResult.Success -> {
                                    snackbarHostState.showSnackbar("تم إلغاء المهمة بنجاح")
                                    loadData()
                                }
                                is AmanResult.Error -> {
                                    snackbarHostState.showSnackbar("فشل إلغاء المهمة: ${res.error.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = cancelReason.isNotBlank()
                ) {
                    Text("تأكيد الإلغاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("تراجع")
                }
            }
        )
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
