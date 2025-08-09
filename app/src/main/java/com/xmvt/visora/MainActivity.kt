package com.xmvt.visora

import Index
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xmvt.visora.config.Routes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            NavGraph(navController)
        }
    }
}

@Composable
fun NavGraph(navController: androidx.navigation.NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MainPages
    ) {
        composable(Routes.DashboardPages) {
            DashboardPages(navController)
        }
        composable(Routes.MainPages) {
            IndexWithPermission(navController)
        }
    }
}

// Contoh pembungkus Index dengan permission request di dalam composable
@Composable
fun IndexWithPermission(navController: androidx.navigation.NavHostController) {
    // Request permission saat composable muncul
    LaunchedEffect(Unit) {
        requestPermissions()
    }

    Index(navController)
}

// Fungsi requestPermissions harus bisa dipanggil di sini (atau dari Index composable)
// Jika kamu punya requestPermissions di Index, kamu bisa pindahkan LaunchedEffect ke sana
fun requestPermissions() {
    // Implementasi permintaan permission runtime kamu di sini
    // Misalnya menggunakan rememberLauncherForActivityResult yang dipanggil di composable Index
    // Kalau pake Compose, lebih baik permission request di-handle langsung di Index composable
}
