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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import java.util.Locale

enum class CameraType {
    FRONT, BACK, OFF
}

@Composable
fun DashboardPages(navController: NavController) {
    val context = LocalContext.current
    val recognizedText = remember { mutableStateOf("Tidak ada") }
    val statusListening = remember { mutableStateOf(false) }

    // Default kamera mati
    val cameraType = remember { mutableStateOf(CameraType.OFF) }
    val showCameraPreview = remember { mutableStateOf(false) }

    val audioPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val speechRecognizerHelper = remember {
        SpeechRecognizerHelper(
            context,
            onResult = { result -> recognizedText.value = result },
            onStartListening = { statusListening.value = true },
            onStopListening = { statusListening.value = false },
            onOpenCamera = { camType ->
                when (camType) {
                    CameraType.FRONT -> {
                        cameraType.value = CameraType.FRONT
                        showCameraPreview.value = true
                    }
                    CameraType.BACK -> {
                        cameraType.value = CameraType.BACK
                        showCameraPreview.value = true
                    }
                    CameraType.OFF -> {
                        showCameraPreview.value = false
                    }
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerHelper.destroy()
        }
    }

    LaunchedEffect(audioPermissionGranted.value) {
        if (audioPermissionGranted.value) {
            speechRecognizerHelper.startListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        Log.d("Gesture", "Double tap detected, starting listening")
                        if (!statusListening.value) {
                            speechRecognizerHelper.startListening()
                        }
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Status listening: ${if (statusListening.value) "Mendengar" else "Berhenti"}",
            color = if (statusListening.value) Color.Green else Color.Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            "Hasil pengenalan suara: ${recognizedText.value}",
            modifier = Modifier.padding(16.dp)
        )

        if (cameraPermissionGranted.value && showCameraPreview.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CameraPreview(
                    cameraSelector = when (cameraType.value) {
                        CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                        CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                        else -> CameraSelector.DEFAULT_FRONT_CAMERA // fallback
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(200.dp)
                )
            }
        } else {
            Text(
                "Kamera dalam keadaan mati atau izin kamera belum diberikan",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun CameraPreview(
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

class SpeechRecognizerHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onStartListening: () -> Unit,
    private val onStopListening: () -> Unit,
    private val onOpenCamera: (CameraType) -> Unit
) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    private val handler = android.os.Handler(context.mainLooper)
    private var isListening = false
    private var isDestroyed = false

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "Ready for speech")
                isListening = true
                onStartListening()
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "Speech ended")
                isListening = false
                onStopListening()
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error code: $error")
                isListening = false
                if (!isDestroyed) {
                    restartListeningWithDelay()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0].lowercase(Locale.getDefault())
                    Log.d("SpeechRecognizer", "Result: $text")
                    onResult(text)

                    when {
                        text == "buka kamera"
                                || text.contains("buka kamera depan")
                                || text.contains("sekarang kamera depan") -> {
                            Log.d("VoiceCommand", "Open front camera")
                            onOpenCamera(CameraType.FRONT)
                        }
                        text.contains("buka kamera belakang")
                                || text.contains("sekarang kamera belakang") -> {
                            Log.d("VoiceCommand", "Open back camera")
                            onOpenCamera(CameraType.BACK)
                        }
                        text.contains("tutup kamera")
                                || text.contains("matikan kamera") -> {
                            Log.d("VoiceCommand", "Turn off camera")
                            onOpenCamera(CameraType.OFF)
                        }
                        "mulai" in text -> {
                            if (!isListening) {
                                Log.d("SpeechRecognizer", "Command recognized: MULAI, start listening")
                                startListening()
                            }
                        }
                        "berhenti" in text || "stop" in text -> {
                            if (isListening) {
                                Log.d("SpeechRecognizer", "Command recognized: BERHENTI, stop listening")
                                stopListening()
                            }
                        }
                    }
                }
                if (!isDestroyed && isListening) {
                    restartListeningWithDelay()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (isListening || isDestroyed) return
        try {
            speechRecognizer.startListening(intent)
            isListening = true
            Log.d("SpeechRecognizer", "Started listening")
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "startListening failed: ${e.message}")
        }
    }

    private fun restartListeningWithDelay() {
        handler.postDelayed({
            if (!isDestroyed) {
                startListening()
            }
        }, 300)
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    fun destroy() {
        isDestroyed = true
        speechRecognizer.destroy()
    }
}
