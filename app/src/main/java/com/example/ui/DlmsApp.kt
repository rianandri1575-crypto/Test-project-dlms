package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CrossoverView
import com.example.ui.components.DelayAlignView
import com.example.ui.components.Equalizer31BandView
import com.example.ui.components.PresetsView
import com.example.ui.components.RealTimeSpectrumVisualizer
import com.example.ui.components.StereoVuMeter
import com.example.ui.components.YouTubePlayerCard
import com.example.ui.theme.AudioAmber
import com.example.ui.theme.AudioCyan
import com.example.ui.theme.AudioGreen
import com.example.ui.theme.AudioRed
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityCard
import com.example.ui.theme.HighDensityLavender
import com.example.ui.theme.HighDensityOnLavender
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.HighDensityYouTubeRed
import com.example.ui.theme.RackBackground
import com.example.ui.theme.RackBorder
import com.example.ui.theme.RackCard
import com.example.ui.theme.RackCardHighlight
import com.example.ui.theme.RackSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class DlmsTab(val label: String, val icon: ImageVector) {
    PLAYER("Player", Icons.Default.PlayCircle),
    EQUALIZER("31-Band EQ", Icons.Default.Equalizer),
    CROSSOVER("Crossover", Icons.Default.ContentCut),
    DELAY("Delay & Align", Icons.Default.AvTimer),
    PRESETS("Presets", Icons.Default.Save)
}

