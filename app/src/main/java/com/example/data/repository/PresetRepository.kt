package com.example.data.repository

import com.example.data.db.AudioPresetDao
import com.example.data.db.AudioPresetEntity
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val dao: AudioPresetDao) {

    val allPresets: Flow<List<AudioPresetEntity>> = dao.getAllPresets()

    suspend fun savePreset(preset: AudioPresetEntity): Long {
        return dao.insertPreset(preset)
    }

    suspend fun deletePreset(id: Long) {
        dao.deletePresetById(id)
    }

    suspend fun ensureDefaultPresets() {
        if (dao.getCount() == 0) {
            val flatEq = List(31) { "0.0" }.joinToString(",")

            // Factory Preset 1: Flat Reference
            dao.insertPreset(
                AudioPresetEntity(
                    name = "Studio Flat Reference",
                    description = "Respons frekuensi murni 0 dB untuk kalibrasi monitor dan uji linearitas.",
                    isFactory = true,
                    eqGainsL = flatEq,
                    eqGainsR = flatEq,
                    hpfEnabled = true,
                    hpfFrequency = 30f,
                    hpfSlope = "LR_24",
                    lpfEnabled = false,
                    lpfFrequency = 20000f,
                    lpfSlope = "LR_24",
                    gainL = 0f,
                    gainR = 0f,
                    delayL = 0f,
                    delayR = 0f,
                    muteL = false,
                    muteR = false,
                    phaseInvertL = false,
                    phaseInvertR = false
                )
            )

            // Factory Preset 2: Live Concert / Sound System
            val liveEq = listOf(
                2.0, 3.5, 4.0, 4.5, 3.0, 2.0, 1.0, 0.0,
                0.0, -1.0, -1.5, -1.0, 0.0, 0.5, 1.0, 1.5,
                2.0, 2.5, 2.0, 1.5, 1.0, 1.5, 2.0, 2.5,
                3.0, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5
            ).joinToString(",") { it.toString() }

            dao.insertPreset(
                AudioPresetEntity(
                    name = "Live Stage / Outdoor",
                    description = "Optimal untuk speaker panggung outdoor, bass bertenaga & vokal menonjol.",
                    isFactory = true,
                    eqGainsL = liveEq,
                    eqGainsR = liveEq,
                    hpfEnabled = true,
                    hpfFrequency = 38f,
                    hpfSlope = "LR_48",
                    lpfEnabled = true,
                    lpfFrequency = 18500f,
                    lpfSlope = "LR_24",
                    gainL = 0f,
                    gainR = 0f,
                    delayL = 0f,
                    delayR = 0f,
                    muteL = false,
                    muteR = false,
                    phaseInvertL = false,
                    phaseInvertR = false
                )
            )

            // Factory Preset 3: Club DJ Bass Boost
            val clubEq = listOf(
                3.0, 5.0, 6.0, 5.5, 4.0, 3.0, 1.5, 0.5,
                0.0, 0.0, -1.0, -1.5, -1.0, 0.0, 0.0, 0.5,
                1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.0,
                4.5, 4.0, 3.0, 2.0, 1.5, 1.0, 0.0
            ).joinToString(",") { it.toString() }

            dao.insertPreset(
                AudioPresetEntity(
                    name = "Club DJ & Heavy Bass",
                    description = "Dorongan sub-bass 40-60Hz dengan treble berkilau untuk musik EDM dan dance.",
                    isFactory = true,
                    eqGainsL = clubEq,
                    eqGainsR = clubEq,
                    hpfEnabled = true,
                    hpfFrequency = 32f,
                    hpfSlope = "LR_48",
                    lpfEnabled = false,
                    lpfFrequency = 20000f,
                    lpfSlope = "LR_24",
                    gainL = 0f,
                    gainR = 0f,
                    delayL = 0f,
                    delayR = 0f,
                    muteL = false,
                    muteR = false,
                    phaseInvertL = false,
                    phaseInvertR = false
                )
            )

            // Factory Preset 4: Vocal & Speech Intelligibility
            val vocalEq = listOf(
                -8.0, -6.0, -4.0, -3.0, -2.0, -1.0, 0.0, 0.0,
                0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5,
                3.5, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0,
                -1.0, -1.5, -2.0, -3.0, -4.0, -5.0, -6.0
            ).joinToString(",") { it.toString() }

            dao.insertPreset(
                AudioPresetEntity(
                    name = "Vocal & Speech Clarity",
                    description = "Memotong gemuruh low-end dan menaikkan kejernihan vokal pada 1-4 kHz.",
                    isFactory = true,
                    eqGainsL = vocalEq,
                    eqGainsR = vocalEq,
                    hpfEnabled = true,
                    hpfFrequency = 75f,
                    hpfSlope = "BW_12",
                    lpfEnabled = true,
                    lpfFrequency = 14000f,
                    lpfSlope = "BW_12",
                    gainL = 0f,
                    gainR = 0f,
                    delayL = 0f,
                    delayR = 0f,
                    muteL = false,
                    muteR = false,
                    phaseInvertL = false,
                    phaseInvertR = false
                )
            )

            // Factory Preset 5: Subwoofer Crossover 100Hz
            val subCutEq = List(31) { 0.0 }.joinToString(",")
            dao.insertPreset(
                AudioPresetEntity(
                    name = "Subwoofer Protection (35Hz-120Hz)",
                    description = "Crossover HPF 35Hz & LPF 120Hz khusus kanal speaker Subwoofer.",
                    isFactory = true,
                    eqGainsL = subCutEq,
                    eqGainsR = subCutEq,
                    hpfEnabled = true,
                    hpfFrequency = 35f,
                    hpfSlope = "LR_48",
                    lpfEnabled = true,
                    lpfFrequency = 120f,
                    lpfSlope = "LR_48",
                    gainL = 1.5f,
                    gainR = 1.5f,
                    delayL = 2.5f,
                    delayR = 2.5f,
                    muteL = false,
                    muteR = false,
                    phaseInvertL = false,
                    phaseInvertR = false
                )
            )
        }
    }
}
