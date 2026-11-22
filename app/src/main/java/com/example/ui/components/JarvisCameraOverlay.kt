package com.example.ui.components

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.LocalJarvisHudColors
import java.io.File

private const val TAG = "JarvisCameraOverlay"

@Composable
fun JarvisCameraOverlay(
    onPhotoCaptured: (Uri) -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hudColors = LocalJarvisHudColors.current

    BackHandler(onBack = onDismiss)

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isCapturing by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(lensFacing) {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preferredSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val activeSelector = when {
                    cameraProvider.hasCamera(preferredSelector) -> preferredSelector
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> {
                        onError("Kamera tidak ditemukan di perangkat ini.")
                        onDismiss()
                        return@addListener
                    }
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    activeSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "CameraX binding failed", e)
                onError("Gagal memulai pratinjau kamera: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.addListener({
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(hudColors.voidBackdrop)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_surface")
        )

        // Subtle HUD tactical reticle overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bracketLen = 28.dp.toPx()
            val stroke = 2.5.dp.toPx()
            val insetX = size.width * 0.12f
            val insetY = size.height * 0.22f
            val rightX = size.width - insetX
            val bottomY = size.height - insetY
            val reticleColor = hudColors.cyanGlow.copy(alpha = 0.72f)

            // Top-left bracket
            drawLine(reticleColor, Offset(insetX, insetY), Offset(insetX + bracketLen, insetY), stroke, StrokeCap.Round)
            drawLine(reticleColor, Offset(insetX, insetY), Offset(insetX, insetY + bracketLen), stroke, StrokeCap.Round)
            // Top-right bracket
            drawLine(reticleColor, Offset(rightX, insetY), Offset(rightX - bracketLen, insetY), stroke, StrokeCap.Round)
            drawLine(reticleColor, Offset(rightX, insetY), Offset(rightX, insetY + bracketLen), stroke, StrokeCap.Round)
            // Bottom-left bracket
            drawLine(reticleColor, Offset(insetX, bottomY), Offset(insetX + bracketLen, bottomY), stroke, StrokeCap.Round)
            drawLine(reticleColor, Offset(insetX, bottomY), Offset(insetX, bottomY - bracketLen), stroke, StrokeCap.Round)
            // Bottom-right bracket
            drawLine(reticleColor, Offset(rightX, bottomY), Offset(rightX - bracketLen, bottomY), stroke, StrokeCap.Round)
            drawLine(reticleColor, Offset(rightX, bottomY), Offset(rightX, bottomY - bracketLen), stroke, StrokeCap.Round)
        }

        // Top HUD Header
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = hudColors.voidBackdrop.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, hudColors.panelBorder),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.camera_hud_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = hudColors.cyanGlow
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_close_camera),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Bottom Capture Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = hudColors.voidBackdrop.copy(alpha = 0.8f),
                    contentColor = hudColors.cyanGlow
                ),
                modifier = Modifier
                    .size(52.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("switch_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.action_switch_camera)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            FilledIconButton(
                onClick = {
                    if (isCapturing) return@FilledIconButton
                    isCapturing = true
                    val outputFile = File(
                        context.cacheDir,
                        "jarvis_scan_${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(outputFile)
                                onPhotoCaptured(savedUri)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isCapturing = false
                                Log.e(TAG, "Photo capture failed", exception)
                                onError("Gagal mengambil foto: ${exception.localizedMessage}")
                            }
                        }
                    )
                },
                enabled = !isCapturing,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = hudColors.cyanGlow,
                    contentColor = hudColors.voidBackdrop
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("capture_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(R.string.action_capture_photo),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(80.dp))
        }
    }
}
