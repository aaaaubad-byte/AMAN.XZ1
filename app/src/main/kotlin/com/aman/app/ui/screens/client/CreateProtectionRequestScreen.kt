package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.PaymentMethod
import com.aman.app.data.model.ProtectionPlan
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*

@Composable
fun CreateProtectionRequestScreen(
    viewModel: ProtectionRequestsViewModel = viewModel(),
    customerId: String?,
    preselectedNumberId: String? = null,
    onBack: () -> Unit,
    onRequestCreatedSuccess: () -> Unit,
    onNavigateToAddNumber: () -> Unit
) {
    val createState by viewModel.createState.collectAsState()
    var transferDataInput by remember { mutableStateOf("") }

    LaunchedEffect(customerId, preselectedNumberId) {
        viewModel.prepareCreateForm(customerId, preselectedNumberId)
    }

    LaunchedEffect(createState) {
        if (createState is CreateRequestUiState.SubmittedSuccess) {
            viewModel.resetCreateState()
            onRequestCreatedSuccess()
        }
    }

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "تقديم طلب حماية رقم",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundMuted)
                .padding(paddingValues)
        ) {
            when (val state = createState) {
                is CreateRequestUiState.Loading -> {
                    AmanLoadingIndicator(message = "جارٍ تجهيز بيانات الطلب...")
                }

                is CreateRequestUiState.Error -> {
                    EmptyView(
                        icon = Icons.Default.Warning,
                        title = "خطأ في تقديم الطلب",
                        description = state.message,
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { viewModel.prepareCreateForm(customerId, preselectedNumberId) }
                    )
                }

                is CreateRequestUiState.Ready, CreateRequestUiState.Submitting -> {
                    val readyState = if (state is CreateRequestUiState.Ready) state else null
                    val numbers = readyState?.availableNumbers ?: emptyList()
                    val selectedNumber = readyState?.selectedNumber
                    val plans = readyState?.availablePlans ?: emptyList()
                    val selectedPlan = readyState?.selectedPlan
                    val paymentMethods = readyState?.availablePaymentMethods ?: emptyList()
                    val selectedPaymentMethod = readyState?.selectedPaymentMethod
                    val isSubmitting = state is CreateRequestUiState.Submitting

                    if (numbers.isEmpty()) {
                        EmptyView(
                            icon = Icons.Default.PhoneIphone,
                            title = "لا توجد أرقام مسجلة",
                            description = "يجب عليك أولاً إضافة رقم هاتف في حسابك لتتمكن من تقديم طلب حماية له.",
                            buttonText = "إضافة رقم هاتف الآن",
                            onButtonClick = onNavigateToAddNumber
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section 1: Select Number
                            AmanCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "1. اختر رقم الهاتف المراد حمايته",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    numbers.forEach { number ->
                                        val isSelected = selectedNumber?.id == number.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Primary else BorderColor,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .background(if (isSelected) Primary.copy(alpha = 0.05f) else SurfaceWhite)
                                                .clickable { viewModel.selectNumber(number) }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { viewModel.selectNumber(number) },
                                                    colors = RadioButtonDefaults.colors(selectedColor = Primary)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = number.phoneNumber,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            ProviderBadge(providerName = number.provider?.name ?: "")
                                        }
                                    }
                                }
                            }

                            // Section 2: Select Plan
                            AmanCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "2. اختر باقة الحماية المناسبة",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (plans.isEmpty()) {
                                        Text(
                                            text = "لا توجد باقات متاحة لهذا المزود حالياً",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                        )
                                    } else {
                                        plans.forEach { plan ->
                                            val isSelected = selectedPlan?.id == plan.id
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Primary else BorderColor,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .background(if (isSelected) Primary.copy(alpha = 0.05f) else SurfaceWhite)
                                                .clickable { viewModel.selectPlan(plan) }
                                                .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = { viewModel.selectPlan(plan) },
                                                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = plan.name,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Text(
                                                            text = "المدة: ${plan.durationDays} يوم",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = TextSecondary
                                                            )
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "${plan.price} ${plan.currency}",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Primary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 3: Select Payment Method
                            AmanCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "3. اختر وسيلة الدفع والتحويل",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    paymentMethods.forEach { method ->
                                        val isSelected = selectedPaymentMethod?.id == method.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Primary else BorderColor,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .background(if (isSelected) Primary.copy(alpha = 0.05f) else SurfaceWhite)
                                                .clickable { viewModel.selectPaymentMethod(method) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.selectPaymentMethod(method) },
                                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = method.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                if (!method.accountNumber.isNullOrBlank()) {
                                                    Text(
                                                        text = "رقم الحساب: ${method.accountNumber}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Primary,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    )
                                                }
                                                val methodInstructions = method.instructions
                                                if (!methodInstructions.isNullOrBlank()) {
                                                    Text(
                                                        text = methodInstructions,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = TextSecondary,
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 4: Transfer Details / Reference
                            AmanCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "4. بيانات التحويل أو السند المالي",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "أدخل رقم الحوالة، أو رقم العملية، أو اسم المحول للتحقق من الدفع.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = transferDataInput,
                                        onValueChange = { transferDataInput = it },
                                        placeholder = { Text("مثال: رقم الحوالة 82910398 أو رقم العملية في المحفظة...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = BorderColor,
                                            focusedContainerColor = SurfaceWhite,
                                            unfocusedContainerColor = SurfaceWhite
                                        )
                                    )
                                }
                            }

                            // Summary & Submission
                            AmanCard {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "ملخص الطلب",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("رقم الهاتف:", style = MaterialTheme.typography.bodySmall)
                                        Text(selectedNumber?.phoneNumber ?: "-", fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الباقة المختارة:", style = MaterialTheme.typography.bodySmall)
                                        Text(selectedPlan?.name ?: "-", fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("المبلغ المطلوب:", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${selectedPlan?.price ?: 0} ${selectedPlan?.currency ?: "ريال"}",
                                            fontWeight = FontWeight.Bold,
                                            color = Primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    AmanButton(
                                        text = if (isSubmitting) "جارٍ إرسال الطلب..." else "إرسال طلب الحماية للمراجعة",
                                        onClick = { viewModel.submitRequest(transferDataInput) },
                                        enabled = !isSubmitting &&
                                                selectedNumber != null &&
                                                selectedPlan != null &&
                                                selectedPaymentMethod != null &&
                                                transferDataInput.isNotBlank()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                is CreateRequestUiState.SubmittedSuccess -> {
                    // handled by LaunchedEffect
                }
            }
        }
    }
}
