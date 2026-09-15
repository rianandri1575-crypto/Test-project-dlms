package com.example.audio

import android.content.Context
import com.example.data.model.ChannelAudioSettings
import com.example.data.model.CrossoverSettings
import com.example.data.model.DlmsUiState
import org.json.JSONArray
import org.json.JSONObject

object DspSettingsStore {
    private const val PREFS = "youtube_dsp_runtime"
    private const val KEY_SETTINGS = "settings"

    /** In-memory cache so the realtime audio thread never does disk I/O per buffer. */
    @Volatile private var cached: Snapshot = Snapshot.default()

    /**
     * SATU saklar DSP global. true = semua pipeline yang berbunyi WAJIB lewat
     * [DspEngineHolder.engine]; false = bypass murni (meter tetap jalan).
     * Default ON agar tombol DSP benar-benar mengaktifkan pemrosesan audio
     * saat musik diputar, juga di Android rendah (API 24-28) tanpa capture.
     */
    @Volatile var dspEnabled: Boolean = true

    /** Realtime-safe snapshot for the audio thread (no disk I/O, no Context). */
    fun readCached(): Snapshot = cached

    fun publish(snapshot: Snapshot) {
        cached = snapshot
    }

    /** Realtime-safe: efektif FLAT (bypass) saat saklar DSP mati. */
    fun readCachedEffective(): Snapshot =
        if (dspEnabled) cached else Snapshot.default()

    fun snapshotFromState(state: DlmsUiState): Snapshot = Snapshot(
        eqL = state.channelL.eqGains.toFloatArray(),
        eqR = state.channelR.eqGains.toFloatArray(),
        gainL = state.channelL.gainDb,
        gainR = state.channelR.gainDb,
        muteL = state.channelL.isMuted,
        muteR = state.channelR.isMuted,
        phaseL = state.channelL.isPhaseInverted,
        phaseR = state.channelR.isPhaseInverted,
        delayL = state.channelL.delayMs,
        delayR = state.channelR.delayMs,
        xL = snapshotOf(state.crossoverL),
        xR = snapshotOf(state.crossoverR),
    )

    private fun snapshotOf(c: CrossoverSettings): CrossoverSnapshot = CrossoverSnapshot(
        highPassEnabled = c.hpfEnabled,
        highPassFrequency = c.hpfFrequency,
        highPassSlopeDb = c.hpfSlope.rollOffDb,
        lowPassEnabled = c.lpfEnabled,
        lowPassFrequency = c.lpfFrequency,
        lowPassSlopeDb = c.lpfSlope.rollOffDb,
    )

    fun write(context: Context, state: DlmsUiState) {
        publish(snapshotFromState(state))
        // Sinkron SATU flag DSP agar thread audio langsung merasakan toggle.
        dspEnabled = state.isDspEnabled
        persist(context, state)
    }

    private fun persist(context: Context, state: DlmsUiState) {
        try {
            val json = JSONObject()
            json.put("eqL", JSONArray(state.channelL.eqGains))
            json.put("eqR", JSONArray(state.channelR.eqGains))
            putChannel(json, "l", state.channelL)
            putChannel(json, "r", state.channelR)
            putCrossover(json, "xL", state.crossoverL)
            putCrossover(json, "xR", state.crossoverR)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_SETTINGS, json.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun read(context: Context): Snapshot {
        val raw = try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SETTINGS, null)
        } catch (_: Exception) { null } ?: return Snapshot.default()
        return try {
            val j = JSONObject(raw)
            Snapshot(
                eqL = readArray(j.optJSONArray("eqL")),
                eqR = readArray(j.optJSONArray("eqR")),
                gainL = j.optJSONObject("l")?.optDouble("gain", 0.0)?.toFloat() ?: 0f,
                gainR = j.optJSONObject("r")?.optDouble("gain", 0.0)?.toFloat() ?: 0f,
                muteL = j.optJSONObject("l")?.optBoolean("mute", false) ?: false,
                muteR = j.optJSONObject("r")?.optBoolean("mute", false) ?: false,
                phaseL = j.optJSONObject("l")?.optBoolean("phase", false) ?: false,
                phaseR = j.optJSONObject("r")?.optBoolean("phase", false) ?: false,
                delayL = j.optJSONObject("l")?.optDouble("delay", 0.0)?.toFloat() ?: 0f,
                delayR = j.optJSONObject("r")?.optDouble("delay", 0.0)?.toFloat() ?: 0f,
                xL = readCrossover(j.optJSONObject("xL")),
                xR = readCrossover(j.optJSONObject("xR"))
            )
        } catch (_: Exception) { Snapshot.default() }
    }

    /** Load persisted settings into the realtime cache (call once at startup). */
    fun preload(context: Context) {
        try { cached = read(context) } catch (_: Exception) { }
    }

    private fun putChannel(j: JSONObject, key: String, c: ChannelAudioSettings) {
        j.put(key, JSONObject().apply {
            put("gain", c.gainDb); put("mute", c.isMuted); put("phase", c.isPhaseInverted); put("delay", c.delayMs)
        })
    }

    private fun putCrossover(j: JSONObject, key: String, c: CrossoverSettings) {
        j.put(key, JSONObject().apply {
            put("hpf", c.hpfEnabled); put("hpfFreq", c.hpfFrequency); put("hpfSlope", c.hpfSlope.rollOffDb)
            put("lpf", c.lpfEnabled); put("lpfFreq", c.lpfFrequency); put("lpfSlope", c.lpfSlope.rollOffDb)
        })
    }

    private fun readArray(a: JSONArray?): FloatArray {
        val out = FloatArray(31)
        if (a != null) for (i in 0 until minOf(31, a.length())) out[i] = a.optDouble(i, 0.0).toFloat()
        return out
    }

    private fun readCrossover(j: JSONObject?): CrossoverSnapshot = if (j == null) {
        CrossoverSnapshot(false, 80f, 0f, false, 16000f, 0f)
    } else CrossoverSnapshot(
        highPassEnabled = j.optBoolean("hpf", false), highPassFrequency = j.optDouble("hpfFreq", 80.0).toFloat(), highPassSlopeDb = j.optDouble("hpfSlope", 0.0).toFloat(),
        lowPassEnabled = j.optBoolean("lpf", false), lowPassFrequency = j.optDouble("lpfFreq", 16000.0).toFloat(), lowPassSlopeDb = j.optDouble("lpfSlope", 0.0).toFloat()
    )

    data class CrossoverSnapshot(
        val highPassEnabled: Boolean, val highPassFrequency: Float, val highPassSlopeDb: Float,
        val lowPassEnabled: Boolean, val lowPassFrequency: Float, val lowPassSlopeDb: Float
    )

    data class Snapshot(
        val eqL: FloatArray, val eqR: FloatArray, val gainL: Float, val gainR: Float,
        val muteL: Boolean, val muteR: Boolean, val phaseL: Boolean, val phaseR: Boolean,
        val delayL: Float, val delayR: Float, val xL: CrossoverSnapshot, val xR: CrossoverSnapshot
    ) {
        companion object {
            fun default() = Snapshot(
                FloatArray(31), FloatArray(31), 0f, 0f, false, false, false, false, 0f, 0f,
                CrossoverSnapshot(false, 80f, 0f, false, 16000f, 0f), CrossoverSnapshot(false, 80f, 0f, false, 16000f, 0f)
            )
        }
    }
}
