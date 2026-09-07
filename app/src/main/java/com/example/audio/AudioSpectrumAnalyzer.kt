package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Live output analyzer backed by Android Visualizer.
 * Visualizer exposes a mono mixed waveform, so the analyzer never invents
 * separate L/R waveform samples. FFT bins are mapped to the real ISO 31-band
 * center frequencies using the callback sample rate.
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

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun start(): Boolean {
        if (!hasPermission()) {
            Log.d(TAG, "RECORD_AUDIO not granted; live Visualizer standby.")
            return false
        }

        return try {
            stop()
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = if (range.size >= 2) range[1].coerceAtMost(2048) else 1024
            val vis = Visualizer(0)
            vis.captureSize = captureSize
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    v: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    if (waveform.isNullOrEmpty()) return

                    // Android Visualizer waveform is a mixed/mono signal. Do not
                    // split the byte array in half and pretend it is stereo.
                    var sumSquares = 0.0
                    for (sampleByte in waveform) {
                        val sample = (sampleByte.toInt() and 0xFF) - 128
                        sumSquares += sample * sample
                    }
                    val rms = (sqrt(sumSquares / waveform.size) / 128.0)
                        .coerceIn(1e-5, 1.0)
                    val db = (20.0 * log10(rms)).toFloat().coerceIn(-72f, 0f)

                    // Visualizer cannot provide true channel-separated RMS.
                    // Report the actual mixed-output level equally rather than
                    // generating a fake stereo difference.
                    onVuUpdate(db, db)
                }

                override fun onFftDataCapture(
                    v: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                    if (fft == null || fft.size < 32) return
                    val fftSize = fft.size
                    val complexBins = fftSize / 2
                    val nyquistHz = (samplingRate / 1000f) / 2f
                    if (nyquistHz <= 0f) return

                    val bands = FloatArray(31)
                    val centers = ISO_31_FREQUENCIES

                    for (band in centers.indices) {
                        val center = centers[band]
                        val low = if (band == 0) 20f
                        else sqrt(centers[band - 1] * center)
                        val high = if (band == centers.lastIndex) 20000f
                        else sqrt(center * centers[band + 1])

                        val startBin = ((low / nyquistHz) * (complexBins - 1))
                            .toInt().coerceIn(1, complexBins - 1)
                        val endBin = ((high / nyquistHz) * (complexBins - 1))
                            .toInt().coerceIn(startBin, complexBins - 1)

                        var energy = 0.0
                        var count = 0
                        for (k in startBin..endBin) {
                            val real = fft[2 * k].toDouble()
                            val imag = fft[2 * k + 1].toDouble()
                            energy += real * real + imag * imag
                            count++
                        }

                        val rmsMagnitude = if (count > 0) sqrt(energy / count) else 0.0
                        // Visualizer FFT magnitude is byte-scaled rather than a
                        // calibrated dBFS meter. Convert to a stable dB-like
                        // display range and clamp to the actual analyzer floor.
                        val db = (20.0 * log10((rmsMagnitude / 128.0).coerceAtLeast(1e-6)))
                            .toFloat()
                            .coerceIn(-72f, 0f)
                        bands[band] = ((db + 72f) / 72f).coerceIn(0f, 1f)
                    }

                    onFftUpdate(bands)
                }
            }, Visualizer.getMaxCaptureRate(), true, true)

            vis.enabled = true
            visualizer = vis
            isEnabled = true
            Log.i(TAG, "Hardware Audio Visualizer attached on session 0.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize Visualizer(0): ${e.message}")
            isEnabled = false
            false
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
