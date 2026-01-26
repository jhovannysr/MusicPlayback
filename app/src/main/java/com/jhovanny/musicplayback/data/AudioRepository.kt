package com.jhovanny.musicplayback.data

import android.app.Application
import com.jhovanny.musicplayback.data.local.AppDatabase
import com.jhovanny.musicplayback.data.local.entities.PlaylistEntity
import com.jhovanny.musicplayback.data.local.entities.PlaylistSongCrossRef
import com.jhovanny.musicplayback.ui.screensimport.PlaylistUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * A repository that acts as a single source of truth for all audio-related data.
 * It abstracts the data sources (local Room database, file system) from the ViewModels.
 * All database operations are executed on a background IO thread.
 *
 * @param application The application context, used to initialize the database.
 */
class AudioRepository (application: Application){
    private val database =
        AppDatabase.getDatabase(application) // 'application' viene de AndroidViewModel
    private val playlistDao = database.playlistDao()
    private val songStatsDao = database.songStatsDao()

    /**
     * Returns a [Flow] that emits the top 20 most played songs.
     * It combines the master list of all songs with playback statistics from the database.
     *
     * @param allSongsFlow A flow representing the master list of all songs, provided by the ViewModel.
     * @return A flow emitting a sorted and filtered list of the most played songs.
     */
    fun getTopPlayedSongsFlow(
        allSongsFlow: Flow<List<Song>> // Recibe el Flow de canciones desde el ViewModel
    ): Flow<List<Song>> {
        // La lógica de combine se queda aquí, encapsulada
        return combine(
            allSongsFlow,
            songStatsDao.getAllStatsFlow() // Usa su propio DAO
        ) { allSongs, statsList ->

            val statsMap = statsList.associate { it.songId to it.playCount }

            allSongs.map { song ->
                val count = statsMap[song.id] ?: 0
                song.copy(playCount = count)
            }
                .filter { it.playCount > 0 }
                .sortedByDescending { it.playCount }
                .take(20)
        }
    }

    /**
     * Returns a [Flow] that emits a list of recently added songs.
     * It filters the master song list to include only songs added within the last 30 days.
     *
     * @param allSongsFlow A flow representing the master list of all songs.
     * @return A flow emitting a sorted list of recently added songs.
     */
    fun getRecentSongsFlow(allSongsFlow: Flow<List<Song>>): Flow<List<Song>> {
        return allSongsFlow.map { allSongs ->
            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
            val limitDate = System.currentTimeMillis() - thirtyDaysInMillis

            allSongs
                .filter { it.dateAdded >= limitDate }
                .sortedByDescending { it.dateAdded }
        }
    }

