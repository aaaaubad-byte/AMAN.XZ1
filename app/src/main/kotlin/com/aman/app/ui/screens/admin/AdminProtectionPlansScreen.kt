package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.ProtectionPlan
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminProtectionPlansScreen(
    viewModel: AdminProtectionPlansViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<ProtectionPlan?>(null) }
    var selectedProviderId by remember { mutableStateOf("") }
    var planName by remember { mutableStateOf("") }
    var planPrice by remember { mutableStateOf("") }
    var planDuration by remember { mutableStateOf("90") }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                currentRoute = Screen.AdminProtectionPlans.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "باقات الحماية",
                    subtitle = "تحديد الباقات والأسعار والمدد لكل شركة اتصالات",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Primary)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Primary,
                    contentColor = SurfaceWhite
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة باقة")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when (val state = uiState) {
                    is AdminPlansUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminPlansUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن بسجل باقات الحماية المعتمدة. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminPlansUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب باقات الحماية", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminPlansUiState.Content -> {
                        Text(
                            text = "الباقات المتاحة (${state.plans.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.plans) { plan ->
                                AdminPlanCard(
                                    plan = plan,
                                    providerName = state.providers.find { it.id == plan.providerId }?.name ?: "شركة اتصالات",
                                    onEdit = {
                                        editingPlan = plan
                                        selectedProviderId = plan.providerId
                                        planName = plan.name
                                        planPrice = plan.price.toString()
                                        planDuration = plan.durationDays.toString()
                                    },
                                    onDisable = { viewModel.disablePlan(plan.id) },
                                    onActivate = { viewModel.activatePlan(plan.id) },
                                    onToggleVisibility = { viewModel.togglePlanVisibility(plan) }
                                )
                            }
                        }
                    }
                }
            }

            if (showAddDialog || editingPlan != null) {
                val state = uiState as? AdminPlansUiState.Content
                val dialogTitle = if (editingPlan != null) "تعديل بيانات الباقة" else "إضافة باقة حماية جديدة"
                val confirmText = if (editingPlan != null) "حفظ" else "إضافة"
                AlertDialog(
                    onDismissRequest = {
                        showAddDialog = false
                        editingPlan = null
                        selectedProviderId = ""
                        planName = ""
                        planPrice = ""
                        planDuration = "90"
                    },
                    title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("اختر الشركة:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            state?.providers?.forEach { prov ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = selectedProviderId == prov.id,
                                        onClick = { selectedProviderId = prov.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(prov.name, fontSize = 13.sp)
                                }
                            }
                            OutlinedTextField(
                                value = planName,
                                onValueChange = { planName = it },
                                label = { Text("اسم الباقة (مثال: باقة 3 أشهر)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = planPrice,
                                onValueChange = { planPrice = it },
                                label = { Text("السعر (ريال يمني)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = planDuration,
                                onValueChange = { planDuration = it },
                                label = { Text("المدة بالأيام (مثال: 90)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val price = planPrice.toDoubleOrNull() ?: 0.0
                                val duration = planDuration.toIntOrNull() ?: 90
                                if (selectedProviderId.isNotBlank() && planName.isNotBlank() && price > 0) {
                                    if (editingPlan != null) {
                                        val updated = editingPlan!!.copy(
                                            providerId = selectedProviderId,
                                            name = planName.trim(),
                                            price = price,
                                            durationDays = duration
                                        )
                                        viewModel.updatePlan(updated)
                                    } else {
                                        viewModel.addPlan(selectedProviderId, planName, price, duration)
                                    }
                                    showAddDialog = false
                                    editingPlan = null
                                    selectedProviderId = ""
                                    planName = ""
                                    planPrice = ""
                                    planDuration = "90"
                                }
                            },
                            enabled = selectedProviderId.isNotBlank() && planName.isNotBlank() && planPrice.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text(confirmText, color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAddDialog = false
                            editingPlan = null
                            selectedProviderId = ""
                            planName = ""
                            planPrice = ""
                            planDuration = "90"
                        }) {
                            Text("إلغاء", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminPlanCard(
    plan: ProtectionPlan,
    providerName: String,
    onEdit: () -> Unit,
    onDisable: () -> Unit,
    onActivate: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    AmanCard {
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
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text("الشركة: $providerName • المدة: ${plan.durationDays} يوم", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${plan.price} ريال", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Primary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (plan.isVisibleToCustomer) LightTeal else BackgroundLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (plan.isVisibleToCustomer) "ظاهرة للعملاء" else "مخفية",
                            color = if (plan.isVisibleToCustomer) Primary else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (plan.isActive) StatusProtectedBg else MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (plan.isActive) "نشطة" else "معطلة",
                            color = if (plan.isActive) StatusProtectedText else MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Text("تعديل", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onToggleVisibility,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (plan.isVisibleToCustomer) TextSecondary else Primary)
            ) {
                Text(
                    text = if (plan.isVisibleToCustomer) "إخفاء عن العملاء" else "إظهار للعملاء",
                    fontSize = 11.sp
                )
            }

            if (plan.isActive) {
                OutlinedButton(
                    onClick = onDisable,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("إيقاف", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onActivate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("تفعيل", fontSize = 12.sp, color = SurfaceWhite)
                }
            }
        }
    }
}
