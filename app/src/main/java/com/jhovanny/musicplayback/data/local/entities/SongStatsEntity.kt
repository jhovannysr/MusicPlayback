package com.jhovanny.musicplayback.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the schema for the `song_stats` table in the local Room database.
 *
 * Each instance of this class corresponds to a single row, tracking playback
 * statistics for a specific song.
 *
 * @property songId The unique identifier for the song, which serves as the primary key.
 *                  This ID should match the song's original ID from the Android MediaStore.
 * @property playCount The total number of times the song has been played. Defaults to 1 on creation.
 * @property lastPlayedTimestamp The timestamp (in milliseconds) of the last time the song was played.
 *                               This is useful for features like "Recently Played".
 */
@Entity(tableName = "song_stats")
data class SongStatsEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int = 1,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)
