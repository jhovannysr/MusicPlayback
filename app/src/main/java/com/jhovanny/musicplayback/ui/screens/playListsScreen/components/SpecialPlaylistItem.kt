package com.jhovanny.musicplayback.ui.screens.playListsScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhovanny.musicplayback.ui.screensimport.PlaylistUiState
import com.jhovanny.musicplayback.ui.theme.CardBackgroundWhite
import com.jhovanny.musicplayback.ui.theme.TextGrayDark
import com.jhovanny.musicplayback.utils.PlaylistConstants

/**
 * A Composable that displays a card item for a special playlist (e.g., Favorites, Recents).
 *
 * @param playlist The state object containing the playlist details.
 * @param icon The vector icon to display for the playlist.
 * @param modifier The modifier to be applied to the component.
 * @param onClick The callback to be invoked when the card is clicked.
 */
@Composable
fun SpecialPlaylistItem(
    playlist: PlaylistUiState,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // A clickable card that serves as the container for the playlist item.
    Card(
        modifier = modifier
            .height(70.dp) // Sets a fixed height for better touch targets.
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundWhite.copy(alpha = 0.6f)
        )
    ) {
        // A horizontal layout for the icon and text content.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Displays the playlist icon.
            Icon(
                imageVector = icon,
                contentDescription = null, // Icon is decorative.
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            // A vertical layout for the playlist name and song count.
            Column(verticalArrangement = Arrangement.Center) {
                // Displays the playlist name.
                Text(
                    text = if(playlist.name == PlaylistConstants.NAME_FAVORITES){"Favorites"} else {"Recents"},
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                // Displays the number of songs in the playlist.
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGrayDark
                )
            }
        }
    }
}