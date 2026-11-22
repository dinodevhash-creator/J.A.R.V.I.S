package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.cloud.CloudMessage
import com.example.ui.JarvisCoreStatus
import com.example.ui.JarvisUiState
import com.example.ui.components.JarvisCameraOverlay
import com.example.ui.components.JarvisCoreOrb
import com.example.ui.components.MessageBubble
import com.example.ui.components.TelemetryStrip
import com.example.ui.theme.LocalJarvisHudColors

@Composable
fun CoreHudScreen(
    uiState: JarvisUiState,
    onSendMessage: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onToggleHandsFree: (Boolean) -> Unit,
    onAttachImage: (Uri?) -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onExecuteAction: (String, String) -> Unit,
    onShowToastMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hudColors = LocalJarvisHudColors.current
    var inputText by rememberSaveable { mutableStateOf("") }
    var showCameraOverlay by rememberSaveable { mutableStateOf(false) }
    var pendingHandsFreeActivation by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> onAttachImage(uri) }
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                if (pendingHandsFreeActivation) {
                    pendingHandsFreeActivation = false
                    onToggleHandsFree(true)
                } else {
                    onStartVoice()
                }
            } else {
                pendingHandsFreeActivation = false
                onShowToastMessage(context.getString(R.string.mic_permission_rationale))
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                showCameraOverlay = true
            } else {
                onShowToastMessage(context.getString(R.string.camera_permission_rationale))
            }
        }
    )

    val handleOpenCamera = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showCameraOverlay = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val handleVoiceClick = {
        if (uiState.isListening) {
            onStopVoice()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                onStartVoice()
            } else {
                pendingHandsFreeActivation = false
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val handleHandsFreeClick = {
        if (uiState.isHandsFreeMode) {
            onToggleHandsFree(false)
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                onToggleHandsFree(true)
            } else {
                pendingHandsFreeActivation = true
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val totalItems = uiState.messages.size + if (uiState.isGenerating) 1 else 0
    LaunchedEffect(totalItems, uiState.streamingText.length) {
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        hudColors.voidBackdrop,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        hudColors.voidBackdrop
                    )
                )
            )
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TelemetryStrip(
                telemetry = uiState.telemetry,
                coreStatus = uiState.coreStatus,
                autoSpeakEnabled = uiState.settings.autoSpeakEnabled,
                isSpeaking = uiState.isSpeaking,
                onToggleAutoSpeak = onToggleAutoSpeak,
                onStopSpeaking = onStopSpeaking
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Hands-Free Voice Loop Control Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.isHandsFreeMode,
                    onClick = handleHandsFreeClick,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (uiState.isHandsFreeMode) {
                                stringResource(R.string.hands_free_active)
                            } else {
                                stringResource(R.string.hands_free_mode)
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = hudColors.emeraldStatus.copy(alpha = 0.2f),
                        selectedLabelColor = hudColors.emeraldStatus,
                        selectedLeadingIconColor = hudColors.emeraldStatus
                    ),
                    modifier = Modifier.testTag("hands_free_button")
                )

                if (uiState.locationInfo != null) {
                    Text(
                        text = uiState.locationInfo.city,
                        style = MaterialTheme.typography.labelSmall,
                        color = hudColors.cyanGlow
                    )
                }
            }

            // Active Hands-Free Banner
            AnimatedVisibility(
                visible = uiState.isHandsFreeMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = hudColors.emeraldStatus.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, hudColors.emeraldStatus.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = hudColors.emeraldStatus,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.hands_free_active),
                            style = MaterialTheme.typography.labelMedium,
                            color = hudColors.emeraldStatus,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onToggleHandsFree(false) },
                            modifier = Modifier.testTag("hands_free_stop_button")
                        ) {
                            Text(
                                text = "Hentikan",
                                color = hudColors.crimsonAlert
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                // Hero Empty State with Full Arc Reactor Core
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    JarvisCoreOrb(
                        status = uiState.coreStatus,
                        voiceAmplitude = uiState.voiceAmplitude,
                        onClick = handleVoiceClick,
                        size = 190.dp,
                        compact = false
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.jarvis_designation),
                        style = MaterialTheme.typography.headlineSmall,
                        color = hudColors.cyanGlow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isListening) {
                            uiState.partialTranscript.ifBlank { "Mendengarkan perintah suara Anda…" }
                        } else {
                            stringResource(R.string.jarvis_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val quickPrompts = listOf(
                        stringResource(R.string.quick_cmd_briefing),
                        stringResource(R.string.quick_cmd_alarm),
                        stringResource(R.string.quick_cmd_protocol),
                        stringResource(R.string.quick_cmd_nearby)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickPrompts.forEachIndexed { index, prompt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, hudColors.panelBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSendMessage(prompt) }
                                    .testTag("quick_prompt_$index")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = hudColors.amberAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Active HUD Compact Core Header + Chat Feed
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JarvisCoreOrb(
                            status = uiState.coreStatus,
                            voiceAmplitude = uiState.voiceAmplitude,
                            onClick = handleVoiceClick,
                            size = 52.dp,
                            compact = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.jarvis_designation),
                                style = MaterialTheme.typography.titleMedium,
                                color = hudColors.cyanGlow
                            )
                            Text(
                                text = when {
                                    uiState.isListening -> uiState.partialTranscript.ifBlank { "Mendengarkan suara Anda…" }
                                    uiState.isGenerating -> "Menyusun respons taktis…"
                                    uiState.isSpeaking -> "Membacakan respons…"
                                    else -> "Ketuk inti untuk perintah suara"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            onSpeak = onSpeakText,
                            onCopy = { text ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("J.A.R.V.I.S.", text))
                                onShowToastMessage(context.getString(R.string.copied_to_clipboard))
                            },
                            onExecuteAction = onExecuteAction
                        )
                    }

                    if (uiState.isGenerating) {
                        item(key = "streaming_bubble") {
                            val streamingMessage = CloudMessage(
                                id = "streaming_temp",
                                role = "model",
                                text = uiState.streamingText.ifBlank { "Menganalisis perintah Anda…" }
                            )
                            MessageBubble(
                                message = streamingMessage,
                                onSpeak = {},
                                onCopy = {},
                                onExecuteAction = { _, _ -> },
                                isStreaming = true
                            )
                        }
                    }
                }
            }

            // Live Listening Indicator Banner
            AnimatedVisibility(
                visible = uiState.isListening,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = hudColors.emeraldStatus.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, hudColors.emeraldStatus.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Gelombang suara aktif",
                            tint = hudColors.emeraldStatus
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.partialTranscript.ifBlank { "Silakan berbicara sekarang…" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Attached Image Thumbnail Preview
            AnimatedVisibility(visible = uiState.attachedImageUri != null) {
                uiState.attachedImageUri?.let { uri ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, hudColors.cyanGlow.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Pratinjau lampiran visual",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Lampiran siap untuk pemindaian visual J.A.R.V.I.S.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onAttachImage(null) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_clear_image),
                                    tint = hudColors.crimsonAlert
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Command Dock
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (uiState.coreStatus == JarvisCoreStatus.LISTENING) {
                        hudColors.emeraldStatus
                    } else {
                        hudColors.panelBorder
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("attach_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.action_attach_image),
                            tint = hudColors.cyanGlow
                        )
                    }

                    IconButton(
                        onClick = handleOpenCamera,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.action_open_camera),
                            tint = hudColors.amberAccent
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.input_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = hudColors.mutedSteel
                            )
                        },
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() || uiState.attachedImageUri != null) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = hudColors.cyanGlow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input_field")
                    )

                    FilledIconButton(
                        onClick = handleVoiceClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (uiState.isListening) {
                                hudColors.emeraldStatus
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (uiState.isListening) {
                                hudColors.voidBackdrop
                            } else {
                                hudColors.cyanGlow
                            }
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mic_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (uiState.isListening) {
                                stringResource(R.string.action_stop_mic)
                            } else {
                                stringResource(R.string.action_mic)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = (inputText.isNotBlank() || uiState.attachedImageUri != null) && !uiState.isGenerating
                    FilledIconButton(
                        onClick = {
                            if (canSend) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = canSend,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = hudColors.cyanGlow,
                            contentColor = hudColors.voidBackdrop,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = hudColors.mutedSteel
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.action_send)
                        )
                    }
                }
            }
        }

        if (showCameraOverlay) {
            JarvisCameraOverlay(
                onPhotoCaptured = { uri ->
                    onAttachImage(uri)
                    showCameraOverlay = false
                },
                onError = { message ->
                    onShowToastMessage(message)
                },
                onDismiss = { showCameraOverlay = false }
            )
        }
    }
}
