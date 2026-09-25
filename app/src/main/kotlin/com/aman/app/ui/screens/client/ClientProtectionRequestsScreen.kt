package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.data.model.ProtectionRequestStatus
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*

@Composable
fun ClientProtectionRequestsScreen(
    viewModel: ProtectionRequestsViewModel = viewModel(),
    customerId: String?,
    onMenuClick: () -> Unit,
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onRequestClick: (String) -> Unit = {}
) {
    val listState by viewModel.listState.collectAsState()
    val selectedTab by viewModel.selectedFilterTab.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadRequests(customerId)
    }

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "طلبات الحماية والتجديد",
                onMenuClick = onMenuClick,
                onNotificationsClick = onNavigateToNotifications
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateRequest,
                containerColor = Primary,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("طلب حماية جديدة", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundMuted)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tabs Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = SurfaceWhite,
                    contentColor = Primary,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = BorderColor) }
                ) {
                    RequestFilterTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { viewModel.setFilterTab(tab) },
                            text = {
                                Text(
                                    text = tab.titleAr,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                when (val state = listState) {
                    is ProtectionRequestsListUiState.Loading -> {
                        AmanLoadingIndicator(message = "جارٍ تحميل طلبات الحماية...")
                    }

                    is ProtectionRequestsListUiState.Error -> {
                        EmptyView(
                            icon = Icons.Default.Warning,
                            title = "تعذر تحميل الطلبات",
                            description = state.message,
                            buttonText = "إعادة المحاولة",
                            onButtonClick = { viewModel.loadRequests(customerId) }
                        )
                    }

                    is ProtectionRequestsListUiState.Success -> {
                        val filteredList = when (selectedTab) {
                            RequestFilterTab.ALL -> state.requests
                            RequestFilterTab.PENDING -> state.requests.filter { it.status == ProtectionRequestStatus.PENDING }
                            RequestFilterTab.APPROVED -> state.requests.filter { it.status == ProtectionRequestStatus.APPROVED }
                            RequestFilterTab.REJECTED -> state.requests.filter { it.status == ProtectionRequestStatus.REJECTED }
                        }

                        if (filteredList.isEmpty()) {
                            val emptyDesc = when (selectedTab) {
                                RequestFilterTab.ALL -> "لم تقدم أي طلب حماية بعد. قم بإنشاء طلب لتفعيل الحماية."
                                RequestFilterTab.PENDING -> "لا توجد طلبات قيد المراجعة حالياً."
                                RequestFilterTab.APPROVED -> "لا توجد طلبات معتمدة."
                                RequestFilterTab.REJECTED -> "لا توجد طلبات مرفوضة."
                            }
                            EmptyView(
                                icon = Icons.Default.ReceiptLong,
                                title = "لا توجد طلبات",
                                description = emptyDesc,
                                buttonText = if (selectedTab == RequestFilterTab.ALL) "إنشاء طلب جديد" else null,
                                onButtonClick = onNavigateToCreateRequest
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredList) { request ->
                                    ProtectionRequestItemCard(request = request, onClick = { onRequestClick(request.id) })
                                }

                                item {
                                    Spacer(modifier = Modifier.height(72.dp))
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
private fun ProtectionRequestItemCard(
    request: ProtectionRequest,
    onClick: () -> Unit = {}
) {
    AmanCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Phone & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = request.customerNumber?.phoneNumber ?: "رقم الهاتف",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProviderBadge(providerName = request.provider?.name ?: "غير محدد")
                }

                StatusBadge(status = request.status.name.lowercase())
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Plan & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الباقة: ${request.plan?.name ?: "باقة الحماية"}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "${request.protectionValue} ${request.plan?.currency ?: "ريال"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Payment info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طريقة الدفع: ${request.paymentMethod?.name ?: "سند تحويل"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                    text = "سند التحويل: ${request.transferData}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Rejection reason if rejected
            if (request.status == ProtectionRequestStatus.REJECTED && !request.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "سبب الرفض: ${request.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date
            Text(
                text = "تاريخ الطلب: ${request.createdAt?.take(10) ?: "اليوم"}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextDisabled,
                    fontSize = 11.sp
                )
            )
        }
    }
}
