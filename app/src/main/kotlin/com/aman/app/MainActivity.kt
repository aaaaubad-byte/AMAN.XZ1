package com.aman.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aman.app.ui.navigation.AmanNavGraph
import com.aman.app.ui.theme.AmanTheme
import com.aman.app.ui.theme.BackgroundLight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmanTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = BackgroundLight
                    ) {
                        val navController = rememberNavController()
                        AmanNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
