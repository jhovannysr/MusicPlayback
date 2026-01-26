package com.jhovanny.musicplayback.ui.screens.searchScreen

import android.R
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.ui.components.AddToPlaylistDialog
import com.jhovanny.musicplayback.ui.components.ScrollComponent
import com.jhovanny.musicplayback.ui.components.SongItem
import com.jhovanny.musicplayback.ui.screens.musicListScreen.components.MenuOptionItemMinimal
import com.jhovanny.musicplayback.ui.theme.IconPrimary
import com.jhovanny.musicplayback.ui.theme.TextWhiteSecondary
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel

/**
 * A Composable that provides a full-screen search interface for finding songs.
 *
 * This screen features a dynamic TopAppBar that adapts for searching and for a multi-select
 * "selection mode". It automatically requests keyboard focus on entry. Results are displayed
 * in a lazy list, and users can interact with songs to play them, add them to playlists,
 * or delete them. It handles its own state logic by observing a [PlayerViewModel].
 *
 * @param viewModel The instance of [PlayerViewModel] that holds the business logic and state
 *   for the search functionality, player controls, and song management.
 * @param onNavigateBack A lambda function to be invoked to navigate back from this screen,
 *   typically called when the user cancels the search.
 * @param onNavigateToPlayer A lambda function to navigate to the main player view, although it is
 *   not directly used in this snippet, it's part of the component's contract.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    // Local context reference.
    val context = LocalContext.current

    // 1. PREPARE THE LAUNCHER FOR DELETE PERMISSION
    // Handles the result from the system dialog for deleting files (Android 10+).
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User confirmed the deletion in the system dialog.
            viewModel.onSongsDeletedSuccess()
        } else {
            // User cancelled the operation; exit selection mode.
            viewModel.setSelectionMode(false)
        }
    }

    // States observed from the ViewModel.
    val query by viewModel.searchQuery.collectAsState()
    val songs by viewModel.searchResults.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    // State for the options BottomSheet menu.
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    var songForMenuParams by remember { mutableStateOf<Song?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // States for multi-selection mode.
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    var songForMenu by remember { mutableStateOf<Song?>(null) }
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    // State related to user playlists for the "Add to Playlist" dialog.
    val savedPlaylists by viewModel.savedPlaylists.collectAsState()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Controls for automatically focusing the search field and showing the keyboard.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // State for managing the scroll position of the results list.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Effect to request focus and show the keyboard when the screen is first displayed.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Overrides the default back button behavior when in selection mode.
    BackHandler(enabled = isSelectionMode) {
        // Action on back press: Exit selection mode and clear selections.
        viewModel.setSelectionMode(false)
        viewModel.setSelectedSongIds(emptySet())
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // The TopAppBar's content changes based on whether selection mode is active.
            if (isSelectionMode) {
                // TopAppBar for Selection Mode.
                TopAppBar(
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    ),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            text = "${selectedSongIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        // Close button to exit selection mode.
                        IconButton(onClick = {
                            viewModel.setSelectionMode(false)
                            viewModel.setSelectedSongIds(emptySet())
                        }) {
                            Icon(
                                Icons.Default.Close, contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Options menu for selected items.
                        IconButton(onClick = { viewModel.setShowBottomSheet(true) }) {
                            Icon(
                                Icons.Default.MoreVert, contentDescription = "Options",
                                tint = IconPrimary
                            )
                        }
                    }
                )
            } else {
                // TopAppBar for Standard Search.
                TopAppBar(
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    ),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        // Search input field.
                        TextField(
                            value = query,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search for a song...", color = MaterialTheme.colorScheme.onSurface) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            // Trailing icon to clear the search text.
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = IconPrimary
                                        )
                                    }
                                }
                            }
                        )
                    },
                    // Decorative search icon on the left.
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                            tint = IconPrimary
                        )
                    },
                    // Cancel button on the right to exit the search screen.
                    actions = {
                        TextButton(onClick = {
                            // Clear search and navigate back.
                            viewModel.onSearchQueryChanged("")
                            onNavigateBack()
                        }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding) // Applies padding from the Scaffold (e.g., for the TopAppBar).
                .fillMaxSize()
        ) {

            // CASE 1: Empty search query (initial state).
            if (query.isEmpty()) {
                // Display a large, centered search icon as a placeholder.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null, // Decorative icon.
                        modifier = Modifier.size(100.dp),
                        tint = IconPrimary
                    )
                }
            }
            // CASE 2: Search has text but yields no results.
            else if (songs.isEmpty()) {
                // Display a "No results" message in the center.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No results",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhiteSecondary
                    )
                }
            }
            // CASE 3: Display the list of search results.
            else {
                LazyColumn(
                    state = listState, // Connects the list state for scrolling control.
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    itemsIndexed(songs) { index, song ->
                        // Calculate individual item states.
                        val isSelectedInMode = selectedSongIds.contains(song.id)
                        val isPlaying = song.id == currentSong?.id // Highlights the currently playing song.

                        SongItem(
                            song = song,
                            isSelected = isPlaying,
                            isSelectionMode = isSelectionMode,
                            isSelectedInMode = isSelectedInMode,
                            // --- CLICK LOGIC ---
                            onClick = {
                                if (isSelectionMode) {
                                    // In selection mode, a click toggles the item's selection state.
                                    viewModel.toggleSelection(song.id)
                                } else {
                                    // Otherwise, play the song and navigate to the player.
                                    viewModel.playSongsFromPlaylist(songs, index)
                                    onNavigateToPlayer()
                                }
                            },

                            // --- LONG-CLICK LOGIC ---
                            onLongClick = {
                                if (!isSelectionMode) {
                                    songForMenuParams = null
                                    // Enter selection mode and select the long-pressed item.
                                    viewModel.setSelectionMode(true)
                                    viewModel.setSelectedSongIds(setOf(song.id))
                                } else {
                                    // If already in selection mode, toggle the item's selection.
                                    viewModel.toggleSelection(song.id)
                                }
                            },

                            // --- INDIVIDUAL MENU (3-dots on the row) ---
                            onMoreClick = {
                                if (!isSelectionMode) {
                                    // Show the bottom sheet menu for this specific song.
                                    songForMenuParams = song
                                    viewModel.setSelectedSongIds(setOf(song.id))
                                    songForMenu = song
                                    viewModel.setShowBottomSheet(true)
                                }
                            }
                        )
                    }
                }
            }

            // --- SIDE SCROLLBAR ---
            // A custom component to display a fast-scroll bar for the list.
            ScrollComponent(songs, listState, currentSong, scope)
        }
    }

    // --- BOTTOM SHEET MENU ---
    // This modal bottom sheet is displayed to show options for a selected song or a group of songs.
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowBottomSheet(false) },
            sheetState = sheetState,
            containerColor = Color(0xFF121212).copy(alpha = 0.98f),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // CASE 1: Menu for a specific song (user tapped the 3-dots icon).
                if (songForMenuParams != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. SMALL COVER ART
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.DarkGray,
                            modifier = Modifier.size(56.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(songForMenuParams?.coverUri)
                                    .crossfade(true)
                                    .error(R.drawable.ic_menu_help) // Default icon on failure.
                                    .build(),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 2. TITLE AND ARTIST
                        Column {
                            Text(
                                text = songForMenuParams?.title ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = songForMenuParams?.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // CASE 2: Selection Mode or generic menu (no specific song).
                else {
                    Text(
                        text = "Options", // Or you could display "${selectedSongIds.size} selected"
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }

                // OPTION 1: ADD TO PLAYLIST
                MenuOptionItemMinimal(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    text = "Add to playlist",
                    onClick = {
                        viewModel.setShowBottomSheet(false)
                        showAddToPlaylistDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OPTION 2: SELECT ALL
                MenuOptionItemMinimal(
                    icon = Icons.Default.SelectAll,
                    text = "Select all",
                    onClick = {
                        viewModel.selectAllSongs(songs)
                        viewModel.setShowBottomSheet(false)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OPTION 3: DELETE (With a subtle red background)
                MenuOptionItemMinimal(
                    icon = Icons.Default.Delete,
                    text = "Delete Song",
                    textColor = Color(0xFFFF5252),
                    containerColor = Color(0xFFFF5252).copy(alpha = 0.1f),
                    onClick = {
                        viewModel.deleteSongs(
                            context = context,
                            onSuccess = {
                                viewModel.onSongsDeletedSuccess(selectedSongIds.toList())
                                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                            },
                            onNeedPermission = { intentSender ->
                                // Launches the system dialog to request file deletion permission.
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(intentSender).build()
                                deleteLauncher.launch(intentSenderRequest)
                            },
                            selectedSong = selectedSongIds
                        )
                        viewModel.setSelectedSongIds(emptySet())
                        viewModel.setShowBottomSheet(false)
                    }
                )
            }
        }
    }

    // --- "ADD TO PLAYLIST" DIALOG ---
    // This dialog is shown to allow the user to add selected songs to a playlist.
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = savedPlaylists, // Pass the list of available playlists.
            onDismiss = { showAddToPlaylistDialog = false },

            // CASE 1: ADD TO A CUSTOM PLAYLIST
            onPlaylistSelected = { playlistId ->
                // A. Determine which song IDs to add.
                val idsToAdd = if (isSelectionMode) {
                    selectedSongIds.toList() // Convert the Set to a List<Long>
                } else {
                    listOfNotNull(songForMenu?.id) // Add only the single song from the menu.
                }

                // B. Call the ViewModel function to perform the action.
                viewModel.addSongsToPlaylist(playlistId, idsToAdd)

                // C. Clean up UI state.
                showAddToPlaylistDialog = false
                viewModel.setSelectionMode(false)
                viewModel.setSelectedSongIds(emptySet())
                songForMenu = null
            },

            // CASE 2: ADD TO FAVORITES
            onFavoritesSelected = {
                // A. Get the song IDs to add.
                val idsToAdd = if (isSelectionMode) {
                    selectedSongIds.toList()
                } else {
                    listOfNotNull(songForMenu?.id)
                }

                // B. Find the special "Favorites" playlist.
                val favoritesPlaylist =
                    savedPlaylists.find { it.name == "Favorites" || it.isSpecial }

                if (favoritesPlaylist != null) {
                    viewModel.addSongsToPlaylist(favoritesPlaylist.id, idsToAdd)
                } else {
                    // This could be handled by creating the playlist or showing a message.
                    println("Favorites playlist must be created first or specific logic implemented.")
                }

                // C. Clean up UI state.
                showAddToPlaylistDialog = false
                viewModel.setSelectionMode(false)
                viewModel.setSelectedSongIds(emptySet())
                songForMenu = null
            }
        )
    }
}
