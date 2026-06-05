package com.weyya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.weyya.app.navigation.WeyYaNavGraph
import com.weyya.app.ui.theme.WeyYaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge without androidx.activity's enableEdgeToEdge(), whose internal
        // Window.setStatusBarColor / setNavigationBarColor / SHORT_EDGES calls are deprecated on
        // Android 15 (SDK 35) and flagged by Play Console. Icon contrast is handled in WeyYaTheme.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WeyYaTheme {
                WeyYaNavGraph()
            }
        }
    }
}
