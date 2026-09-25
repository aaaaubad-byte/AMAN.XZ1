package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
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
import com.aman.app.data.model.PaymentTask
import com.aman.app.data.model.TaskStatus
import com.aman.app.ui.components.*
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminPaymentTasksScreen(
    viewModel: AdminPaymentTasksViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cancellingTaskId by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    var reschedulingTaskId by remember { mutableStateOf<String?>(null) }
    var newDueDate by remember { mutableStateOf("") }
    var rescheduleReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadTasks()
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
                currentRoute = Screen.AdminPaymentTasks.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "مهام الدفع والتجديد",
                    subtitle = "إدارة دورات الدفع الدورية وإكمال المهام وإعادة جدولتها",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadTasks() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Primary)
                        }
                    }
                )
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
                    is AdminTasksUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminTasksUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن بسجل مهام السداد المجدولة. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminTasksUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب مهام الدفع", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminTasksUiState.Content -> {
                        // Filters
                        val filters = listOf(
                            null to "الكل (${state.allTasks.size})",
                            TaskStatus.UPCOMING to "قادمة (${state.allTasks.count { it.displayStatus(visibilityWindowDays = 7) == TaskStatus.UPCOMING }})",
                            TaskStatus.DUE to "اليوم (${state.allTasks.count { it.displayStatus(visibilityWindowDays = 7) == TaskStatus.DUE || it.displayStatus(visibilityWindowDays = 7) == TaskStatus.DUE_SOON }})",
                            TaskStatus.OVERDUE to "متأخرة (${state.allTasks.count { it.displayStatus(visibilityWindowDays = 7) == TaskStatus.OVERDUE }})",
                            TaskStatus.COMPLETED to "مكتملة (${state.allTasks.count { it.displayStatus(visibilityWindowDays = 7) == TaskStatus.COMPLETED }})",
                            TaskStatus.CANCELLED to "ملغاة (${state.allTasks.count { it.displayStatus(visibilityWindowDays = 7) == TaskStatus.CANCELLED }})"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filters) { (status, label) ->
                                val isSelected = state.selectedStatus == status
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Primary else SurfaceWhite)
                                        .clickable { viewModel.setFilter(status) }
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

                        if (state.filteredTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Checklist, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("لا توجد مهام دفع في هذا التبويب", color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.filteredTasks) { task ->
                                    AdminTaskCard(
                                        task = task,
                                        onComplete = { viewModel.completeTask(task.id) },
                                        onCancel = {
                                            cancellingTaskId = task.id
                                            cancelReason = ""
                                        },
                                        onReschedule = {
                                            reschedulingTaskId = task.id
                                            newDueDate = ""
                                            rescheduleReason = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cancel Task Dialog
            if (cancellingTaskId != null) {
                AlertDialog(
                    onDismissRequest = { cancellingTaskId = null },
                    title = { Text("إلغاء مهمة الدفع", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    text = {
                        Column {
                            Text("يرجى كتابة سبب الإلغاء للتوثيق في سجل العمليات:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = cancelReason,
                                onValueChange = { cancelReason = it },
                                placeholder = { Text("سبب الإلغاء...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = cancellingTaskId
                                if (id != null && cancelReason.isNotBlank()) {
                                    viewModel.cancelTask(id, cancelReason)
                                    cancellingTaskId = null
                                }
                            },
                            enabled = cancelReason.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("تأكيد الإلغاء", color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { cancellingTaskId = null }) {
                            Text("رجوع", color = TextSecondary)
                        }
                    }
                )
            }

            // Reschedule Task Dialog
            if (reschedulingTaskId != null) {
                AlertDialog(
                    onDismissRequest = { reschedulingTaskId = null },
                    title = { Text("إعادة جدولة المهمة", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newDueDate,
                                onValueChange = { newDueDate = it },
                                label = { Text("تاريخ الاستحقاق الجديد (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = rescheduleReason,
                                onValueChange = { rescheduleReason = it },
                                label = { Text("سبب إعادة الجدولة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = reschedulingTaskId
                                if (id != null && newDueDate.isNotBlank() && rescheduleReason.isNotBlank()) {
                                    viewModel.rescheduleTask(id, newDueDate, rescheduleReason)
                                    reschedulingTaskId = null
                                }
                            },
                            enabled = newDueDate.isNotBlank() && rescheduleReason.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("تأكيد الجدولة", color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { reschedulingTaskId = null }) {
                            Text("إلغاء", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminTaskCard(
    task: PaymentTask,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onReschedule: () -> Unit
) {
    val displayStatus = task.displayStatus(visibilityWindowDays = 7)
    val isPendingAction = displayStatus == TaskStatus.UPCOMING || displayStatus == TaskStatus.DUE || displayStatus == TaskStatus.OVERDUE

    AmanCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = task.customerNumber?.phoneNumber ?: "مهمة سداد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "رقم المهمة: #TSK-${task.id.take(8).uppercase()} • المبلغ: ${task.amount} ريال",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "تاريخ الاستحقاق: ${task.dueDate.take(10)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            StatusBadge(status = displayStatus.name)
        }

        if (isPendingAction) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إكمال", fontSize = 12.sp, color = SurfaceWhite)
                }
                OutlinedButton(
                    onClick = onReschedule,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("تأجيل", fontSize = 12.sp, color = Primary)
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        }
    }
}
