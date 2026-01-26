package com.jhovanny.musicplayback.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jhovanny.musicplayback.data.local.entities.PlaylistEntity
import com.jhovanny.musicplayback.data.local.entities.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for all operations related to playlists.
 *
 * This interface defines the contract for interacting with the `playlists` table
 * and the `playlist_song_cross_ref` join table. It uses [Flow] for reactive
 * data observation and `suspend` functions for non-blocking, background execution.
 */
@Dao
interface PlaylistDao {

    // --- Playlist Entity Operations ---

    /**
     * Retrieves all playlists from the database, ordered by their ID.
     * @return A [Flow] that emits the list of playlists whenever the data changes.
     */
    @Query("SELECT * FROM playlists ORDER BY id ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    /**
     * Inserts a new playlist into the database. If a playlist with the same primary key
     * already exists, it will be replaced.
     * @param playlist The [PlaylistEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    /**
     * Deletes a playlist from the database. Room also handles deleting the associated
     * cross-references due to foreign key constraints.
     * @param playlist The [PlaylistEntity] to delete.
     */
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    /**
     * Retrieves a single playlist by its unique ID.
     * @param id The ID of the playlist to find.
     * @return The [PlaylistEntity] if found, otherwise `null`.
     */
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    /**
     * Retrieves a single playlist by its name. Useful for finding special playlists like "Favorites".
     * @param name The name of the playlist to find.
     * @return The [PlaylistEntity] if found, otherwise `null`.
     */
    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    /**
     * Updates an existing playlist in the database.
     * @param playlist The [PlaylistEntity] with updated data to save.
     */
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    // --- Playlist-Song Cross-Reference Operations ---

    /**
     * Inserts a cross-reference to link a song to a playlist. If the link
     * already exists, it is ignored.
     * @param crossRef The [PlaylistSongCrossRef] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    /**
     * Deletes a cross-reference, effectively unlinking a song from a playlist.
     * @param crossRef The [PlaylistSongCrossRef] to delete.
     */
    @Delete
    suspend fun deletePlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    /**
     * Deletes a song from a specific playlist using their IDs.
     * @param playlistId The ID of the playlist.
     * @param songId The ID of the song to remove.
     */
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /**
     * Removes a song's references from ALL playlists. This is useful when a song
     * is deleted from the device.
     * @param songIds The list of song IDs to remove from all playlists.
     */
    @Query("DELETE FROM playlist_song_cross_ref WHERE songId IN (:songIds)")
    suspend fun removeSongsFromAllPlaylists(songIds: List<Long>)

    /**
     * Updates a list of cross-references. This is used to save the new order
     * after a reordering operation.
     * @param refs The list of [PlaylistSongCrossRef] objects with updated `orderIndex` values.
     */
    @Update
    suspend fun updatePlaylistSongCrossRefs(refs: List<PlaylistSongCrossRef>)


    // --- Complex Query Operations ---

    /**
     * A temporary data class to hold the results of a complex query that joins
     * playlist data with aggregated statistics.
     * @property playlist The embedded [PlaylistEntity] object.
     * @property songCount The total number of songs in the playlist.
     * @property firstSongId The ID of the first song added to the playlist, used for cover art. Can be null.
     */
    data class PlaylistWithStats(
        @Embedded val playlist: PlaylistEntity,
        val songCount: Int,
        val firstSongId: Long?
    )

    /**
     * Retrieves all playlists along with their song count and the ID of the first song.
     * This is an efficient query that uses subqueries to aggregate data.
     * @return A [Flow] emitting a list of [PlaylistWithStats] objects.
     */
    @Query(
        """
        SELECT 
            p.*, 
            (SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = p.id) as songCount,
            (SELECT songId FROM playlist_song_cross_ref WHERE playlistId = p.id ORDER BY orderIndex ASC LIMIT 1) as firstSongId
        FROM playlists p
    """
    )
    fun getPlaylistsWithStats(): Flow<List<PlaylistWithStats>>

    /**
     * Retrieves all song IDs for a specific playlist, ordered by their custom `orderIndex`.
     * @param playlistId The ID of the playlist.
     * @return A [Flow] emitting the ordered list of song IDs.
     */
    @Query("SELECT songId FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getSongIdsFromPlaylist(playlistId: Long): Flow<List<Long>>

    /**
     * Retrieves all cross-reference objects for a specific playlist, ordered correctly.
     * This is needed for reordering operations where the full objects must be modified.
     * @param playlistId The ID of the playlist.
     * @return A list of [PlaylistSongCrossRef] objects.
     */
    @Query("SELECT * FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getCrossRefsForPlaylist(playlistId: Long): List<PlaylistSongCrossRef>

    /**
     * Checks if a specific song already exists in a specific playlist.
     * @param playlistId The ID of the playlist.
     * @param songId The ID of the song.
     * @return `true` if the song is in the playlist, `false` otherwise.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    /**
     * Gets the total number of songs in a specific playlist.
     * @param playlistId The ID of the playlist.
     * @return The count of songs as an [Int].
     */
    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getSongCountForPlaylist(playlistId: Long): Int
}