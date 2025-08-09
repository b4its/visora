package com.xmvt.visora

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xmvt.visora.config.Routes
import android.os.Build


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi database (boleh di luar Compose)


        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current


            NavHost(
                navController = navController,
                startDestination = Routes.MainPages  // default start
            ) {
                composable(Routes.DashboardPages) {
                    Dashboard(navController)
                }
                composable(Routes.MainPages) {
                    Index(navController)
                }
            }


        }
    }
}
