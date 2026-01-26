package com.jhovanny.musicplayback.ui.screens.playListsScreen.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jhovanny.musicplayback.ui.screensimport.PlaylistUiState
import com.jhovanny.musicplayback.ui.theme.IconPrimary

/**
 * A Composable that displays a grid item for a user-created playlist.
 *
 * This item is designed to be square and visually rich. It shows the playlist's cover art
 * if available, or a default icon otherwise. It includes the playlist's name, song count,
 * and an options button. It supports both single-click navigation and long-press to show a menu.
 *
 * @param playlist The [PlaylistUiState] object containing the data to display.
 * @param onClick A lambda function to be invoked when the item is clicked, typically for navigation.
 * @param onShowMenu A lambda function to be invoked on a long-click or when the options button is pressed, used to show a context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserPlaylistItem(
    playlist: PlaylistUiState,
    onClick: () -> Unit,
    onShowMenu: () -> Unit
) {
    // Local state to handle click debouncing for this specific item.
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // The main container for the playlist item, with a combined click for tap and long-press.
    Card(
        modifier = Modifier
            .aspectRatio(1f) // Ensures the card is a perfect square.
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onShowMenu // Triggers the menu on long press.
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium // Applies soft rounded corners.
    ) {
        // A Box to layer the background, content, and overlay elements.
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 1. BACKGROUND (IMAGE OR ICON) ---
            // Conditionally display either the playlist cover or a default icon.
            if (playlist.coverUri != null && playlist.songCount > 0) {
                // CASE A: If there are songs, display the album cover.
                AsyncImage(
                    model = playlist.coverUri,
                    contentDescription = null, // Decorative image.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // A dark overlay to ensure text is readable over the image.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                )
            } else {
                // CASE B: If empty, display a centered music icon.
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(64.dp) // Slightly larger icon.
                        .align(Alignment.Center)
                        .padding(bottom = 20.dp) // Moves the icon up to make space for text.
                )
            }

            // Options button (3-dots).
            IconButton(
                onClick = {
                    // Debounce logic to prevent rapid multiple clicks.
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > 500) {
                        lastClickTime = currentTime
                        onShowMenu()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = IconPrimary
                )
            }

            // Text content at the bottom.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Displays the playlist name.
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Displays the song count.
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}