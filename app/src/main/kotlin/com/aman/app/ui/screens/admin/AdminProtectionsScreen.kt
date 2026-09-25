package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.Protection
import com.aman.app.data.model.ProtectionDisplayStatus
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.StatusBadge
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminProtectionsScreen(
    viewModel: AdminProtectionsViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedProtection by remember { mutableStateOf<Protection?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProtections()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                currentRoute = Screen.AdminProtections.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "إدارة الحمايات",
                    subtitle = "متابعة الحمايات النشطة والمنتهية واللقطات التاريخية",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadProtections() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Primary)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when (val state = uiState) {
                    is AdminProtectionsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminProtectionsUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن بسجل الحمايات السحابي. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminProtectionsUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب بيانات الحمايات", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminProtectionsUiState.Content -> {
                        // Search Field
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("بحث برقم الهاتف أو رقم الوثيقة...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Filters
                        val filters = listOf(
                            null to "الكل (${state.allProtections.size})",
                            ProtectionDisplayStatus.ACTIVE to "نشطة",
                            ProtectionDisplayStatus.NEEDS_RENEWAL to "تحتاج تجديد",
                            ProtectionDisplayStatus.EXPIRED to "منتهية"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filters) { (filterStatus, label) ->
                                val isSelected = state.selectedFilter == filterStatus
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Primary else SurfaceWhite)
                                        .clickable { viewModel.setFilter(filterStatus) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) SurfaceWhite else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (state.filteredProtections.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("لا توجد حمايات مطابقة", color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.filteredProtections) { prot ->
                                    AdminProtectionItemCard(
                                        prot = prot,
                                        onClick = { selectedProtection = prot }
                                    )
                                }
                            }
                        }
                    }
                }

                // Protection Details Dialog
                selectedProtection?.let { prot ->
                    val displayStatus = prot.calculateDisplayStatus()
                    val daysRemaining = prot.daysRemaining()
                    AlertDialog(
                        onDismissRequest = { selectedProtection = null },
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تفاصيل الحماية",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                StatusBadge(status = displayStatus.name)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("رقم الوثيقة:", fontSize = 12.sp, color = TextSecondary)
                                    Text("#PROT-" + prot.id.take(8).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("رقم الهاتف المحمي:", fontSize = 12.sp, color = TextSecondary)
                                    Text(prot.customerNumber?.phoneNumber ?: "-", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الشركة المشغلة:", fontSize = 12.sp, color = TextSecondary)
                                    Text(prot.provider?.name ?: "-", fontSize = 12.sp, color = TextPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("باقة الحماية:", fontSize = 12.sp, color = TextSecondary)
                                    Text(prot.plan?.name ?: "-", fontSize = 12.sp, color = TextPrimary)
                                }
                                HorizontalDivider(color = BorderColor)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("تاريخ بدء الحماية:", fontSize = 12.sp, color = TextSecondary)
                                    Text(prot.startDate.take(10), fontSize = 12.sp, color = TextPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("تاريخ انتهاء الحماية:", fontSize = 12.sp, color = TextSecondary)
                                    Text(prot.endDate.take(10), fontSize = 12.sp, color = TextPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المدة المعتمدة:", fontSize = 12.sp, color = TextSecondary)
                                    Text("" + prot.durationAtPurchase + " يوم", fontSize = 12.sp, color = TextPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الأيام المتبقية:", fontSize = 12.sp, color = TextSecondary)
                                    Text("" + daysRemaining + " يوم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (daysRemaining <= 5) MaterialTheme.colorScheme.error else Primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("القيمة التاريخية:", fontSize = 12.sp, color = TextSecondary)
                                    Text("" + prot.priceAtPurchase + " ريال", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val custId = prot.customerId
                                    selectedProtection = null
                                    onNavigate(Screen.AdminCustomerDetails.createRoute(custId))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("عرض ملف العميل", color = SurfaceWhite)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { selectedProtection = null }) {
                                Text("إغلاق", color = TextSecondary)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminProtectionItemCard(
    prot: Protection,
    onClick: () -> Unit = {}
) {
    val displayStatus = prot.calculateDisplayStatus()
    val daysRemaining = prot.daysRemaining()

    AmanCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = prot.customerNumber?.phoneNumber ?: "حماية رقم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "رقم الوثيقة: #PROT-${prot.id.take(8).uppercase()} • الشركة: ${prot.provider?.name ?: "-"}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "الباقة: ${prot.plan?.name ?: "-"}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            StatusBadge(status = displayStatus.name)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("قيمة الشراء التاريخية", fontSize = 11.sp, color = TextSecondary)
                Text("${prot.priceAtPurchase} ريال", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column {
                Text("المدة المعتمدة", fontSize = 11.sp, color = TextSecondary)
                Text("${prot.durationAtPurchase} يوم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column {
                Text("المتبقي", fontSize = 11.sp, color = TextSecondary)
                Text("$daysRemaining يوم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (daysRemaining <= 5) MaterialTheme.colorScheme.error else Primary)
            }
        }
    }
}
