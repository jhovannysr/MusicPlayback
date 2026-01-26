package com.jhovanny.musicplayback.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.jhovanny.musicplayback.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ScrollComponent(
    songs: List<Song>,
    listState: LazyListState,
    currentSong: Song?,
    scope: CoroutineScope,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
//            .padding(top = 150.dp, bottom = 16.dp),
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        // --- SCROLLBAR LATERAL (Nativa Simple) ---
        if (songs.isNotEmpty()) {
            //1. Estado y Variables Comunes
            var listHeightPx by remember { mutableFloatStateOf(1f) } // Altura real en píxeles
            val totalItems = songs.size
            val layoutInfo = listState.layoutInfo

            // 2. Cálculo inteligente de la posición del SCROLL (el thumb blanco)
            // Usamos la lógica mejorada para listas pequeñas y grandes
            val realScrollPercentage =
                remember(layoutInfo, totalItems, listState.firstVisibleItemIndex) {
                    if (totalItems == 0) 0f
                    else if (!listState.canScrollForward) 1f // Abajo del todo
                    else if (!listState.canScrollBackward) 0f // Arriba del todo
                    else {
                        val firstIndex = listState.firstVisibleItemIndex
                        val visibleCount = layoutInfo.visibleItemsInfo.size
                        // Espacio total recorrible (Total - Lo que veo)
                        val maxScrollableIndex = (totalItems - visibleCount).coerceAtLeast(1)
                        (firstIndex.toFloat() / maxScrollableIndex.toFloat()).coerceIn(0f, 1f)
                    }
                }

            // 3. Cálculo de la posición de la CANCIÓN ACTUAL (el icono de nota)
            val currentSongIndex = remember(songs, currentSong) {
                songs.indexOfFirst { it.id == currentSong?.id }
            }

            // Porcentaje de posición de la canción actual (0..1)
            val playingSongPercentage = if (currentSongIndex != -1 && totalItems > 0) {
                currentSongIndex.toFloat() / totalItems.toFloat()
            } else null

            // 4. Estados para el arrastre manual
            var isDragging by remember { mutableStateOf(false) }
            var dragPosition by remember { mutableFloatStateOf(0f) } // Posición visual mientras arrastras

            // Si arrastras, mandas tú. Si no, manda la lista.
            val displayPercentage = if (isDragging) dragPosition else realScrollPercentage

            // --- CONTENEDOR PRINCIPAL DE LA BARRA ---
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(30.dp) // Zona táctil ancha
                    .padding(vertical = 4.dp)
                    .onGloballyPositioned { coordinates ->
                        listHeightPx = coordinates.size.height.toFloat()
                    }
                    // Lógica de arrastre del Thumb blanco
                    .draggable(
                        orientation = Orientation.Vertical,
                        onDragStarted = { offset ->
                            isDragging = true
                            val newPos = (offset.y / listHeightPx).coerceIn(0f, 1f)
                            dragPosition = newPos
                            val targetIndex = (newPos * totalItems).toInt()
                            scope.launch { listState.scrollToItem(targetIndex) }
                        },
                        onDragStopped = { isDragging = false },
                        state = rememberDraggableState { delta ->
                            val deltaPercentage = delta / listHeightPx
                            val newPos = (dragPosition + deltaPercentage).coerceIn(0f, 1f)
                            dragPosition = newPos
                            val targetIndex = (newPos * totalItems).toInt()
                            scope.launch { listState.scrollToItem(targetIndex) }
                        }
                    )
            ) {
                // A) Línea de fondo (Raíl)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
                )

                // B) INDICADOR DE CANCIÓN SONANDO (Nota Musical) 🎵
                // Solo se muestra si la canción sonando está en esta lista
                if (playingSongPercentage != null) {
                    val playingBias = (playingSongPercentage * 2) - 1

                    val context = androidx.compose.ui.platform.LocalContext.current

                    IconButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(currentSongIndex)
                            }
                        },
                        modifier = Modifier
                            .align(BiasAlignment(0f, playingBias.coerceIn(-1f, 1f)))
                            .size(20.dp) // Un pelín más grande para que se vea bien el GIF
                            .offset(x = (-12).dp)
                    ) {
                        // Usamos AsyncImage de Coil para cargar el GIF
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(com.jhovanny.musicplayback.R.drawable.ic_animation_current_song)
                                .decoderFactory(
                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                        coil.decode.ImageDecoderDecoder.Factory() // Ahora sí lo reconocerá
                                    } else {
                                        coil.decode.GifDecoder.Factory()          // Y este también
                                    }
                                )
                                .build(),
                            contentDescription = "Canción sonando",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }

                // C) El Indicador de Scroll ("Thumb" blanco)
                val thumbBias = (displayPercentage * 2) - 1

                Box(
                    modifier = Modifier
                        .align(BiasAlignment(0f, thumbBias.coerceIn(-1f, 1f)))
                        .width(4.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
