package com.jhovanny.musicplayback.ui.screens.addSongsScreen.components

import android.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple

/**
 * A composable that displays a single song item with a "floating glass" aesthetic,
 * designed for use in a selection list.
 *
 * It shows the song's cover, title, and artist, along with a checkbox. The entire
 * component is clickable to toggle its selection state. It provides clear visual
 * feedback (color, border, and font weight changes) when selected.
 */
@Composable
fun SelectableSongItemGlass(
    song: Song,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    // Animate the border and background colors based on the selection state for smooth visual feedback.
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) TextHighlightPurple else Color.White.copy(alpha = 0.1f),
        label = "border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            Color.Black.copy(alpha = 0.7f) // Darker when selected
        else
            Color.Black.copy(alpha = 0.2f), // "Smoked glass" effect
        label = "container"
    )

    // The main container that creates the "glass card" look and feel.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp) // External margin between cards
            .clip(RoundedCornerShape(16.dp)) // Rounded card shape
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onToggleSelection() } // The entire card is clickable to toggle selection
            .padding(8.dp) // Internal padding for content
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. ALBUM ART OR FALLBACK ICON
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(50.dp)
            ) {
                // Asynchronously loads the song's cover image.
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.coverUri)
                        .crossfade(true)
                        .error(R.drawable.ic_menu_help)
                        .build(),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                )

                // If the song has no cover art, display a default music note icon.
                if (song.coverUri == null) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. SONG TITLE AND ARTIST
            Column(modifier = Modifier.weight(1f)) {
                // Text styles change based on selection state.
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TextHighlightPurple else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. SELECTION CHECKBOX
            // A checkbox to provide a clear visual indicator of the selection status.
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TextHighlightPurple,
                    checkmarkColor = Color.White,
                    uncheckedColor = Color.White.copy(alpha = 0.5f),
                    disabledCheckedColor = Color.Gray
                )
            )
        }
    }
}