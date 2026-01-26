package com.jhovanny.musicplayback.ui.screens.playListsScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A Composable that displays a single, clickable option item for a menu,
 * such as in a bottom sheet. It consists of an icon and a text label.
 *
 * @param icon The [ImageVector] to be displayed at the start of the item.
 * @param text The string label for the menu option.
 * @param textColor The color for both the icon and the text. Defaults to the onSurface color from the current theme.
 * @param onClick The lambda function to be executed when the item is clicked.
 */
@Composable
fun MenuOptionItem(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    // A clickable row that serves as the container for the menu item.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp), // Comfortable padding for touch targets.
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Displays the leading icon for the menu option.
        Icon(
            imageVector = icon,
            contentDescription = null, // The icon is decorative, described by the text.
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        // Displays the text label for the menu option.
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}