package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*

@Composable
fun AddNumberScreen(
    viewModel: ClientNumbersViewModel = viewModel(),
    onBack: () -> Unit,
    onNumberAddedSuccess: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    val detectedProvider by viewModel.detectedProvider.collectAsState()
    val addState by viewModel.addState.collectAsState()

    LaunchedEffect(addState) {
        if (addState is AddNumberUiState.Success) {
            viewModel.resetAddState()
            onNumberAddedSuccess()
        }
    }

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "إضافة رقم هاتف جديد",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card explaining auto-detection
                AmanCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "التعرف التلقائي على مزود الخدمة",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يقوم النظام تلقائياً بتحديد شركة الاتصالات التابع لها رقمك بمجرد كتابته استناداً إلى بادئة الرقم، دون الحاجة للاختيار اليدوي.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                // Phone Input Card
                AmanCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "رقم الهاتف المراد حمايته",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                if (input.length <= 9 && input.all { it.isDigit() }) {
                                    phoneNumber = input
                                    viewModel.onPhoneNumberChanged(input)
                                }
                            },
                            placeholder = { Text("مثال: 771234567") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            },
                            trailingIcon = {
                                Text(
                                    text = "${phoneNumber.length}/9",
                                    color = if (phoneNumber.length == 9) Primary else TextDisabled,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        )

                        // Real-time Provider Detection Indicator
                        if (detectedProvider != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .background(Primary.copy(alpha = 0.05f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "المزود المكتشف:",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }

                                    ProviderBadge(providerName = detectedProvider?.name ?: "")
                                }
                            }
                        } else if (phoneNumber.length >= 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF3C7))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "البادئة غير معروفة. البادئات المعتمدة: 77, 78, 73, 71, 70",
                                        color = Color(0xFF92400E),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Error Banner if submission failed
                        val currentAddState = addState
                        if (currentAddState is AddNumberUiState.Error) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEE2E2))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentAddState.message,
                                        color = Color(0xFFDC2626),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        AmanButton(
                            text = if (addState is AddNumberUiState.Submitting) "جارٍ تسجيل الرقم..." else "تسجيل الرقم وحفظه",
                            onClick = { viewModel.submitNewNumber(phoneNumber) },
                            enabled = phoneNumber.length == 9 && addState !is AddNumberUiState.Submitting
                        )
                    }
                }

                // Supported Providers Reference Card
                AmanCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "شركات الاتصالات والبادئات المدعومة:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("يمن موبايل (Yemen Mobile)", style = MaterialTheme.typography.bodySmall)
                            Text("77, 78", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("يو (YOU Telecom)", style = MaterialTheme.typography.bodySmall)
                            Text("73", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("سبأفون (SabaFon)", style = MaterialTheme.typography.bodySmall)
                            Text("71", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("واي (Y Telecom)", style = MaterialTheme.typography.bodySmall)
                            Text("70", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
