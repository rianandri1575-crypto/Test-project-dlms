package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_presets")
data class AudioPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val isFactory: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),

    // Left channel EQ (comma-separated 31 floats)
    val eqGainsL: String,
    // Right channel EQ (comma-separated 31 floats)
    val eqGainsR: String,

    // Crossover
    val hpfEnabled: Boolean,
    val hpfFrequency: Float,
    val hpfSlope: String,

    val lpfEnabled: Boolean,
    val lpfFrequency: Float,
    val lpfSlope: String,

    // Gains & Delay
    val gainL: Float,
    val gainR: Float,
    val delayL: Float,
    val delayR: Float,
    val muteL: Boolean,
    val muteR: Boolean,
    val phaseInvertL: Boolean,
    val phaseInvertR: Boolean
)
