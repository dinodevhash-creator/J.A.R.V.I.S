package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.system.DeviceTelemetry
import com.example.ui.JarvisCoreStatus
import com.example.ui.theme.LocalJarvisHudColors

@Composable
fun TelemetryStrip(
    telemetry: DeviceTelemetry,
    coreStatus: JarvisCoreStatus,
    autoSpeakEnabled: Boolean,
    isSpeaking: Boolean,
    onToggleAutoSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hudColors = LocalJarvisHudColors.current
    val statusColor = when (coreStatus) {
        JarvisCoreStatus.STANDBY -> hudColors.cyanGlow
        JarvisCoreStatus.LISTENING -> hudColors.emeraldStatus
        JarvisCoreStatus.THINKING -> hudColors.amberAccent
        JarvisCoreStatus.SPEAKING -> hudColors.cyanGlow
        JarvisCoreStatus.ERROR -> hudColors.crimsonAlert
    }

    val statusText = when (coreStatus) {
        JarvisCoreStatus.STANDBY -> stringResource(R.string.status_standby)
        JarvisCoreStatus.LISTENING -> stringResource(R.string.status_listening)
        JarvisCoreStatus.THINKING -> stringResource(R.string.status_thinking)
        JarvisCoreStatus.SPEAKING -> stringResource(R.string.status_speaking)
        JarvisCoreStatus.ERROR -> stringResource(R.string.status_error)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, hudColors.panelBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    contentDescription = "Status baterai",
                    tint = hudColors.amberAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${telemetry.batteryPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = if (telemetry.networkStatus == "OFFLINE") Icons.Default.WifiOff else Icons.Default.Wifi,
                    contentDescription = "Status jaringan",
                    tint = if (telemetry.networkStatus == "OFFLINE") hudColors.crimsonAlert else hudColors.emeraldStatus,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = telemetry.networkStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    if (isSpeaking) onStopSpeaking() else onToggleAutoSpeak()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("tts_toggle_button")
            ) {
                Icon(
                    imageVector = if (autoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (isSpeaking) {
                        stringResource(R.string.action_stop_speaking)
                    } else if (autoSpeakEnabled) {
                        "Matikan suara otomatis"
                    } else {
                        "Aktifkan suara otomatis"
                    },
                    tint = if (autoSpeakEnabled) hudColors.cyanGlow else hudColors.mutedSteel
                )
            }
        }
    }
}
