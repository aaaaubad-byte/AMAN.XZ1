package com.aman.app.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val customerBottomNavItems = listOf(
    BottomNavItem(Screen.ClientHome.route, "الرئيسية", Icons.Default.Home),
    BottomNavItem(Screen.MyNumbers.route, "أرقامي", Icons.Default.Phone),
    BottomNavItem(Screen.Protections.route, "الحمايات", Icons.Default.Security),
    BottomNavItem(Screen.ProtectionRequests.route, "الطلبات", Icons.Default.ReceiptLong),
    BottomNavItem(Screen.Settings.route, "الإعدادات", Icons.Default.Settings)
)

@Composable
fun AmanCustomerBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.height(68.dp),
        containerColor = SurfaceWhite,
        tonalElevation = 8.dp
    ) {
        customerBottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) Primary else TextSecondary
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Primary else TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = LightTeal,
                    selectedIconColor = Primary,
                    unselectedIconColor = TextSecondary,
                    selectedTextColor = Primary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
