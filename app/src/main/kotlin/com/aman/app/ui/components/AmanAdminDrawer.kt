package com.aman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.ui.navigation.Screen
import com.aman.app.ui.theme.*

data class AdminDrawerItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopAppBar(
    title: String,
    subtitle: String? = null,
    onNavigationClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "قائمة الإدارة",
                    tint = Primary
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceWhite,
            titleContentColor = TextPrimary,
            navigationIconContentColor = Primary
        )
    )
}

@Composable
fun AdminDrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val items = listOf(
        AdminDrawerItem("لوحة الإدارة الرئيسية", Icons.Default.Dashboard, Screen.AdminDashboard.route),
        AdminDrawerItem("العملاء", Icons.Default.People, Screen.AdminCustomers.route),
        AdminDrawerItem("أرقام العملاء", Icons.Default.PhoneAndroid, Screen.AdminCustomerNumbers.route),
        AdminDrawerItem("طلبات الحماية", Icons.Default.Assignment, Screen.AdminProtectionRequests.route),
        AdminDrawerItem("الحمايات النشطة والمنتهية", Icons.Default.Shield, Screen.AdminProtections.route),
        AdminDrawerItem("شركات الاتصالات", Icons.Default.CellTower, Screen.AdminTelecomProviders.route),
        AdminDrawerItem("باقات الحماية", Icons.Default.CardGiftcard, Screen.AdminProtectionPlans.route),
        AdminDrawerItem("طرق الدفع", Icons.Default.AccountBalanceWallet, Screen.AdminPaymentMethods.route),
        AdminDrawerItem("مهام الدفع", Icons.Default.Checklist, Screen.AdminPaymentTasks.route),
        AdminDrawerItem("إعدادات المهام", Icons.Default.Tune, Screen.AdminTaskSettings.route),
        AdminDrawerItem("الإشعارات", Icons.Default.Notifications, Screen.AdminNotifications.route),
        AdminDrawerItem("إعدادات النظام", Icons.Default.Settings, Screen.AdminSystemSettings.route),
        AdminDrawerItem("سجل العمليات والتدقيق", Icons.Default.History, Screen.AdminAuditLogs.route)
    )

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AMAN | أمان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "لوحة تحكم مدير النظام",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = BorderColor
            )

            // Menu Items List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val itemBg = if (isSelected) LightTeal else Color.Transparent
                    val itemContentColor = if (isSelected) Primary else TextPrimary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(itemBg)
                            .clickable {
                                onNavigate(item.route)
                                onCloseDrawer()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = itemContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = itemContentColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.badgeCount != null && item.badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Primary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${item.badgeCount}",
                                    color = SurfaceWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = BorderColor
            )

            // Logout / Return to Customer Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onLogout()
                        onCloseDrawer()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "تسجيل الخروج",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
