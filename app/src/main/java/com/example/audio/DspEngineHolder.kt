package com.example.audio

/**
 * SATU-SATUNYA DSP engine untuk seluruh pipeline audio.
 *
 * Root cause bug sebelumnya: beberapa pipeline bisa memakai instance
 * DlmsDspEngine berbeda (capture service vs generator) sehingga setting EQ /
 * crossover / delay yang diubah user tidak terdengar di semua sumber.
 * Semua pemrosesan PCM16 stereo HARUS lewat [engine] ini.
 *
 * Engine 48 kHz allocation-free; ringan untuk Android rendah (API 24+).
 */
object DspEngineHolder {
    val engine: DlmsDspEngine = DlmsDspEngine(48_000)
}
