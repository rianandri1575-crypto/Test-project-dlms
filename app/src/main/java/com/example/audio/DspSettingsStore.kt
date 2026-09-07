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

    fun write(context: Context, state: DlmsUiState) {
        val json = JSONObject()
        json.put("eqL", JSONArray(state.channelL.eqGains))
        json.put("eqR", JSONArray(state.channelR.eqGains))
        putChannel(json, "l", state.channelL)
        putChannel(json, "r", state.channelR)
        putCrossover(json, "xL", state.crossoverL)
        putCrossover(json, "xR", state.crossoverR)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SETTINGS, json.toString()).apply()
    }

    fun read(context: Context): Snapshot {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SETTINGS, null) ?: return Snapshot.default()
        return try {
            val j = JSONObject(raw)
            Snapshot(
                eqL = readArray(j.optJSONArray("eqL")), eqR = readArray(j.optJSONArray("eqR")),
                gainL = j.optJSONObject("l")?.optDouble("gain", 0.0)?.toFloat() ?: 0f,
                gainR = j.optJSONObject("r")?.optDouble("gain", 0.0)?.toFloat() ?: 0f,
                muteL = j.optJSONObject("l")?.optBoolean("mute", false) ?: false,
                muteR = j.optJSONObject("r")?.optBoolean("mute", false) ?: false,
                phaseL = j.optJSONObject("l")?.optBoolean("phase", false) ?: false,
                phaseR = j.optJSONObject("r")?.optBoolean("phase", false) ?: false,
                delayL = j.optJSONObject("l")?.optDouble("delay", 0.0)?.toFloat() ?: 0f,
                delayR = j.optJSONObject("r")?.optDouble("delay", 0.0)?.toFloat() ?: 0f,
                xL = readCrossover(j.optJSONObject("xL")), xR = readCrossover(j.optJSONObject("xR"))
            )
        } catch (_: Exception) { Snapshot.default() }
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
