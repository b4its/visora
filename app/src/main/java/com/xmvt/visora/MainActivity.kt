package com.xmvt.visora

import Index
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
fun IndexWithPermission(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        Index(navController)
    } else {
        Text(
            text = "Izin kamera diperlukan untuk menampilkan preview",
            color = Color.Red,
            modifier = Modifier.padding(16.dp)
        )
    }
}
