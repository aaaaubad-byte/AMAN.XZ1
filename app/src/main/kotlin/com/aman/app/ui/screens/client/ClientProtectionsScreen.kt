package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.aman.app.data.model.Protection
import com.aman.app.data.model.ProtectionDisplayStatus
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*

@Composable
fun ClientProtectionsScreen(
    viewModel: ClientProtectionsViewModel = viewModel(),
    customerId: String?,
    onMenuClick: () -> Unit,
    onNavigateToCreateRequest: (String?) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onProtectionClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadProtections(customerId)
    }

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "حماياتي المسجلة",
                onMenuClick = onMenuClick,
                onNotificationsClick = onNavigateToNotifications
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToCreateRequest(null) },
                containerColor = Primary,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
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
                // Filter Tabs Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = SurfaceWhite,
                    contentColor = Primary,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = BorderColor) }
                ) {
                    ProtectionFilterTab.entries.forEach { tab ->
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

                when (val state = uiState) {
                    is ProtectionsUiState.Loading -> {
                        AmanLoadingIndicator(message = "جارٍ تحميل الحمايات...")
                    }

                    is ProtectionsUiState.Error -> {
                        EmptyView(
                            icon = Icons.Default.Warning,
                            title = "تعذر تحميل الحمايات",
                            description = state.message,
                            buttonText = "إعادة المحاولة",
                            onButtonClick = { viewModel.loadProtections(customerId) }
                        )
                    }

                    is ProtectionsUiState.Success -> {
                        val filteredList = viewModel.getFilteredProtections(state.protections, selectedTab)

                        if (filteredList.isEmpty()) {
                            val emptyDesc = when (selectedTab) {
                                ProtectionFilterTab.ALL -> "لم يتم تفعيل أي حماية بعد. قم بتقديم طلب حماية لأرقامك."
                                ProtectionFilterTab.ACTIVE -> "لا توجد حمايات نشطة حالياً."
                                ProtectionFilterTab.NEEDS_RENEWAL -> "لا توجد حمايات شارفت على الانتهاء."
                                ProtectionFilterTab.EXPIRED -> "لا توجد حمايات منتهية."
                            }
                            EmptyView(
                                icon = Icons.Default.Shield,
                                title = "لا توجد نتائج",
                                description = emptyDesc,
                                buttonText = if (selectedTab == ProtectionFilterTab.ALL) "طلب حماية لرقمك" else null,
                                onButtonClick = { onNavigateToCreateRequest(null) }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredList) { protection ->
                                    ProtectionDetailCard(
                                        protection = protection,
                                        onClick = { onProtectionClick(protection.id) },
                                        onRenew = { onNavigateToCreateRequest(protection.customerNumberId) }
                                    )
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
private fun ProtectionDetailCard(
    protection: Protection,
    onClick: () -> Unit = {},
    onRenew: () -> Unit
) {
    val displayStatus = protection.calculateDisplayStatus()
    val daysRemaining = protection.daysRemaining()

    AmanCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Phone & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = protection.customerNumber?.phoneNumber ?: "رقم الهاتف",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProviderBadge(providerName = protection.provider?.name ?: "غير محدد")
                }

                ProtectionDisplayBadge(status = displayStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Plan & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الباقة المفعلة",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = protection.plan?.name ?: "${protection.durationAtPurchase} يوم",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "المتبقي",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = "$daysRemaining يوم",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (displayStatus) {
                                ProtectionDisplayStatus.ACTIVE -> Color(0xFF16A34A)
                                ProtectionDisplayStatus.NEEDS_RENEWAL -> Color(0xFFD97706)
                                ProtectionDisplayStatus.EXPIRED -> Color(0xFFDC2626)
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تاريخ البدء: ${protection.startDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                    text = "تاريخ الانتهاء: ${protection.endDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            // Renewal action if needed or expired
            if (displayStatus == ProtectionDisplayStatus.NEEDS_RENEWAL || displayStatus == ProtectionDisplayStatus.EXPIRED) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRenew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (displayStatus == ProtectionDisplayStatus.NEEDS_RENEWAL) Color(0xFFD97706) else Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (displayStatus == ProtectionDisplayStatus.NEEDS_RENEWAL) "تجديد الحماية قبل الانتهاء" else "إعادة تفعيل الحماية",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