@Composable
fun DlmsApp(
    viewModel: DlmsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spectrumLevels by viewModel.spectrumLevels.collectAsStateWithLifecycle()
    val peakLevels by viewModel.peakLevels.collectAsStateWithLifecycle()
    val vuLevelL by viewModel.vuLevelL.collectAsStateWithLifecycle()
    val vuLevelR by viewModel.vuLevelR.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(RackBackground),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = HighDensityCard,
                contentColor = HighDensityTextPrimary,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .testTag("dlms_bottom_nav")
            ) {
                DlmsTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HighDensityOnLavender,
                            selectedTextColor = HighDensityLavender,
                            indicatorColor = HighDensityLavender,
                            unselectedIconColor = HighDensityTextSecondary.copy(alpha = 0.6f),
                            unselectedTextColor = HighDensityTextSecondary.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(HighDensityBg)
                .statusBarsPadding()
        ) {
            // TOP HIGH DENSITY HEADER
            HighDensityHeader(
                currentTitle = uiState.currentYouTubeTitle,
                isYouTubePlaying = uiState.isYouTubePlaying,
                isSignalPlaying = uiState.isSignalGeneratorPlaying,
                currentPresetName = uiState.currentPresetName,
                onResetFlat = {
                    viewModel.resetEqFlat()
                    scope.launch {
                        snackbarHostState.showSnackbar("Equalizer di-reset FLAT (0 dB)")
                    }
                },
                onQuickSave = {
                    selectedTab = 4 // Navigate to Presets tab
                }
            )

            // SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. PINNED REAL-TIME RTA SPECTRUM VISUALIZER (Requirement 7)
                RealTimeSpectrumVisualizer(
                    levels = spectrumLevels,
                    peakLevels = peakLevels,
                    eqGains = uiState.channelL.eqGains,
                    crossover = uiState.crossover
                )

                // 2. PINNED STEREO VU METER
                StereoVuMeter(
                    levelDbL = vuLevelL,
                    levelDbR = vuLevelR,
                    isMutedL = uiState.channelL.isMuted,
                    isMutedR = uiState.channelR.isMuted
                )

                // 3. TAB CONTENT
                when (selectedTab) {
                    0 -> {
                        // YouTube Player & DSP Generator (Requirement 1)
                        YouTubePlayerCard(
                            currentVideoId = uiState.currentYouTubeVideoId,
                            currentTitle = uiState.currentYouTubeTitle,
                            isPlaying = uiState.isYouTubePlaying,
                            isSignalPlaying = uiState.isSignalGeneratorPlaying,
                            signalType = uiState.signalGeneratorType,
                            onTrackSelected = { id, title ->
                                viewModel.selectYouTubeTrack(id, title)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Memutar track: $title")
                                }
                            },
                            onTogglePlay = { viewModel.setYouTubePlaying(it) },
                            onToggleSignal = { viewModel.toggleSignalGenerator(it) },
                            onStopSignal = { viewModel.stopSignalGenerator() }
                        )
                    }

                    1 -> {
                        // 31-Band Equalizer for Output L & R (Requirement 4)
                        Equalizer31BandView(
                            activeChannel = uiState.activeChannel,
                            channelL = uiState.channelL,
                            channelR = uiState.channelR,
                            onSelectChannel = { viewModel.setActiveChannel(it) },
                            onBandGainChange = { band, gain -> viewModel.setEqGain(band, gain) },
                            onResetFlat = {
                                viewModel.resetEqFlat()
                                scope.launch {
                                    snackbarHostState.showSnackbar("31-Band EQ di-reset flat (0 dB)")
                                }
                            },
                            onChannelGainChange = { isLeft, gain -> viewModel.setChannelGain(isLeft, gain) },
                            onToggleMute = { viewModel.toggleMute(it) },
                            onApplyCurve = {
                                viewModel.applyEqCurve(it)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Kurva EQ berhasil diterapkan")
                                }
                            }
                        )
                    }

                    2 -> {
                        // Crossover High-Pass & Low-Pass Filters (Requirement 6)
                        CrossoverView(
                            crossover = uiState.crossover,
                            onHpfEnabledChange = { viewModel.setHpfEnabled(it) },
                            onHpfFrequencyChange = { viewModel.setHpfFrequency(it) },
                            onHpfSlopeChange = { viewModel.setHpfSlope(it) },
                            onLpfEnabledChange = { viewModel.setLpfEnabled(it) },
                            onLpfFrequencyChange = { viewModel.setLpfFrequency(it) },
                            onLpfSlopeChange = { viewModel.setLpfSlope(it) }
                        )
                    }

                    3 -> {
                        // Speaker Delay & Phase Alignment
                        DelayAlignView(
                            channelL = uiState.channelL,
                            channelR = uiState.channelR,
                            onDelayChange = { isLeft, delay -> viewModel.setDelayMs(isLeft, delay) },
                            onPhaseToggle = { viewModel.togglePhase(it) },
                            onMuteToggle = { viewModel.toggleMute(it) },
                            onGainChange = { isLeft, gain -> viewModel.setChannelGain(isLeft, gain) }
                        )
                    }

                    4 -> {
                        // Preset Management (Requirement 5)
                        PresetsView(
                            presets = presets,
                            currentPresetName = uiState.currentPresetName,
                            onLoadPreset = { preset ->
                                viewModel.loadPreset(preset)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Preset '${preset.name}' dimuat!")
                                }
                            },
                            onSavePreset = { name, desc ->
                                viewModel.saveCurrentAsPreset(name, desc)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Preset '$name' tersimpan!")
                                }
                            },
                            onDeletePreset = { preset ->
                                viewModel.deletePreset(preset)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Preset '${preset.name}' dihapus.")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HighDensityHeader(
    currentTitle: String,
    isYouTubePlaying: Boolean,
    isSignalPlaying: Boolean,
    currentPresetName: String,
    onResetFlat: () -> Unit,
    onQuickSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = HighDensityBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: YouTube Red rounded badge + Now Playing status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HighDensityYouTubeRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playback Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isYouTubePlaying) "NOW PLAYING YOUTUBE" else if (isSignalPlaying) "DSP TONE GENERATOR" else "DLMS AUDIO DSP",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityLavender,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (currentTitle.isNotBlank()) currentTitle else "Preset: $currentPresetName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HighDensityTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }
            }

            // Right: High Density Save preset icon + Reset Flat pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Save Button (w-10 h-10 rounded-full bg-[#49454F])
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(HighDensityBorder)
                        .clickable(onClick = onQuickSave),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Preset",
                        tint = HighDensityLavender,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset Flat Pill Button (px-4 h-10 rounded-full bg-[#D0BCFF] text-[#381E72])
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(HighDensityLavender)
                        .clickable(onClick = onResetFlat)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RESET FLAT",
                        color = HighDensityOnLavender,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
