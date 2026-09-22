package com.aman.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.data.model.AppUser
import com.aman.app.ui.theme.*

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun AmanCustomerDrawerContent(
    currentRoute: String,
    user: AppUser?,
    unreadNotificationsCount: Int = 0,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuItems = listOf(
        DrawerMenuItem(Screen.ClientHome.route, "الرئيسية", Icons.Default.Home),
        DrawerMenuItem(Screen.MyNumbers.route, "أرقامي", Icons.Default.Phone),
        DrawerMenuItem(Screen.Protections.route, "حماياتي", Icons.Default.Security),
        DrawerMenuItem(Screen.ProtectionRequests.route, "طلبات الحماية", Icons.Default.ReceiptLong),
        DrawerMenuItem(Screen.Notifications.route, "الإشعارات", Icons.Default.Notifications, unreadNotificationsCount),
        DrawerMenuItem(Screen.Settings.route, "الإعدادات", Icons.Default.Settings),
        DrawerMenuItem(Screen.Help.route, "المساعدة والدعم", Icons.Default.HelpOutline),
        DrawerMenuItem(Screen.Terms.route, "الشروط والأحكام", Icons.Default.Description),
        DrawerMenuItem(Screen.Privacy.route, "سياسة الخصوصية", Icons.Default.Policy),
        DrawerMenuItem(Screen.About.route, "عن أمان", Icons.Default.Info)
    )

    ModalDrawerSheet(
        modifier = modifier.fillMaxWidth(0.80f),
        drawerContainerColor = SurfaceWhite,
        drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Header: User Profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = (user?.name?.firstOrNull() ?: 'أ').toString()
                        Text(
                            text = initial,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = user?.name ?: "عميل أمان",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = user?.email ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (user?.role?.isManagerOrAdmin == true) "مدير النظام" else "حساب عميل",
                                color = Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = BorderColor
            )

            // Menu Items List (Scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                if (user?.role?.isManagerOrAdmin == true) {
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.AdminPanelSettings,
                                contentDescription = "لوحة الإدارة",
                                tint = Primary
                            )
                        },
                        label = {
                            Text(
                                text = "لوحة الإدارة والتحكم",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            )
                        },
                        selected = false,
                        onClick = { onNavigate(Screen.AdminDashboard.route) },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Primary.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = BorderColor
                    )
                }

                menuItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) Primary else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Primary else TextPrimary
                                )
                            )
                        },
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ) {
                                    Text(item.badgeCount.toString())
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = { onNavigate(item.route) },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Primary.copy(alpha = 0.12f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = BorderColor
            )

            // Logout Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSignOut)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "تسجيل الخروج",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "تسجيل الخروج",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "AMAN | أمان v1.0.0",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextDisabled,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
