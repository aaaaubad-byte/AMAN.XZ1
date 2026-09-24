package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
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
import com.aman.app.data.model.PaymentMethod
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminPaymentMethodsScreen(
    viewModel: AdminPaymentMethodsViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var walletName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadMethods()
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
                currentRoute = Screen.AdminPaymentMethods.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "طرق الدفع والتحويل",
                    subtitle = "إدارة المحافظ الإلكترونية والحسابات البنكية المعتمدة",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.loadMethods() }) {
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
                    Icon(Icons.Default.Add, contentDescription = "إضافة وسيلة دفع")
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
                    is AdminPaymentMethodsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is AdminPaymentMethodsUiState.ConfigurationPending -> {
                        AmanCard {
                            Text("حالة المزامنة السحابية", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("جاري الاتصال الآمن ببوابات وطرق الدفع السحابية. اضغط على زر التحديث لإعادة المزامنة.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminPaymentMethodsUiState.Error -> {
                        AmanCard {
                            Text("تعذر جلب وسائل الدفع", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is AdminPaymentMethodsUiState.Content -> {
                        Text(
                            text = "وسائل الدفع المسجلة (${state.methods.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.methods) { method ->
                                AdminPaymentMethodCard(
                                    method = method,
                                    onEdit = {
                                        editingMethod = method
                                        walletName = method.name
                                        accountNumber = method.accountNumber
                                        holderName = method.accountOwnerName
                                        instructions = method.paymentInstructions ?: ""
                                    },
                                    onDisable = { viewModel.disableMethod(method.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (showAddDialog || editingMethod != null) {
                val dialogTitle = if (editingMethod != null) "تعديل وسيلة الدفع" else "إضافة وسيلة دفع جديدة"
                val confirmText = if (editingMethod != null) "حفظ" else "إضافة"
                AlertDialog(
                    onDismissRequest = {
                        showAddDialog = false
                        editingMethod = null
                        walletName = ""
                        accountNumber = ""
                        holderName = ""
                        instructions = ""
                    },
                    title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = walletName,
                                onValueChange = { walletName = it },
                                label = { Text("اسم المحفظة / البنك (مثال: محفظة جوالي)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("رقم الحساب / المحفظة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = holderName,
                                onValueChange = { holderName = it },
                                label = { Text("اسم صاحب الحساب") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = instructions,
                                onValueChange = { instructions = it },
                                label = { Text("تعليمات التحويل للعميل") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (walletName.isNotBlank() && accountNumber.isNotBlank()) {
                                    if (editingMethod != null) {
                                        val updated = editingMethod!!.copy(
                                            name = walletName.trim(),
                                            accountNumber = accountNumber.trim(),
                                            accountOwnerName = holderName.trim(),
                                            paymentInstructions = if (instructions.isBlank()) null else instructions.trim()
                                        )
                                        viewModel.updateMethod(updated)
                                    } else {
                                        viewModel.addMethod(walletName, accountNumber, holderName, instructions)
                                    }
                                    showAddDialog = false
                                    editingMethod = null
                                    walletName = ""
                                    accountNumber = ""
                                    holderName = ""
                                    instructions = ""
                                }
                            },
                            enabled = walletName.isNotBlank() && accountNumber.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text(confirmText, color = SurfaceWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAddDialog = false
                            editingMethod = null
                            walletName = ""
                            accountNumber = ""
                            holderName = ""
                            instructions = ""
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
fun AdminPaymentMethodCard(
    method: PaymentMethod,
    onEdit: () -> Unit,
    onDisable: () -> Unit
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
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(method.walletName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text("الحساب: ${method.accountNumber} • ${method.accountHolderName}", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (method.isActive) StatusProtectedBg else MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (method.isActive) "مفعلة" else "معطلة",
                    color = if (method.isActive) StatusProtectedText else MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!method.paymentInstructions.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "التعليمات: ${method.paymentInstructions}",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Text("تعديل")
            }

            if (method.isActive) {
                OutlinedButton(
                    onClick = onDisable,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تعطيل")
                }
            }
        }
    }
}
