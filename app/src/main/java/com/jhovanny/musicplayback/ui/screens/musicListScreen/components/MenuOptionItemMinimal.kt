package com.jhovanny.musicplayback.ui.screens.musicListScreen.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A reusable, minimal-style composable for displaying a single option in a menu or bottom sheet.
 *
 * This component renders a clickable surface containing an icon and a text label. It is designed
 * to be flexible, allowing customization of colors for different states, such as a
 * standard option versus a destructive action (e.g., "Delete").
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param text The string text to be displayed next to the icon.
 * @param textColor The color of the icon and the text. Defaults to a semi-transparent white.
 * @param containerColor The background color of the clickable surface. Defaults to a very subtle, almost transparent white.
 * @param onClick The callback lambda to be invoked when the user clicks on the item.
 */
@Composable
fun MenuOptionItemMinimal(
    icon: ImageVector,
    text: String,
    textColor: Color = Color.White.copy(alpha = 0.9f),
    containerColor: Color = Color.White.copy(alpha = 0.08f),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp), // Generous internal padding for a clean look.
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // The text label provides context, so the icon can be decorative.
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}