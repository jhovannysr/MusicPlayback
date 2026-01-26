package com.jhovanny.musicplayback.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhovanny.musicplayback.ui.screensimport.PlaylistUiState
import com.jhovanny.musicplayback.ui.theme.BoxBackground
import com.jhovanny.musicplayback.ui.theme.BoxText
import com.jhovanny.musicplayback.ui.theme.BoxTextSecondary
import com.jhovanny.musicplayback.ui.theme.IconRed

// --- NUEVO DISEÑO ---

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistUiState>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onFavoritesSelected: () -> Unit
    // Podrías añadir un onNewPlaylistSelected() si quieres
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BoxBackground, // Un fondo oscuro y elegante
        title = {
            Text(
                text = "Add to...",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = BoxText
            )
        },
        text = {
            // El contenido ahora será una cuadrícula
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Dos columnas
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TARJETA "FAVORITAS" (Siempre la primera)
                item {
                    PlaylistCard(
                        // Pasamos un objeto temporal para reutilizar el Composable
                        playlist = PlaylistUiState(id = -1, name = "Favorites", songCount = 0),
                        icon = {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Favoritas",
                                tint = IconRed,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        onClick = onFavoritesSelected
                    )
                }

                // (Opcional) Tarjeta para "Nueva Playlist"
                // item {
                //     PlaylistCard(
                //         playlist = PlaylistUiState(id = -2, name = "Nueva Playlist", songCount = 0),
                //         icon = {
                //             Icon(
                //                 Icons.Default.Add,
                //                 contentDescription = "Nueva Playlist",
                //                 tint = BoxText,
                //                 modifier = Modifier.size(48.dp)
                //             )
                //         },
                //         onClick = { /* onNewPlaylistSelected() */ }
                //     )
                // }

                // 2. LISTA DE PLAYLISTS DEL USUARIO
                items(playlists.filter { !it.isSpecial && it.name != "Favoritas" }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        icon = { // Icono por defecto si no hay carátula
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Playlist",
                                tint = BoxText.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        onClick = { onPlaylistSelected(playlist.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = BoxText, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}


@Composable
private fun PlaylistCard(
    playlist: PlaylistUiState,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f) // Tarjetas cuadradas
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent), // El fondo lo da el Box
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Un gradiente sutil o un color sólido
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(1.dp, BoxText.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                // Aquí podrías usar una AsyncImage si tuvieras la carátula de la playlist
                // Por ahora, usamos el icono que nos pasan.
                icon()

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = playlist.name,
                    color = BoxText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Ocultamos el contador si es una acción especial como "Favoritas"
                if (playlist.id >= 0) {
                    Text(
                        text = "${playlist.songCount} songs",
                        color = BoxTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
