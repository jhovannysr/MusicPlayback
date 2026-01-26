package com.jhovanny.musicplayback.ui.screens.libraryScreen.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhovanny.musicplayback.ui.components.TopSongsRow
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel


/**
 * `PlaylistRowComponent` is a self-contained, reusable composable responsible for
 * displaying a single row for a specific user-created playlist.
 *
 * It fetches the songs for a given `playlistId` from the ViewModel and displays them
 * using the generic `TopSongsRow` component. It only renders itself if the
 * playlist contains at least one song. This component handles the logic for
 * playing a song from the list or navigating to the playlist's detailed view.
 */
@Composable
fun PlaylistRowComponent(
    viewModel: PlayerViewModel,
    playlistId: Long,
    playlistName: String,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (Long, String) -> Unit,
) {
    // A state variable to prevent rapid, repeated clicks on the title.
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // Observe the list of songs for the given `playlistId` as a state.
    // This ensures that if songs are added or removed from the playlist elsewhere,
    // this component will automatically update its UI.
    val songs by viewModel.getSongsForPlaylistFlow(playlistId).collectAsState(initial = emptyList())

    // The entire component is only rendered if the playlist actually contains songs.
    if (songs.isNotEmpty()) {
        // This is a reusable UI component that displays a title and a horizontal list of songs.
        TopSongsRow(
            name = playlistName,
            songs = songs,
            onSongClick = { clickedSong ->
                // Find the exact index of the clicked song within the current list.
                val index = songs.indexOfFirst { it.id == clickedSong.id }
                if (index != -1) {
                    // Tell the ViewModel to start playback from the selected song within this specific playlist.
                    viewModel.playSongsFromPlaylist(songs, index)
                }
            },
            onTitleClick = {
                // A simple debounce mechanism to prevent navigating multiple times from a single burst of clicks.
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > 500) {
                    lastClickTime = currentTime
                    // Trigger the navigation callback to open the detailed screen for this playlist.
                    onNavigateToDetail(playlistId, playlistName)
                }
            }
        )
        // Add vertical space after the row to separate it from the next component.
        Spacer(modifier = Modifier.height(16.dp))
    }
}