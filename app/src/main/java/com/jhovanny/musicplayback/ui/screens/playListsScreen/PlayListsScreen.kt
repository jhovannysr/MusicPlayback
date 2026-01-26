package com.jhovanny.musicplayback.ui.screensimport

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jhovanny.musicplayback.data.local.entities.PlaylistEntity
import com.jhovanny.musicplayback.ui.screens.musicListScreen.components.MenuOptionItemMinimal
import com.jhovanny.musicplayback.ui.screens.playListsScreen.components.CreatePlaylistDialog
import com.jhovanny.musicplayback.ui.screens.playListsScreen.components.MenuOptionItem
import com.jhovanny.musicplayback.ui.screens.playListsScreen.components.SpecialPlaylistItem
import com.jhovanny.musicplayback.ui.screens.playListsScreen.components.UserPlaylistItem
import com.jhovanny.musicplayback.ui.theme.CardBackgroundWhite
import com.jhovanny.musicplayback.ui.theme.IconPrimary
import com.jhovanny.musicplayback.ui.theme.TextWarning
import com.jhovanny.musicplayback.ui.theme.TextGrayDark
import com.jhovanny.musicplayback.utils.PlaylistConstants
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

/**
 * `PlayListsScreen` is a Jetpack Compose screen that serves as the main library view
 * for all playlists. It organizes playlists into two distinct sections:
 *
 * 1.  **Special Playlists**: A horizontal row at the top for system-managed lists
 *     like "Favorites" and "Recents".
 * 2.  **User Playlists**: A vertical grid displaying all playlists created by the user.
 *
 * The screen includes a Floating Action Button (FAB) to create new playlists and
 * provides functionality to manage existing ones (e.g., add songs, delete) through
 * dialogs and bottom sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayListsScreen(
    viewModel: PlayerViewModel,
    onNavigateToDetail: (Long, String) -> Unit, // Callback to navigate to a playlist's detail view.
    onNavigateToAddSongs: (Long) -> Unit     // Callback to navigate to the "Add Songs" screen.
) {
    // Observe the list of special playlists ("Favorites", "Recents") from the ViewModel.
    val specialPlaylists by viewModel.specialPlaylistsStats.collectAsState()
    // Observe the list of user-created playlists.
    val userPlaylists by viewModel.userPlaylistsStats.collectAsState()

    // Holds the playlist that is currently selected for showing the options menu. Null if no menu is shown.
    var selectedPlaylistForMenu by remember { mutableStateOf<PlaylistUiState?>(null) }
    // State to control the visibility of the "Create Playlist" dialog.
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    // State to control the visibility of the "Delete Playlist" confirmation dialog.
    var showDeleteConfirmDialog by remember { mutableStateOf<PlaylistUiState?>(null) }

    // State for controlling the ModalBottomSheet (options menu).
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // A reusable function to safely close the bottom sheet menu.
    val closeMenu: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                selectedPlaylistForMenu = null // Clear selection only after it's fully hidden.
            }
        }
    }

    // --- CONSTANTS ---
    val NAME_FAVORITES = PlaylistConstants.NAME_FAVORITES
    val NAME_RECENTS = PlaylistConstants.NAME_RECENT
    val ID_FAVORITES = PlaylistConstants.ID_FAVORITES
    val ID_RECENT = PlaylistConstants.ID_RECENT

    Scaffold(
        containerColor = Color.Transparent, // Make the background see-through.
        floatingActionButton = {
            // FAB to open the dialog for creating a new playlist.
            FloatingActionButton(
                onClick = { showCreatePlaylistDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new playlist")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Respetamos el padding del Scaffold si fuera necesario
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Special Playlists Section (Favorites / Recents)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()            .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Defines the special playlists to be displayed and their order.
//                    val targetNames = listOf("Liked Songs", "Recents")
                     val targetNames = listOf(NAME_FAVORITES, NAME_RECENTS)

                    targetNames.forEachIndexed { index, targetName ->

                        // Finds the actual playlist data from the state.
                        val realPlaylist = specialPlaylists.find { it.name == targetName }

                        // If the playlist exists, its data is used. Otherwise, a placeholder is shown while it loads.
                        val playlistToDisplay = realPlaylist ?: PlaylistUiState(
                            id = if (targetName == NAME_FAVORITES) ID_FAVORITES else ID_RECENT,
                            name = targetName,
                            songCount = 0,
                            isSpecial = true
                        )

                        // Logic to prevent double-clicks.
                        var lastClickTime by remember { mutableLongStateOf(0L) }
                        val icon = if (targetName == NAME_FAVORITES) Icons.Default.Favorite else Icons.Default.AccessTime

                        SpecialPlaylistItem(
                            playlist = playlistToDisplay,
                            icon = icon,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 500) {
                                    lastClickTime = currentTime

                                    onNavigateToDetail(playlistToDisplay.id, playlistToDisplay.name)
                                }
                            }
                        )

                        // Adds space between the special playlist items.
                        if (index < targetNames.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                // 2. Separator Title
                Text(
                    text = "Your Playlists",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 3. User Playlists Grid
                if (userPlaylists.isEmpty()) {
                    // Displays a message when there are no user-created playlists.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("You have no created playlists", color = Color.Gray)
                    }
                } else {
                    // A variable to manage click debounce for the entire grid.
                    var lastClickTime by remember { mutableLongStateOf(0L) }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 80.dp // Extra bottom padding for the FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(userPlaylists) { playlist ->
                            UserPlaylistItem(
                                playlist = playlist,
                                onClick = {
                                    // Click debounce logic to prevent multiple rapid navigations.
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime > 500) { // 500ms cooldown
                                        lastClickTime = currentTime

                                        // Navigates to the detail screen of the selected playlist.
                                        onNavigateToDetail(playlist.id, playlist.name)
                                    }
                                },
                                onShowMenu = { selectedPlaylistForMenu = playlist }
                            )
                        }
                    }
                }
            }

            // Options Menu (Bottom Sheet)
            // Displays a modal bottom sheet when a playlist is selected for the menu.
            if (selectedPlaylistForMenu != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedPlaylistForMenu = null },
                    sheetState = sheetState,
                    containerColor = Color.Black.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Displays the name of the selected playlist.
                        Text(
                            text = selectedPlaylistForMenu!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // OPTION 1: Add songs
                        // Navigates to the screen for adding songs to the current playlist.
                        MenuOptionItem(
                            icon = Icons.Default.PlaylistAdd,
                            text = "Add songs",
                            onClick = {
                                val plId = selectedPlaylistForMenu?.id
                                closeMenu() // Closes the bottom sheet.
                                if (plId != null) {
                                    onNavigateToAddSongs(plId)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // OPTION 2: Delete playlist
                        // Shows a confirmation dialog to delete the current playlist.
                        MenuOptionItem(
                            icon = Icons.Default.Delete,
                            text = "Delete playlist",
                            textColor = TextWarning.copy(alpha = 0.8f),
                            onClick = {
                                val pl = selectedPlaylistForMenu
                                closeMenu()
                                showDeleteConfirmDialog = pl // Shows the confirmation dialog.
                            }
                        )
                    }
                }
            }

            // Create Playlist Dialog
            // Shows a dialog to create a new playlist.
            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onCreate = { playlistName ->
                        // DATABASE CALL
                        viewModel.createPlaylist(playlistName)
                        showCreatePlaylistDialog = false
                    }
                )
            }

            // Delete Confirmation Dialog
            // Shows a confirmation dialog before deleting a playlist.
            if (showDeleteConfirmDialog != null) {
                val playlistUi = showDeleteConfirmDialog!!
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = null },
                    title = { Text("Delete playlist") },
                    text = { Text("Are you sure you want to delete \"${playlistUi.name}\"?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Converts UiState back to an Entity to delete it.
                                val entityToDelete =
                                    PlaylistEntity(
                                        id = playlistUi.id,
                                        name = playlistUi.name,
                                        isSpecial = playlistUi.isSpecial
                                    )

                                // DATABASE CALL
                                viewModel.deletePlaylist(entityToDelete)

                                showDeleteConfirmDialog = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Displays a modal bottom sheet when a playlist is selected via the menu.
            if (selectedPlaylistForMenu != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedPlaylistForMenu = null },
                    sheetState = sheetState,
                    containerColor = Color(0xFF121212).copy(alpha = 0.98f),
                    contentColor = Color.White
                ) {
                    // Content layout for the bottom sheet.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Displays the title of the selected playlist.
                        Text(
                            text = selectedPlaylistForMenu!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // OPTION 1: Add songs
                        // This menu item navigates to the screen for adding songs to the playlist.
                        MenuOptionItemMinimal(
                            icon = Icons.Default.PlaylistAdd,
                            text = "Add songs",
                            onClick = {
                                val plId = selectedPlaylistForMenu?.id
                                closeMenu() // Closes the bottom sheet.
                                if (plId != null) {
                                    onNavigateToAddSongs(plId)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // OPTION 2: Delete playlist
                        // This menu item triggers a confirmation dialog for deleting the playlist.
                        MenuOptionItemMinimal(
                            icon = Icons.Default.Delete,
                            text = "Delete playlist",
                            textColor = TextWarning.copy(alpha = 0.8f),
                            onClick = {
                                val pl = selectedPlaylistForMenu
                                closeMenu()
                                // Shows the confirmation dialog.
                                showDeleteConfirmDialog = pl
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- DATA MODEL (Local para UI) ---
data class PlaylistUiState(
    val id: Long,
    val name: String,
    val songCount: Int,
    val isSpecial: Boolean = false,
    val coverUri: Any? = null
)

