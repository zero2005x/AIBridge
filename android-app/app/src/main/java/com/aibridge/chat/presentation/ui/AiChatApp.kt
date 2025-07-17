package com.aibridge.chat.presentation.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aibridge.chat.presentation.ui.chat.ChatScreen
import com.aibridge.chat.presentation.ui.login.LoginScreen
import com.aibridge.chat.presentation.ui.settings.SettingsScreen
import com.aibridge.chat.presentation.ui.portal.PortalManagementScreen
import com.aibridge.chat.presentation.viewmodel.AuthViewModel
import com.aibridge.chat.presentation.viewmodel.LoginStatus
import com.aibridge.chat.presentation.viewmodel.SettingsViewModel
import com.aibridge.chat.presentation.viewmodel.PortalManagementViewModel

@Composable
fun AiChatApp(
    windowSizeClass: WindowSizeClass,
    loginStatus: LoginStatus,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = if (loginStatus == LoginStatus.LoggedIn) "chat" else "login"
    ) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("chat") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("chat") {
            ChatScreen(
                windowSizeClass = windowSizeClass,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("chat") { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToPortalManagement = {
                    navController.navigate("portal_management")
                }
            )
        }
        
        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("portal_management") {
            val portalManagementViewModel: PortalManagementViewModel = hiltViewModel()
            PortalManagementScreen(
                viewModel = portalManagementViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
