package com.aman.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.app.data.model.AppNotification
import com.aman.app.ui.components.AdminDrawerContent
import com.aman.app.ui.components.AdminTopAppBar
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminNotificationsScreen(
    viewModel: AdminNotificationsViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                currentRoute = Screen.AdminNotifications.route,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopAppBar(
                    title = "الإشعارات الإدارية",
                    subtitle = "سجل الإشعارات المرسلة للمستخدمين",
                    onNavigationClick = { scope.launch { drawerState.open() } }
                )
            },
            containerColor = BackgroundLight
        ) { paddingValues ->
            when (val state = uiState) {
                is AdminNotificationsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is AdminNotificationsUiState.ConfigurationPending -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text("جاري الاتصال بمركز الإشعارات السحابي...", color = TextSecondary)
                    }
                }
                is AdminNotificationsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = Danger)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadData() }) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                is AdminNotificationsUiState.Content -> {
                    val filteredNotifications = when (selectedTab) {
                        1 -> state.notifications.filter { !it.isRead }
                        else -> state.notifications
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = SurfaceWhite,
                            contentColor = Primary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("جميع الإشعارات (${state.notifications.size})") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("غير المقروءة (${state.notifications.count { !it.isRead }})") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredNotifications.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد إشعارات في هذا التبويب", color = TextSecondary)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredNotifications, key = { it.id }) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onNavigate(Screen.AdminNotificationDetail.createRoute(item.id))
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (item.isRead) SurfaceWhite else Primary.copy(alpha = 0.05f)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (item.isRead) TextSecondary.copy(alpha = 0.1f)
                                                        else Primary.copy(alpha = 0.1f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (item.isRead) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                                                    contentDescription = null,
                                                    tint = if (item.isRead) TextSecondary else Primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = item.createdAt?.take(19)?.replace("T", " ") ?: "—",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
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
        }
    }
}
