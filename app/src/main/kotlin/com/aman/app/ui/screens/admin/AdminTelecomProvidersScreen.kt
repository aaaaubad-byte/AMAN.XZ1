package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.TelecomPrefix
import com.aman.app.data.model.TelecomProvider
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminTelecomProvidersScreen(
    viewModel: AdminTelecomProvidersViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<TelecomProvider?>(null) }
    var newName by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var newLength by remember { mutableStateOf("9") }
    var newOrder by remember { mutableStateOf("1") }

    // Prefix Dialog states
    var prefixProviderTarget by remember { mutableStateOf<TelecomProvider?>(null) }
    var editingPrefixTarget by remember { mutableStateOf<Pair<TelecomProvider, TelecomPrefix>?>(null) }
    var prefixInputValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadProviders()
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
                currentRoute = Screen.AdminTelecomProviders.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "شركات الاتصالات",
                    subtitle = "إدارة مزودي الخدمة وبادئات الأرقام وترتيب العرض",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadProviders() }) {
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
                    Icon(Icons.Default.Add, contentDescription = "إضافة شركة")
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
                    is AdminTelecomUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminTelecomUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن بسجل شركات الاتصالات والبادئات. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminTelecomUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب شركات الاتصالات", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminTelecomUiState.Content -> {
                        Text(
                            text = "الشركات المسجلة (${state.providers.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.providers, key = { it.id }) { prov ->
                                val prefixes = state.prefixesByProvider[prov.id] ?: emptyList()
                                AdminProviderCard(
                                    provider = prov,
                                    prefixes = prefixes,
                                    onEdit = {
                                        editingProvider = prov
                                        newName = prov.name
                                        newCode = prov.code
                                        newLength = prov.numberLength.toString()
                                        newOrder = prov.displayOrder.toString()
                                    },
                                    onDisable = { viewModel.disableProvider(prov.id) },
                                    onActivate = { viewModel.activateProvider(prov.id) },
                                    onToggleVisibility = { viewModel.toggleProviderVisibility(prov) },
                                    onAddPrefixClick = {
                                        prefixProviderTarget = prov
                                        prefixInputValue = ""
                                    },
                                    onEditPrefixClick = { prefix ->
                                        editingPrefixTarget = prov to prefix
                                        prefixInputValue = prefix.prefix
                                    },
                                    onTogglePrefixStatus = { prefix ->
                                        viewModel.togglePrefixStatus(prefix.id, !prefix.isActive)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Dialog for Add/Edit Provider
            if (showAddDialog || editingProvider != null) {
                val dialogTitle = if (editingProvider != null) "تعديل بيانات الشركة" else "إضافة شركة اتصالات جديدة"
                val confirmText = if (editingProvider != null) "حفظ" else "إضافة"
                AlertDialog(
                    onDismissRequest = {
                        showAddDialog = false
                        editingProvider = null
                        newName = ""
                        newCode = ""
                        newLength = "9"
                        newOrder = "1"
                    },
                    title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("اسم الشركة (مثال: يمن موبايل)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newCode,
                                onValueChange = { newCode = it },
                                label = { Text("رمز الشركة (مثال: YE-YM)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newLength,
                                onValueChange = { newLength = it },
                                label = { Text("طول أرقام المشتركين (الافتراضي 9)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newOrder,
                                onValueChange = { newOrder = it },
                                label = { Text("ترتيب الظهور") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newName.isNotBlank() && newCode.isNotBlank()) {
                                    val length = newLength.toIntOrNull() ?: 9
                                    val order = newOrder.toIntOrNull() ?: 1
                                    if (editingProvider != null) {
                                        val updated = editingProvider!!.copy(
                                            name = newName.trim(),
                                            code = newCode.trim(),
                                            numberLength = length,
                                            displayOrder = order
                                        )
                                        viewModel.updateProvider(updated)
                                    } else {
                                        viewModel.addProvider(newName, newCode, length, order)
                                    }
                                    showAddDialog = false
                                    editingProvider = null
                                    newName = ""
                                    newCode = ""
                                    newLength = "9"
                                    newOrder = "1"
                                }
                            },
                            enabled = newName.isNotBlank() && newCode.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text(confirmText, color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAddDialog = false
                            editingProvider = null
                            newName = ""
                            newCode = ""
                            newLength = "9"
                            newOrder = "1"
                        }) {
                            Text("إلغاء", color = TextSecondary)
                        }
                    }
                )
            }

            // Dialog for Add Prefix
            prefixProviderTarget?.let { provider ->
                AlertDialog(
                    onDismissRequest = { prefixProviderTarget = null },
                    title = { Text("إضافة بادئة لشركة ${provider.name}", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = prefixInputValue,
                            onValueChange = { prefixInputValue = it },
                            label = { Text("البادئة (مثال: 77)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (prefixInputValue.isNotBlank()) {
                                    viewModel.addPrefix(provider.id, prefixInputValue.trim())
                                    prefixProviderTarget = null
                                }
                            },
                            enabled = prefixInputValue.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("إضافة", color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { prefixProviderTarget = null }) {
                            Text("إلغاء", color = TextSecondary)
                        }
                    }
                )
            }

            // Dialog for Edit Prefix
            editingPrefixTarget?.let { (provider, prefix) ->
                AlertDialog(
                    onDismissRequest = { editingPrefixTarget = null },
                    title = { Text("تعديل البادئة (${provider.name})", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = prefixInputValue,
                            onValueChange = { prefixInputValue = it },
                            label = { Text("البادئة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (prefixInputValue.isNotBlank()) {
                                    viewModel.updatePrefix(prefix.id, prefixInputValue.trim(), prefix.isActive)
                                    editingPrefixTarget = null
                                }
                            },
                            enabled = prefixInputValue.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("حفظ", color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingPrefixTarget = null }) {
                            Text("إلغاء", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminProviderCard(
    provider: TelecomProvider,
    prefixes: List<TelecomPrefix>,
    onEdit: () -> Unit,
    onDisable: () -> Unit,
    onActivate: () -> Unit,
    onToggleVisibility: () -> Unit,
    onAddPrefixClick: () -> Unit,
    onEditPrefixClick: (TelecomPrefix) -> Unit,
    onTogglePrefixStatus: (TelecomPrefix) -> Unit
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
                    Icon(Icons.Default.CellTower, contentDescription = null, tint = Primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text("الرمز: ${provider.code} • طول الرقم: ${provider.numberLength} • ترتيب: ${provider.displayOrder}", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Customer visibility badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (provider.isVisibleToCustomer) LightTeal else BackgroundLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (provider.isVisibleToCustomer) "ظاهرة للعملاء" else "مخفية",
                        color = if (provider.isVisibleToCustomer) Primary else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (provider.isActive) StatusProtectedBg else MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (provider.isActive) "نشطة" else "معطلة",
                        color = if (provider.isActive) StatusProtectedText else MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prefixes Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundLight)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "البادئات المعتمدة (${prefixes.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = onAddPrefixClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة بادئة", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
            }

            if (prefixes.isEmpty()) {
                Text(
                    text = "لا توجد بادئات مسجلة لهذه الشركة",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    prefixes.forEach { prefix ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (prefix.isActive) SurfaceWhite else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = prefix.prefix,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (prefix.isActive) TextPrimary else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onEditPrefixClick(prefix) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(12.dp), tint = Primary)
                            }
                            IconButton(
                                onClick = { onTogglePrefixStatus(prefix) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = if (prefix.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (prefix.isActive) "تعطيل" else "تفعيل",
                                    modifier = Modifier.size(12.dp),
                                    tint = if (prefix.isActive) Primary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons: Edit, Toggle Visibility, Activate/Disable
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (provider.isVisibleToCustomer) TextSecondary else Primary)
            ) {
                Text(
                    text = if (provider.isVisibleToCustomer) "إخفاء عن العملاء" else "إظهار للعملاء",
                    fontSize = 11.sp
                )
            }

            if (provider.isActive) {
                OutlinedButton(
                    onClick = onDisable,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تعطيل", fontSize = 12.sp)
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
