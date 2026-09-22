package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.StatusBadge
import com.aman.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerDetailsScreen(
    customerId: String,
    viewModel: AdminCustomersViewModel = viewModel(),
    onBack: () -> Unit
) {
    val detailsState by viewModel.detailsState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadCustomerDetails(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("تفاصيل العميل", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val state = detailsState) {
                is CustomerDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is CustomerDetailsUiState.Error -> {
                    AmanCard {
                        Text("خطأ في جلب تفاصيل العميل", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(state.message, color = TextSecondary, fontSize = 13.sp)
                    }
                }
                is CustomerDetailsUiState.Content -> {
                    val user = state.details.user
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // User Profile Summary
                        item {
                            AmanCard {
                                Text(user.name.ifBlank { "عميل أمان" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(user.email, fontSize = 13.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("رقم العضوية:", fontSize = 12.sp, color = TextSecondary)
                                    Text("#AM-${user.id.take(8).uppercase()}", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Registered Numbers
                        item {
                            Text(
                                text = "أرقام الهواتف المسجلة (${state.details.numbers.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        if (state.details.numbers.isEmpty()) {
                            item {
                                AmanCard {
                                    Text("لا توجد أرقام هواتف مسجلة لهذا العميل حالياً", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(state.details.numbers) { num ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = Primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(num.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(num.label ?: "بدون تسمية", fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }
                                        StatusBadge(status = num.verificationStatus)
                                    }
                                }
                            }
                        }

                        // Protections
                        item {
                            Text(
                                text = "سجل الحمايات (${state.details.protections.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        if (state.details.protections.isEmpty()) {
                            item {
                                AmanCard {
                                    Text("لا توجد حمايات سابقة أو نشطة", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(state.details.protections) { prot ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = Primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("القيمة: ${prot.priceAtPurchase} ريال", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("المدة: ${prot.durationAtPurchase} يوم", fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }
                                        StatusBadge(status = prot.status.name)
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
