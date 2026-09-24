package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.TaskSettings
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.components.AmanButton
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminTaskSettingsScreen(
    viewModel: AdminTaskSettingsViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
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
                currentRoute = Screen.AdminTaskSettings.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "إعدادات مهام الدفع",
                    subtitle = "تكوين دورات الدفع الدورية، مبالغ السداد، وأيام التنبيه",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadSettings() }) {
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
                    is AdminTaskSettingsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminTaskSettingsUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن بضوابط وإعدادات المهام السحابية. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminTaskSettingsUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب إعدادات المهام", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminTaskSettingsUiState.Content -> {
                        if (state.settings.isEmpty()) {
                            AmanCard {
                                Text("لا توجد إعدادات مهام مسجلة حالياً", color = TextSecondary, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.settings) { setting ->
                                    TaskSettingEditorCard(
                                        settings = setting,
                                        onSave = { updated -> viewModel.updateSettings(updated) }
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

@Composable
fun TaskSettingEditorCard(
    settings: TaskSettings,
    onSave: (TaskSettings) -> Unit
) {
    var firstTaskEnabled by remember(settings) { mutableStateOf(settings.firstTaskEnabled) }
    var firstTaskAmount by remember(settings) { mutableStateOf(settings.firstTaskAmount?.toString() ?: "") }
    var recurringEnabled by remember(settings) { mutableStateOf(settings.recurringTaskEnabled) }
    var recurringCycleDays by remember(settings) { mutableStateOf(settings.recurringCycleDays.toString()) }
    var recurringAmount by remember(settings) { mutableStateOf(settings.recurringTaskAmount.toString()) }
    var visibleDays by remember(settings) { mutableStateOf(settings.daysVisibleBeforeDue.toString()) }
    var manualReschedule by remember(settings) { mutableStateOf(settings.manualRescheduleEnabled) }

    AmanCard {
        Text("إعدادات مهام الشركة", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))

        // First Task Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("توليد مهمة أولى عند قبول الحماية", fontSize = 13.sp)
            Switch(
                checked = firstTaskEnabled,
                onCheckedChange = { firstTaskEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = LightTeal)
            )
        }

        if (firstTaskEnabled) {
            OutlinedTextField(
                value = firstTaskAmount,
                onValueChange = { firstTaskAmount = it },
                label = { Text("مبلغ المهمة الأولى (ريال)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)

        // Recurring Task Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("توليد مهام دورية بعد كل سداد", fontSize = 13.sp)
            Switch(
                checked = recurringEnabled,
                onCheckedChange = { recurringEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = LightTeal)
            )
        }

        if (recurringEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = recurringCycleDays,
                    onValueChange = { recurringCycleDays = it },
                    label = { Text("أيام الدورة (مثال: 30)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = recurringAmount,
                    onValueChange = { recurringAmount = it },
                    label = { Text("المبلغ الدوري (ريال)") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = visibleDays,
            onValueChange = { visibleDays = it },
            label = { Text("عدد أيام ظهور المهمة قبل تاريخ الاستحقاق") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("السماح بإعادة الجدولة اليدوية", fontSize = 13.sp)
            Switch(
                checked = manualReschedule,
                onCheckedChange = { manualReschedule = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = LightTeal)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        AmanButton(
            text = "حفظ الإعدادات",
            onClick = {
                val updated = settings.copy(
                    firstTaskEnabled = firstTaskEnabled,
                    firstTaskAmount = firstTaskAmount.toDoubleOrNull(),
                    recurringTaskEnabled = recurringEnabled,
                    repeatIntervalDays = recurringCycleDays.toIntOrNull() ?: settings.repeatIntervalDays,
                    recurringTaskAmount = recurringAmount.toDoubleOrNull() ?: settings.recurringTaskAmount,
                    daysVisibleBeforeDue = visibleDays.toIntOrNull() ?: settings.daysVisibleBeforeDue,
                    manualRescheduleEnabled = manualReschedule
                )
                onSave(updated)
            }
        )
    }
}
