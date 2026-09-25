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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AuditLog
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogDetailScreen(
    logId: String,
    onBack: () -> Unit,
    repository: AdminRepository = remember { AdminRepositoryImpl() }
) {
    var auditLog by remember { mutableStateOf<AuditLog?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = repository.getAuditLog(logId)) {
                is AmanResult.Success -> {
                    auditLog = res.data
                    isLoading = false
                }
                is AmanResult.Error -> {
                    errorMessage = res.error.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(logId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تفاصيل سجل التدقيق",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        auditLog?.let {
                            Text(
                                text = "العملية: ${it.actionType}",
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
                            Text(text = errorMessage ?: "تعذر جلب تفاصيل السجل", color = Danger, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadData() }) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                auditLog != null -> {
                    val log = auditLog!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. بطاقة نوع الإجراء والوقت
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
                                            .background(Primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = Primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = log.actionType,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = log.createdAt?.take(19)?.replace("T", " ") ?: "—",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LightTeal)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = log.affectedTable ?: "عام",
                                        color = Primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 2. بيانات المنفذ والكيان
                        AmanCard {
                            Text(
                                text = "أطراف العملية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AuditDetailRow(
                                label = "المنفذ (Actor)",
                                value = log.actorName ?: log.actorId ?: "النظام الآلي"
                            )
                            log.actorId?.let { aid ->
                                AuditDetailRow(label = "معرف المنفذ", value = aid)
                            }
                            AuditDetailRow(
                                label = "الكيان المتأثر (Entity)",
                                value = log.affectedTable ?: "—"
                            )
                            log.affectedRecord?.let { recId ->
                                AuditDetailRow(label = "معرف السجل (Entity ID)", value = recId)
                            }
                            AuditDetailRow(
                                label = "معرف التدقيق (Log ID)",
                                value = log.id
                            )
                        }

                        // 3. تفاصيل العملية (Details)
                        if (!log.details.isNullOrBlank()) {
                            AmanCard {
                                Text(
                                    text = "بيانات وتفاصيل العملية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BackgroundLight)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = log.details ?: "",
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // 4. الحقول المتغيرة / البيانات السابقة والجديدة (Metadata & Changes)
                        if (!log.previousState.isNullOrBlank() || !log.newState.isNullOrBlank()) {
                            AmanCard {
                                Text(
                                    text = "تتبع التغييرات (الحالة السابقة والجديدة)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                if (!log.previousState.isNullOrBlank()) {
                                    Text(
                                        text = "البيانات السابقة:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BackgroundLight)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = log.previousState ?: "",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                if (!log.newState.isNullOrBlank()) {
                                    Text(
                                        text = "البيانات الجديدة:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LightTeal.copy(alpha = 0.5f))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = log.newState ?: "",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
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
private fun AuditDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
