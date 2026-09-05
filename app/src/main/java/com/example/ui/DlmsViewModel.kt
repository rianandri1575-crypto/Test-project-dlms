package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSignalGenerator
import com.example.data.db.AppDatabase
import com.example.data.db.AudioPresetEntity
import com.example.data.model.ChannelAudioSettings
import com.example.data.model.ChannelSelect
import com.example.data.model.CrossoverSettings
import com.example.data.model.CrossoverSlope
import com.example.data.model.DlmsUiState
import com.example.data.model.ISO_31_FREQUENCIES
import com.example.data.model.SignalType
import com.example.data.repository.PresetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.log10
import kotlin.math.pow

class DlmsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = PresetRepository(database.audioPresetDao())
    private val signalGenerator = AudioSignalGenerator()
    private val random = Random()

    private val _uiState = MutableStateFlow(DlmsUiState())
    val uiState: StateFlow<DlmsUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<AudioPresetEntity>> = repository.allPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Real-time RTA 31-band levels (normalized 0.0f to 1.0f)
    private val _spectrumLevels = MutableStateFlow(List(31) { 0.05f })
    val spectrumLevels: StateFlow<List<Float>> = _spectrumLevels.asStateFlow()

    // Peak hold levels
    private val _peakLevels = MutableStateFlow(List(31) { 0.05f })
    val peakLevels: StateFlow<List<Float>> = _peakLevels.asStateFlow()

    // Real-time VU Meters for Output L & R (-60 dB to +6 dB scale)
    private val _vuLevelL = MutableStateFlow(-60f)
    val vuLevelL: StateFlow<Float> = _vuLevelL.asStateFlow()

    private val _vuLevelR = MutableStateFlow(-60f)
    val vuLevelR: StateFlow<Float> = _vuLevelR.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultPresets()
        }
        startSpectrumAnimationLoop()
    }

    override fun onCleared() {
        super.onCleared()
        signalGenerator.stop()
    }

    // --- Channel & EQ Controls ---

    fun setActiveChannel(channel: ChannelSelect) {
        _uiState.update { it.copy(activeChannel = channel) }
    }

    fun setEqGain(bandIndex: Int, gainDb: Float) {
        val clampedGain = gainDb.coerceIn(-12f, 12f)
        _uiState.update { state ->
            when (state.activeChannel) {
                ChannelSelect.LEFT -> {
                    val newGains = state.channelL.eqGains.toMutableList()
                    newGains[bandIndex] = clampedGain
                    state.copy(channelL = state.channelL.copy(eqGains = newGains))
                }
                ChannelSelect.RIGHT -> {
                    val newGains = state.channelR.eqGains.toMutableList()
                    newGains[bandIndex] = clampedGain
                    state.copy(channelR = state.channelR.copy(eqGains = newGains))
                }
                ChannelSelect.LINKED -> {
                    val newGainsL = state.channelL.eqGains.toMutableList()
                    val newGainsR = state.channelR.eqGains.toMutableList()
                    newGainsL[bandIndex] = clampedGain
                    newGainsR[bandIndex] = clampedGain
                    state.copy(
                        channelL = state.channelL.copy(eqGains = newGainsL),
                        channelR = state.channelR.copy(eqGains = newGainsR)
                    )
                }
            }
        }
    }

    fun resetEqFlat() {
        _uiState.update { state ->
            val flatList = List(31) { 0f }
            when (state.activeChannel) {
                ChannelSelect.LEFT -> state.copy(channelL = state.channelL.copy(eqGains = flatList))
                ChannelSelect.RIGHT -> state.copy(channelR = state.channelR.copy(eqGains = flatList))
                ChannelSelect.LINKED -> state.copy(
                    channelL = state.channelL.copy(eqGains = flatList),
                    channelR = state.channelR.copy(eqGains = flatList)
                )
            }
        }
    }

    fun applyEqCurve(curve: List<Float>) {
        if (curve.size != 31) return
        _uiState.update { state ->
            when (state.activeChannel) {
                ChannelSelect.LEFT -> state.copy(channelL = state.channelL.copy(eqGains = curve))
                ChannelSelect.RIGHT -> state.copy(channelR = state.channelR.copy(eqGains = curve))
                ChannelSelect.LINKED -> state.copy(
                    channelL = state.channelL.copy(eqGains = curve),
                    channelR = state.channelR.copy(eqGains = curve)
                )
            }
        }
    }

    fun setChannelGain(isLeft: Boolean, gainDb: Float) {
        val clamped = gainDb.coerceIn(-60f, 12f)
        _uiState.update { state ->
            if (state.activeChannel == ChannelSelect.LINKED) {
                state.copy(
                    channelL = state.channelL.copy(gainDb = clamped),
                    channelR = state.channelR.copy(gainDb = clamped)
                )
            } else if (isLeft) {
                state.copy(channelL = state.channelL.copy(gainDb = clamped))
            } else {
                state.copy(channelR = state.channelR.copy(gainDb = clamped))
            }
        }
    }

    fun toggleMute(isLeft: Boolean) {
        _uiState.update { state ->
            if (state.activeChannel == ChannelSelect.LINKED) {
                val newMute = !state.channelL.isMuted
                state.copy(
                    channelL = state.channelL.copy(isMuted = newMute),
                    channelR = state.channelR.copy(isMuted = newMute)
                )
            } else if (isLeft) {
                state.copy(channelL = state.channelL.copy(isMuted = !state.channelL.isMuted))
            } else {
                state.copy(channelR = state.channelR.copy(isMuted = !state.channelR.isMuted))
            }
        }
    }

    fun togglePhase(isLeft: Boolean) {
        _uiState.update { state ->
            if (isLeft) {
                state.copy(channelL = state.channelL.copy(isPhaseInverted = !state.channelL.isPhaseInverted))
            } else {
                state.copy(channelR = state.channelR.copy(isPhaseInverted = !state.channelR.isPhaseInverted))
            }
        }
    }

    fun setDelayMs(isLeft: Boolean, delayMs: Float) {
        val clamped = delayMs.coerceIn(0f, 100f)
        _uiState.update { state ->
            if (state.activeChannel == ChannelSelect.LINKED) {
                state.copy(
                    channelL = state.channelL.copy(delayMs = clamped),
                    channelR = state.channelR.copy(delayMs = clamped)
                )
            } else if (isLeft) {
                state.copy(channelL = state.channelL.copy(delayMs = clamped))
            } else {
                state.copy(channelR = state.channelR.copy(delayMs = clamped))
            }
        }
    }

    // --- Crossover Controls ---

    fun setHpfEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(crossover = state.crossover.copy(hpfEnabled = enabled))
        }
    }

    fun setHpfFrequency(freq: Float) {
        val clamped = freq.coerceIn(20f, 10000f)
        _uiState.update { state ->
            state.copy(crossover = state.crossover.copy(hpfFrequency = clamped))
        }
    }

    fun setHpfSlope(slope: CrossoverSlope) {
        _uiState.update { state ->
            state.copy(crossover = state.crossover.copy(hpfSlope = slope))
        }
    }

    fun setLpfEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(crossover = state.crossover.copy(lpfEnabled = enabled))
        }
    }

    fun setLpfFrequency(freq: Float) {
        val clamped = freq.coerceIn(100f, 20000f)
        _uiState.update { state ->
            state.copy(crossover = state.crossover.copy(lpfFrequency = clamped))
        }
    }

    fun setLpfSlope(slope: CrossoverSlope) {
        _uiState.update { state ->
            state.copy(crossover = state.crossover.copy(lpfSlope = slope))
        }
    }

    // --- YouTube & Signal Player ---

    fun setYouTubePlaying(playing: Boolean) {
        _uiState.update { it.copy(isYouTubePlaying = playing) }
        if (playing && _uiState.value.isSignalGeneratorPlaying) {
            stopSignalGenerator()
        }
    }

    fun selectYouTubeTrack(videoId: String, title: String) {
        _uiState.update {
            it.copy(
                currentYouTubeVideoId = videoId,
                currentYouTubeTitle = title,
                isYouTubePlaying = true
            )
        }
        if (_uiState.value.isSignalGeneratorPlaying) {
            stopSignalGenerator()
        }
    }

    fun toggleSignalGenerator(type: SignalType) {
        val currentlyPlaying = _uiState.value.isSignalGeneratorPlaying
        val sameType = _uiState.value.signalGeneratorType == type

        if (currentlyPlaying && sameType) {
            stopSignalGenerator()
        } else {
            signalGenerator.start(type, 0.4f)
            _uiState.update {
                it.copy(
                    isSignalGeneratorPlaying = true,
                    signalGeneratorType = type,
                    isYouTubePlaying = false
                )
            }
        }
    }

    fun stopSignalGenerator() {
        signalGenerator.stop()
        _uiState.update { it.copy(isSignalGeneratorPlaying = false) }
    }

    // --- Presets (Room DB) ---

    fun saveCurrentAsPreset(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val preset = AudioPresetEntity(
                name = name.ifBlank { "Preset ${System.currentTimeMillis() % 1000}" },
                description = description,
                isFactory = false,
                eqGainsL = state.channelL.eqGains.joinToString(","),
                eqGainsR = state.channelR.eqGains.joinToString(","),
                hpfEnabled = state.crossover.hpfEnabled,
                hpfFrequency = state.crossover.hpfFrequency,
                hpfSlope = state.crossover.hpfSlope.name,
                lpfEnabled = state.crossover.lpfEnabled,
                lpfFrequency = state.crossover.lpfFrequency,
                lpfSlope = state.crossover.lpfSlope.name,
                gainL = state.channelL.gainDb,
                gainR = state.channelR.gainDb,
                delayL = state.channelL.delayMs,
                delayR = state.channelR.delayMs,
                muteL = state.channelL.isMuted,
                muteR = state.channelR.isMuted,
                phaseInvertL = state.channelL.isPhaseInverted,
                phaseInvertR = state.channelR.isPhaseInverted
            )
            repository.savePreset(preset)
            _uiState.update { it.copy(currentPresetName = preset.name) }
        }
    }

    fun loadPreset(preset: AudioPresetEntity) {
        val gainsL = preset.eqGainsL.split(",").mapNotNull { it.trim().toFloatOrNull() }
        val gainsR = preset.eqGainsR.split(",").mapNotNull { it.trim().toFloatOrNull() }

        val finalGainsL = if (gainsL.size == 31) gainsL else List(31) { 0f }
        val finalGainsR = if (gainsR.size == 31) gainsR else List(31) { 0f }

        val hpfSlope = try {
            CrossoverSlope.valueOf(preset.hpfSlope)
        } catch (_: Exception) {
            CrossoverSlope.LR_24
        }

        val lpfSlope = try {
            CrossoverSlope.valueOf(preset.lpfSlope)
        } catch (_: Exception) {
            CrossoverSlope.LR_24
        }

        _uiState.update { state ->
            state.copy(
                channelL = state.channelL.copy(
                    gainDb = preset.gainL,
                    isMuted = preset.muteL,
                    isPhaseInverted = preset.phaseInvertL,
                    delayMs = preset.delayL,
                    eqGains = finalGainsL
                ),
                channelR = state.channelR.copy(
                    gainDb = preset.gainR,
                    isMuted = preset.muteR,
                    isPhaseInverted = preset.phaseInvertR,
                    delayMs = preset.delayR,
                    eqGains = finalGainsR
                ),
                crossover = state.crossover.copy(
                    hpfEnabled = preset.hpfEnabled,
                    hpfFrequency = preset.hpfFrequency,
                    hpfSlope = hpfSlope,
                    lpfEnabled = preset.lpfEnabled,
                    lpfFrequency = preset.lpfFrequency,
                    lpfSlope = lpfSlope
                ),
                currentPresetName = preset.name
            )
        }
    }

    fun deletePreset(preset: AudioPresetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePreset(preset.id)
        }
    }

    // --- Real-time RTA Spectrum & VU Meter Physics ---

    private fun startSpectrumAnimationLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            var step = 0f
            val currentLevels = FloatArray(31) { 0.05f }
            val currentPeaks = FloatArray(31) { 0.05f }
            val peakHoldCounters = IntArray(31) { 0 }

            while (isActive) {
                step += 0.25f
                val state = _uiState.value
                val isPlaying = state.isYouTubePlaying || state.isSignalGeneratorPlaying
                val isMutedBoth = state.channelL.isMuted && state.channelR.isMuted

                // Base energy profiles
                val activeEqGains = when (state.activeChannel) {
                    ChannelSelect.LEFT -> state.channelL.eqGains
                    ChannelSelect.RIGHT -> state.channelR.eqGains
                    ChannelSelect.LINKED -> state.channelL.eqGains.zip(state.channelR.eqGains) { l, r -> (l + r) / 2f }
                }

                val avgGainDb = if (isMutedBoth) -60f else maxOf(state.channelL.gainDb, state.channelR.gainDb)
                val gainMultiplier = if (isMutedBoth) 0.001f else 10f.pow(avgGainDb / 20f).coerceIn(0.05f, 2.5f)

                for (i in 0 until 31) {
                    val freq = ISO_31_FREQUENCIES[i]
                    val eqGain = activeEqGains.getOrElse(i) { 0f }

                    // Calculate Crossover attenuation at this frequency
                    var crossoverAttenuationDb = 0f
                    if (state.crossover.hpfEnabled && state.crossover.hpfSlope != CrossoverSlope.BYPASS) {
                        if (freq < state.crossover.hpfFrequency) {
                            val octavesBelow = (log10(state.crossover.hpfFrequency / freq) / log10(2.0)).toFloat()
                            crossoverAttenuationDb -= octavesBelow * state.crossover.hpfSlope.rollOffDb
                        }
                    }
                    if (state.crossover.lpfEnabled && state.crossover.lpfSlope != CrossoverSlope.BYPASS) {
                        if (freq > state.crossover.lpfFrequency) {
                            val octavesAbove = (log10(freq / state.crossover.lpfFrequency) / log10(2.0)).toFloat()
                            crossoverAttenuationDb -= octavesAbove * state.crossover.lpfSlope.rollOffDb
                        }
                    }

                    // Combined filter factor in linear scale
                    val totalDb = (eqGain + crossoverAttenuationDb).coerceIn(-48f, 15f)
                    val filterFactor = 10f.pow(totalDb / 20f)

                    val targetLevel: Float = if (!isPlaying) {
                        // Ambient low floor
                        (0.04f + random.nextFloat() * 0.03f) * filterFactor
                    } else if (state.isSignalGeneratorPlaying) {
                        // Specific generator signatures
                        when (state.signalGeneratorType) {
                            SignalType.SINE_1KHZ -> {
                                if (i in 16..18) 0.88f * filterFactor else 0.04f * filterFactor
                            }
                            SignalType.SINE_40HZ -> {
                                if (i in 2..4) 0.92f * filterFactor else 0.03f * filterFactor
                            }
                            SignalType.SINE_100HZ -> {
                                if (i in 6..8) 0.90f * filterFactor else 0.04f * filterFactor
                            }
                            SignalType.PINK_NOISE -> {
                                // -3dB/octave slope pink noise energy
                                val pinkWeight = (1.0f / (freq.toDouble().pow(0.2))).toFloat() * 1.5f
                                (0.35f * pinkWeight + (random.nextFloat() * 0.12f)) * filterFactor
                            }
                            SignalType.SWEEP_20_20K -> {
                                val activeIdx = ((step * 1.2f).toInt() % 31)
                                val dist = kotlin.math.abs(i - activeIdx)
                                if (dist == 0) 0.95f * filterFactor
                                else if (dist == 1) 0.5f * filterFactor
                                else 0.03f * filterFactor
                            }
                        }
                    } else {
                        // YouTube music dynamics: musical rhythm spectrum (sub bass punch, vocal mid energy, shimmer)
                        val rhythmBass = (kotlin.math.sin(step * 0.8 + i * 0.2).toFloat() * 0.5f + 0.5f)
                        val rhythmMid = (kotlin.math.cos(step * 1.3 + i * 0.4).toFloat() * 0.5f + 0.5f)
                        val rhythmTreble = (kotlin.math.sin(step * 1.7 + i * 0.6).toFloat() * 0.5f + 0.5f)

                        val bandEnergy = when (i) {
                            in 0..6 -> 0.45f + rhythmBass * 0.45f // Sub & Bass
                            in 7..14 -> 0.35f + rhythmMid * 0.35f // Low-Mids
                            in 15..23 -> 0.40f + rhythmMid * 0.40f // Mids & Presence
                            else -> 0.30f + rhythmTreble * 0.30f // Highs & Brilliance
                        }
                        (bandEnergy * gainMultiplier * filterFactor).coerceIn(0.02f, 0.98f)
                    }

                    // Ballistics: fast attack, smooth decay
                    val clampedTarget = targetLevel.coerceIn(0.01f, 1.0f)
                    if (clampedTarget > currentLevels[i]) {
                        currentLevels[i] += (clampedTarget - currentLevels[i]) * 0.65f // Fast attack
                    } else {
                        currentLevels[i] += (clampedTarget - currentLevels[i]) * 0.20f // Smooth decay
                    }

                    // Peak hold ballistics
                    if (currentLevels[i] > currentPeaks[i]) {
                        currentPeaks[i] = currentLevels[i]
                        peakHoldCounters[i] = 12 // Hold for ~400ms
                    } else {
                        if (peakHoldCounters[i] > 0) {
                            peakHoldCounters[i]--
                        } else {
                            currentPeaks[i] = (currentPeaks[i] - 0.025f).coerceAtLeast(currentLevels[i])
                        }
                    }
                }

                _spectrumLevels.value = currentLevels.toList()
                _peakLevels.value = currentPeaks.toList()

                // VU Meter calculation
                val maxL = if (state.channelL.isMuted) -60f else {
                    val baseEnergy = currentLevels.take(16).average().toFloat()
                    val db = (baseEnergy * 40f - 30f + state.channelL.gainDb).coerceIn(-60f, 6f)
                    db
                }
                val maxR = if (state.channelR.isMuted) -60f else {
                    val baseEnergy = currentLevels.takeLast(16).average().toFloat()
                    val db = (baseEnergy * 40f - 30f + state.channelR.gainDb).coerceIn(-60f, 6f)
                    db
                }

                _vuLevelL.value = maxL
                _vuLevelR.value = maxR

                delay(35) // ~28-30 FPS for buttery smooth pro audio RTA
            }
        }
    }
}
