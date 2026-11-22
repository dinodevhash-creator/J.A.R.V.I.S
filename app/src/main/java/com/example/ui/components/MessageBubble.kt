package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.ActionType
import com.example.data.cloud.CloudMessage
import com.example.ui.theme.LocalJarvisHudColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: CloudMessage,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onExecuteAction: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false
) {
    val hudColors = LocalJarvisHudColors.current
    val isUser = message.role == "user"
    val date = message.createdAt?.toDate() ?: Date()
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = when {
                message.errorFlag -> MaterialTheme.colorScheme.errorContainer
                isUser -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                else -> MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    message.errorFlag -> hudColors.crimsonAlert.copy(alpha = 0.65f)
                    isUser -> hudColors.cyanGlow.copy(alpha = 0.45f)
                    else -> hudColors.panelBorder
                }
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (message.errorFlag) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Kesalahan",
                                tint = hudColors.crimsonAlert,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isUser) "KOMANDAN" else "J.A.R.V.I.S.",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                message.errorFlag -> hudColors.crimsonAlert
                                isUser -> hudColors.cyanGlow
                                else -> hudColors.amberAccent
                            }
                        )
                    }
                    Text(
                        text = if (isStreaming) "STREAMING…" else timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = hudColors.mutedSteel
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val actionType = message.actionType
                val actionLabel = message.actionLabel
                val actionPayload = message.actionPayload
                if (!isUser && !actionType.isNullOrBlank() && actionType != ActionType.NONE.name && !actionLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val parsedType = runCatching { ActionType.valueOf(actionType) }.getOrDefault(ActionType.NONE)
                    if (parsedType == ActionType.ADD_DIRECTIVE) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = hudColors.emeraldStatus.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, hudColors.emeraldStatus.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Protokol disimpan",
                                    tint = hudColors.emeraldStatus,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Disinkronkan ke tab Protokol",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = hudColors.emeraldStatus
                                )
                            }
                        }
                    } else {
                        val actionIcon = when (parsedType) {
                            ActionType.SET_ALARM -> Icons.Default.Alarm
                            ActionType.SET_TIMER -> Icons.Default.Timer
                            ActionType.WEB_SEARCH -> Icons.Default.Search
                            ActionType.OPEN_CALENDAR -> Icons.Default.CalendarMonth
                            ActionType.OPEN_MAPS -> Icons.Default.Map
                            else -> Icons.Default.Bolt
                        }
                        FilledTonalButton(
                            onClick = { onExecuteAction(actionType, actionPayload.orEmpty()) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = hudColors.amberAccent.copy(alpha = 0.18f),
                                contentColor = hudColors.amberAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("execute_action_button_${message.id}")
                        ) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (!isUser && !isStreaming && !message.errorFlag) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("speak_message_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.action_speak_reply),
                                tint = hudColors.cyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onCopy(message.text) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("copy_message_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy_reply),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
