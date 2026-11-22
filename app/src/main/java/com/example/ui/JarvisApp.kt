package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.auth.signOutFromJarvis
import com.example.ui.screens.CoreHudScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.ProtocolsScreen
import com.example.ui.theme.LocalJarvisHudColors

enum class JarvisTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CORE(R.string.nav_core, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    STUDIO(R.string.nav_studio, Icons.Filled.Palette, Icons.Outlined.Palette),
    PROTOCOLS(R.string.nav_protocols, Icons.Filled.Checklist, Icons.Outlined.Checklist),
    DIAGNOSTICS(R.string.nav_diagnostics, Icons.Filled.Tune, Icons.Outlined.Tune)
}

@Composable
fun JarvisApp(
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(JarvisTab.CORE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hudColors = LocalJarvisHudColors.current

    BackHandler(enabled = currentTab != JarvisTab.CORE) {
        currentTab = JarvisTab.CORE
    }

    LaunchedEffect(uiState.statusBanner) {
        val message = uiState.statusBanner
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusBanner()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = hudColors.voidBackdrop,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        JarvisTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    viewModel.refreshTelemetry()
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = hudColors.cyanGlow,
                                    selectedTextColor = hudColors.cyanGlow,
                                    indicatorColor = hudColors.cyanGlow.copy(alpha = 0.16f),
                                    unselectedIconColor = hudColors.mutedSteel,
                                    unselectedTextColor = hudColors.mutedSteel
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        JarvisTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    viewModel.refreshTelemetry()
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = hudColors.cyanGlow,
                                    selectedTextColor = hudColors.cyanGlow,
                                    indicatorColor = hudColors.cyanGlow.copy(alpha = 0.16f),
                                    unselectedIconColor = hudColors.mutedSteel,
                                    unselectedTextColor = hudColors.mutedSteel
                                ),
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                when (currentTab) {
                    JarvisTab.CORE -> CoreHudScreen(
                        uiState = uiState,
                        onSendMessage = viewModel::sendMessage,
                        onStartVoice = viewModel::startVoiceListening,
                        onStopVoice = viewModel::stopVoiceListening,
                        onToggleHandsFree = viewModel::setHandsFreeMode,
                        onAttachImage = viewModel::attachImage,
                        onSpeakText = viewModel::speakText,
                        onStopSpeaking = viewModel::stopSpeaking,
                        onToggleAutoSpeak = {
                            viewModel.setAutoSpeak(!uiState.settings.autoSpeakEnabled)
                        },
                        onExecuteAction = viewModel::executeAction,
                        onShowToastMessage = viewModel::showNotice,
                        modifier = Modifier.weight(1f)
                    )

                    JarvisTab.STUDIO -> ImageStudioScreen(
                        uiState = uiState,
                        onSetReferenceUri = viewModel::setStudioReferenceImage,
                        onGenerateOrEdit = viewModel::generateOrEditStudioImage,
                        onSaveToGallery = viewModel::saveGeneratedImageToGallery,
                        onShowNotice = viewModel::showNotice,
                        modifier = Modifier.weight(1f)
                    )

                    JarvisTab.PROTOCOLS -> ProtocolsScreen(
                        uiState = uiState,
                        onAddDirective = viewModel::addDirective,
                        onToggleDirective = viewModel::toggleDirective,
                        onDeleteDirective = viewModel::deleteDirective,
                        onQuickAlarm = { viewModel.triggerQuickAlarm(6, 0, "J.A.R.V.I.S. Briefing") },
                        onQuickTimer = { viewModel.triggerQuickTimer(300, "Sesi Fokus J.A.R.V.I.S.") },
                        onQuickCalendar = { viewModel.triggerQuickCalendar("Agenda Taktis J.A.R.V.I.S.") },
                        onQuickSearch = { viewModel.triggerQuickWebSearch("Berita teknologi terbaru") },
                        onRefreshLocation = viewModel::refreshLocation,
                        onOpenMaps = viewModel::openMapsSearch,
                        onGenerateBriefing = { viewModel.generateDailyBriefing(speakAloud = true) },
                        onSpeakBriefing = viewModel::speakText,
                        onToggleBriefingSchedule = viewModel::setDailyBriefingSchedule,
                        onShowNotice = viewModel::showNotice,
                        modifier = Modifier.weight(1f)
                    )

                    JarvisTab.DIAGNOSTICS -> DiagnosticsScreen(
                        uiState = uiState,
                        onSetAutoSpeak = viewModel::setAutoSpeak,
                        onSetSpeechRate = viewModel::setSpeechRate,
                        onSetLanguage = viewModel::setLanguageCode,
                        onSetPersona = viewModel::setPersona,
                        onSetDailyBriefingSchedule = viewModel::setDailyBriefingSchedule,
                        onSendTestBriefingNotification = viewModel::sendTestBriefingNotification,
                        onClearConversation = viewModel::clearConversation,
                        onRefreshTelemetry = viewModel::refreshTelemetry,
                        onSignOut = {
                            viewModel.setHandsFreeMode(false)
                            signOutFromJarvis(credentialManager, scope)
                        },
                        onShowNotice = viewModel::showNotice,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
