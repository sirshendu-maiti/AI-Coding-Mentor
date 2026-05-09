package com.euphoria.aimentor.ui.components

import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCapture(
    onImageCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // FIX: Properly shut down the executor when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(flashEnabled) {
        imageCapture?.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraCapture", "Camera init failed: ${e.message}")
                    cameraError = "Camera unavailable: ${e.message}"
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Error overlay
        cameraError?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷 $error", color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Go Back", color = Color.White)
                    }
                }
            }
        }

        // Scanning Animation Overlay
        ScanningOverlay()

        // Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) Color.Yellow else Color.White
                    )
                }
            }

            // Capture Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(4.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        val photoFile = File(context.cacheDir, "ocr_capture_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        imageCapture?.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val savedUri = Uri.fromFile(photoFile)
                                    onImageCaptured(savedUri)
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    Log.e("CameraCapture", "Capture failed: ${exc.message}")
                                }
                            }
                        )
                    }
            )
        }
    }
}

@Composable
fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "linePosition"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Draw the scanning line
        val lineY = height * yOffset
        
        clipRect(
            left = width * 0.1f,
            top = height * 0.2f,
            right = width * 0.9f,
            bottom = height * 0.8f
        ) {
            drawLine(
                color = Color(0xFF6C63FF).copy(alpha = 0.8f),
                start = Offset(width * 0.1f, lineY),
                end = Offset(width * 0.9f, lineY),
                strokeWidth = 4.dp.toPx()
            )
            
            // Subtle glow around the line
            drawRect(
                color = Color(0xFF6C63FF).copy(alpha = 0.1f),
                topLeft = Offset(width * 0.1f, lineY - 20.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(width * 0.8f, 40.dp.toPx())
            )
        }
        
        // Draw corner markers
        val cornerSize = 40.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val padding = width * 0.1f
        val topPadding = height * 0.2f
        val bottomPadding = height * 0.8f
        val rightPadding = width * 0.9f
        
        val cornerColor = Color.White.copy(alpha = 0.6f)
        
        // Top Left
        drawLine(cornerColor, Offset(padding, topPadding), Offset(padding + cornerSize, topPadding), strokeWidth)
        drawLine(cornerColor, Offset(padding, topPadding), Offset(padding, topPadding + cornerSize), strokeWidth)
        
        // Top Right
        drawLine(cornerColor, Offset(rightPadding, topPadding), Offset(rightPadding - cornerSize, topPadding), strokeWidth)
        drawLine(cornerColor, Offset(rightPadding, topPadding), Offset(rightPadding, topPadding + cornerSize), strokeWidth)
        
        // Bottom Left
        drawLine(cornerColor, Offset(padding, bottomPadding), Offset(padding + cornerSize, bottomPadding), strokeWidth)
        drawLine(cornerColor, Offset(padding, bottomPadding), Offset(padding, bottomPadding - cornerSize), strokeWidth)
        
        // Bottom Right
        drawLine(cornerColor, Offset(rightPadding, bottomPadding), Offset(rightPadding - cornerSize, bottomPadding), strokeWidth)
        drawLine(cornerColor, Offset(rightPadding, bottomPadding), Offset(rightPadding, bottomPadding - cornerSize), strokeWidth)
    }
}
