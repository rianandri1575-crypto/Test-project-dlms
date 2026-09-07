package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Android Visualizer fallback for non-DSP playback. The DSP path uses
 * LiveAudioMetrics directly from the captured stereo PCM stream.
 */
class AudioSpectrumAnalyzer(
    private val context: Context,
    private val onFftUpdate: (FloatArray) -> Unit,
    private val onVuUpdate: (Float, Float) -> Unit
) {
    private var visualizer: Visualizer? = null
    @Volatile var isEnabled = false
        private set

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun start(): Boolean {
        if (!hasPermission()) return false
        return try {
            stop()
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = if (range.size >= 2) range[1].coerceAtMost(2048) else 1024
            val vis = Visualizer(0)
            vis.captureSize = captureSize
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    val samples = waveform ?: return
                    if (samples.isEmpty()) return
                    var sumSquares = 0.0
                    for (sampleByte in samples) {
                        val sample = (sampleByte.toInt() and 0xFF) - 128
                        sumSquares += sample * sample
                    }
                    val rms = (sqrt(sumSquares / samples.size) / 128.0).coerceAtLeast(1e-5)
                    val db = (20.0 * log10(rms)).toFloat().coerceIn(-72f, 0f)
                    // Visualizer is a mixed signal; do not invent L/R separation.
                    onVuUpdate(db, db)
                }

                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    val data = fft ?: return
                    if (data.size < 32) return
                    val complexBins = data.size / 2
                    val nyquistHz = (samplingRate.toDouble() / 1000.0) / 2.0
                    if (nyquistHz <= 0.0) return
                    val bands = FloatArray(31)
                    for (band in ISO_31_FREQUENCIES.indices) {
                        val center = ISO_31_FREQUENCIES[band].toDouble()
                        val low = if (band == 0) 20.0 else sqrt(ISO_31_FREQUENCIES[band - 1].toDouble() * center)
                        val high = if (band == ISO_31_FREQUENCIES.lastIndex) 20000.0 else sqrt(center * ISO_31_FREQUENCIES[band + 1].toDouble())
                        val startBin = ((low / nyquistHz) * (complexBins - 1)).toInt().coerceIn(1, complexBins - 1)
                        val endBin = ((high / nyquistHz) * (complexBins - 1)).toInt().coerceIn(startBin, complexBins - 1)
                        var energy = 0.0
                        var count = 0
                        for (k in startBin..endBin) {
                            val real = data[2 * k].toDouble()
                            val imag = data[2 * k + 1].toDouble()
                            energy += real * real + imag * imag
                            count++
                        }
                        val magnitude = if (count > 0) sqrt(energy / count) else 0.0
                        val db = (20.0 * log10((magnitude / 128.0).coerceAtLeast(1e-6))).coerceIn(-72.0, 0.0)
                        bands[band] = ((db + 72.0) / 72.0).toFloat().coerceIn(0f, 1f)
                    }
                    onFftUpdate(bands)
                }
            }, Visualizer.getMaxCaptureRate(), true, true)
            vis.enabled = true
            visualizer = vis
            isEnabled = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize Visualizer: ${e.message}")
            isEnabled = false
            false
        }
    }

    fun stop() {
        try { visualizer?.enabled = false } catch (_: Exception) {}
        try { visualizer?.release() } catch (_: Exception) {}
        visualizer = null
        isEnabled = false
    }

    companion object {
        private const val TAG = "AudioSpectrumAnalyzer"
        val ISO_31_FREQUENCIES = floatArrayOf(
            20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f,
            200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
            2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 20000f
        )
    }
}
