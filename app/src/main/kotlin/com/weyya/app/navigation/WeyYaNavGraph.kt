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

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val LOG = "log"
}

@Composable
fun WeyYaNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
    ) {
        composable(Routes.MAIN) {
            MainScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.PRIVACY) {
            PrivacyDashboardScreen(navController = navController)
        }
        composable(Routes.LOG) {
            LogScreen(navController = navController)
        }
    }
}
