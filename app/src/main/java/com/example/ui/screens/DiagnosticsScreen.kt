package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.preferences.JarvisPersona
import com.example.ui.JarvisUiState
import com.example.ui.theme.LocalJarvisHudColors
import kotlin.math.roundToInt

@Composable
fun DiagnosticsScreen(
    uiState: JarvisUiState,
    onSetAutoSpeak: (Boolean) -> Unit,
    onSetSpeechRate: (Float) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetPersona: (JarvisPersona) -> Unit,
    onSetDailyBriefingSchedule: (Boolean, Int, Int) -> Unit,
    onSendTestBriefingNotification: () -> Unit,
    onClearConversation: () -> Unit,
    onRefreshTelemetry: () -> Unit,
    onSignOut: () -> Unit,
    onShowNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hudColors = LocalJarvisHudColors.current
    var showClearDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                onSetDailyBriefingSchedule(true, uiState.settings.briefingHour, uiState.settings.briefingMinute)
            } else {
                onShowNotice(context.getString(R.string.notification_permission_rationale))
            }
        }
    )

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Bersihkan Log Percakapan Cloud?") },
            text = { Text("Semua riwayat obrolan tersinkron dengan J.A.R.V.I.S. untuk akun Anda akan dihapus permanen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearConversation()
                        showClearDialog = false
                    }
                ) {
                    Text("Hapus", color = hudColors.crimsonAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

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
            item(key = "diag_header") {
                Text(
                    text = stringResource(R.string.diagnostics_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = hudColors.cyanGlow
                )
            }

            // Google Account & Cloud Sync Status
            item(key = "google_account_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profil Komandan",
                            tint = hudColors.cyanGlow,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.userDisplayName.ifBlank { "Komandan J.A.R.V.I.S." },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.userEmail.ifBlank { "Sinkronisasi Cloud Aktif" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = hudColors.amberAccent),
                            border = BorderStroke(1.dp, hudColors.amberAccent.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.auth_sign_out))
                        }
                    }
                }
            }

            // Scheduled Daily Briefing Settings
            item(key = "briefing_schedule_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = hudColors.amberAccent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.briefing_schedule_toggle),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Notifikasi harian otomatis pada jam terpilih",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.dailyBriefingEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        onSetDailyBriefingSchedule(
                                            false,
                                            uiState.settings.briefingHour,
                                            uiState.settings.briefingMinute
                                        )
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onSetDailyBriefingSchedule(
                                            true,
                                            uiState.settings.briefingHour,
                                            uiState.settings.briefingMinute
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = hudColors.voidBackdrop,
                                    checkedTrackColor = hudColors.emeraldStatus
                                ),
                                modifier = Modifier.testTag("diag_briefing_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(6, 7, 8, 20).forEach { hour ->
                                FilterChip(
                                    selected = uiState.settings.briefingHour == hour,
                                    onClick = {
                                        onSetDailyBriefingSchedule(
                                            uiState.settings.dailyBriefingEnabled,
                                            hour,
                                            0
                                        )
                                    },
                                    label = { Text("%02d:00".format(hour)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = hudColors.cyanGlow.copy(alpha = 0.2f),
                                        selectedLabelColor = hudColors.cyanGlow
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onSendTestBriefingNotification()
                                    }
                                },
                                modifier = Modifier.testTag("test_briefing_notif_button")
                            ) {
                                Text("Uji Notifikasi", color = hudColors.cyanGlow)
                            }
                        }
                    }
                }
            }

            // Voice Output Settings
            item(key = "voice_settings") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = hudColors.cyanGlow
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.setting_auto_speak),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.setting_auto_speak_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.autoSpeakEnabled,
                                onCheckedChange = onSetAutoSpeak,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = hudColors.voidBackdrop,
                                    checkedTrackColor = hudColors.cyanGlow
                                ),
                                modifier = Modifier.testTag("auto_speak_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val ratePercent = (uiState.settings.speechRate * 100).roundToInt() / 100f
                        Text(
                            text = "${stringResource(R.string.setting_speech_rate)}: ${ratePercent}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = uiState.settings.speechRate,
                            onValueChange = onSetSpeechRate,
                            valueRange = 0.7f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = hudColors.cyanGlow,
                                activeTrackColor = hudColors.cyanGlow
                            ),
                            modifier = Modifier.testTag("speech_rate_slider")
                        )
                    }
                }
            }

            // Language & Persona Settings
            item(key = "language_persona") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = hudColors.amberAccent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.setting_language),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("id-ID" to "Bahasa Indonesia", "en-US" to "English (US)").forEach { (code, label) ->
                                FilterChip(
                                    selected = uiState.settings.languageCode == code,
                                    onClick = { onSetLanguage(code) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = hudColors.cyanGlow.copy(alpha = 0.2f),
                                        selectedLabelColor = hudColors.cyanGlow
                                    ),
                                    modifier = Modifier.testTag("lang_chip_$code")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = hudColors.cyanGlow
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.setting_persona),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        JarvisPersona.entries.forEach { persona ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSetPersona(persona) }
                                    .minimumInteractiveComponentSize()
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                                    .testTag("persona_option_${persona.name}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.settings.persona == persona,
                                    onClick = { onSetPersona(persona) },
                                    colors = RadioButtonDefaults.colors(selectedColor = hudColors.cyanGlow)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = persona.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = persona.systemTag,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hardware & AI Engine Diagnostics
            item(key = "system_diagnostics") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = hudColors.emeraldStatus
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Telemetri Inti Sistem",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = onRefreshTelemetry) {
                                Text("Segarkan", color = hudColors.cyanGlow)
                            }
                        }

                        DiagnosticRow("Model Utama AI", "Gemini 3.8 Flash (Firebase AI Logic)")
                        DiagnosticRow("Klasifikasi Aksi", "Gemini 3.5 Flash-Lite (JSON Schema)")
                        DiagnosticRow("Studio Visual AI", "Gemini Nano Banana 2.1")
                        DiagnosticRow("Sinkronisasi Cloud", "Cloud Firestore + Google Auth")
                        DiagnosticRow("Sintesis Suara (TTS)", if (uiState.ttsReady) "AKTIF" else "MENYIAPKAN")
                        DiagnosticRow("Perangkat Keras", uiState.telemetry.deviceLabel)
                        DiagnosticRow("Daya Baterai", "${uiState.telemetry.batteryPercent}% (${if (uiState.telemetry.isCharging) "Mengisi" else "Baterai"})")
                        DiagnosticRow("Jaringan", uiState.telemetry.networkStatus)
                    }
                }
            }

            item(key = "clear_chat_button") {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = hudColors.crimsonAlert),
                    border = BorderStroke(1.dp, hudColors.crimsonAlert.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.setting_clear_chat))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
