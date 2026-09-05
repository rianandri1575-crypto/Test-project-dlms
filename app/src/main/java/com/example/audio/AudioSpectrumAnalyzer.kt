package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Captures live hardware audio output FFT and waveform using Android Visualizer API.
 * Maps FFT bins directly to the 31 ISO standard graphic equalizer bands.
 */
class AudioSpectrumAnalyzer(
    private val context: Context,
    private val onFftUpdate: (FloatArray) -> Unit,
    private val onVuUpdate: (Float, Float) -> Unit
) {
    private var visualizer: Visualizer? = null
    @Volatile
    var isEnabled = false
        private set

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(): Boolean {
        if (!hasPermission()) {
            Log.d(TAG, "RECORD_AUDIO not granted; live Visualizer standby.")
            return false
        }

        try {
            stop()
            val captureRange = Visualizer.getCaptureSizeRange()
            val captureSize = if (captureRange != null && captureRange.size >= 2) {
                captureRange[1].coerceAtMost(1024)
            } else {
                512
            }

            val vis = Visualizer(0)
            vis.captureSize = captureSize
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    v: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    if (waveform == null || waveform.isEmpty()) return
                    val size = waveform.size
                    val half = size / 2

                    var sumSquareL = 0.0
                    for (i in 0 until half) {
                        val sample = (waveform[i].toInt() and 0xFF) - 128
                        sumSquareL += sample * sample
                    }
                    var sumSquareR = 0.0
                    for (i in half until size) {
                        val sample = (waveform[i].toInt() and 0xFF) - 128
                        sumSquareR += sample * sample
                    }

                    val rmsL = (sqrt(sumSquareL / max(1, half)) / 128.0).toFloat().coerceIn(0.001f, 1f)
                    val rmsR = (sqrt(sumSquareR / max(1, half)) / 128.0).toFloat().coerceIn(0.001f, 1f)

                    // Convert linear RMS to dB scale (-60 dB to +6 dB)
                    val dbL = (20.0 * kotlin.math.log10(rmsL.toDouble())).toFloat().coerceIn(-60f, 6f)
                    val dbR = (20.0 * kotlin.math.log10(rmsR.toDouble())).toFloat().coerceIn(-60f, 6f)

                    onVuUpdate(dbL, dbR)
                }

                override fun onFftDataCapture(
                    v: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                    if (fft == null || fft.size < 32) return
                    val numBins = fft.size / 2
                    val bands = FloatArray(31)

                    // Map the linear FFT bins into 31 ISO bands logarithmically
                    for (b in 0 until 31) {
                        val startFraction = b.toFloat() / 31f
                        val endFraction = (b + 1).toFloat() / 31f

                        // Logarithmic warping so low frequencies have dedicated bin mapping
                        val binStart = (startFraction * startFraction * (numBins - 1)).toInt().coerceIn(0, numBins - 1)
                        val binEnd = ((endFraction * endFraction * (numBins - 1)).toInt() + 1).coerceIn(binStart + 1, numBins)

                        var maxMag = 0f
                        for (k in binStart until binEnd) {
                            val r = fft[2 * k].toFloat()
                            val j = fft[2 * k + 1].toFloat()
                            val mag = hypot(r.toDouble(), j.toDouble()).toFloat()
                            if (mag > maxMag) maxMag = mag
                        }

                        // Normalize magnitude to 0.0f .. 1.0f
                        bands[b] = (maxMag / 96f).coerceIn(0f, 1f)
                    }

                    onFftUpdate(bands)
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)

            vis.enabled = true
            visualizer = vis
            isEnabled = true
            Log.i(TAG, "Hardware Audio Visualizer attached on session 0.")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize Visualizer(0): ${e.message}")
            isEnabled = false
            return false
        }
    }

    fun stop() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        isEnabled = false
    }

    companion object {
        private const val TAG = "AudioSpectrumAnalyzer"
    }
}
