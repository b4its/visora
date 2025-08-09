import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.xmvt.visora.config.Routes

@Composable
fun Index(navController: NavController) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    fun requestPermissions() {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Minta izin saat composable pertama kali muncul
    LaunchedEffect(Unit) {
        requestPermissions()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Visora",
                fontSize = 20.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                onClick = {
                    if (hasCameraPermission && hasAudioPermission) {
                        startLiveKitSession()
                        navController.navigate(Routes.DashboardPages)
                    } else {
                        requestPermissions()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.hsl(0f, 1f, 0.2f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = hasCameraPermission && hasAudioPermission
            ) {
                Text(text = "Nyalakan Mic & Kamera", fontSize = 20.sp)
            }

            if (!hasCameraPermission || !hasAudioPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "App membutuhkan akses kamera dan mikrofon",
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun startLiveKitSession() {
    println("Mulai sesi LiveKit: streaming video & audio")
}
