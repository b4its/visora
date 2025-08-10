package com.xmvt.visora

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import java.util.Locale

@Composable
fun DashboardPages(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recognizedText = remember { mutableStateOf("Tidak ada") }

    // Permission state launcher
    val cameraPermissionGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val audioPermissionGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        cameraPermissionGranted.value = permissions[Manifest.permission.CAMERA] ?: false
        audioPermissionGranted.value = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted.value || !audioPermissionGranted.value) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    val speechRecognizerHelper = remember {
        SpeechRecognizerHelper(context) {
            recognizedText.value = it
        }
    }

    // Clean up SpeechRecognizer on disposal
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerHelper.destroy()
        }
    }

    val available = SpeechRecognizer.isRecognitionAvailable(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TopBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFADD8E6))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Visora",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,  // pusat horizontal
            verticalArrangement = Arrangement.Center             // pusat vertikal (bisa diganti kalau mau posisi vertikal lain)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (cameraPermissionGranted.value) {
                CameraPreview(modifier = Modifier.fillMaxWidth(0.8f).height(200.dp))
            } else {
                Text(
                    "Izin kamera diperlukan untuk menampilkan preview",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Wajah terdeteksi: Tidak ada",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Suara terdeteksi: ${recognizedText.value}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (audioPermissionGranted.value) {
                        speechRecognizerHelper.startListening()
                    } else {
                        // Request permission again if needed
                        requestPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECORD_AUDIO)
                        )
                    }
                }
            ) {
                Text("Mulai Dengarkan Suara")
            }
        }



        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Speech recognition tersedia: $available",
            fontSize = 14.sp,
            color = if (available) Color.Green else Color.Red
        )
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner as LifecycleOwner, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

class SpeechRecognizerHelper(
    context: Context,
    private val onResult: (String) -> Unit
) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "Speech ended")
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error code: $error")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d("SpeechRecognizer", "Result: ${matches[0]}")
                    onResult(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Optional: handle partial results if needed
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
