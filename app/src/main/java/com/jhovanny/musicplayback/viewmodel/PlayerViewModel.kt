package com.jhovanny.musicplayback.viewmodel

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.jhovanny.musicplayback.data.AudioLoader
import com.jhovanny.musicplayback.data.AudioRepository
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.service.SimpleMediaService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.jhovanny.musicplayback.data.local.AppDatabase
import com.jhovanny.musicplayback.data.local.entities.PlaylistEntity
import com.jhovanny.musicplayback.data.local.entities.PlaylistSongCrossRef
import com.jhovanny.musicplayback.data.local.entities.SongStatsEntity
import com.jhovanny.musicplayback.ui.screensimport.PlaylistUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Manages the UI state for the main player and orchestrates interactions
 * between the user, the media playback service, and the local database.
 *
 * NOTE FOR REVIEWERS:
 * This ViewModel has grown significantly as features were added. It currently manages
 * playback, file loading, search, selection mode, and playlist management. In a
 * production environment with more time, I would refactor this into smaller,
 * more focused ViewModels (e.g., PlayerViewModel, LibraryViewModel, PlaylistViewModel)
 * to better adhere to the Single Responsibility Principle. However, for this portfolio
 * project, I prioritized demonstrating a wide range of functionalities in a single,
 * well-documented class.
 *
 * @param application The application context, required for AndroidViewModel.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Manages the connection to the media playback service.
     * This should only be accessed through the public methods.
     */
    private var mediaController: MediaController? = null
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    /**
     * Utility class to load audio files from the device's MediaStore.
     */
    private val audioLoader = AudioLoader(application)

    /**
     * The list of songs currently being managed by the player service.
     * This list can be either in original order or shuffled. The UI should render this list.
     */
    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    val songList = _songList.asStateFlow()

    /**
     * The original, sorted (A-Z) list of all songs loaded from the device.
     * This is used as the master list and to restore order when shuffle is disabled.
     */
    private val _originalSongList = MutableStateFlow<List<Song>>(emptyList())
    val originalSongList = _originalSongList.asStateFlow()

    /**
     * Represents the current song that is playing or paused.
     * The UI observes this to display the current track's metadata (title, artist, cover).
     * It's null if no song is loaded.
     */
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    /**
     * `true` if media is currently playing, `false` otherwise.
     * The UI uses this to show a play/pause icon.
     */
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    /**
     * The current playback position of the song, in milliseconds.
     * The UI observes this to update the progress bar.
     */
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    /**
     * The total duration of the current song, in milliseconds.
     * The UI uses this to set the maximum value of the progress bar.
     */
    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    /**
     * `true` if shuffle mode is enabled, `false` otherwise.
     * The UI observes this to show the shuffle button's active/inactive state.
     */
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    /**
     * The current repeat mode of the player.
     * See [Player.REPEAT_MODE_OFF], [Player.REPEAT_MODE_ONE], [Player.REPEAT_MODE_ALL].
     */
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    // This holds the original, unshuffled list for the current context (e.g., "All Songs" or a specific playlist).
    // It's essential for restoring the correct order when shuffle is turned off.
    private val _currentContextList = MutableStateFlow<List<Song>>(emptyList())

    /**
     * Loads all audio files from the device's MediaStore on a background thread.
     * It sorts the files alphabetically and updates the main song lists for the UI.
     */
    fun loadAudioFiles() {
        // Launch a coroutine in the ViewModel's scope to perform the file loading off the main thread.
        viewModelScope.launch {
            var files = audioLoader.getAllAudioFiles()
            files = sortSongsCustom(files)
            _originalSongList.value = files
            _songList.value = files
        }
    }


    /**
     * Initializes the connection to the [SimpleMediaService] and sets up player state synchronization.
     * This should be called once when the ViewModel is created. It also ensures
     * that special playlists like "Favorites" and "Recents" exist in the database.
     */
    fun initController() {
        // Define the session token to connect to our background media service.
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), SimpleMediaService::class.java)
        )

        // Asynchronously build the MediaController.
        mediaControllerFuture = MediaController.Builder(getApplication(), sessionToken)
            .buildAsync()

        // Add a listener that executes when the controller is successfully connected.
        mediaControllerFuture?.addListener({
            try {
                mediaController = mediaControllerFuture?.get()

                // Set up listeners to react to player events (e.g., song changes, play/pause).
                setupPlayerListener()

                // --- Initial State Synchronization ---
                _isPlaying.value = mediaController?.isPlaying == true
                _repeatMode.value = mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF
                updateCurrentMetadata()

                if (mediaController?.isPlaying == true) {
                    startProgressUpdater()
                }
            } catch (e: Exception) {
                // Log any errors that occur during controller connection.
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())

        // Launch a one-time task to initialize or migrate database entries.
        viewModelScope.launch(Dispatchers.IO) {
            // Ensure the special "Favorites" and "Recents" playlists exist.
            val hasFavorites = playlistDao.getPlaylistByName("Favoritas") != null
            if (!hasFavorites) {
                playlistDao.insertPlaylist(PlaylistEntity(name = "Favoritas", isSpecial = true))
            }

            val hasRecents = playlistDao.getPlaylistByName("Recientes") != null
            if (!hasRecents) {
                playlistDao.insertPlaylist(PlaylistEntity(name = "Recientes", isSpecial = true))
            }

            // Data migration: Ensure existing playlists are correctly marked as "special".
            // This handles cases where the app was updated from a previous version.
            val existingFav = playlistDao.getPlaylistByName("Favoritas")
            if (existingFav != null && !existingFav.isSpecial) {
                playlistDao.updatePlaylist(existingFav.copy(isSpecial = true))
            }

            val existingRecent = playlistDao.getPlaylistByName("Recientes")
            if (existingRecent != null && !existingRecent.isSpecial) {
                playlistDao.updatePlaylist(existingRecent.copy(isSpecial = true))
            }
        }
    }

    /**
     * Sets up a [Player.Listener] to react to events from the [MediaController].
     * This is the primary mechanism for synchronizing the UI state with the actual player state.
     * This function should only be called once the mediaController is initialized.
     */
    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            /**
             * Called when the player starts or stops playing.
             * Updates the play/pause state and starts the progress bar updater.
             */
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressUpdater()
            }

            /**
             * Called when the player moves to a different media item in the playlist.
             * Triggers a UI update to show the new song's metadata.
             */
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentMetadata()
            }

            /**
             * Called when the overall state of the player changes (e.g., ready, buffering, ended).
             */
            override fun onPlaybackStateChanged(playbackState: Int) {
                // When the player is ready, update the total duration of the track.
                if (playbackState == Player.STATE_READY) {
                    val d = mediaController?.duration ?: 0L
                    if (d > 0) _duration.value = d
                }

                // When the entire playlist has finished.
                if (playbackState == Player.STATE_ENDED) {
                    _currentPosition.value = 0L
                    _isPlaying.value = false
                }
            }

            /**
             * Called when the repeat mode changes.
             * Updates the UI to reflect the new repeat mode icon.
             */
            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    /**
     * Ensures the media service is running in the foreground.
     */
    private fun ensureServiceIsStarted() {
        try {
            val intent = Intent(getApplication(), SimpleMediaService::class.java)
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Plays the main list of all songs, starting from a specific index.
     * This function resets the shuffle mode and sets the "All Songs" list as the current playback context.
     * @param index The index of the song to start playing from.
     */
    fun playPlaylist(index: Int) {
        val controller = mediaController ?: return

        // Ensure the service is active before sending commands.
        ensureServiceIsStarted()

        // Set the current playback context to the master list of all songs.
        val allSongs = _originalSongList.value
        _currentContextList.value = allSongs
        if (allSongs.isEmpty()) return

        // If shuffle was on, disable it, as the user has selected a specific track order.
        if (_isShuffleEnabled.value) {
            _isShuffleEnabled.value = false
        }
        _songList.value = allSongs

        // 1. Convert the entire song list to MediaItems for the player service.
        val mediaItems = allSongs.map { songToMediaItem(it) }

        // 2. Set the full playlist on the controller, telling it where to start.
        controller.setMediaItems(mediaItems, index, 0)

        // 3. Prepare and start playback.
        controller.prepare()
        controller.play()
    }

    /**
     * Toggles the shuffle mode on and off for the current playback context.
     * It preserves the currently playing song and its progress.
     */
    fun toggleShuffle() {
        val controller = mediaController ?: return
        val currentSong = _currentSong.value ?: return

        // Use the global song list as a fallback if no specific context is set.
        if (_currentContextList.value.isEmpty()) {
            _currentContextList.value = _originalSongList.value
        }

        val shouldShuffle = !_isShuffleEnabled.value
        _isShuffleEnabled.value = shouldShuffle
        android.widget.Toast.makeText(
            getApplication(),
            if (shouldShuffle) "Shuffle On" else "Shuffle Off",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        if (shouldShuffle) {
            // --- ENABLE SHUFFLE ---
            // 1. Create a shuffled list from the current context (e.g., a specific playlist or all songs).
            val shuffledList = _currentContextList.value.toMutableList()
            shuffledList.remove(currentSong) // Temporarily remove the current song.
            shuffledList.shuffle() // Shuffle the rest of the list.
            shuffledList.add(0, currentSong) // Add the current song back to the very beginning.

            // 2. Update the UI and the player service with the new shuffled queue.
            _songList.value = shuffledList
            val mediaItems = shuffledList.map { songToMediaItem(it) }

            // 3. Set the new queue, starting at index 0 (the current song) and preserving its position.
            controller.setMediaItems(mediaItems, 0, controller.currentPosition)

        } else {
            // --- DISABLE SHUFFLE ---
            // 1. Restore the original, unshuffled list to the UI.
            val originalList = _currentContextList.value
            _songList.value = originalList

            // 2. Find the position of the current song within the original list.
            val originalIndex = originalList.indexOfFirst { it.id == currentSong.id }
            if (originalIndex == -1) return // Should not happen, but as a safeguard.

            // 3. Resend the original list to the player, seeking to the correct index.
            val mediaItems = originalList.map { songToMediaItem(it) }
            controller.setMediaItems(mediaItems, originalIndex, controller.currentPosition)
        }
    }

    /**
     * Toggles the playback state between playing and paused.
     */
    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    /**
     * Cycles through the available repeat modes: OFF -> REPEAT_ALL -> REPEAT_ONE -> OFF.
     * It also displays a Toast message to inform the user of the change.
     */
    fun toggleRepeatMode() {
        val controller = mediaController ?: return

        // Cycle through modes: OFF -> ALL -> ONE -> OFF
        val newMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF // Fallback to default
        }

        // Apply the new mode to the controller and update the UI state.
        _repeatMode.value = newMode
        controller.repeatMode = newMode

        // Provide user feedback via a Toast message.
        val message = when (newMode) {
            Player.REPEAT_MODE_ALL -> "Repeat All"
            Player.REPEAT_MODE_ONE -> "Repeat One"
            else -> "Repeat Off"
        }
        android.widget.Toast.makeText(getApplication(), message, android.widget.Toast.LENGTH_SHORT)
            .show()
    }


    /**
     * Skips to a specific song within the current playlist.
     * @param song The [Song] object to skip to.
     */
    fun skipToSong(song: Song) {
        val controller = mediaController ?: return
        val index = _songList.value.indexOfFirst { it.id == song.id }

        // The player service already has the current playlist (shuffled or not),
        // so we only need to command it to seek to the correct index.
        if (index != -1) {
            controller.seekTo(index, 0L)
            if (!controller.isPlaying) controller.play()
        }
    }


    /**
     * Skips to the next media item in the current playlist.
     */
    fun skipNext() {
        mediaController?.seekToNext()
    }

    /**
     * Skips to the previous media item in the current playlist.
     */
    fun skipPrevious() {
        mediaController?.seekToPrevious()
    }

    /**
     * Seeks to a specific position within the current song.
     * @param pos The position to seek to, in milliseconds.
     */
    fun seekTo(pos: Long) {
        mediaController?.seekTo(pos)
        // Provide immediate visual feedback on the progress bar, even before the player confirms.
        _currentPosition.value = pos
    }

    /**
     * Updates the ViewModel's state to reflect the metadata of the currently playing song.
     * * It finds the full [Song] object from the master list and updates the song's duration.
     */
    private fun updateCurrentMetadata() {
        val mediaItem = mediaController?.currentMediaItem ?: return
        val mediaId = mediaItem.mediaId.toLongOrNull()

        if (mediaId != null) {
            // Find the complete Song object in our master list to get all its details.
            val song = _originalSongList.value.find { it.id == mediaId }
            _currentSong.value = song
        }

        // Also update the duration, as it might change with the new track.
        val duration = mediaController?.duration ?: 0L
        if (duration > 0) {
            _duration.value = duration
        }
    }

    /**
     * Starts a coroutine that periodically updates the playback progress.
     * The loop continues as long as the player is playing and the ViewModel is active.
     */
    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                _currentPosition.value = mediaController?.currentPosition ?: 0L
                delay(1000L) // Update every second.
            }
        }
    }

    /**
     * A helper function to convert a [Song] data object into a [MediaItem]
     * @param song The input [Song] to convert.
     * @return A [MediaItem] instance ready for playback.
     */
    private fun songToMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(Uri.parse(song.coverUri))
                    .build()
            )
            .build()
    }

    /**
     * Provides a custom sorting logic for a list of songs.
     * @param songs The list of [Song] objects to sort.
     * @return A new, sorted list of [Song] objects.
     */
    private fun sortSongsCustom(songs: List<Song>): List<Song> {
        // return songs.sortedBy { it.title.lowercase() } // A simple alphabetical sort is often sufficient and cleaner.
        return songs.sortedWith(compareBy<Song> {
            val title = it.title.trim().lowercase()
            when {
                title.isEmpty() -> "zzz"
                title.first().isDigit() -> "0$title"
                !title.first()
                    .isLetter() && title.length > 1 -> title.drop(1)
                else -> title
            }
        }.thenBy { it.title.lowercase() })
    }

    /**
     * Holds the ID of the currently active playlist. This is `null` if the user is playing
     * from the main "All Songs" list. The UI can observe this to highlight the active playlist.
     */
    private val _currentPlaylistId = MutableStateFlow<Long?>(null)
    val currentPlaylistId = _currentPlaylistId.asStateFlow()

    /**
     * Plays a specific list of songs (like a playlist or search results) from a given start index.
     * This function is crucial for setting a specific "playback context".
     * @param songs The list of [Song] objects to play.
     * @param startIndex The index within the list to start playback from.
     * @param playlistId The optional ID of the playlist being played. Setting this updates [currentPlaylistId].
     */
    fun playSongsFromPlaylist(songs: List<Song>, startIndex: Int, playlistId: Long? = null) {
        val controller = mediaController ?: return

        ensureServiceIsStarted()

        // 1. Set the active playlist ID. This allows the UI to know which playlist is playing.
        _currentPlaylistId.value = playlistId

        // 2. Define this list as the current "context" for playback operations like shuffle.
        _currentContextList.value = songs

        // 3. Disable shuffle mode, as starting a playlist implies a specific order.
        if (_isShuffleEnabled.value) {
            _isShuffleEnabled.value = false
        }

        // 4. Update the UI's song list and send the new media items to the player service.
        _songList.value = songs
        val mediaItems = songs.map { songToMediaItem(it) }

        controller.setMediaItems(mediaItems, startIndex, 0)
        controller.prepare()
        controller.play()
    }

    /**
     * Called when the ViewModel is about to be destroyed.
     * It releases the future connection to the [MediaController] but does *not* release the
     * controller itself. This allows the playback service to continue running in the background
     * even if the UI is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        // Release the future, not the controller, to allow background playback.
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
    }

    // =========================================================================================
    //
    //                                     --- SEARCH ---
    //
    // =========================================================================================
    /**
     * Holds the current text entered by the user in the search bar.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    /**
     * A flow that emits the list of songs matching the current [searchQuery].
     * The UI observes this to display the search results.
     */
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    /**
     * Updates the search query and filters the master song list to produce search results.
     * If the query is blank, the results are cleared.
     *
     * @param query The new text from the search input field.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        } else {
            // Filter the master list based on song title or artist, ignoring case.
            _searchResults.value = _originalSongList.value.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true)
            }
        }
    }

    // =========================================================================================
    //
    //                              --- FILE DELETION ---
    //
    // =========================================================================================
    /**
     * Stores the IDs of songs pending deletion, used as a fallback for the success callback.
     */
    private var _pendingDeleteIds: List<Long> = emptyList()

    /**
     * Initiates the process of deleting songs from the device's MediaStore.
     * It handles different Android versions, requesting user permission for Android 11+
     * or attempting direct deletion on older versions.
     * @param context The application context.
     * @param onSuccess A callback to execute if deletion succeeds directly (for older Android versions).
     * @param onNeedPermission A callback that provides an [IntentSender] to the UI to request user permission.
     * @param selectedSong The set of song IDs to be deleted.
     */
    fun deleteSongs(
        context: Context,
        onSuccess: () -> Unit,
        onNeedPermission: (IntentSender) -> Unit,
        selectedSong: Set<Long>
    ) {
        val idsToDelete = selectedSong.toList()
        if (idsToDelete.isEmpty()) return

        // Store the song IDs in case the success callback needs them later.
        _pendingDeleteIds = idsToDelete

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uris = idsToDelete.map { id ->
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // For Android 11 (API 30) and above, use the modern, safe API.
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    // The UI must launch this intent to get user confirmation.
                    withContext(Dispatchers.Main) {
                        onNeedPermission(pendingIntent.intentSender)
                    }
                } else {
                    // For older Android versions, attempt direct deletion.
                    // This may require handling RecoverableSecurityException on Android 10.
                    for (uri in uris) {
                        context.contentResolver.delete(uri, null, null)
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            } catch (e: SecurityException) {
                // Specifically handle cases on Android 10 where permission is recoverable.
                val recoverable = e as? RecoverableSecurityException
                if (recoverable != null) {
                    withContext(Dispatchers.Main) {
                        onNeedPermission(recoverable.userAction.actionIntent.intentSender)
                    }
                } else {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Cleans up the app state after songs have been successfully deleted from the device.
     * It handles player state, removes the songs from all local playlists, and reloads the media.
     *
     * @param deletedIds The list of IDs that were confirmed as deleted. If null, it uses the
     * last known selection as a fallback.
     */
    fun onSongsDeletedSuccess(deletedIds: List<Long>? = null) {
        // Determine the final list of IDs that were deleted.
        val finalDeletedIds = deletedIds ?: _selectedSongIds.value.toList().ifEmpty { _pendingDeleteIds }
        if (finalDeletedIds.isEmpty()) {
            loadAudioFiles() // Just in case, refresh and exit.
            return
        }

        // 1. Manage player state if a currently playing song was deleted.
        val currentSongId = _currentSong.value?.id
        if (currentSongId != null && finalDeletedIds.contains(currentSongId)) {
            if (mediaController?.hasNextMediaItem() == true) {
                // If there's a next song, just skip to it.
                skipNext()
            } else {
                // If it was the last song, stop playback entirely.
                stopPlayback()
            }
        }

        // 2. Clean up database references on a background thread.
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.removeSongsFromAllPlaylists(finalDeletedIds)

            // 3. Finally, update the UI and reload all audio files.
            withContext(Dispatchers.Main) {
                setSelectionMode(false)
                _selectedSongIds.value = emptySet()
                loadAudioFiles()
            }
        }
    }

    /**
     * A helper function to completely stop media playback and clear the player's state.
     */
    fun stopPlayback() {
        viewModelScope.launch(Dispatchers.Main) {
            mediaController?.stop()
            mediaController?.clearMediaItems()
            _isPlaying.value = false
            _currentSong.value = null
            _currentPosition.value = 0L
        }
    }

    // =========================================================================================
    //
    //                               --- SELECTION MODE ---
    //
    // =========================================================================================
    /**
     * `true` if the UI should be in multi-select mode, `false` otherwise.
     * This globally controls the visibility of selection UI elements like checkboxes.
     */
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    /**
     * A set containing the unique IDs of all currently selected songs.
     */
    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds = _selectedSongIds.asStateFlow()

    /**
     * Controls the visibility of a bottom sheet, typically used for actions on selected items
     * (e.g., "Add to Playlist").
     */
    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet = _showBottomSheet.asStateFlow()

    /**
     * Explicitly enables or disables selection mode.
     * When disabled, it automatically clears the current selection.
     *
     * @param active `true` to enable selection mode, `false` to disable it.
     */
    fun setSelectionMode(active: Boolean) {
        _isSelectionMode.value = active
        // If selection mode is turned off, clear any selected items.
        if (!active) {
            _selectedSongIds.value = emptySet()
        }
    }

    /**
     * Directly sets the collection of selected song IDs.
     * @param ids The new set of song IDs to be marked as selected.
     */
    fun setSelectedSongIds(ids: Set<Long>) {
        _selectedSongIds.value = ids
    }

    /**
     * Toggles the selection state of a single song.
     * If the song is already selected, it will be deselected, and vice-versa.
     * It also automatically disables selection mode if no songs remain selected.
     *
     * @param songId The unique ID of the song to toggle.
     */
    fun toggleSelection(songId: Long) {
        val currentSelection = _selectedSongIds.value
        val newSelection = if (currentSelection.contains(songId)) {
            currentSelection - songId // Deselect
        } else {
            currentSelection + songId // Select
        }

        _selectedSongIds.value = newSelection

        // Automatically exit selection mode if the last item is deselected.
        if (newSelection.isEmpty()) {
            setSelectionMode(false)
        }
    }

    /**
     * Selects all songs currently visible on the screen.
     *
     * @param songs The list of [Song] objects currently displayed to the user.
     */
    fun selectAllSongs(songs: List<Song>) {
        val allIds = songs.map { it.id }.toSet()
        _isSelectionMode.value = true
        _selectedSongIds.value = allIds
    }

    /**
     * Shows or hides the actions bottom sheet.
     *
     * @param active `true` to show the bottom sheet, `false` to hide it.
     */
    fun setShowBottomSheet(active: Boolean) {
        _showBottomSheet.value = active
    }


    // ===========================================================================================
    //
    //                        --- DATABASE & REPOSITORY INTERACTIONS ---
    //
    //// =========================================================================================

    // --- Private Connections ---
    private val repository = AudioRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val playlistDao = database.playlistDao()
    private val songStatsDao = database.songStatsDao()

    // --- UI State Flows ---

    /**
     * A flow that emits the list of songs for the currently viewed playlist.
     * The UI collects this to display the contents of a specific playlist screen.
     */
    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylistSongs = _currentPlaylistSongs.asStateFlow()

    /**
     * `true` if the currently playing song is marked as a favorite, `false` otherwise.
     * The UI observes this to show the state of the favorite icon.
     */
    private val _isCurrentSongFavorite = MutableStateFlow(false)
    val isCurrentSongFavorite = _isCurrentSongFavorite.asStateFlow()

    /**
     * A state flow that emits a list of the top 20 most played songs.
     * The list is automatically updated and sorted by play count, powered by the repository.
     */
    val topPlayedSongs: StateFlow<List<Song>> = repository.getTopPlayedSongsFlow(
        allSongsFlow = _originalSongList // Provide the master song list to the repository.
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * A state flow that emits a list of recently played songs.
     * This list is derived from playback history and managed by the repository.
     */
    val recentSongs: StateFlow<List<Song>> = repository.getRecentSongsFlow(
        allSongsFlow = _originalSongList
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily, // 'Lazily' is efficient for data not always on screen.
        initialValue = emptyList()
    )

    // --- Methods ---

    /**
     * Checks if the currently playing song exists in the "Favorites" playlist
     * and updates the [isCurrentSongFavorite] state accordingly.
     */
    fun checkIsFavorite() {
        val song = _currentSong.value ?: return
        viewModelScope.launch {
            // Delegate the check to the repository and update the UI with the result.
            _isCurrentSongFavorite.value = repository.isSongInFavorites(song.id)
        }
    }

    /**
     * A flow that emits a list of all user-created playlists, including their song count.
     * This is derived from the master list of songs and playlist data.
     */
    val savedPlaylists: StateFlow<List<PlaylistUiState>> = repository.getSavedPlaylistsFlow(
        allSongsFlow = _originalSongList
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * A flow that emits UI states for special, system-managed playlists like "Favorites" and "Recents".
     * It calculates their stats by combining other flows from the ViewModel.
     */
    val specialPlaylistsStats: StateFlow<List<PlaylistUiState>> =
        repository.getSpecialPlaylistsFlow(
            savedPlaylistsFlow = savedPlaylists,
            recentSongsFlow = recentSongs
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * A flow that emits UI states only for user-created playlists (non-special).
     * The UI uses this to display the list of playlists that the user can edit or delete.
     */
    val userPlaylistsStats: StateFlow<List<PlaylistUiState>> = repository.getUserPlaylistsFlow(
        savedPlaylistsFlow = savedPlaylists
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Creates a new, empty playlist with the given name.
     *
     * @param name The name for the new playlist.
     */
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    /**
     * Deletes a given playlist from the database.
     *
     * @param playlist The [PlaylistEntity] to be deleted. The function will extract its ID.
     */
    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
        }
    }

    /**
     * Loads the songs belonging to a specific playlist into the [currentPlaylistSongs] state flow.
     * This is typically called when a user navigates to a playlist's detail screen.
     *
     * @param playlistId The ID of the playlist whose songs should be loaded.
     */
    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            // Collect the song IDs from the repository for the given playlist.
            repository.getSongIdsForPlaylistFlow(playlistId).collect { songIds ->
                // Filter the master song list to get the full song objects.
                val allSongs = _originalSongList.value
                val playlistSongs = songIds.mapNotNull { id ->
                    allSongs.find { it.id == id }
                }
                // Update the UI state with the found songs.
                _currentPlaylistSongs.value = playlistSongs
            }
        }
    }

    /**
     * Returns a new Flow that will emit the list of [Song] objects for a given playlist ID.
     * This is useful for composables that need to collect songs of a playlist reactively.
     *
     * @param playlistId The ID of the playlist.
     * @return A [Flow] that emits the list of songs for that playlist.
     */
    fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<Song>> {
        // Delegate the call to the repository, providing the necessary master song list.
        return repository.getSongsForPlaylistFlow(
            playlistId = playlistId,
            allSongsFlow = _originalSongList
        )
    }

    /**
     * Adds or removes the currently playing song from the "Favorites" playlist.
     * It provides instant UI feedback by toggling the state optimistically.
     */
    fun toggleFavorite() {
        val song = _currentSong.value ?: return
        val isCurrentlyFavorite = _isCurrentSongFavorite.value

        viewModelScope.launch {
            if (isCurrentlyFavorite) {
                repository.removeSongFromFavorites(song.id)
            } else {
                repository.addSongToFavorites(song.id)
            }
        }
        _isCurrentSongFavorite.value = !isCurrentlyFavorite
    }

    /**
     * Adds a list of songs to a specific playlist.
     *
     * @param playlistId The ID of the target playlist.
     * @param songIds The list of song IDs to add.
     */
    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    /**
     * Removes a list of songs from a specific playlist and exits selection mode.
     *
     * @param playlistId The ID of the target playlist.
     * @param songIds The list of song IDs to remove.
     */
    fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            repository.removeSongsFromPlaylist(playlistId, songIds)

            // UI-related cleanup remains in the ViewModel.
            setSelectionMode(false)
        }
    }

    /**
     * Persists the new order of songs within a playlist after a drag-and-drop operation.
     *
     * @param playlistId The ID of the playlist being reordered.
     * @param fromIndex The original index of the item being moved.
     * @param toIndex The new index of the item being moved.
     */
    fun reorderPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorderPlaylist(playlistId, fromIndex, toIndex)
        }
    }

}