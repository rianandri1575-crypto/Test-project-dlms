package com.example.audio

/** Compatibility aliases for the older DSP engine property names. */
val DspSettingsStore.CrossoverSnapshot.hpf: Boolean get() = hpfEnabled
val DspSettingsStore.CrossoverSnapshot.lpf: Boolean get() = lpfEnabled
val DspSettingsStore.CrossoverSnapshot.hpfFreq: Float get() = hpfFrequency
val DspSettingsStore.CrossoverSnapshot.lpfFreq: Float get() = lpfFrequency
