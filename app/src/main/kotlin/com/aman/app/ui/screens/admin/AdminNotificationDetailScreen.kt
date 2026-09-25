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
import com.aman.app.data.model.AppNotification
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationDetailScreen(
    notificationId: String,
    onBack: () -> Unit,
    repository: AdminRepository = remember { AdminRepositoryImpl() }
) {
    var notification by remember { mutableStateOf<AppNotification?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMarkingRead by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = repository.getNotification(notificationId)) {
                is AmanResult.Success -> {
                    notification = res.data
                    isLoading = false
                }
                is AmanResult.Error -> {
                    errorMessage = res.error.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(notificationId) {
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
                    Text(
                        text = "تفاصيل الإشعار",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
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
                            Text(text = errorMessage ?: "تعذر جلب تفاصيل الإشعار", color = Danger, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadData() }) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                notification != null -> {
                    val notif = notification!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // بطاقة الحالة ورأس الإشعار
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
                                                if (notif.isRead) TextSecondary.copy(alpha = 0.1f)
                                                else Primary.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (notif.isRead) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                                            contentDescription = null,
                                            tint = if (notif.isRead) TextSecondary else Primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (notif.isRead) "إشعار مقروء" else "إشعار غير مقروء",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (notif.isRead) TextSecondary else Primary
                                        )
                                        Text(
                                            text = notif.createdAt?.take(19)?.replace("T", " ") ?: "—",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (notif.isRead) BackgroundLight else LightTeal)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (notif.isRead) "مقروء" else "جديد",
                                        color = if (notif.isRead) TextSecondary else Primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // نص الإشعار
                        AmanCard {
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = notif.message,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        }

                        // البيانات الوصفية (Metadata)
                        AmanCard {
                            Text(
                                text = "البيانات الوصفية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("معرف الإشعار", fontSize = 13.sp, color = TextSecondary)
                                Text(notif.id, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                            notif.userId?.let { uid ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("معرف المستخدم", fontSize = 13.sp, color = TextSecondary)
                                    Text(uid, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                            }
                            notif.actionUrl?.let { link ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("الرابط أو المرجع", fontSize = 13.sp, color = TextSecondary)
                                    Text(link, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Primary)
                                }
                            }
                        }

                        // زر تحديد كمقروء عند عدم القراءة
                        if (!notif.isRead) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isMarkingRead = true
                                    scope.launch {
                                        when (val markRes = repository.markNotificationAsRead(notif.id)) {
                                            is AmanResult.Success -> {
                                                notification = notif.copy(isRead = true)
                                                actionMessage = "تم تحديد الإشعار كمقروء بنجاح"
                                            }
                                            is AmanResult.Error -> {
                                                actionMessage = markRes.error.message
                                            }
                                        }
                                        isMarkingRead = false
                                    }
                                },
                                enabled = !isMarkingRead,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحديد كمقروء", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
