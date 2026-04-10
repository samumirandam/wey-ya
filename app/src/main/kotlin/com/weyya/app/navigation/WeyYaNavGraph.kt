package com.weyya.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.weyya.app.ui.log.LogScreen
import com.weyya.app.ui.main.MainScreen
import com.weyya.app.ui.privacy.PrivacyDashboardScreen
import com.weyya.app.ui.settings.SettingsScreen

@Composable
fun WeyYaNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = "main",
    ) {
        composable("main") {
            MainScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("privacy") {
            PrivacyDashboardScreen(navController = navController)
        }
        composable("log") {
            LogScreen(navController = navController)
        }
    }
}
