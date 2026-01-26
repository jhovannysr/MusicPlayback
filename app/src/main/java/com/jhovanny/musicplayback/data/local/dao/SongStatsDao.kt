package com.jhovanny.musicplayback.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jhovanny.musicplayback.data.local.entities.SongStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for operations related to song playback statistics.
 *
 * This interface defines the contract for interacting with the `song_stats` table.
 * It provides methods to read, insert, and update song playback data.
 */
@Dao
interface SongStatsDao {
    /**
     * Retrieves all song statistics from the database as a reactive stream.
     *
     * @return A [Flow] that emits the complete list of [SongStatsEntity] whenever
     *         the data in the `song_stats` table changes.
     */
    @Query("SELECT * FROM song_stats")
    fun getAllStatsFlow(): Flow<List<SongStatsEntity>>

    /**
     * Retrieves all song statistics from the database in a single, one-shot operation.
     *
     * @return A `List<SongStatsEntity>` containing all current stats.
     */
    @Query("SELECT * FROM song_stats")
    suspend fun getAllStats(): List<SongStatsEntity>

    /**
     * Retrieves the statistics for a single song by its ID.
     *
     * @param songId The ID of the song to look up.
     * @return The [SongStatsEntity] for the given song, or `null` if no entry is found.
     */
    @Query("SELECT * FROM song_stats WHERE songId = :songId")
    suspend fun getStat(songId: Long): SongStatsEntity?

    /**
     * Inserts a new statistics entry or updates an existing one.
     *
     * If a `SongStatsEntity` with the same `songId` already exists, it will be
     * replaced with the new data.
     *
     * @param stats The [SongStatsEntity] object to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: SongStatsEntity)

    /**
     * Atomically increments the play count for a specific song by 1.
     *
     * This is an efficient way to update the count without needing to fetch the entity first.
     *
     * @param songId The ID of the song whose play count should be incremented.
     */
    @Query("UPDATE song_stats SET playCount = playCount + 1 WHERE songId = :songId")
    suspend fun incrementPlayCount(songId: Long)
}
