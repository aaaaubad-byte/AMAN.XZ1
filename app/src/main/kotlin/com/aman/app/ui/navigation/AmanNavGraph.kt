package com.aman.app.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.aman.app.data.model.AppUser
import com.aman.app.data.model.UserRole
import com.aman.app.ui.screens.admin.*
import com.aman.app.ui.screens.auth.AuthViewModel
import com.aman.app.ui.screens.auth.LoginScreen
import com.aman.app.ui.screens.auth.RegisterScreen
import com.aman.app.ui.screens.client.*
import com.aman.app.ui.screens.client.info.*
import com.aman.app.ui.screens.splash.SplashScreen
import kotlinx.coroutines.launch

@Composable
fun AmanNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<AppUser?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    val isTopLevelCustomerRoute = currentRoute in listOf(
        Screen.ClientHome.route,
        Screen.MyNumbers.route,
        Screen.Protections.route,
        Screen.ProtectionRequests.route
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isTopLevelCustomerRoute,
            drawerContent = {
                AmanCustomerDrawerContent(
                    currentRoute = currentRoute,
                    user = currentUser,
                    unreadNotificationsCount = 0,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.ClientHome.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onSignOut = {
                        scope.launch { drawerState.close() }
                        authViewModel.signOut {
                            currentUser = null
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route
            ) {
                // 1. Splash Screen & Session Restoration
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onTimeout = {
                            authViewModel.restoreSession { restoredUser ->
                                if (restoredUser != null) {
                                    currentUser = restoredUser
                                    val destination = if (restoredUser.role.isManagerOrAdmin) {
                                        Screen.AdminDashboard.route
                                    } else {
                                        Screen.ClientHome.route
                                    }
                                    navController.navigate(destination) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }

                // 2. Authentication: Login
                composable(Screen.Login.route) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            authViewModel.restoreSession { user ->
                                currentUser = user
                                val destination = if (user?.role?.isManagerOrAdmin == true) {
                                    Screen.AdminDashboard.route
                                } else {
                                    Screen.ClientHome.route
                                }
                                navController.navigate(destination) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(Screen.Register.route)
                        }
                    )
                }

                // 3. Authentication: Register
                composable(Screen.Register.route) {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = {
                            authViewModel.restoreSession { user ->
                                currentUser = user
                                navController.navigate(Screen.ClientHome.route) {
                                    popUpTo(Screen.Register.route) { inclusive = true }
                                }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }

                // 4. Customer Home Dashboard
                composable(Screen.ClientHome.route) {
                    ClientHomeScreen(
                        customerId = currentUser?.id,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        onNavigateToAddNumber = {
                            navController.navigate(Screen.AddNumber.route)
                        },
                        onNavigateToMyNumbers = {
                            navController.navigate(Screen.MyNumbers.route)
                        },
                        onNavigateToProtections = {
                            navController.navigate(Screen.Protections.route)
                        },
                        onNavigateToRequests = {
                            navController.navigate(Screen.ProtectionRequests.route)
                        },
                        onNavigateToCreateRequest = { numberId ->
                            val targetRoute = if (numberId != null) {
                                "client/create_request?numberId=$numberId"
                            } else {
                                Screen.CreateProtectionRequest.route
                            }
                            navController.navigate(targetRoute)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        }
                    )
                }

                // 5. Customer Numbers List
                composable(Screen.MyNumbers.route) {
                    ClientNumbersScreen(
                        customerId = currentUser?.id,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        onNavigateToAddNumber = {
                            navController.navigate(Screen.AddNumber.route)
                        },
                        onNavigateToCreateRequest = { numberId ->
                            navController.navigate("client/create_request?numberId=$numberId")
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        }
                    )
                }

                // 6. Add Phone Number (Auto-provider detection)
                composable(Screen.AddNumber.route) {
                    AddNumberScreen(
                        onBack = { navController.popBackStack() },
                        onNumberAddedSuccess = { navController.popBackStack() }
                    )
                }

                // 7. Customer Protections List
                composable(Screen.Protections.route) {
                    ClientProtectionsScreen(
                        customerId = currentUser?.id,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        onNavigateToCreateRequest = { numberId ->
                            val targetRoute = if (numberId != null) {
                                "client/create_request?numberId=$numberId"
                            } else {
                                Screen.CreateProtectionRequest.route
                            }
                            navController.navigate(targetRoute)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        }
                    )
                }

                // 8. Protection Requests List
                composable(Screen.ProtectionRequests.route) {
                    ClientProtectionRequestsScreen(
                        customerId = currentUser?.id,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        onNavigateToCreateRequest = {
                            navController.navigate(Screen.CreateProtectionRequest.route)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        }
                    )
                }

                // 9. Create Protection Request Form
                composable(Screen.CreateProtectionRequest.route) {
                    CreateProtectionRequestScreen(
                        customerId = currentUser?.id,
                        preselectedNumberId = null,
                        onBack = { navController.popBackStack() },
                        onRequestCreatedSuccess = {
                            navController.navigate(Screen.ProtectionRequests.route) {
                                popUpTo(Screen.ClientHome.route)
                            }
                        },
                        onNavigateToAddNumber = {
                            navController.navigate(Screen.AddNumber.route)
                        }
                    )
                }

                composable(
                    route = "client/create_request?numberId={numberId}",
                    arguments = listOf(
                        navArgument("numberId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val preselectedNumberId = backStackEntry.arguments?.getString("numberId")
                    CreateProtectionRequestScreen(
                        customerId = currentUser?.id,
                        preselectedNumberId = preselectedNumberId,
                        onBack = { navController.popBackStack() },
                        onRequestCreatedSuccess = {
                            navController.navigate(Screen.ProtectionRequests.route) {
                                popUpTo(Screen.ClientHome.route)
                            }
                        },
                        onNavigateToAddNumber = {
                            navController.navigate(Screen.AddNumber.route)
                        }
                    )
                }

                // 10. Notifications
                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        customerId = currentUser?.id,
                        onBack = { navController.popBackStack() }
                    )
                }

                // 11. Account / Settings
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        user = currentUser,
                        onBack = { navController.popBackStack() },
                        onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                        onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                        onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                        onNavigateToAbout = { navController.navigate(Screen.About.route) },
                        onSignOut = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // 12. Info Pages
                composable(Screen.Help.route) {
                    HelpScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.Terms.route) {
                    TermsScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.Privacy.route) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.About.route) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                // ==========================================
                // 13. Stage 4: Administration Layer Routes
                // ==========================================

                composable(Screen.AdminDashboard.route) {
                    AdminDashboardScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminCustomers.route) {
                    AdminCustomersScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminCustomerNumbers.route) {
                    AdminCustomerNumbersScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.AdminCustomerDetails.route,
                    arguments = listOf(
                        navArgument("customerId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                    AdminCustomerDetailsScreen(
                        customerId = customerId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AdminProtectionRequests.route) {
                    AdminProtectionRequestsScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminProtections.route) {
                    AdminProtectionsScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminTelecomProviders.route) {
                    AdminTelecomProvidersScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminProtectionPlans.route) {
                    AdminProtectionPlansScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminPaymentMethods.route) {
                    AdminPaymentMethodsScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminPaymentTasks.route) {
                    AdminPaymentTasksScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminNotifications.route) {
                    AdminNotificationsScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminTaskSettings.route) {
                    AdminTaskSettingsScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminSystemSettings.route) {
                    AdminSystemSettingsScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AdminAuditLogs.route) {
                    AdminAuditLogsScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = {
                            authViewModel.signOut {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
