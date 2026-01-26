package com.jhovanny.musicplayback.ui.screens.musicListScreen

import android.Manifest
import android.R
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onGloballyPositioned
import com.jhovanny.musicplayback.ui.components.AddToPlaylistDialog
import com.jhovanny.musicplayback.ui.components.SongItem
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.ui.components.ScrollComponent
import com.jhovanny.musicplayback.ui.screens.musicListScreen.components.MenuOptionItemMinimal

/**
 * A composable screen that displays the main list of all audio files available on the device.
 *
 * This screen is responsible for several key user interactions:
 * - Displaying a scrollable list of all songs using [LazyColumn].
 * - Handling single taps on a song to start playback and navigate to the player.
 * - Implementing a selection mode via long-press, allowing users to select multiple songs.
 * - Displaying a [ModalBottomSheet] with options for the current selection (e.g., add to playlist, delete).
 * - Managing runtime permissions for reading audio files and for scoped storage deletion on modern Android versions.
 * - Providing a Floating Action Button to navigate to the search screen.
 *
 * @param viewModel The shared [PlayerViewModel] that provides state and handles all business logic.
 * @param onNavigateToPlayer A callback function to navigate to the full-screen player UI.
 * @param onNavigateToSearch A callback function to navigate to the search screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val context = LocalContext.current

    // Set up the ActivityResultLauncher to handle the system's confirmation dialog for deleting files.
    // This is part of Android's Scoped Storage security measures.
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User confirmed the deletion. The ViewModel will now handle the file removal.
            viewModel.onSongsDeletedSuccess()
        } else {
            // User cancelled the deletion. Exit selection mode.
            viewModel.setSelectionMode(false)
        }
    }

    // --- State Observation ---
    // Observe various states from the ViewModel to drive the UI.
    val songs by viewModel.originalSongList.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlayerVisible = currentSong != null

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // States for the options bottom sheet.
    var songForMenu by remember { mutableStateOf<Song?>(null) }
    var songForMenuParams by remember { mutableStateOf<Song?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()

    // States for multi-selection mode.
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()

    // States for the "Add to Playlist" dialog.
    val savedPlaylists by viewModel.savedPlaylists.collectAsState()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Set up the ActivityResultLauncher for requesting the media permission.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, the ViewModel should have already loaded the music.
            // If not, a refresh could be triggered here.
        }
    }

    // This effect runs once to ensure the necessary permissions are checked on screen entry.
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    // Intercept the back button press when in selection mode to disable it,
    // instead of navigating away from the screen.
    BackHandler(enabled = isSelectionMode) {
        viewModel.setSelectionMode(false)
    }

    // The main layout of the screen is a Scaffold.
    Scaffold(
        containerColor = Color.Transparent, // Let the global background show through.
        floatingActionButton = {
            // FAB to navigate to the search screen.
            FloatingActionButton(
                onClick = { onNavigateToSearch() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search for a song")
            }
        }
    ) { padding ->
        // The main content area of the screen, which is primarily a scrollable list of songs.
        // It's structured as a Column to allow for future elements above or below the list.
        Column(
            modifier = Modifier
                .fillMaxSize().padding(top = 10.dp)
                // Dynamically adjust the bottom padding to account for the navigation bar
                // and avoid being obscured by the MiniPlayer.
                .then(
                    if (!isPlayerVisible) {
                        // If the MiniPlayer is not visible, add padding to respect the system navigation bar.
                        Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    } else {
                        // If the MiniPlayer is visible, it provides its own padding, so we only add a small margin.
                        Modifier.padding(bottom = 16.dp)
                    }
                )
        ) {
            // This Box expands to fill all available vertical space. This ensures the LazyColumn
            // within it has a defined size, which is crucial for performance and for the
            // fast scrollbar to calculate its position correctly.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Key modifier: makes this Box take all remaining vertical space.
            ) {
                // This is the primary component for displaying the list of songs efficiently.
                // It only composes and lays out the items currently visible on screen.
                LazyColumn(
                    state = listState, // Connect the list state for programmatic scrolling.
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 10.dp) // Leave space for the fast scrollbar.
                ) {
                    itemsIndexed(songs) { index, song ->
                        val isPlaying = song.id == currentSong?.id
                        val isSelectedInMode = selectedSongIds.contains(song.id)

                        // The SongItem composable encapsulates the layout and logic for a single row.
                        SongItem(
                            song = song,
                            isSelected = isPlaying, // Highlight if it's the currently playing song.
                            isSelectionMode = isSelectionMode,
                            isSelectedInMode = isSelectedInMode, // Visual state for multi-selection.
                            onClick = {
                                if (isSelectionMode) {
                                    // In selection mode, a click toggles the item's selected state.
                                    viewModel.toggleSelection(song.id)
                                } else {
                                    // Otherwise, a click starts playback from this song's position in the list.
                                    viewModel.playPlaylist(index)
                                    onNavigateToPlayer()
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    // A long click initiates selection mode and selects the current item.
                                    songForMenuParams = null
                                    viewModel.setSelectionMode(true)
                                    viewModel.setSelectedSongIds(setOf(song.id))
                                } else {
                                    // If already in selection mode, a long click behaves like a normal click.
                                    viewModel.toggleSelection(song.id)
                                }
                            },
                            onMoreClick = {
                                if (!isSelectionMode) {
                                    // The 'more' button opens a context menu for the specific song.
                                    songForMenuParams = song
                                    viewModel.setSelectedSongIds(setOf(song.id))
                                    songForMenu = song
                                    viewModel.setShowBottomSheet(true)
                                }
                            }
                        )
                    }
                }

                // This is a custom fast scrollbar component that appears on the side of the list,
                // allowing the user to quickly navigate through a large number of songs.
                ScrollComponent(songs, listState, currentSong, scope)
            }
        }
    }

    // This handles the display of the bottom sheet menu for song options.
    // It is shown when the user long-presses a song or selects "More Options" in selection mode.
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowBottomSheet(false) },
            sheetState = sheetState,
            // Use a dark, semi-transparent color for an elegant, modern look.
            containerColor = Color(0xFF121212).copy(alpha = 0.98f),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // The content of the bottom sheet is conditional.
                // It shows either info for a single song or a generic title.

                // Case 1: A specific song was selected (e.g., by tapping the 'more' icon).
                if (songForMenuParams != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Display a small album art thumbnail.
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.DarkGray,
                            modifier = Modifier.size(56.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(songForMenuParams?.coverUri)
                                    .crossfade(true)
                                    .error(R.drawable.ic_menu_help) // Fallback icon.
                                    .build(),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 2. Display the song's title and artist.
                        Column {
                            Text(
                                text = songForMenuParams?.title ?: "Desconocido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = songForMenuParams?.artist ?: "Artista desconocido",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Case 2: The menu was opened during multi-selection mode.
                else {
                    Text(
                        text = "Options", // A generic title for the options menu.
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }

                // --- Menu Options ---

                // Option 1: Add to a playlist.
                MenuOptionItemMinimal(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    text = "Add to playlist",
                    onClick = {
                        viewModel.setShowBottomSheet(false)
                        showAddToPlaylistDialog = true // This will trigger the AddToPlaylistDialog.
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Select all songs in the current list.
                MenuOptionItemMinimal(
                    icon = Icons.Default.SelectAll,
                    text = "Select all ",
                    onClick = {
                        viewModel.selectAllSongs(songs)
                        viewModel.setShowBottomSheet(false)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3: Delete the selected song(s).
                // This option is styled to indicate a destructive action.
                MenuOptionItemMinimal(
                    icon = Icons.Default.Delete,
                    text = "Delete Song",
                    textColor = Color(0xFFFF5252), // Soft red for the text.
                    // A subtle red background to further hint at a dangerous action.
                    containerColor = Color(0xFFFF5252).copy(alpha = 0.1f),
                    onClick = {
                        // The delete logic is complex and handled by the ViewModel.
                        // It manages Scoped Storage permissions via the deleteLauncher.
                        viewModel.deleteSongs(
                            context = context,
                            onSuccess = {
                                viewModel.onSongsDeletedSuccess(selectedSongIds.toList())
                                Toast.makeText(context, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
                            },
                            onNeedPermission = { intentSender ->
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(intentSender).build()
                                deleteLauncher.launch(intentSenderRequest)
                            },
                            selectedSong = selectedSongIds
                        )
                        // Clean up the UI state after initiating the delete action.
                        viewModel.setSelectedSongIds(emptySet())
                        viewModel.setShowBottomSheet(false)
                    }
                )
            }
        }
    }

    // This block handles the display of the "Add to Playlist" dialog.
    // It becomes visible when `showAddToPlaylistDialog` is set to true, typically// after the user selects the "Add to playlist" option from the bottom sheet menu.
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            // Pass the current list of saved playlists to the dialog to be displayed.
            playlists = savedPlaylists,
            // The action to perform when the user dismisses the dialog (e.g., by tapping outside).
            onDismiss = { showAddToPlaylistDialog = false },

            // This callback is invoked when the user selects a regular, user-created playlist.
            onPlaylistSelected = { playlistId ->
                // Determine which songs to add. It's either the multi-selection set
                // or the single song from the context menu.
                val idsToAdd = if (isSelectionMode) {
                    selectedSongIds.toList()
                } else {
                    listOfNotNull(songForMenu?.id)
                }

                // Call the ViewModel to handle the database operation of adding songs to the playlist.
                viewModel.addSongsToPlaylist(playlistId, idsToAdd)
                Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()

                // --- Cleanup ---
                // Reset the UI state after the operation is complete.
                showAddToPlaylistDialog = false
                viewModel.setSelectionMode(false) // Exit selection mode.
                viewModel.setSelectedSongIds(emptySet()) // Clear the selection.
                songForMenu = null // Clear the single song context.
            },

            // This callback is invoked when the user specifically selects the "Favorites" option.
            onFavoritesSelected = {
                // The logic to get the song IDs is the same as above.
                val idsToAdd = if (isSelectionMode) {
                    selectedSongIds.toList()
                } else {
                    listOfNotNull(songForMenu?.id)
                }

                // Find the special "Favorites" playlist from the list of saved playlists.
                val favoritesPlaylist =
                    savedPlaylists.find { it.isSpecial } // Searching by `isSpecial` is more robust.

                if (favoritesPlaylist != null) {
                    // If the "Favorites" playlist exists, add the songs to it.
                    viewModel.addSongsToPlaylist(favoritesPlaylist.id, idsToAdd)
                    Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
                } else {
                    // This case handles a scenario where the special playlist hasn't been created yet.
                    // A more robust implementation might create it on the fly here.
                    println("Favorites playlist not found. Implement creation logic if needed.")
                }

                // --- Cleanup ---
                // The same cleanup logic is needed here to reset the UI state.
                showAddToPlaylistDialog = false
                viewModel.setSelectionMode(false)
                viewModel.setSelectedSongIds(emptySet())
                songForMenu = null
            }
        )
    }
}
