package com.jhovanny.musicplayback.ui.screens.addSongsScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhovanny.musicplayback.ui.screens.addSongsScreen.components.SelectableSongItemGlass
import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple
import com.jhovanny.musicplayback.viewmodel.PlayerViewModel

/**
 * `AddSongsScreen` is a Jetpack Compose screen that allows a user to select multiple
 * songs from their device's library and add them to a specific playlist.
 *
 * It displays a list of songs that are not already in the target playlist. The user
 * can tap on songs to select them. A top bar shows a count of selected items, and a
 * Floating Action Button (FAB) appears to confirm the addition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsScreen(
    playlistId: Long,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onSongsAdded: () -> Unit
) {
    // Observe the full list of songs from the ViewModel.
    val allSongs by viewModel.originalSongList.collectAsState()

    // When the screen loads, trigger the ViewModel to load the songs already in the current playlist.
    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistSongs(playlistId)
    }
    // Observe the list of songs already present in the playlist.
    val currentPlaylistSongs by viewModel.currentPlaylistSongs.collectAsState()

    // Filter the device's songs to get a list of songs available to be added.
    // This is memoized with 'remember' for performance, re-calculating only when the source lists change.
    val songsAvailableToAdd = remember(allSongs, currentPlaylistSongs) {
        val currentIds = currentPlaylistSongs.map { it.id }.toSet()
        allSongs.filter { !currentIds.contains(it.id) }
    }

    // A stateful list to keep track of which songs the user has selected.
    // Using mutableStateListOf ensures that UI components observing this list will recompose on changes.
    val selectedIds = remember { mutableStateListOf<Long>() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // The top bar displays the screen title and a dynamic count of selected songs.
            TopAppBar(
                title = {
                    Column {
                        Text("Add songs", color = Color.White)
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    // Back button to trigger the onBack navigation callback.
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            // The FAB is only displayed if at least one song has been selected.
            if (selectedIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        // When clicked, add the selected songs to the playlist and call the completion callback.
                        viewModel.addSongsToPlaylist(playlistId, selectedIds.toList())
                        onSongsAdded()
                    },
                    containerColor = TextHighlightPurple,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Check, null) },
                    text = { Text("Add ${selectedIds.size}") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            // If there are no songs left to add, display a centered message.
            if (songsAvailableToAdd.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No more songs to add", color = Color.Gray)
                }
            } else {
                // Otherwise, display the list of available songs in a LazyColumn for performance.
                LazyColumn(
                    // Add padding to prevent content from being hidden behind the FAB and TopAppBar.
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                ) {
                    items(songsAvailableToAdd) { song ->
                        val isSelected = selectedIds.contains(song.id)

                        // Use a custom composable to display each selectable song item.
                        SelectableSongItemGlass(
                            song = song,
                            isSelected = isSelected,
                            onToggleSelection = {
                                // This callback handles the logic for selecting and deselecting a song.
                                if (isSelected) {
                                    selectedIds.remove(song.id)
                                } else {
                                    selectedIds.add(song.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