    /**
     * Checks if a given song is in the "Favorites" playlist.
     *
     * @param songId The ID of the song to check.
     * @return `true` if the song is in "Favorites", `false` otherwise.
     */
    suspend fun isSongInFavorites(songId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            val favPlaylist = playlistDao.getPlaylistByName("Favoritas")
            if (favPlaylist != null) {
                playlistDao.isSongInPlaylist(favPlaylist.id, songId)
            } else {
                false
            }
        }
    }


    /**
     * Adds a song to the "Favorites" playlist.
     *
     * @param songId The ID of the song to add.
     */
    suspend fun addSongToFavorites(songId: Long) {
        withContext(Dispatchers.IO) {
            val favPlaylist = playlistDao.getPlaylistByName("Favoritas") ?: return@withContext
            if (!playlistDao.isSongInPlaylist(favPlaylist.id, songId)) {
                val count = playlistDao.getSongCountForPlaylist(favPlaylist.id)
                val ref = PlaylistSongCrossRef(favPlaylist.id, songId, orderIndex = count)
                playlistDao.insertPlaylistSongCrossRef(ref)
            }
        }
    }

    /**
     * Removes a song from the "Favorites" playlist.
     *
     * @param songId The ID of the song to remove.
     */
    suspend fun removeSongFromFavorites(songId: Long) {
        withContext(Dispatchers.IO) {
            val favPlaylist = playlistDao.getPlaylistByName("Favoritas") ?: return@withContext
            playlistDao.removeSongFromPlaylist(favPlaylist.id, songId)
        }
    }

    /**
     * Returns a [Flow] that emits a list of all saved playlists with their metadata.
     * This includes the song count and the cover URI of the first song in each playlist.
     *
     * @param allSongsFlow A flow representing the master list of all songs, used to find cover art.
     * @return A flow emitting a list of [PlaylistUiState] objects.
     */
    fun getSavedPlaylistsFlow(
        allSongsFlow: Flow<List<Song>>
    ): Flow<List<PlaylistUiState>> {
        return playlistDao.getPlaylistsWithStats()
            .combine(allSongsFlow) { stats, allSongs ->
                stats.map { item ->
                    val firstSong = allSongs.find { it.id == item.firstSongId }
                    PlaylistUiState(
                        id = item.playlist.id,
                        name = item.playlist.name,
                        songCount = item.songCount,
                        isSpecial = item.playlist.isSpecial,
                        coverUri = firstSong?.coverUri
                    )
                }
            }
    }

    /**
     * Returns a [Flow] that emits a list of special playlists (e.g., "Favorites", "Recents")
     * with updated statistics. For example, it calculates the count for "Recents" dynamically.
     *
     * @param savedPlaylistsFlow A flow of all playlists.
     * @param recentSongsFlow A flow of recently played songs, used to update the "Recents" playlist stats.
     * @return A flow emitting a list of special playlists with updated UI state.
     */
    fun getSpecialPlaylistsFlow(
        savedPlaylistsFlow: Flow<List<PlaylistUiState>>, // Recibe el Flow que creamos antes
        recentSongsFlow: Flow<List<Song>>           // Recibe el Flow de canciones recientes
    ): Flow<List<PlaylistUiState>> {
        return combine(
            savedPlaylistsFlow,
            recentSongsFlow
        ) { savedList, recentList ->
            savedList
                .filter { it.isSpecial } // Filtramos solo las especiales
                .map { playlistState ->
                    // Si es la lista "Recientes", sobrescribimos sus datos
                    if (playlistState.name == "Recientes") {
                        playlistState.copy(
                            songCount = recentList.size,
                            coverUri = recentList.firstOrNull()?.coverUri
                        )
                    } else {
                        // Para "Favoritas" u otras, las dejamos como vienen
                        playlistState
                    }
                }
                .sortedBy { it.name } // Opcional: para mantener un orden consistente
        }
    }

    /**
     * Returns a [Flow] that emits a list of user-created playlists only (excluding special ones).
     *
     * @param savedPlaylistsFlow A flow of all playlists.
     * @return A flow emitting a filtered list of user-created playlists.
     */
    fun getUserPlaylistsFlow(
        savedPlaylistsFlow: Flow<List<PlaylistUiState>>
    ): Flow<List<PlaylistUiState>> {
        return savedPlaylistsFlow.map { list ->
            list.filter { !it.isSpecial }
        }
    }

    /**
     * Creates a new, empty playlist with the given name.
     *
     * @param name The name for the new playlist.
     */
    suspend fun createPlaylist(name: String) {
        withContext(Dispatchers.IO) {
            val newPlaylist = PlaylistEntity(name = name, isSpecial = false) // Aseguramos que no es especial
            playlistDao.insertPlaylist(newPlaylist)
        }
    }

    /**
     * Deletes a playlist and its associated song references from the database.
     *
     * @param playlistId The ID of the playlist to delete.
     */
    suspend fun deletePlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            val playlist = playlistDao.getPlaylistById(playlistId) // Necesitarás este método en el DAO
            if (playlist != null) {
                playlistDao.deletePlaylist(playlist)
            }
        }
    }

    /**
     * Returns a [Flow] that emits a list of song IDs for a specific playlist.
     *
     * @param playlistId The ID of the playlist.
     * @return A flow emitting the list of song IDs.
     */
    fun getSongIdsForPlaylistFlow(playlistId: Long): Flow<List<Long>> {
        return playlistDao.getSongIdsFromPlaylist(playlistId)
    }

    /**
     * Returns a [Flow] that emits the full [Song] objects for a specific playlist,
     * maintaining the correct order.
     *
     * @param playlistId The ID of the playlist.
     * @param allSongsFlow A flow representing the master list of all songs.
     * @return A flow emitting the ordered list of songs for the playlist.
     */
    fun getSongsForPlaylistFlow(
        playlistId: Long,
        allSongsFlow: Flow<List<Song>> // Recibe el Flow de todas las canciones
    ): Flow<List<Song>> {
        // 1. Obtenemos el Flow de IDs de la playlist desde el DAO
        return playlistDao.getSongIdsFromPlaylist(playlistId)
            .combine(allSongsFlow) { songIds, allSongs ->
                // 2. Usamos el orden de 'songIds' para mapear sobre la lista completa de canciones
                songIds.mapNotNull { id ->
                    // Esto asegura que la lista final tenga el mismo orden que los IDs
                    allSongs.find { it.id == id }
                }
            }
    }

    /**
     * Adds a list of songs to a specific playlist, ensuring no duplicates are added
     * and maintaining the correct order index.
     *
     * @param playlistId The ID of the target playlist.
     * @param songIds The list of song IDs to add.
     */
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        withContext(Dispatchers.IO) {
            val currentRefs = playlistDao.getCrossRefsForPlaylist(playlistId)
            var nextIndex = if (currentRefs.isEmpty()) 0 else currentRefs.maxOf { it.orderIndex } + 1

            songIds.forEach { songId ->
                if (currentRefs.none { it.songId == songId }) {
                    val crossRef = PlaylistSongCrossRef(
                        playlistId = playlistId,
                        songId = songId,
                        orderIndex = nextIndex
                    )
                    playlistDao.insertPlaylistSongCrossRef(crossRef)
                    nextIndex++
                }
            }
        }
    }

    /**
     * Removes a list of songs from a specific playlist.
     *
     * @param playlistId The ID of the target playlist.
     * @param songIds The list of song IDs to remove.
     */
    suspend fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>) {
        withContext(Dispatchers.IO) {
            songIds.forEach { songId ->
                // This assumes the DAO can delete a reference using a primary key composite.
                // If not, you might need to fetch the full cross-ref object first.
                val ref = PlaylistSongCrossRef(playlistId, songId)
                playlistDao.deletePlaylistSongCrossRef(ref)
            }
        }
    }

    /**
     * Persists the new order of songs within a playlist after a drag-and-drop operation.
     *
     * @param playlistId The ID of the playlist being reordered.
     * @param fromIndex The original index of the item being moved.
     * @param toIndex The new index of the item after being moved.
     */
    suspend fun reorderPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        withContext(Dispatchers.IO) {
            val crossRefs = playlistDao.getCrossRefsForPlaylist(playlistId).toMutableList()

            if (fromIndex !in crossRefs.indices || toIndex !in crossRefs.indices) return@withContext

            // Reorder the list in memory
            val item = crossRefs.removeAt(fromIndex)
            crossRefs.add(toIndex, item)

            // Recalculate all order indices to ensure they are sequential
            val updatedRefs = crossRefs.mapIndexed { index, ref ->
                ref.copy(orderIndex = index)
            }

            // Persist the entire updated list of references to the database
            playlistDao.updatePlaylistSongCrossRefs(updatedRefs)
        }
    }
}