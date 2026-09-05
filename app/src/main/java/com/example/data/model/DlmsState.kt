package com.example.data.model

/**
 * Standard ISO 1/3-octave 31 frequencies (in Hz) used by professional 31-band graphic equalizers.
 */
val ISO_31_FREQUENCIES = listOf(
    20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f,
    125f, 160f, 200f, 250f, 315f, 400f, 500f, 630f,
    800f, 1000f, 1250f, 1600f, 2000f, 2500f, 3150f, 4000f,
    5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 20000f
)

fun formatFrequency(freq: Float): String {
    return if (freq >= 1000f) {
        val k = freq / 1000f
        if (k == k.toInt().toFloat()) "${k.toInt()}k" else "${k}k"
    } else {
        if (freq == freq.toInt().toFloat()) "${freq.toInt()}" else "$freq"
    }
}

enum class CrossoverSlope(val label: String, val order: Int, val rollOffDb: Float) {
    BYPASS("Bypass", 0, 0f),
    BW_12("12 dB/oct (BW)", 2, 12f),
    LR_24("24 dB/oct (LR)", 4, 24f),
    LR_48("48 dB/oct (LR)", 8, 48f)
}

enum class ChannelSelect {
    LEFT, RIGHT, LINKED
}

data class ChannelAudioSettings(
    val gainDb: Float = 0f, // -60 to +12 dB
    val isMuted: Boolean = false,
    val isPhaseInverted: Boolean = false, // 0 deg or 180 deg
    val delayMs: Float = 0f, // 0 to 100 ms
    val eqGains: List<Float> = List(31) { 0f } // 31 bands, -12 to +12 dB
) {
    val delayDistanceMeters: Float
        get() = (delayMs * 0.343f) // Speed of sound ~343 m/s => 0.343 m/ms

    val delayDistanceFeet: Float
        get() = delayDistanceMeters * 3.28084f
}

data class CrossoverSettings(
    val hpfEnabled: Boolean = true,
    val hpfFrequency: Float = 35f, // 20 Hz to 10 kHz
    val hpfSlope: CrossoverSlope = CrossoverSlope.LR_24,

    val lpfEnabled: Boolean = false,
    val lpfFrequency: Float = 18000f, // 100 Hz to 20 kHz
    val lpfSlope: CrossoverSlope = CrossoverSlope.LR_24
)

data class DlmsUiState(
    val channelL: ChannelAudioSettings = ChannelAudioSettings(),
    val channelR: ChannelAudioSettings = ChannelAudioSettings(),
    val crossover: CrossoverSettings = CrossoverSettings(),
    val activeChannel: ChannelSelect = ChannelSelect.LINKED,
    val masterGainDb: Float = 0f,
    val isMasterMuted: Boolean = false,
    val currentPresetName: String = "Default Flat",
    val isSignalGeneratorPlaying: Boolean = false,
    val signalGeneratorType: SignalType = SignalType.SINE_1KHZ,
    val currentYouTubeVideoId: String = "5qap5aO4i9A", // Default Lofi/Chill audio test stream
    val currentYouTubeTitle: String = "Lofi Hip Hop Radio - Beats to relax/study to",
    val isYouTubePlaying: Boolean = false
)

enum class SignalType(val label: String) {
    SINE_1KHZ("Sine 1 kHz"),
    SINE_40HZ("Sub Sine 40 Hz"),
    SINE_100HZ("Kick 100 Hz"),
    PINK_NOISE("Pink Noise"),
    SWEEP_20_20K("Sine Sweep 20Hz-20kHz")
}
