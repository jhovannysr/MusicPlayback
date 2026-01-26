package com.jhovanny.musicplayback.ui.screens.playlistDetailScreen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.ui.components.AddToPlaylistDialog
import com.jhovanny.musicplayback.ui.components.MiniPlayer
import com.jhovanny.musicplayback.ui.components.SongItemPersonalize
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.ui.components.ScrollComponent
import com.jhovanny.musicplayback.ui.screens.musicListScreen.components.MenuOptionItemMinimal
import com.jhovanny.musicplayback.ui.theme.IconPrimary
import com.jhovanny.musicplayback.ui.theme.TextWarning
import com.jhovanny.musicplayback.utils.PlaylistConstants

/**
 * A Composable that displays the contents of a specific playlist.
 *
 * This screen shows the list of songs within a given playlist (like "Favorites", "Recents",
 * or any user-created list). It supports various user interactions, including playing songs,
 * reordering them (drag and drop), a multi-selection mode for batch operations (like adding to another
 * playlist or deleting), and navigating to other screens.
 *
 * @param playlistId The unique ID of the playlist to be displayed. This is crucial for fetching the correct songs.
 * @param playlistName The name of the playlist, used for display in the TopAppBar and for special logic (e.g., "Recents").
 * @param viewModel An instance of [PlayerViewModel] that provides the business logic and state for this screen.
 * @param onBack A lambda function to be invoked to navigate back to the previous screen.
 * @param onNavigateToAddSongs A lambda function that triggers navigation to a screen where the user can add more songs to the current playlist. It passes the current `playlistId`.
 * @param onDeletePlaylist A lambda function to be invoked when the user chooses to delete the entire current playlist.
 * @param onNavigateToPlayer A lambda function that triggers navigation to the full-screen player UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToAddSongs: (Long) -> Unit,
    onDeletePlaylist: () -> Unit,
    onNavigateToPlayer: () -> Unit,
) {
    LaunchedEffect(playlistId) {
        println("LaunchedEffect playlistId")
        viewModel.loadPlaylistSongs(playlistId)
    }

    // Local context
    val context = LocalContext.current

    // 1. PREPARE THE LAUNCHER FOR DELETE PERMISSION
    // This launcher handles the result from the system's confirmation dialog
    // when requesting permission to delete media files (required for Android 10+).
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User confirmed the deletion in the system dialog.
            viewModel.onSongsDeletedSuccess()
            // Optional: Show a Toast or Snackbar to confirm.
            // Toast.makeText(context, "Songs deleted", Toast.LENGTH_SHORT).show()
        } else {
            // User denied permission or cancelled the dialog; exit selection mode.
            viewModel.setSelectionMode(false)
        }
    }

    // --- Constants ---
    // Local constants for special playlist names and IDs for easy access.
    val NAME_FAVORITES = PlaylistConstants.NAME_FAVORITES
    val NAME_RECENTS = PlaylistConstants.NAME_RECENT
    val ID_FAVORITES = PlaylistConstants.ID_FAVORITES
    val ID_RECENT = PlaylistConstants.ID_RECENT

    // --- ViewModel State Observation ---
    // The list of songs for the current playlist.
    // It uses `remember` with keys to re-trigger the flow collection only when
    // the playlist ID or name changes.
    val songs by remember(playlistId, playlistName) {
        // If viewing the "Recents" playlist, use the specific flow for it.
        if (playlistName == NAME_RECENTS) {
            viewModel.recentSongs
        } else {
            // For any other playlist (e.g., Favorites or user-created), use the standard logic.
            viewModel.getSongsForPlaylistFlow(playlistId)
        }
    }.collectAsState(initial = emptyList())

    // State for the currently playing song to highlight it in the list.
    val currentSong by viewModel.currentSong.collectAsState()
    // State to manage the play/pause status of the mini-player.
    val isPlaying by viewModel.isPlaying.collectAsState()
    // States for the progress bar in the mini-player.
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    // --- UI and Menu States ---
    // Manages the visibility of the playlist's options menu (in the TopAppBar).
    var showPlaylistOptions by remember { mutableStateOf(false) }

    // Determines if the MiniPlayer should be visible at the bottom of the screen.
    val showMiniPlayer = currentSong != null

    // Manages the state for the options BottomSheet menu.
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Ensures the bottom sheet always fully expands.
    )
    // Holds the specific song for which the bottom sheet menu was opened.
    var songForMenuParams by remember { mutableStateOf<Song?>(null) }

    // Status to control the visibility of the delete confirmation dialog.
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // --- Selection Mode States ---
    // A set containing the IDs of all songs selected by the user.
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    // Holds the song when a menu is opened for a single item.
    var songForMenu by remember { mutableStateOf<Song?>(null) }
    // A boolean flag indicating if the UI is currently in multi-selection mode.
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    // --- Playlist Dialog States ---
    // The list of all saved playlists, used for the "Add to Playlist" dialog.
    val savedPlaylists by viewModel.savedPlaylists.collectAsState()
    // Manages the visibility of the "Add to Playlist" dialog.
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // --- Song Reordering States ---
    // The state for the LazyColumn, used to control scrolling.
    val listState = rememberLazyListState()
    // The state for the reorderable list. This callback is triggered when the user
    // drops an item in its new position, persisting the change.
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.reorderPlaylist(playlistId, from.index, to.index)
    }

    // A coroutine scope for launching async operations like scrolling.
    val scope = rememberCoroutineScope()

    // --- Screen Navigation and Back Handling ---
    // State to prevent multiple back navigation calls while exiting.
    var isExiting by remember { mutableStateOf(false) }

    // A unified and safe function to handle exiting the screen.
    val exitScreen = {
        if (!isExiting) { // Prevents double calls.
            isExiting = true
            onBack()
        }
    }

    // --- Back Handlers ---
    // Intercepts the system back button press to ensure safe exit logic is used.
    BackHandler(enabled = !isExiting) {
        exitScreen()
    }

    // Intercepts the back button press only when in selection mode.
    BackHandler(enabled = isSelectionMode) {
        // The action is to exit selection mode and clear any selected items.
        viewModel.setSelectionMode(false)
        viewModel.setSelectedSongIds(emptySet())
    }

    // AlertDialog to confirm the deletion of the playlist
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete playlist") },
            text = { Text("Are you sure you want to delete \"$playlistName\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeletePlaylist()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // The main layout structure for the screen, using Scaffold to easily place
    // the TopAppBar and a potential BottomAppBar (for the MiniPlayer).
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // The TopAppBar's content changes based on whether selection mode is active.
            if (isSelectionMode) {
                // --- TopAppBar for Selection Mode ---
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
                    // Displays the number of selected items.
                    title = {
                        Text(
                            text = "${selectedSongIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    // A close button to exit selection mode.
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.setSelectionMode(false)
                            viewModel.setSelectedSongIds(emptySet())
                        }) {
                            Icon(
                                Icons.Default.Close, contentDescription = "Cancel",
                                tint = IconPrimary
                            )
                        }
                    },
                    // An options button (3-dots) for actions on the selected items.
                    actions = {
                        IconButton(onClick = { viewModel.setShowBottomSheet(true) }) {
                            Icon(
                                Icons.Default.MoreVert, contentDescription = "Options",
                                tint = IconPrimary
                            )
                        }
                    }
                )
            } else {
                // --- Default TopAppBar for Viewing the Playlist ---
                TopAppBar(
                    // The title is customized for the "Recents" playlist.
                    title = if (playlistName == "Recientes") {
                        {
                            Text(
                                "Recents (30 days)",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        { Text(if(playlistName == "Favoritas") {"Favorites"} else {playlistName}, color = MaterialTheme.colorScheme.onSurface) }
                    },
                    // The standard back button.
                    navigationIcon = {
                        IconButton(onClick = {
                            // Uses the safe exit function to navigate back.
                            exitScreen()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = IconPrimary
                            )
                        }
                    },
                    // An options button for playlist-level actions (e.g., rename, delete playlist).
                    actions = {
                        IconButton(onClick = { showPlaylistOptions = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = IconPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

            }
        },
        // --- Bottom Bar ---
        // The bottom bar is reserved for the MiniPlayer.
        bottomBar = {
            // The MiniPlayer is only shown if there is a currently playing song.
            if (showMiniPlayer && currentSong != null) {
                MiniPlayer(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition, // Pass the current playback position.
                    duration = duration,
                    onPlayPause = { viewModel.togglePlayPause() },
                    // Pass the callbacks for next and previous track actions.
                    onNext = { viewModel.skipNext() },
                    onPrev = { viewModel.skipPrevious() },
                    onClick = onNavigateToPlayer // Navigate to the full player on click.
                )
            }
        }
    ) { padding ->
    Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),

            ) {
            if (songs.isEmpty() && playlistId != ID_RECENT) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("This playlist is empty", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onNavigateToAddSongs(playlistId) }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Add Songs",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                println("Listas de canciones size: " + songs.size)
                // Songs lists
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                )
                {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->

                        ReorderableItem(reorderableState, key = song.id) { isDragging ->

                            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                            val backgroundColor =
                                if (isDragging) MaterialTheme.colorScheme.surface else Color.Transparent

                            // Songs states
                            val isPlaying = song.id == currentSong?.id
                            val isSelectedInMode = selectedSongIds.contains(song.id)

                            Surface(
                                shadowElevation = elevation,
                                color = Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                SongItemPersonalize(
                                    song = song,
                                    isSelected = isPlaying,
                                    isSelectionMode = isSelectionMode,
                                    isSelectedInMode = isSelectedInMode,

                                    // Short cick
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleSelection(song.id)
                                        } else {
                                            viewModel.playSongsFromPlaylist(songs, index)
                                            onNavigateToPlayer()
                                        }
                                    },

                                    // Long click
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            // Entrar en Modo Selección_
                                            songForMenuParams = null
                                            viewModel.setSelectionMode(true)
                                            viewModel.setSelectedSongIds(setOf(song.id))
                                        } else {
                                            viewModel.toggleSelection(song.id)
                                        }
                                    },

                                    // Individual Menú
                                    onMoreClick = {
                                        if (!isSelectionMode) {
                                            songForMenuParams = song
                                            viewModel.setSelectedSongIds(setOf(song.id))
                                            songForMenu = song
                                            viewModel.setShowBottomSheet(true)
                                        }
                                    },

                                    dragModifier = if (playlistId != ID_RECENT) {
                                        println("playlistId $playlistId")
                                        println("Otras Listas")
                                        println("ID_RECENT $ID_RECENT")
                                        androidx.compose.ui.Modifier.draggableHandle()
                                    } else {
                                        println("playlistId $playlistId")
                                        println("Lista recientes")
                                        androidx.compose.ui.Modifier
                                            .alpha(0f)
                                            .size(0.dp)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            println("songs: " + songs.size)
            println("listState: " + listState)
            println("currentSong: " + currentSong?.title)
            // --- SCROLLBAR LATERAL
            ScrollComponent(songs, listState, currentSong, scope)

            // --- PLAYLIST MENU (Bottom Sheet) ---
            if (showPlaylistOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showPlaylistOptions = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF121212).copy(alpha = 0.98f),
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = if(playlistName == "Favoritas") {"Favorites"} else if(playlistName == "Recientes") {"Recents"} else {playlistName},
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (playlistId != ID_RECENT && playlistId != ID_FAVORITES) {
                            MenuOptionItemMinimal(
                                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                text = "Add Songs",
                                onClick = {
                                    showPlaylistOptions = false
                                    onNavigateToAddSongs(playlistId)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MenuOptionItemMinimal(
                                icon = Icons.Default.Delete,
                                text = "Delete Playlist",
                                textColor = TextWarning.copy(alpha = 0.8f),
                                onClick = {
                                    showPlaylistOptions = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        } else if(playlistId == ID_RECENT){
                            MenuOptionItemMinimal(
                                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                text = "Add to playlist",
                                onClick = {
                                    showPlaylistOptions = false
                                    viewModel.setSelectionMode(true)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MenuOptionItemMinimal(
                                icon = Icons.Default.Delete,
                                text = "Delete Songs",
                                textColor = TextWarning.copy(alpha = 0.8f),
                                onClick = {
                                    showPlaylistOptions = false
                                    viewModel.setSelectionMode(true)
                                }
                            )
                        } else if(playlistId == ID_FAVORITES){
                            MenuOptionItemMinimal(
                                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                text = "Add Songs",
                                onClick = {
                                    showPlaylistOptions = false
                                    onNavigateToAddSongs(playlistId)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MenuOptionItemMinimal(
                                icon = Icons.Default.Delete,
                                text = "Delete Songs",
                                textColor = TextWarning.copy(alpha = 0.8f),
                                onClick = {
                                    showPlaylistOptions = false
                                    viewModel.setSelectionMode(true)
                                }
                            )
                        }
                    }
                }
            }

            // --- BOTTOM SHEET ---
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
                        if (songForMenuParams != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // SMALL COVER
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.DarkGray,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(songForMenuParams?.coverUri)
                                            .crossfade(true)
                                            .error(android.R.drawable.ic_menu_help) // Icono por defecto si falla
                                            .build(),
                                        contentDescription = "Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Title and artist
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
                        // Selection Mode or generic menu
                        else {
                            Text(
                                text = "Options",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }
                        MenuOptionItemMinimal(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            text = "Add to playlist",
                            onClick = {
                                viewModel.setShowBottomSheet(false)
                                showAddToPlaylistDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MenuOptionItemMinimal(
                            icon = Icons.Default.SelectAll,
                            text = "Select all",
                            onClick = {
                                viewModel.selectAllSongs(songs)
                                viewModel.setShowBottomSheet(false)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MenuOptionItemMinimal(
                            icon = Icons.Default.Delete,
                            text = "Delete Songs",
                            textColor = Color(0xFFFF5252),
                            containerColor = Color(0xFFFF5252).copy(alpha = 0.1f),
                            onClick = {
                                viewModel.deleteSongs(
                                    context = context,
                                    onSuccess = {
                                        viewModel.onSongsDeletedSuccess(selectedSongIds.toList())
                                    },
                                    onNeedPermission = { intentSender ->
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
                        if (playlistId != ID_RECENT) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MenuOptionItemMinimal(
                                icon = Icons.Default.Delete,
                                text = "Remove from playlist",
                                textColor = Color(0xFFFF5252),
                                containerColor = Color(0xFFFF5252).copy(alpha = 0.1f),
                                onClick = {
                                    if (selectedSongIds.isNotEmpty()) {
                                        viewModel.removeSongsFromPlaylist(
                                            playlistId = playlistId,
                                            songIds = selectedSongIds.toList()
                                        )
                                    }
                                    viewModel.setShowBottomSheet(false)
                                }
                            )
                        }
                    }
                }
            }

            // Menu to add to playlist
            if (showAddToPlaylistDialog) {
                AddToPlaylistDialog(
                    playlists = savedPlaylists,
                    onDismiss = { showAddToPlaylistDialog = false },

                    onPlaylistSelected = { playlistId ->
                        val idsToAdd = if (isSelectionMode) {
                            selectedSongIds.toList()
                        } else {
                            listOfNotNull(songForMenu?.id)
                        }

                        viewModel.addSongsToPlaylist(playlistId, idsToAdd)
                        Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()

                        showAddToPlaylistDialog = false
                        viewModel.setSelectionMode(false)
                        viewModel.setSelectedSongIds(emptySet())
                    },

                    // Add to Favourites
                    onFavoritesSelected = {
                        val idsToAdd = if (isSelectionMode) {
                            selectedSongIds.toList()
                        } else {
                            listOfNotNull(songForMenu?.id)
                        }

                        val favoritesPlaylist =
                            savedPlaylists.find { it.name == NAME_FAVORITES || it.isSpecial }

                        if (favoritesPlaylist != null) {
                            viewModel.addSongsToPlaylist(favoritesPlaylist.id, idsToAdd)
                            Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                        } else {
                            println("Debes crear la playlist Favoritas primero o implementar la lógica específica")
                        }

                        showAddToPlaylistDialog = false
                        viewModel.setSelectionMode(false)
                        viewModel.setSelectedSongIds(emptySet())
                    }
                )
            }
        }
    }

    // Invisible shield for changing playlists
    // If isExiting is true, we place an invisible layer on top that captures all clicks.
    if (isExiting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                )
        )
    }
}