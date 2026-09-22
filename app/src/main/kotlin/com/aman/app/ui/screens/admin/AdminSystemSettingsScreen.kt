package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.components.AmanButton
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminSystemSettingsScreen(
    viewModel: AdminSystemSettingsViewModel = viewModel(),
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
                currentRoute = Screen.AdminSystemSettings.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "إعدادات النظام",
                    subtitle = "تخصيص معلومات التطبيق، الدعم، الشروط وسياسة الخصوصية",
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when (val state = uiState) {
                    is AdminSystemSettingsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminSystemSettingsUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("تنبيه الاتصال بقاعدة البيانات", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("الاتصال بـ Supabase معلق بانتظار تزويد المفاتيح الحقيقية.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminSystemSettingsUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب إعدادات النظام", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminSystemSettingsUiState.Content -> {
                        var appName by remember(state.settings) { mutableStateOf(state.settings.appName) }
                        var contactInfo by remember(state.settings) { mutableStateOf(state.settings.contactInfo ?: "") }
                        var terms by remember(state.settings) { mutableStateOf(state.settings.termsAndConditions ?: "") }
                        var privacy by remember(state.settings) { mutableStateOf(state.settings.privacyPolicy ?: "") }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AmanCard {
                                Text("المعلومات الأساسية للتطبيق", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = appName,
                                    onValueChange = { appName = it },
                                    label = { Text("اسم التطبيق") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = contactInfo,
                                    onValueChange = { contactInfo = it },
                                    label = { Text("بيانات التواصل والدعم الفني") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            AmanCard {
                                Text("الشروط والأحكام وسياسة الخصوصية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = terms,
                                    onValueChange = { terms = it },
                                    label = { Text("الشروط والأحكام") },
                                    minLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = privacy,
                                    onValueChange = { privacy = it },
                                    label = { Text("سياسة الخصوصية") },
                                    minLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            AmanButton(
                                text = "حفظ إعدادات النظام",
                                onClick = {
                                    viewModel.saveSettings(appName, contactInfo, terms, privacy)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
