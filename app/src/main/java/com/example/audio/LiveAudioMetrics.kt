package com.example.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/** Real-time metrics calculated from the actual PCM stream used by the DSP pipeline. */
object LiveAudioMetrics {
    private const val FFT_SIZE = 1024
    private const val MIN_DB = -72f

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _spectrum = MutableStateFlow(List(31) { 0f })
    val spectrum: StateFlow<List<Float>> = _spectrum.asStateFlow()

    private val _leftDb = MutableStateFlow(MIN_DB)
    val leftDb: StateFlow<Float> = _leftDb.asStateFlow()

    private val _rightDb = MutableStateFlow(MIN_DB)
    val rightDb: StateFlow<Float> = _rightDb.asStateFlow()

    private val window = FloatArray(FFT_SIZE) { i ->
        (0.5 - 0.5 * cos(2.0 * Math.PI * i / (FFT_SIZE - 1))).toFloat()
    }
    private val real = DoubleArray(FFT_SIZE)
    private val imag = DoubleArray(FFT_SIZE)
    private val centers = floatArrayOf(
        20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f,
        200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
        2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 20000f
    )

    fun setActive(value: Boolean) {
        _active.value = value
        if (!value) clear()
    }

    fun pushPcm16Stereo(pcm: ShortArray, sampleRate: Int) {
        if (pcm.size < 4 || sampleRate <= 0) return
        val frames = minOf(pcm.size / 2, FFT_SIZE)
        if (frames < 16) return

        var sumL = 0.0
        var sumR = 0.0
        for (i in 0 until frames) {
            val l = pcm[i * 2].toDouble() / 32768.0
            val r = pcm[i * 2 + 1].toDouble() / 32768.0
            sumL += l * l
            sumR += r * r
            val mono = ((l + r) * 0.5) * window[i]
            real[i] = mono
            imag[i] = 0.0
        }
        for (i in frames until FFT_SIZE) {
            real[i] = 0.0
            imag[i] = 0.0
        }

        _leftDb.value = rmsToDb(sqrt(sumL / frames))
        _rightDb.value = rmsToDb(sqrt(sumR / frames))

        fft(real, imag)
        val binHz = sampleRate.toDouble() / FFT_SIZE
        val values = FloatArray(31)
        for (b in centers.indices) {
            val low = if (b == 0) 20.0 else sqrt(centers[b - 1].toDouble() * centers[b])
            val high = if (b == centers.lastIndex) 20000.0 else sqrt(centers[b].toDouble() * centers[b + 1])
            val first = (low / binHz).toInt().coerceIn(1, FFT_SIZE / 2 - 1)
            val last = (high / binHz).toInt().coerceIn(first, FFT_SIZE / 2 - 1)
            var energy = 0.0
            var count = 0
            for (k in first..last) {
                energy += real[k] * real[k] + imag[k] * imag[k]
                count++
            }
            // Convert band energy to an RMS-like full-scale value.
            val bandRms = if (count > 0) sqrt(energy / count) * 2.0 / FFT_SIZE
            else 0.0
            val db = rmsToDb(bandRms)
            values[b] = ((db + 72f) / 72f).coerceIn(0f, 1f)
        }

        // Fast attack + natural release, avoiding a jittery digital wall.
        val previous = _spectrum.value
        _spectrum.value = values.mapIndexed { i, target ->
            val old = previous.getOrElse(i) { 0f }
            old + (target - old) * if (target > old) 0.65f else 0.18f
        }
    }

    private fun rmsToDb(rms: Double): Float =
        (20.0 * log10(rms.coerceAtLeast(1e-7))).toFloat().coerceIn(MIN_DB, 0f)

    private fun fft(re: DoubleArray, im: DoubleArray) {
        var j = 0
        for (i in 1 until FFT_SIZE) {
            var bit = FFT_SIZE shr 1
            while ((j and bit) != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= FFT_SIZE) {
            val angle = -2.0 * Math.PI / len
            val wLenR = cos(angle)
            val wLenI = sin(angle)
            var i = 0
            while (i < FFT_SIZE) {
                var wr = 1.0
                var wi = 0.0
                for (k in 0 until len / 2) {
                    val uR = re[i + k]
                    val uI = im[i + k]
                    val vR = re[i + k + len / 2] * wr - im[i + k + len / 2] * wi
                    val vI = re[i + k + len / 2] * wi + im[i + k + len / 2] * wr
                    re[i + k] = uR + vR
                    im[i + k] = uI + vI
                    re[i + k + len / 2] = uR - vR
                    im[i + k + len / 2] = uI - vI
                    val nextWr = wr * wLenR - wi * wLenI
                    wi = wr * wLenI + wi * wLenR
                    wr = nextWr
                }
                i += len
            }
            len = len shl 1
        }
    }

    fun clear() {
        _spectrum.value = List(31) { 0f }
        _leftDb.value = MIN_DB
        _rightDb.value = MIN_DB
    }
}
