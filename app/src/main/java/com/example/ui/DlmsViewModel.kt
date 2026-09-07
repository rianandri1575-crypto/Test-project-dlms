package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSignalGenerator
import com.example.audio.AudioSpectrumAnalyzer
import com.example.audio.DspSettingsStore
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
    @Volatile private var lastHardwareFft: FloatArray? = null
    @Volatile private var lastHardwareVuL: Float? = null
    @Volatile private var lastHardwareVuR: Float? = null
    @Volatile private var lastFftTimestamp = 0L
    @Volatile private var youTubePlayheadTime = 0.0

    private val _uiState = MutableStateFlow(DlmsUiState())
    val uiState: StateFlow<DlmsUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<AudioPresetEntity>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _spectrumLevels = MutableStateFlow(List(31) { 0.0f })
    val spectrumLevels: StateFlow<List<Float>> = _spectrumLevels.asStateFlow()
    private val _peakLevels = MutableStateFlow(List(31) { 0.0f })
    val peakLevels: StateFlow<List<Float>> = _peakLevels.asStateFlow()
    private val _vuLevelL = MutableStateFlow(-60f)
    val vuLevelL: StateFlow<Float> = _vuLevelL.asStateFlow()
    private val _vuLevelR = MutableStateFlow(-60f)
    val vuLevelR: StateFlow<Float> = _vuLevelR.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { repository.ensureDefaultPresets() }
        // Every UI change is published to the native audio path immediately.
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.collect { DspSettingsStore.write(getApplication(), it) }
        }
        initHardwareAnalyzer(application)
        startSpectrumAnimationLoop()
    }

    private fun initHardwareAnalyzer(app: Application) {
        spectrumAnalyzer = AudioSpectrumAnalyzer(app,
            onFftUpdate = { bands -> lastHardwareFft = bands; lastFftTimestamp = System.currentTimeMillis() },
            onVuUpdate = { dbL, dbR -> lastHardwareVuL = dbL; lastHardwareVuR = dbR })
        spectrumAnalyzer?.start()
    }

    fun startHardwareVisualizer() { spectrumAnalyzer?.start() }
    override fun onCleared() { signalGenerator.stop(); spectrumAnalyzer?.stop(); super.onCleared() }

    fun setActiveChannel(channel: ChannelSelect) { _uiState.update { it.copy(activeChannel = channel) } }

    fun setEqGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0..30) return
        val g = gainDb.coerceIn(-12f, 12f)
        _uiState.update { s -> when (s.activeChannel) {
            ChannelSelect.LEFT -> s.copy(channelL = s.channelL.copy(eqGains = s.channelL.eqGains.toMutableList().also { it[bandIndex] = g }))
            ChannelSelect.RIGHT -> s.copy(channelR = s.channelR.copy(eqGains = s.channelR.eqGains.toMutableList().also { it[bandIndex] = g }))
            ChannelSelect.LINKED -> s.copy(channelL = s.channelL.copy(eqGains = s.channelL.eqGains.toMutableList().also { it[bandIndex] = g }), channelR = s.channelR.copy(eqGains = s.channelR.eqGains.toMutableList().also { it[bandIndex] = g }))
        } }
    }

    fun resetEqFlat() = _uiState.update { s ->
        val f = List(31) { 0f }
        when (s.activeChannel) {
            ChannelSelect.LEFT -> s.copy(channelL = s.channelL.copy(eqGains = f))
            ChannelSelect.RIGHT -> s.copy(channelR = s.channelR.copy(eqGains = f))
            ChannelSelect.LINKED -> s.copy(channelL = s.channelL.copy(eqGains = f), channelR = s.channelR.copy(eqGains = f))
        }
    }

    fun applyEqCurve(curve: List<Float>) {
        if (curve.size != 31) return
        _uiState.update { s -> when (s.activeChannel) {
            ChannelSelect.LEFT -> s.copy(channelL = s.channelL.copy(eqGains = curve))
            ChannelSelect.RIGHT -> s.copy(channelR = s.channelR.copy(eqGains = curve))
            ChannelSelect.LINKED -> s.copy(channelL = s.channelL.copy(eqGains = curve), channelR = s.channelR.copy(eqGains = curve))
        } }
    }

    fun setChannelGain(isLeft: Boolean, gainDb: Float) {
        val g = gainDb.coerceIn(-60f, 12f)
        _uiState.update { s -> if (s.activeChannel == ChannelSelect.LINKED) s.copy(channelL = s.channelL.copy(gainDb = g), channelR = s.channelR.copy(gainDb = g)) else if (isLeft) s.copy(channelL = s.channelL.copy(gainDb = g)) else s.copy(channelR = s.channelR.copy(gainDb = g)) }
    }
    fun toggleMute(isLeft: Boolean) { _uiState.update { s -> if (s.activeChannel == ChannelSelect.LINKED) { val m = !s.channelL.isMuted; s.copy(channelL = s.channelL.copy(isMuted = m), channelR = s.channelR.copy(isMuted = m)) } else if (isLeft) s.copy(channelL = s.channelL.copy(isMuted = !s.channelL.isMuted)) else s.copy(channelR = s.channelR.copy(isMuted = !s.channelR.isMuted)) } }
    fun togglePhase(isLeft: Boolean) { _uiState.update { s -> if (isLeft) s.copy(channelL = s.channelL.copy(isPhaseInverted = !s.channelL.isPhaseInverted)) else s.copy(channelR = s.channelR.copy(isPhaseInverted = !s.channelR.isPhaseInverted)) } }
    fun setDelayMs(isLeft: Boolean, delayMs: Float) { val d = delayMs.coerceIn(0f, 100f); _uiState.update { s -> if (s.activeChannel == ChannelSelect.LINKED) s.copy(channelL = s.channelL.copy(delayMs = d), channelR = s.channelR.copy(delayMs = d)) else if (isLeft) s.copy(channelL = s.channelL.copy(delayMs = d)) else s.copy(channelR = s.channelR.copy(delayMs = d)) } }

    fun setCrossoverChannel(channel: ChannelSelect) { _uiState.update { it.copy(crossoverChannel = channel) } }
    fun setHpfEnabled(enabled: Boolean, targetChannel: ChannelSelect? = null) { updateCrossover(targetChannel) { it.copy(hpfEnabled = enabled) } }
    fun setHpfFrequency(freq: Float, targetChannel: ChannelSelect? = null) { updateCrossover(targetChannel) { it.copy(hpfFrequency = freq.coerceIn(20f, 10000f)) } }
    fun setHpfSlope(slope: CrossoverSlope, targetChannel: ChannelSelect? = null) { updateCrossover(targetChannel) { it.copy(hpfSlope = slope) } }
    fun setLpfEnabled(enabled: Boolean, targetChannel: ChannelSelect? = null) { updateCrossover(targetChannel) { it.copy(lpfEnabled = enabled) } }
    fun setLpfFrequency(freq: Float, targetChannel: ChannelSelect? = null) { updateCrossover(targetChannel) { it.copy(lpfFrequency = freq.coerceIn(100f, 20000f)) } }
    fun setLpfSlope(slope: CrossoverSlope, targetChannel: ChannelSelect? = null) { updateCrossover(targetChannel) { it.copy(lpfSlope = slope) } }
    private fun updateCrossover(target: ChannelSelect?, change: (CrossoverSettings) -> CrossoverSettings) {
        val c = target ?: _uiState.value.crossoverChannel
        _uiState.update { s -> when (c) {
            ChannelSelect.LEFT -> s.copy(crossoverL = change(s.crossoverL), crossover = change(s.crossover))
            ChannelSelect.RIGHT -> s.copy(crossoverR = change(s.crossoverR))
            ChannelSelect.LINKED -> s.copy(crossoverL = change(s.crossoverL), crossoverR = change(s.crossoverR), crossover = change(s.crossover))
        } }
    }
    fun copyCrossoverLtoR() { _uiState.update { it.copy(crossoverR = it.crossoverL) } }
    fun copyCrossoverRtoL() { _uiState.update { it.copy(crossoverL = it.crossoverR) } }

    fun onYouTubePlayerStateChanged(state: Int) { setYouTubePlaying(state == 1) }
    fun onYouTubeTimeTick(currentTimeSeconds: Double) { youTubePlayheadTime = currentTimeSeconds }
    fun setYouTubePlaying(playing: Boolean) { _uiState.update { it.copy(isYouTubePlaying = playing) }; if (playing && _uiState.value.isSignalGeneratorPlaying) stopSignalGenerator() }
    fun selectYouTubeTrack(videoId: String, title: String) { _uiState.update { it.copy(currentYouTubeVideoId = videoId, currentYouTubeTitle = title, isYouTubePlaying = true) }; if (_uiState.value.isSignalGeneratorPlaying) stopSignalGenerator() }
    fun toggleSignalGenerator(type: SignalType) { val p = _uiState.value.isSignalGeneratorPlaying; val same = _uiState.value.signalGeneratorType == type; if (p && same) stopSignalGenerator() else { signalGenerator.start(type, 0.4f); _uiState.update { it.copy(isSignalGeneratorPlaying = true, signalGeneratorType = type, isYouTubePlaying = false) } } }
    fun stopSignalGenerator() { signalGenerator.stop(); _uiState.update { it.copy(isSignalGeneratorPlaying = false) } }

    fun saveCurrentAsPreset(name: String, description: String) { viewModelScope.launch(Dispatchers.IO) { val s = _uiState.value; repository.savePreset(AudioPresetEntity(name = name.ifBlank { "Preset ${System.currentTimeMillis() % 1000}" }, description = description, isFactory = false, eqGainsL = s.channelL.eqGains.joinToString(","), eqGainsR = s.channelR.eqGains.joinToString(","), hpfEnabled = s.crossover.hpfEnabled, hpfFrequency = s.crossover.hpfFrequency, hpfSlope = s.crossover.hpfSlope.name, lpfEnabled = s.crossover.lpfEnabled, lpfFrequency = s.crossover.lpfFrequency, lpfSlope = s.crossover.lpfSlope.name, gainL = s.channelL.gainDb, gainR = s.channelR.gainDb, delayL = s.channelL.delayMs, delayR = s.channelR.delayMs, muteL = s.channelL.isMuted, muteR = s.channelR.isMuted, phaseInvertL = s.channelL.isPhaseInverted, phaseInvertR = s.channelR.isPhaseInverted).also { _uiState.update { x -> x.copy(currentPresetName = it.name) } }) } }
    fun loadPreset(preset: AudioPresetEntity) {
        val l = preset.eqGainsL.split(",").mapNotNull { it.toFloatOrNull() }.let { if (it.size == 31) it else List(31) { 0f } }
        val r = preset.eqGainsR.split(",").mapNotNull { it.toFloatOrNull() }.let { if (it.size == 31) it else List(31) { 0f } }
        val hs = runCatching { CrossoverSlope.valueOf(preset.hpfSlope) }.getOrDefault(CrossoverSlope.LR_24)
        val ls = runCatching { CrossoverSlope.valueOf(preset.lpfSlope) }.getOrDefault(CrossoverSlope.LR_24)
        _uiState.update { s -> s.copy(channelL = s.channelL.copy(gainDb = preset.gainL, isMuted = preset.muteL, isPhaseInverted = preset.phaseInvertL, delayMs = preset.delayL, eqGains = l), channelR = s.channelR.copy(gainDb = preset.gainR, isMuted = preset.muteR, isPhaseInverted = preset.phaseInvertR, delayMs = preset.delayR, eqGains = r), crossover = s.crossover.copy(hpfEnabled = preset.hpfEnabled, hpfFrequency = preset.hpfFrequency, hpfSlope = hs, lpfEnabled = preset.lpfEnabled, lpfFrequency = preset.lpfFrequency, lpfSlope = ls), currentPresetName = preset.name) }
    }
    fun deletePreset(preset: AudioPresetEntity) { viewModelScope.launch(Dispatchers.IO) { repository.deletePreset(preset.id) } }

    private fun startSpectrumAnimationLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            var simStep = 0.0; val levels = FloatArray(31); val peaks = FloatArray(31); val holds = IntArray(31)
            while (isActive) {
                simStep += 0.04; val s = _uiState.value; val playing = s.isYouTubePlaying || s.isSignalGeneratorPlaying
                val hw = (System.currentTimeMillis() - lastFftTimestamp) < 400 && lastHardwareFft != null; val fft = lastHardwareFft
                val gains = when (s.activeChannel) { ChannelSelect.LEFT -> s.channelL.eqGains; ChannelSelect.RIGHT -> s.channelR.eqGains; ChannelSelect.LINKED -> s.channelL.eqGains.zip(s.channelR.eqGains) { a,b -> (a+b)/2f } }
                val cross = when (s.crossoverChannel) { ChannelSelect.LEFT -> s.crossoverL; ChannelSelect.RIGHT -> s.crossoverR; ChannelSelect.LINKED -> s.crossoverL }
                val gm = if (s.channelL.isMuted && s.channelR.isMuted) 0f else 10f.pow(maxOf(s.channelL.gainDb, s.channelR.gainDb) / 20f).coerceIn(0.05f, 2.5f)
                if (!playing) { for (i in 0 until 31) { levels[i] *= .70f; peaks[i] *= .80f; holds[i]=0 }; _spectrumLevels.value=levels.toList(); _peakLevels.value=peaks.toList(); _vuLevelL.value=-60f; _vuLevelR.value=-60f; delay(35); continue }
                for (i in 0 until 31) { val f=ISO_31_FREQUENCIES[i]; var x=gains.getOrElse(i){0f}; if(cross.hpfEnabled && f<cross.hpfFrequency) x -= log10(cross.hpfFrequency/f)/log10(2.0).toFloat()*cross.hpfSlope.rollOffDb; if(cross.lpfEnabled && f>cross.lpfFrequency) x -= log10(f/cross.lpfFrequency)/log10(2.0).toFloat()*cross.lpfSlope.rollOffDb; val ff=10f.pow(x.coerceIn(-48f,15f)/20f); val target=if(hw&&fft!=null)(fft.getOrElse(i){0f}*ff*gm).coerceIn(0f,1f) else (0.25f+0.45f*sin(simStep*4+i).toFloat().absoluteValue)*ff*gm; levels[i] += (target-levels[i])*(if(target>levels[i]).70f else .22f); if(levels[i]>peaks[i]){peaks[i]=levels[i];holds[i]=10}else if(holds[i]>0)holds[i]--else peaks[i]=(peaks[i]-.03f).coerceAtLeast(levels[i]) }
                _spectrumLevels.value=levels.toList(); _peakLevels.value=peaks.toList(); _vuLevelL.value=if(s.channelL.isMuted)-60f else (if(hw&&lastHardwareVuL!=null)lastHardwareVuL!! else levels.take(16).average().toFloat()*42f-28f)+s.channelL.gainDb; _vuLevelR.value=if(s.channelR.isMuted)-60f else (if(hw&&lastHardwareVuR!=null)lastHardwareVuR!! else levels.takeLast(16).average().toFloat()*42f-28f)+s.channelR.gainDb; delay(35)
            }
        }
    }
}
