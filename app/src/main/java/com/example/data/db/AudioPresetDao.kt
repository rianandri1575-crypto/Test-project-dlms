package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioPresetDao {
    @Query("SELECT * FROM audio_presets ORDER BY isFactory DESC, createdAt DESC")
    fun getAllPresets(): Flow<List<AudioPresetEntity>>

    @Query("SELECT * FROM audio_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Long): AudioPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: AudioPresetEntity): Long

    @Query("DELETE FROM audio_presets WHERE id = :id AND isFactory = 0")
    suspend fun deletePresetById(id: Long)

    @Query("SELECT COUNT(*) FROM audio_presets")
    suspend fun getCount(): Int
}
