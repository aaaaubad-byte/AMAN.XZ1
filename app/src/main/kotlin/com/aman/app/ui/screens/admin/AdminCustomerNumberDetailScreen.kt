package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aman.app.data.model.CustomerNumberDetails
import com.aman.app.data.model.CustomerNumberStatus
import com.aman.app.data.model.ProtectionStatus
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.StatusBadge
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerNumberDetailScreen(
    numberId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    repository: AdminRepository = remember { AdminRepositoryImpl() }
) {
    var details by remember { mutableStateOf<CustomerNumberDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val res = repository.getCustomerNumberDetails(numberId)) {
                is AmanResult.Success -> {
                    details = res.data
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
                    Column {
                        Text(
                            text = "تفاصيل رقم العميل",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        details?.let {
                            Text(
                                text = it.number.phoneNumber,
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
                            Text(text = errorMessage ?: "حدث خطأ غير متوقع", color = Danger, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadData() }) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                details != null -> {
                    val data = details!!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // 1. بيانات الرقم الأساسية
                        item {
                            AmanCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Primary)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = data.number.phoneNumber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = data.provider?.name ?: data.number.provider?.name ?: "شركة اتصالات معتمدة",
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    StatusBadge(status = if (data.number.status == CustomerNumberStatus.ACTIVE) "نشط" else "معطل")
                                }
                            }
                        }

                        // 2. بيانات العميل
                        item {
                            AmanCard {
                                Text(
                                    text = "بيانات العميل المالك للرقم",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                data.customer?.let { cust ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = cust.email, fontSize = 12.sp, color = TextSecondary)
                                            cust.phoneNumber?.let { p ->
                                                Text(text = "الهاتف: $p", fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { onNavigate(Screen.AdminCustomerDetails.createRoute(cust.id)) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                                        ) {
                                            Text("ملف العميل", fontSize = 12.sp)
                                        }
                                    }
                                } ?: Text(
                                    text = "معرف العميل: ${data.number.customerId}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // 3. الحماية الحالية
                        item {
                            AmanCard {
                                Text(
                                    text = "الحماية الحالية للرقم",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                data.currentProtection?.let { prot ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(LightTeal),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Shield, contentDescription = null, tint = Primary)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "القيمة: ${prot.priceAtPurchase} ريال",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "المدة: ${prot.durationAtPurchase} يوم • تبدأ ${prot.startDate?.take(10) ?: "—"}",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "تنتهي: ${prot.endDate?.take(10) ?: "—"}",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        StatusBadge(status = prot.status.name)
                                    }
                                } ?: run {
                                    Text(
                                        text = "لا توجد وثيقة حماية نشطة مسجلة لهذا الرقم حالياً",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // 4. طلبات الحماية المرتبطة بهذا الرقم
                        item {
                            Text(
                                text = "طلبات الحماية المرتبطة (${data.requests.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        if (data.requests.isEmpty()) {
                            item {
                                AmanCard {
                                    Text("لا توجد طلبات حماية مسجلة لهذا الرقم", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(data.requests) { req ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate(Screen.AdminProtectionRequestDetail.createRoute(req.id)) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = req.planNameSnapshot ?: req.plan?.name ?: "طلب اشتراك حماية",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "المبلغ: ${req.amount} ريال • التاريخ: ${req.createdAt?.take(10) ?: "—"}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        StatusBadge(status = req.status.name)
                                    }
                                }
                            }
                        }

                        // 5. مهام الدفع المرتبطة
                        item {
                            Text(
                                text = "مهام الدفع المرتبطة (${data.tasks.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        if (data.tasks.isEmpty()) {
                            item {
                                AmanCard {
                                    Text("لا توجد مهام دفع مرتبطة بهذا الرقم", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(data.tasks) { task ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate(Screen.AdminPaymentTaskDetail.createRoute(task.id)) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "مهمة سداد: ${task.amount} ريال",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "تاريخ الاستحقاق: ${task.dueDate}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        StatusBadge(status = task.status.name)
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
