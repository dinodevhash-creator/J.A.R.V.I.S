package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.cloud.CloudDirective
import com.example.ui.JarvisUiState
import com.example.ui.theme.LocalJarvisHudColors

@Composable
fun ProtocolsScreen(
    uiState: JarvisUiState,
    onAddDirective: (String, String) -> Unit,
    onToggleDirective: (CloudDirective) -> Unit,
    onDeleteDirective: (String) -> Unit,
    onQuickAlarm: () -> Unit,
    onQuickTimer: () -> Unit,
    onQuickCalendar: () -> Unit,
    onQuickSearch: () -> Unit,
    onRefreshLocation: () -> Unit,
    onOpenMaps: (String) -> Unit,
    onGenerateBriefing: () -> Unit,
    onSpeakBriefing: (String) -> Unit,
    onToggleBriefingSchedule: (Boolean, Int, Int) -> Unit,
    onShowNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hudColors = LocalJarvisHudColors.current
    var directiveInput by rememberSaveable { mutableStateOf("") }
    var selectedPriority by rememberSaveable { mutableStateOf("NORMAL") }
    var mapsSearchInput by rememberSaveable { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                onRefreshLocation()
            } else {
                onShowNotice(context.getString(R.string.location_permission_rationale))
            }
        }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                onToggleBriefingSchedule(true, uiState.settings.briefingHour, uiState.settings.briefingMinute)
            } else {
                onShowNotice(context.getString(R.string.notification_permission_rationale))
            }
        }
    )

    val activeCount = uiState.directives.count { !it.completed }

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
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            item(key = "hero_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_jarvis_hud_banner),
                        contentDescription = "Latar HUD Taktis J.A.R.V.I.S.",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        hudColors.voidBackdrop.copy(alpha = 0.88f),
                                        hudColors.voidBackdrop.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.protocols_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = hudColors.cyanGlow
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$activeCount direktif aktif • ${uiState.telemetry.deviceLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Smart Location & Maps Card
            item(key = "location_maps_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = hudColors.cyanGlow
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.location_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (fine || coarse) {
                                        onRefreshLocation()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                enabled = !uiState.isFetchingLocation,
                                border = BorderStroke(1.dp, hudColors.cyanGlow.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detect_location_button")
                            ) {
                                if (uiState.isFetchingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = hudColors.cyanGlow
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.location_detect_button),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = hudColors.cyanGlow
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val locText = uiState.locationInfo?.let { loc ->
                            "${loc.addressLabel} (${String.format("%.4f, %.4f", loc.latitude, loc.longitude)})"
                        } ?: "Lokasi belum dipindai. Ketuk 'Deteksi Lokasi' untuk konteks GPS akurat."
                        Text(
                            text = locText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = mapsSearchInput,
                                onValueChange = { mapsSearchInput = it },
                                placeholder = {
                                    Text(
                                        text = "Cari tempat (mis. SPBU, kopi, rumah sakit)…",
                                        color = hudColors.mutedSteel,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = hudColors.cyanGlow,
                                    unfocusedBorderColor = hudColors.panelBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("maps_search_input")
                            )
                            Button(
                                onClick = {
                                    val query = mapsSearchInput.trim().ifBlank { "tempat menarik terdekat" }
                                    onOpenMaps(query)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = hudColors.amberAccent,
                                    contentColor = hudColors.voidBackdrop
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_maps_button")
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.location_open_maps))
                            }
                        }
                    }
                }
            }

            // Daily Briefing & Scheduled Reminder Card
            item(key = "daily_briefing_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = hudColors.amberAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.briefing_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.settings.dailyBriefingEnabled) {
                                            "Jadwal aktif pukul %02d:%02d".format(
                                                uiState.settings.briefingHour,
                                                uiState.settings.briefingMinute
                                            )
                                        } else {
                                            "Pengingat terjadwal nonaktif"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = uiState.settings.dailyBriefingEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        onToggleBriefingSchedule(
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
                                        onToggleBriefingSchedule(
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
                                modifier = Modifier.testTag("briefing_schedule_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onGenerateBriefing,
                            enabled = !uiState.isGeneratingBriefing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = hudColors.cyanGlow,
                                contentColor = hudColors.voidBackdrop
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("generate_briefing_button")
                        ) {
                            if (uiState.isGeneratingBriefing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = hudColors.voidBackdrop
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.briefing_generate_now))
                        }

                        uiState.dailyBriefingText?.let { briefing ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, hudColors.cyanGlow.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = briefing,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { onSpeakBriefing(briefing) }) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = hudColors.cyanGlow,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Bacakan", color = hudColors.cyanGlow)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "system_tools") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.system_tools_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = hudColors.amberAccent
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            QuickSystemToolCard(
                                icon = Icons.Default.Alarm,
                                title = "Alarm 06:00",
                                subtitle = "Bangun pagi",
                                accent = hudColors.cyanGlow,
                                onClick = onQuickAlarm,
                                testTag = "quick_alarm_card"
                            )
                        }
                        item {
                            QuickSystemToolCard(
                                icon = Icons.Default.Timer,
                                title = "Timer 5m",
                                subtitle = "Fokus cepat",
                                accent = hudColors.amberAccent,
                                onClick = onQuickTimer,
                                testTag = "quick_timer_card"
                            )
                        }
                        item {
                            QuickSystemToolCard(
                                icon = Icons.Default.CalendarMonth,
                                title = "Agenda Baru",
                                subtitle = "Kalender sistem",
                                accent = hudColors.emeraldStatus,
                                onClick = onQuickCalendar,
                                testTag = "quick_calendar_card"
                            )
                        }
                        item {
                            QuickSystemToolCard(
                                icon = Icons.Default.Search,
                                title = "Riset Web",
                                subtitle = "Buka pencarian",
                                accent = hudColors.cyanGlow,
                                onClick = onQuickSearch,
                                testTag = "quick_search_card"
                            )
                        }
                    }
                }
            }

            item(key = "add_directive_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, hudColors.panelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.protocols_add_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = directiveInput,
                            onValueChange = { directiveInput = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.protocols_input_hint),
                                    color = hudColors.mutedSteel
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = hudColors.cyanGlow,
                                unfocusedBorderColor = hudColors.panelBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("directive_title_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("HIGH" to "TINGGI", "NORMAL" to "NORMAL", "ROUTINE" to "RUTIN").forEach { (key, label) ->
                                    FilterChip(
                                        selected = selectedPriority == key,
                                        onClick = { selectedPriority = key },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = hudColors.cyanGlow.copy(alpha = 0.2f),
                                            selectedLabelColor = hudColors.cyanGlow
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (directiveInput.isNotBlank()) {
                                        onAddDirective(directiveInput, selectedPriority)
                                        directiveInput = ""
                                    }
                                },
                                enabled = directiveInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = hudColors.cyanGlow,
                                    contentColor = hudColors.voidBackdrop
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("add_directive_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.protocols_add_button))
                            }
                        }
                    }
                }
            }

            if (uiState.directives.isEmpty()) {
                item(key = "empty_directives") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, hudColors.panelBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = hudColors.cyanGlow.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.protocols_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.protocols_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.directives, key = { it.id }) { directive ->
                    DirectiveCard(
                        directive = directive,
                        onToggle = { onToggleDirective(directive) },
                        onDelete = { onDeleteDirective(directive.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSystemToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val hudColors = LocalJarvisHudColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hudColors.panelBorder),
        modifier = Modifier
            .width(136.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DirectiveCard(
    directive: CloudDirective,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val hudColors = LocalJarvisHudColors.current
    val priorityColor = when (directive.priority) {
        "HIGH" -> hudColors.amberAccent
        "ROUTINE" -> hudColors.emeraldStatus
        else -> hudColors.cyanGlow
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (directive.completed) hudColors.panelBorder.copy(alpha = 0.5f) else hudColors.panelBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("directive_item_${directive.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = directive.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = hudColors.emeraldStatus,
                    uncheckedColor = priorityColor
                ),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directive.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (directive.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (directive.completed) hudColors.mutedSteel else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PRIORITAS ${directive.priority}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (directive.completed) hudColors.mutedSteel else priorityColor
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_directive_${directive.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus direktif",
                    tint = hudColors.crimsonAlert.copy(alpha = 0.85f)
                )
            }
        }
    }
}
