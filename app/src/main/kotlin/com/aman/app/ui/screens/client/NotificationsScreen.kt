package com.aman.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.AppNotification
import com.aman.app.ui.components.*
import com.aman.app.ui.theme.*

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    customerId: String?,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadNotifications(customerId)
    }

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "الإشعارات والتنبيهات",
                onBackClick = onBack,
                actions = {
                    val state = uiState
                    if (state is NotificationsUiState.Success && state.notifications.any { !it.isRead }) {
                        TextButton(onClick = { viewModel.markAllAsRead(customerId) }) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديد الكل كمقروء", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Primary))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundMuted)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotificationsUiState.Loading -> {
                    AmanLoadingIndicator(message = "جارٍ تحميل الإشعارات...")
                }

                is NotificationsUiState.Error -> {
                    EmptyView(
                        icon = Icons.Default.Warning,
                        title = "تعذر تحميل الإشعارات",
                        description = state.message,
                        buttonText = "إعادة المحاولة",
                        onButtonClick = { viewModel.loadNotifications(customerId) }
                    )
                }

                is NotificationsUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        EmptyView(
                            icon = Icons.Default.Notifications,
                            title = "لا توجد إشعارات حالياً",
                            description = "ستظهر هنا إشعارات حالة طلباتك، ومواعيد تجديد حماية أرقامك."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.notifications) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = {
                                        if (!notification.isRead) {
                                            viewModel.markAsRead(notification.id, customerId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    AmanCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (notification.isRead) BackgroundMuted else Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification.isRead) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (notification.isRead) TextDisabled else Primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (notification.isRead) TextSecondary else TextPrimary,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = notification.createdAt?.take(16)?.replace("T", " ") ?: "اليوم",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextDisabled,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
