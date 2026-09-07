package com.example.audio

val DspSettingsStore.CrossoverSnapshot.hpfEnabled: Boolean get() = hpf
val DspSettingsStore.CrossoverSnapshot.lpfEnabled: Boolean get() = lpf
val DspSettingsStore.CrossoverSnapshot.hpfFrequency: Float get() = hpfFreq
val DspSettingsStore.CrossoverSnapshot.lpfFrequency: Float get() = lpfFreq
