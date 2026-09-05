package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSignalGenerator
import com.example.audio.AudioSpectrumAnalyzer
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

class DlmsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = PresetRepository(database.audioPresetDao())
    private val signalGenerator = AudioSignalGenerator()
    private val random = Random()

    private var spectrumAnalyzer: AudioSpectrumAnalyzer? = null
    @Volatile
    private var lastHardwareFft: FloatArray? = null
    @Volatile
    private var lastHardwareVuL: Float? = null
    @Volatile
    private var lastHardwareVuR: Float? = null
    @Volatile
    private var lastFftTimestamp = 0L
    @Volatile
    private var youTubePlayheadTime = 0.0

    private val _uiState = MutableStateFlow(DlmsUiState())
    val uiState: StateFlow<DlmsUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<AudioPresetEntity>> = repository.allPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Real-time RTA 31-band levels (normalized 0.0f to 1.0f)
    private val _spectrumLevels = MutableStateFlow(List(31) { 0.0f })
    val spectrumLevels: StateFlow<List<Float>> = _spectrumLevels.asStateFlow()

    // Peak hold levels
    private val _peakLevels = MutableStateFlow(List(31) { 0.0f })
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
        initHardwareAnalyzer(application)
        startSpectrumAnimationLoop()
    }

    private fun initHardwareAnalyzer(app: Application) {
        spectrumAnalyzer = AudioSpectrumAnalyzer(
            context = app,
            onFftUpdate = { bands ->
                lastHardwareFft = bands
                lastFftTimestamp = System.currentTimeMillis()
            },
            onVuUpdate = { dbL, dbR ->
                lastHardwareVuL = dbL
                lastHardwareVuR = dbR
            }
        )
        spectrumAnalyzer?.start()
    }

    fun startHardwareVisualizer() {
        spectrumAnalyzer?.start()
    }

    override fun onCleared() {
        super.onCleared()
        signalGenerator.stop()
        spectrumAnalyzer?.stop()
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

    // --- Crossover Controls with Output L / Output R / LINKED ---

    fun setCrossoverChannel(channel: ChannelSelect) {
        _uiState.update { it.copy(crossoverChannel = channel) }
    }

    fun setHpfEnabled(enabled: Boolean, targetChannel: ChannelSelect? = null) {
        val channel = targetChannel ?: _uiState.value.crossoverChannel
        _uiState.update { state ->
            when (channel) {
                ChannelSelect.LEFT -> state.copy(
                    crossoverL = state.crossoverL.copy(hpfEnabled = enabled),
                    crossover = state.crossover.copy(hpfEnabled = enabled)
                )
                ChannelSelect.RIGHT -> state.copy(
                    crossoverR = state.crossoverR.copy(hpfEnabled = enabled)
                )
                ChannelSelect.LINKED -> state.copy(
                    crossoverL = state.crossoverL.copy(hpfEnabled = enabled),
                    crossoverR = state.crossoverR.copy(hpfEnabled = enabled),
                    crossover = state.crossover.copy(hpfEnabled = enabled)
                )
            }
        }
    }

    fun setHpfFrequency(freq: Float, targetChannel: ChannelSelect? = null) {
        val clamped = freq.coerceIn(20f, 10000f)
        val channel = targetChannel ?: _uiState.value.crossoverChannel
        _uiState.update { state ->
            when (channel) {
                ChannelSelect.LEFT -> state.copy(
                    crossoverL = state.crossoverL.copy(hpfFrequency = clamped),
                    crossover = state.crossover.copy(hpfFrequency = clamped)
                )
                ChannelSelect.RIGHT -> state.copy(
                    crossoverR = state.crossoverR.copy(hpfFrequency = clamped)
                )
                ChannelSelect.LINKED -> state.copy(
                    crossoverL = state.crossoverL.copy(hpfFrequency = clamped),
                    crossoverR = state.crossoverR.copy(hpfFrequency = clamped),
                    crossover = state.crossover.copy(hpfFrequency = clamped)
                )
            }
        }
    }

    fun setHpfSlope(slope: CrossoverSlope, targetChannel: ChannelSelect? = null) {
        val channel = targetChannel ?: _uiState.value.crossoverChannel
        _uiState.update { state ->
            when (channel) {
                ChannelSelect.LEFT -> state.copy(
                    crossoverL = state.crossoverL.copy(hpfSlope = slope),
                    crossover = state.crossover.copy(hpfSlope = slope)
                )
                ChannelSelect.RIGHT -> state.copy(
                    crossoverR = state.crossoverR.copy(hpfSlope = slope)
                )
                ChannelSelect.LINKED -> state.copy(
                    crossoverL = state.crossoverL.copy(hpfSlope = slope),
                    crossoverR = state.crossoverR.copy(hpfSlope = slope),
                    crossover = state.crossover.copy(hpfSlope = slope)
                )
            }
        }
    }

    fun setLpfEnabled(enabled: Boolean, targetChannel: ChannelSelect? = null) {
        val channel = targetChannel ?: _uiState.value.crossoverChannel
        _uiState.update { state ->
            when (channel) {
                ChannelSelect.LEFT -> state.copy(
                    crossoverL = state.crossoverL.copy(lpfEnabled = enabled),
                    crossover = state.crossover.copy(lpfEnabled = enabled)
                )
                ChannelSelect.RIGHT -> state.copy(
                    crossoverR = state.crossoverR.copy(lpfEnabled = enabled)
                )
                ChannelSelect.LINKED -> state.copy(
                    crossoverL = state.crossoverL.copy(lpfEnabled = enabled),
                    crossoverR = state.crossoverR.copy(lpfEnabled = enabled),
                    crossover = state.crossover.copy(lpfEnabled = enabled)
                )
            }
        }
    }

    fun setLpfFrequency(freq: Float, targetChannel: ChannelSelect? = null) {
        val clamped = freq.coerceIn(100f, 20000f)
        val channel = targetChannel ?: _uiState.value.crossoverChannel
        _uiState.update { state ->
            when (channel) {
                ChannelSelect.LEFT -> state.copy(
                    crossoverL = state.crossoverL.copy(lpfFrequency = clamped),
                    crossover = state.crossover.copy(lpfFrequency = clamped)
                )
                ChannelSelect.RIGHT -> state.copy(
                    crossoverR = state.crossoverR.copy(lpfFrequency = clamped)
                )
                ChannelSelect.LINKED -> state.copy(
                    crossoverL = state.crossoverL.copy(lpfFrequency = clamped),
                    crossoverR = state.crossoverR.copy(lpfFrequency = clamped),
                    crossover = state.crossover.copy(lpfFrequency = clamped)
                )
            }
        }
    }

    fun setLpfSlope(slope: CrossoverSlope, targetChannel: ChannelSelect? = null) {
        val channel = targetChannel ?: _uiState.value.crossoverChannel
        _uiState.update { state ->
            when (channel) {
                ChannelSelect.LEFT -> state.copy(
                    crossoverL = state.crossoverL.copy(lpfSlope = slope),
                    crossover = state.crossover.copy(lpfSlope = slope)
                )
                ChannelSelect.RIGHT -> state.copy(
                    crossoverR = state.crossoverR.copy(lpfSlope = slope)
                )
                ChannelSelect.LINKED -> state.copy(
                    crossoverL = state.crossoverL.copy(lpfSlope = slope),
                    crossoverR = state.crossoverR.copy(lpfSlope = slope),
                    crossover = state.crossover.copy(lpfSlope = slope)
                )
            }
        }
    }

    fun copyCrossoverLtoR() {
        _uiState.update { state ->
            state.copy(crossoverR = state.crossoverL)
        }
    }

    fun copyCrossoverRtoL() {
        _uiState.update { state ->
            state.copy(crossoverL = state.crossoverR)
        }
    }

    // --- YouTube & Signal Player ---

    fun onYouTubePlayerStateChanged(state: Int) {
        // YT.PlayerState: 1 = PLAYING, 2 = PAUSED, 0 = ENDED, 3 = BUFFERING
        val isPlaying = (state == 1)
        setYouTubePlaying(isPlaying)
    }

    fun onYouTubeTimeTick(currentTimeSeconds: Double) {
        youTubePlayheadTime = currentTimeSeconds
    }

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
            var simStep = 0.0
            val currentLevels = FloatArray(31) { 0.0f }
            val currentPeaks = FloatArray(31) { 0.0f }
            val peakHoldCounters = IntArray(31) { 0 }

            while (isActive) {
                simStep += 0.04
                val state = _uiState.value
                val isPlaying = state.isYouTubePlaying || state.isSignalGeneratorPlaying
                val isHardwareActive = (System.currentTimeMillis() - lastFftTimestamp) < 400 && lastHardwareFft != null
                val hwFft = lastHardwareFft

                // Base energy profiles
                val activeEqGains = when (state.activeChannel) {
                    ChannelSelect.LEFT -> state.channelL.eqGains
                    ChannelSelect.RIGHT -> state.channelR.eqGains
                    ChannelSelect.LINKED -> state.channelL.eqGains.zip(state.channelR.eqGains) { l, r -> (l + r) / 2f }
                }

                val activeCrossover = when (state.crossoverChannel) {
                    ChannelSelect.LEFT -> state.crossoverL
                    ChannelSelect.RIGHT -> state.crossoverR
                    ChannelSelect.LINKED -> state.crossoverL // default linked
                }

                val avgGainDb = if (state.channelL.isMuted && state.channelR.isMuted) -60f else maxOf(state.channelL.gainDb, state.channelR.gainDb)
                val gainMultiplier = if (state.channelL.isMuted && state.channelR.isMuted) 0f else 10f.pow(avgGainDb / 20f).coerceIn(0.05f, 2.5f)

                if (!isPlaying) {
                    // Truly silence and decay when music is stopped/paused
                    for (i in 0 until 31) {
                        currentLevels[i] = (currentLevels[i] * 0.70f).coerceAtLeast(0f)
                        currentPeaks[i] = (currentPeaks[i] * 0.80f).coerceAtLeast(0f)
                        peakHoldCounters[i] = 0
                    }
                    _spectrumLevels.value = currentLevels.toList()
                    _peakLevels.value = currentPeaks.toList()
                    _vuLevelL.value = -60f
                    _vuLevelR.value = -60f
                    delay(35)
                    continue
                }

                for (i in 0 until 31) {
                    val freq = ISO_31_FREQUENCIES[i]
                    val eqGain = activeEqGains.getOrElse(i) { 0f }

                    // Calculate Crossover attenuation at this frequency
                    var crossoverAttenuationDb = 0f
                    if (activeCrossover.hpfEnabled && activeCrossover.hpfSlope != CrossoverSlope.BYPASS) {
                        if (freq < activeCrossover.hpfFrequency) {
                            val octavesBelow = (log10(activeCrossover.hpfFrequency / freq) / log10(2.0)).toFloat()
                            crossoverAttenuationDb -= octavesBelow * activeCrossover.hpfSlope.rollOffDb
                        }
                    }
                    if (activeCrossover.lpfEnabled && activeCrossover.lpfSlope != CrossoverSlope.BYPASS) {
                        if (freq > activeCrossover.lpfFrequency) {
                            val octavesAbove = (log10(freq / activeCrossover.lpfFrequency) / log10(2.0)).toFloat()
                            crossoverAttenuationDb -= octavesAbove * activeCrossover.lpfSlope.rollOffDb
                        }
                    }

                    // Filter factor in linear scale
                    val totalDb = (eqGain + crossoverAttenuationDb).coerceIn(-48f, 15f)
                    val filterFactor = 10f.pow(totalDb / 20f)

                    val targetLevel: Float = if (isHardwareActive && hwFft != null) {
                        // LIVE HARDWARE AUDIO ANALYSIS
                        val rawFft = hwFft.getOrElse(i) { 0f }
                        (rawFft * filterFactor * gainMultiplier).coerceIn(0.0f, 1.0f)
                    } else if (state.isSignalGeneratorPlaying) {
                        // DSP Hardware generator signatures
                        when (state.signalGeneratorType) {
                            SignalType.SINE_1KHZ -> if (i in 16..18) 0.90f * filterFactor else 0.02f
                            SignalType.SINE_40HZ -> if (i in 2..4) 0.94f * filterFactor else 0.02f
                            SignalType.SINE_100HZ -> if (i in 6..8) 0.92f * filterFactor else 0.02f
                            SignalType.PINK_NOISE -> {
                                val pinkWeight = (1.0f / (freq.toDouble().pow(0.2))).toFloat() * 1.5f
                                (0.35f * pinkWeight + (random.nextFloat() * 0.12f)) * filterFactor
                            }
                            SignalType.SWEEP_20_20K -> {
                                val activeIdx = ((simStep * 5.0).toInt() % 31)
                                val dist = abs(i - activeIdx)
                                if (dist == 0) 0.95f * filterFactor
                                else if (dist == 1) 0.45f * filterFactor
                                else 0.02f
                            }
                        }
                    } else {
                        // REALISTIC MUSICAL DYNAMICS synced to playhead
                        val t = if (youTubePlayheadTime > 0.0) youTubePlayheadTime else simStep
                        val beatBpm = 126.0
                        val beatPos = (t * (beatBpm / 60.0))
                        val beatFraction = (beatPos % 1.0)

                        // Transient punch (kick on beat, snare on 2 & 4)
                        val isKick = (beatFraction < 0.22)
                        val kickIntensity = if (isKick) (1.0 - beatFraction / 0.22).toFloat() else 0f

                        val isSnare = ((beatPos % 2.0) in 1.0..1.25)
                        val snareIntensity = if (isSnare) (1.0 - ((beatPos % 2.0) - 1.0) / 0.25).toFloat() else 0f

                        // 16th note hi-hat ticks
                        val hatPos = (beatPos * 4.0 % 1.0)
                        val hatIntensity = if (hatPos < 0.3) (1.0 - hatPos / 0.3).toFloat() else 0.1f

                        val bassEnergy = 0.35f + kickIntensity * 0.55f + (sin(t * 4.0 + i).toFloat() * 0.1f)
                        val midEnergy = 0.30f + snareIntensity * 0.45f + (cos(t * 3.0 + i * 0.5).toFloat() * 0.15f)
                        val trebleEnergy = 0.25f + hatIntensity * 0.40f + (sin(t * 8.0 + i).toFloat() * 0.1f)

                        val bandEnergy = when (i) {
                            in 0..6 -> bassEnergy // 20Hz - 80Hz
                            in 7..14 -> 0.30f + midEnergy * 0.8f // 100Hz - 500Hz
                            in 15..23 -> midEnergy // 630Hz - 4kHz
                            else -> trebleEnergy // 5kHz - 20kHz
                        }

                        (bandEnergy * gainMultiplier * filterFactor).coerceIn(0.02f, 0.98f)
                    }

                    // Ballistics: fast attack, smooth musical decay
                    val clampedTarget = targetLevel.coerceIn(0.0f, 1.0f)
                    if (clampedTarget > currentLevels[i]) {
                        currentLevels[i] += (clampedTarget - currentLevels[i]) * 0.70f // Fast attack
                    } else {
                        currentLevels[i] += (clampedTarget - currentLevels[i]) * 0.22f // Smooth decay
                    }

                    // Peak hold ballistics
                    if (currentLevels[i] > currentPeaks[i]) {
                        currentPeaks[i] = currentLevels[i]
                        peakHoldCounters[i] = 10 // Hold for ~350ms
                    } else {
                        if (peakHoldCounters[i] > 0) {
                            peakHoldCounters[i]--
                        } else {
                            currentPeaks[i] = (currentPeaks[i] - 0.03f).coerceAtLeast(currentLevels[i])
                        }
                    }
                }

                _spectrumLevels.value = currentLevels.toList()
                _peakLevels.value = currentPeaks.toList()

                // VU Meter calculation with stereo separation and live hardware tracking
                val dbL = if (state.channelL.isMuted) -60f else {
                    if (isHardwareActive && lastHardwareVuL != null) {
                        (lastHardwareVuL!! + state.channelL.gainDb).coerceIn(-60f, 6f)
                    } else {
                        val baseL = currentLevels.take(16).average().toFloat()
                        (baseL * 42f - 28f + state.channelL.gainDb).coerceIn(-60f, 6f)
                    }
                }

                val dbR = if (state.channelR.isMuted) -60f else {
                    if (isHardwareActive && lastHardwareVuR != null) {
                        (lastHardwareVuR!! + state.channelR.gainDb).coerceIn(-60f, 6f)
                    } else {
                        val baseR = currentLevels.takeLast(16).average().toFloat()
                        (baseR * 42f - 28f + state.channelR.gainDb).coerceIn(-60f, 6f)
                    }
                }

                _vuLevelL.value = dbL
                _vuLevelR.value = dbR

                delay(35) // ~28 FPS smooth audio RTA
            }
        }
    }
}
