package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Allocation-free stereo PCM16 DSP core. */
class DlmsDspEngine(private val sampleRate: Int = 48_000) {
    private val eqL = Array(31) { Biquad() }
    private val eqR = Array(31) { Biquad() }
    private val hpfL = Array(4) { Biquad() }
    private val hpfR = Array(4) { Biquad() }
    private val lpfL = Array(4) { Biquad() }
    private val lpfR = Array(4) { Biquad() }
    private val maxDelaySamples = sampleRate / 10
    private val delayL = FloatArray(maxDelaySamples + 1)
    private val delayR = FloatArray(maxDelaySamples + 1)
    private var delayIndex = 0

    fun processPcm16Stereo(data: ShortArray, settings: DspSettingsStore.Snapshot) {
        configure(settings)
        val dl = (settings.delayL * sampleRate / 1000f).toInt().coerceIn(0, maxDelaySamples)
        val dr = (settings.delayR * sampleRate / 1000f).toInt().coerceIn(0, maxDelaySamples)
        val gainL = 10f.pow(settings.gainL / 20f)
        val gainR = 10f.pow(settings.gainR / 20f)
        var i = 0
        while (i + 1 < data.size) {
            var l = data[i] / 32768f
            var r = data[i + 1] / 32768f
            if (settings.muteL) l = 0f
            if (settings.muteR) r = 0f
            l = applyChain(l, eqL, hpfL, lpfL, settings.xL)
            r = applyChain(r, eqR, hpfR, lpfR, settings.xR)
            delayL[delayIndex] = l
            delayR[delayIndex] = r
            l = delayL[(delayIndex - dl + delayL.size) % delayL.size]
            r = delayR[(delayIndex - dr + delayR.size) % delayR.size]
            delayIndex = (delayIndex + 1) % delayL.size
            if (settings.phaseL) l = -l
            if (settings.phaseR) r = -r
            l *= gainL
            r *= gainR
            data[i] = (l.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            data[i + 1] = (r.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            i += 2
        }
    }

    private fun applyChain(input: Float, eq: Array<Biquad>, highPass: Array<Biquad>, lowPass: Array<Biquad>, crossover: DspSettingsStore.CrossoverSnapshot): Float {
        var value = input
        for (filter in eq) value = filter.process(value)
        if (crossover.highPassEnabled) for (filter in highPass) value = filter.process(value)
        if (crossover.lowPassEnabled) for (filter in lowPass) value = filter.process(value)
        return value
    }

    private fun configure(s: DspSettingsStore.Snapshot) {
        for (i in 0 until 31) {
            eqL[i].setPeaking(sampleRate.toFloat(), ISO_FREQS[i], 1f, s.eqL[i])
            eqR[i].setPeaking(sampleRate.toFloat(), ISO_FREQS[i], 1f, s.eqR[i])
        }
        configureCrossover(hpfL, s.xL.highPassFrequency, true)
        configureCrossover(hpfR, s.xR.highPassFrequency, true)
        configureCrossover(lpfL, s.xL.lowPassFrequency, false)
        configureCrossover(lpfR, s.xR.lowPassFrequency, false)
    }

    private fun configureCrossover(chain: Array<Biquad>, freq: Float, highPass: Boolean) {
        for (i in chain.indices) {
            val section = i / 2
            val q = if (section == 0) 0.5411961f else 1.306563f
            if (highPass) chain[i].setHighPass(sampleRate.toFloat(), freq, q)
            else chain[i].setLowPass(sampleRate.toFloat(), freq, q)
        }
    }

    private class Biquad {
        private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
        private var a1 = 0f; private var a2 = 0f
        private var z1 = 0f; private var z2 = 0f
        fun process(x: Float): Float {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }
        fun setPeaking(fs: Float, f: Float, q: Float, gainDb: Float) {
            val a = 10f.pow(gainDb / 40f)
            val w = 2f * PI.toFloat() * f / fs
            val alpha = sin(w) / (2f * q)
            val c = cos(w)
            val bb0 = 1f + alpha * a
            val bb1 = -2f * c
            val bb2 = 1f - alpha * a
            val aa0 = 1f + alpha / a
            val aa1 = -2f * c
            val aa2 = 1f - alpha / a
            set(bb0 / aa0, bb1 / aa0, bb2 / aa0, aa1 / aa0, aa2 / aa0)
        }
        fun setHighPass(fs: Float, f: Float, q: Float) {
            val w = 2f * PI.toFloat() * f / fs
            val alpha = sin(w) / (2f * q)
            val c = cos(w)
            val aa0 = 1f + alpha
            set((1f + c) / 2f / aa0, -(1f + c) / aa0, (1f + c) / 2f / aa0, -2f * c / aa0, (1f - alpha) / aa0)
        }
        fun setLowPass(fs: Float, f: Float, q: Float) {
            val w = 2f * PI.toFloat() * f / fs
            val alpha = sin(w) / (2f * q)
            val c = cos(w)
            val aa0 = 1f + alpha
            set((1f - c) / 2f / aa0, (1f - c) / aa0, (1f - c) / 2f / aa0, -2f * c / aa0, (1f - alpha) / aa0)
        }
        private fun set(nb0: Float, nb1: Float, nb2: Float, na1: Float, na2: Float) {
            b0 = nb0; b1 = nb1; b2 = nb2; a1 = na1; a2 = na2
        }
    }

    companion object {
        private val ISO_FREQS = floatArrayOf(20f,25f,31.5f,40f,50f,63f,80f,100f,125f,160f,200f,250f,315f,400f,500f,630f,800f,1000f,1250f,1600f,2000f,2500f,3150f,4000f,5000f,6300f,8000f,10000f,12500f,16000f,20000f)
    }
}
