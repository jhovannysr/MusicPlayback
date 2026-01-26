package com.jhovanny.musicplayback.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple
import com.jhovanny.musicplayback.ui.theme.TextWhite

@Composable
fun TopSongsRow(
    name: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onTitleClick: () -> Unit,
    currentlyPlayingSongId: Long? = null
) {
    val listState = rememberLazyListState()

    // Scroll inicial suave
    LaunchedEffect(key1 = songs) {
        if (songs.size > 1) {
            listState.animateScrollToItem(index = 0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // --- CABECERA MEJORADA ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    // Sombra para que se lea siempre sobre el video
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = TextWhite
            )

            IconButton(
                onClick = onTitleClick,
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver todo",
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(songs) { _, song ->
                val isPlaying = currentlyPlayingSongId == song.id
                ModernSongCard(song, isPlaying, onSongClick)
            }
        }
    }
}

@Composable
fun ModernSongCard(
    song: Song,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    // Animación para el borde si está sonando
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp) // Ancho fijo de la columna
    ) {
        // --- 1. CONTENEDOR DE LA CARÁTULA ---
        Box(
            modifier = Modifier
                .size(120.dp)
                // Si está sonando, añadimos un borde animado brillante
                .then(
                    if (isPlaying) Modifier.border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                TextHighlightPurple,
                                Color.White,
                                TextHighlightPurple
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) else Modifier
                )
                // Sombra de color difusa detrás (Glow Effect)
                .drawBehind {
                    if (isPlaying) {
                        drawCircle(
                            color = TextHighlightPurple.copy(alpha = 0.4f * borderAlpha),
                            radius = size.width / 1.5f,
                            center = center
                        )
                    }
                }
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSongClick(song) }
        ) {
            // Imagen Principal
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUri)
                    .crossfade(true)
                    .error(android.R.drawable.ic_menu_help)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Fallback si no hay imagen
            if (song.coverUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Overlay degradado inferior para mejorar contraste (opcional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )

            // Indicador de Play si está sonando
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    MiniVisualizerRow() // Reutilizamos tu visualizador
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. TÍTULO Y ARTISTA ---
        // Fuera de la imagen para un look más limpio tipo Apple Music
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            ),
            color = if (isPlaying) TextHighlightPurple else TextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start, // Alineado a la izquierda
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            ),
            color = TextWhite.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Necesitas traer tu MiniVisualizer aquí si no es global, o usar este simple:
@Composable
fun MiniVisualizerRow() {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(20.dp)
    ) {
        val transition = rememberInfiniteTransition(label = "viz")
        repeat(3) { i ->
            val height by transition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(300 + (i * 100), easing = LinearEasing), RepeatMode.Reverse
                ), label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextHighlightPurple)
            )
        }
    }
}


//package com.jhovanny.musicplayback.ui.components
//
//import androidx.compose.animation.core.copy
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.aspectRatio
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ChevronRight
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.blur
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.text.font.FontStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import coil.compose.AsyncImage
//import coil.request.ImageRequest
//import com.jhovanny.musicplayback.data.Song
//import com.jhovanny.musicplayback.ui.theme.TextWhite
//import androidx.compose.animation.core.LinearEasing
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.tween
//import androidx.compose.ui.draw.rotate
//
//
//@Composable
//fun TopSongsRow(
//    name: String,
//    songs: List<Song>,
//    onSongClick: (Song) -> Unit,
//    onTitleClick: () -> Unit,
//    currentlyPlayingSongId: Long? = null
//) {
//    val listState = rememberLazyListState()
//
//    LaunchedEffect(key1 = songs) {
//        if (songs.size > 1) {
//            listState.scrollToItem(index = 1)
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 6.dp)
//    ) {
//        // --- CABECERA (Título + Botón) ---
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp) // Padding lateral para que no se pegue a los bordes
//                .padding(bottom = 6.dp),
//            horizontalArrangement = Arrangement.SpaceBetween, // Texto a la izquierda, botón a la derecha
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Título
//            Text(
//                text = name,
//                style = MaterialTheme.typography.titleMedium,
//                color = TextWhite
//            )
//
//            // Botón de navegación (Flecha)
//            IconButton(
//                onClick = onTitleClick, // <--- Acción al hacer click
//                modifier = Modifier.size(24.dp) // Tamaño discreto
//            ) {
//                Icon(
//                    imageVector = Icons.Default.ChevronRight,
//                    contentDescription = "Ver todo",
//                    tint = TextWhite.copy(alpha = 0.7f)
//                )
//            }
//        }
//
//        LazyRow(
//            contentPadding = PaddingValues(horizontal = 0.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            itemsIndexed(songs) { _, song ->
//                Box(
//                    modifier = Modifier
//                        .width(140.dp)
//                        .height(120.dp) // Tamaño rectangular fijo
//                        .clip(RoundedCornerShape(22.dp))
//                        .clickable { onSongClick(song) }
//                        .background(Color.Black) // Fondo base por si falla la imagen
//                ) {
//                    // 1. FONDO DE AMBIENTE
//                    AsyncImage(
//                        model = ImageRequest.Builder(LocalContext.current)
//                            .data(song.coverUri)
//                            .crossfade(true)
//                            .build(),
//                        contentDescription = null,
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .blur(radius = 10.dp)
//                            .background(Color.Black.copy(alpha = 0.5f))
//                    )
//
//                    // 2. EL VINILO EN EL CENTRO
//                    Box(
//                        modifier = Modifier.align(Alignment.Center),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        // Disco negro
//                        Box(
//                            modifier = Modifier
//                                .size(90.dp)
//                                .shadow(8.dp, CircleShape)
//                                .clip(CircleShape)
//                                .background(Color(0xFF111111))
//                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
//                        )
//
//                        // Carátula circular central
//                        AsyncImage(
//                            model = ImageRequest.Builder(LocalContext.current)
//                                .data(song.coverUri)
//                                .crossfade(true)
//                                .build(),
//                            contentDescription = null,
//                            contentScale = ContentScale.Crop,
//                            modifier = Modifier
//                                .size(70.dp)
//                                .clip(CircleShape)
//                        )
//
//                        // Agujero central
//                        Box(
//                            modifier = Modifier
//                                .size(6.dp)
//                                .clip(CircleShape)
//                                .background(Color.Black)
//                        )
//                    }
//
//                    // 3. TÍTULO SUPERPUESTO
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .align(Alignment.BottomCenter)
//                            .background(
//                                Brush.verticalGradient(
//                                    colors = listOf(
//                                        Color.Transparent,
//                                        Color.Black.copy(alpha = 0.9f)
//                                    )
//                                )
//                            )
//                            .padding(horizontal = 8.dp, vertical = 6.dp)
//                    ) {
//                        Text(
//                            text = song.title,
//                            style = MaterialTheme.typography.bodySmall.copy(
//                                fontWeight = FontWeight.Bold,
//                                fontSize = 11.sp
//                            ),
//                            color = TextWhite,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis,
//                            textAlign = TextAlign.Center,
//                            modifier = Modifier.fillMaxWidth()
//                        )
//                    }
//                }
//            }
//        }
//    }
//}