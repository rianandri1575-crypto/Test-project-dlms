package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Generator nada uji (sine / pink noise / sweep). Output-nya dirutekan lewat
 * SATU [DspEngineHolder.engine] + [LiveAudioMetrics] sehingga tombol DSP,
 * VU meter, dan spectrum selalu merefleksikan audio yang benar-benar berbunyi.
 */
class AudioSignalGenerator {

    private var audioTrack: AudioTrack? = null
    private var generatorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    // 48 kHz agar cocok dengan engine DSP + pipeline capture, tanpa resampling.
    private val sampleRate = 48_000
    private val random = Random()
    // SATU engine untuk semua pipeline — jangan pernah instantiate DlmsDspEngine lain.
    private val dsp = DspEngineHolder.engine

    @Volatile
    var isRunning = false
        private set

    /**
     * @param dspEnabled true = proses via engine DSP + dorong ke LiveAudioMetrics.
     * false = bypass murni (tetap dorong metering agar VU/spectrum hidup).
     * Default mengikuti SATU saklar global [DspSettingsStore.dspEnabled] agar
     * tombol DSP langsung terdengar tanpa argumen manual.
     */
    fun start(type: SignalType, gainLinear: Float = 0.5f, dspEnabled: Boolean = DspSettingsStore.dspEnabled) {
        stop()
        isRunning = true

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, sampleRate / 10)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        generatorJob = scope.launch {
            val chunk = ShortArray(2048)
            var phase = 0.0
            var sweepTime = 0.0

            // Pink noise filter state
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0

            while (isActive && isRunning) {
                for (i in 0 until chunk.size step 2) {
                    val sampleValue: Double = when (type) {
                        SignalType.SINE_1KHZ -> {
                            phase += 2.0 * PI * 1000.0 / sampleRate
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                            sin(phase)
                        }
                        SignalType.SINE_40HZ -> {
                            phase += 2.0 * PI * 40.0 / sampleRate
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                            sin(phase)
                        }
                        SignalType.SINE_100HZ -> {
                            phase += 2.0 * PI * 100.0 / sampleRate
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                            sin(phase)
                        }
                        SignalType.PINK_NOISE -> {
                            // Paul Kellet's filtered pink noise generator
                            val white = (random.nextDouble() * 2.0 - 1.0)
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
                            b6 = white * 0.115926
                            (pink * 0.11).coerceIn(-1.0, 1.0)
                        }
                        SignalType.SWEEP_20_20K -> {
                            // 5-second logarithmic sine sweep from 20 Hz to 20,000 Hz
                            val duration = 5.0
                            val progress = (sweepTime % duration) / duration
                            val currentFreq = 20.0 * (20000.0 / 20.0).pow(progress)
                            phase += 2.0 * PI * currentFreq / sampleRate
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                            sweepTime += 1.0 / sampleRate
                            sin(phase)
                        }
                    }

                    val shortVal = (sampleValue * gainLinear * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                    chunk[i] = shortVal     // Left
                    chunk[i + 1] = shortVal // Right
                }

                // Tombol DSP harus benar-benar terdengar: pakai SATU engine yang
                // sama dengan pipeline capture; metering memakai PCM yang sama
                // persis dengan yang dikirim ke AudioTrack (bukan fake/random).
                // Cek ulang flag global per-chunk agar toggle saat musik sedang
                // diputar langsung terdengar (penting di Android rendah).
                if (dspEnabled && DspSettingsStore.dspEnabled) {
                    dsp.processPcm16Stereo(chunk, DspSettingsStore.readCached())
                }
                LiveAudioMetrics.setActive(true)
                LiveAudioMetrics.pushPcm16Stereo(chunk, sampleRate)

                try {
                    track.write(chunk, 0, chunk.size)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        generatorJob?.cancel()
        generatorJob = null
        LiveAudioMetrics.setActive(false)
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
