package com.jhovanny.musicplayback.ui.screens.playListsScreen.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

/**
 * A Composable that displays an alert dialog for creating a new playlist.
 *
 * This dialog prompts the user to enter a name for the new playlist in a text field.
 * The creation is confirmed via a button, which is only enabled when the input
 * text is not blank.
 *
 * @param onDismiss A lambda function to be invoked when the user dismisses the dialog
 *   (e.g., by clicking the "Cancel" button or tapping outside).
 * @param onCreate A lambda function that is called when the "Create" button is clicked,
 *   passing the entered playlist name as a [String].
 */
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    // State to hold the text entered by the user in the text field.
    var text by remember { mutableStateOf("") }

    // The main dialog container.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            // Text field for the user to input the playlist name.
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            // The "Create" button, which triggers the onCreate callback.
            Button(
                onClick = { if (text.isNotBlank()) onCreate(text) },
                // The button is disabled if the text field is empty.
                enabled = text.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            // The "Cancel" button, which dismisses the dialog.
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}