package com.aman.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aman.app.ui.screens.admin.AdminHomeScreen
import com.aman.app.ui.screens.auth.LoginScreen
import com.aman.app.ui.screens.auth.RegisterScreen
import com.aman.app.ui.screens.client.ClientHomeScreen
import com.aman.app.ui.screens.splash.SplashScreen

@Composable
fun AmanNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ClientHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.ClientHome.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ClientHome.route) {
            ClientHomeScreen(
                onNavigateToAddNumber = {},
                onNavigateToRequests = {},
                onSwitchToAdmin = {
                    navController.navigate(Screen.AdminDashboard.route)
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminHomeScreen(
                onBackToClient = {
                    navController.popBackStack()
                }
            )
        }
    }
}
