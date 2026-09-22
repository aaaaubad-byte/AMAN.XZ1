package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.NumberProtectionStatus
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*

@Composable
fun ClientNumbersScreen(
    viewModel: ClientNumbersViewModel = viewModel(),
    customerId: String?,
    onMenuClick: () -> Unit,
    onNavigateToAddNumber: () -> Unit,
    onNavigateToCreateRequest: (String) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val listState by viewModel.listState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadNumbers(customerId)
    }

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "أرقامي المسجلة",
                onMenuClick = onMenuClick,
                onNotificationsClick = onNavigateToNotifications
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddNumber,
                containerColor = Primary,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة رقم جديد", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundMuted)
                .padding(paddingValues)
        ) {
            when (val state = listState) {
                is NumbersListUiState.Loading -> {
                    AmanLoadingIndicator(message = "جارٍ تحميل قائمة الأرقام...")
                }

                is NumbersListUiState.Error -> {
                    EmptyView(
                        icon = Icons.Default.PhoneIphone,
                        title = "تعذر تحميل الأرقام",
                        description = state.message,
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { viewModel.loadNumbers(customerId) }
                    )
                }

                is NumbersListUiState.Success -> {
                    if (state.numbers.isEmpty()) {
                        EmptyView(
                            icon = Icons.Default.PhoneIphone,
                            title = "لا توجد أرقام مسجلة",
                            description = "لم تقم بإضافة أي رقم حتى الآن. أضف رقمك لتأمينه وحمايته من الإلغاء.",
                            buttonText = "إضافة رقمك الآن",
                            onButtonClick = onNavigateToAddNumber
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "لديك ${state.numbers.size} رقم مسجل في حسابك",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            items(state.numbers) { number ->
                                CustomerNumberItemCard(
                                    number = number,
                                    onRequestProtection = { onNavigateToCreateRequest(number.id) }
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

@Composable
private fun CustomerNumberItemCard(
    number: CustomerNumber,
    onRequestProtection: () -> Unit
) {
    AmanCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = number.phoneNumber,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ProviderBadge(providerName = number.provider?.name ?: "غير محدد")
                }

                NumberProtectionBadge(status = number.protectionStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تاريخ الإضافة: ${number.createdAt?.take(10) ?: "اليوم"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                if (number.protectionStatus == NumberProtectionStatus.UNPROTECTED) {
                    Button(
                        onClick = onRequestProtection,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("طلب حماية الرقم", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
