package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.StudioAspectRatio
import com.example.ui.ImageStudioState
import com.example.ui.JarvisCoreStatus
import com.example.ui.JarvisUiState
import com.example.ui.components.JarvisCameraOverlay
import com.example.ui.components.JarvisCoreOrb
import com.example.ui.theme.LocalJarvisHudColors

@Composable
fun ImageStudioScreen(
    uiState: JarvisUiState,
    onSetReferenceUri: (Uri?) -> Unit,
    onGenerateOrEdit: (String, StudioAspectRatio) -> Unit,
    onSaveToGallery: () -> Unit,
    onShowNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hudColors = LocalJarvisHudColors.current
    var promptText by rememberSaveable { mutableStateOf("") }
    var selectedAspect by rememberSaveable { mutableStateOf(StudioAspectRatio.SQUARE) }
    var showCamera by rememberSaveable { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> onSetReferenceUri(uri) }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) showCamera = true
            else onShowNotice(context.getString(R.string.camera_permission_rationale))
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(hudColors.voidBackdrop),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            item(key = "studio_header") {
                Column {
                    Text(
                        text = stringResource(R.string.studio_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = hudColors.cyanGlow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.studio_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "studio_controls") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.studio_prompt_hint),
                                    color = hudColors.mutedSteel
                                )
                            },
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = hudColors.cyanGlow,
                                unfocusedBorderColor = hudColors.panelBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Rasio Aspek",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioAspectRatio.entries.forEach { ratio ->
                                FilterChip(
                                    selected = selectedAspect == ratio,
                                    onClick = { selectedAspect = ratio },
                                    label = { Text(ratio.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = hudColors.cyanGlow.copy(alpha = 0.2f),
                                        selectedLabelColor = hudColors.cyanGlow
                                    ),
                                    modifier = Modifier.testTag("aspect_chip_${ratio.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                border = BorderStroke(1.dp, hudColors.panelBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("studio_pick_reference")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih Foto")
                            }

                            OutlinedButton(
                                onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) showCamera = true
                                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                border = BorderStroke(1.dp, hudColors.panelBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("studio_camera_reference")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kamera")
                            }
                        }

                        uiState.studioReferenceUri?.let { refUri ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, hudColors.amberAccent.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = refUri,
                                        contentDescription = "Gambar referensi suntingan",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Mode Sunting Gambar Aktif",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = hudColors.amberAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onSetReferenceUri(null) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus gambar referensi",
                                            tint = hudColors.crimsonAlert
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val isLoading = uiState.studioState is ImageStudioState.Loading
                        Button(
                            onClick = { onGenerateOrEdit(promptText, selectedAspect) },
                            enabled = promptText.isNotBlank() && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = hudColors.cyanGlow,
                                contentColor = hudColors.voidBackdrop
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("studio_generate_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.studioReferenceUri != null) {
                                    stringResource(R.string.studio_edit_button)
                                } else {
                                    stringResource(R.string.studio_generate_button)
                                }
                            )
                        }
                    }
                }
            }

            item(key = "studio_output") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = uiState.studioState) {
                            is ImageStudioState.Idle -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 28.dp)
                                ) {
                                    JarvisCoreOrb(
                                        status = JarvisCoreStatus.STANDBY,
                                        voiceAmplitude = 0f,
                                        onClick = {},
                                        size = 110.dp,
                                        compact = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Masukkan deskripsi visual atau lampirkan foto untuk disunting oleh Gemini Nano Banana 2.1.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            is ImageStudioState.Loading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 32.dp)
                                ) {
                                    CircularProgressIndicator(color = hudColors.cyanGlow)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.studio_generating),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = hudColors.amberAccent,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            is ImageStudioState.Error -> {
                                Text(
                                    text = "Galat Studio Visual: ${state.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = hudColors.crimsonAlert,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                            is ImageStudioState.Success -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        bitmap = state.bitmap.asImageBitmap(),
                                        contentDescription = "Hasil gambar AI: ${state.prompt}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onSaveToGallery,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = hudColors.emeraldStatus,
                                            contentColor = hudColors.voidBackdrop
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("studio_save_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.studio_save_gallery))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCamera) {
            JarvisCameraOverlay(
                onPhotoCaptured = { uri ->
                    onSetReferenceUri(uri)
                    showCamera = false
                },
                onError = { msg -> onShowNotice(msg) },
                onDismiss = { showCamera = false }
            )
        }
    }
}
