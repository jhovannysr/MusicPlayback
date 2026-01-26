package com.jhovanny.musicplayback.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.data.Song
import com.jhovanny.musicplayback.ui.theme.IconPrimary
import com.jhovanny.musicplayback.ui.theme.TextGray
import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isSelectedInMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // ANIMACIÓN DE SELECCIÓN:
    // Si está seleccionada, el borde se vuelve morado y el fondo un poco más visible.
    // Si no, es casi transparente para dejar ver el video.
    val borderColor by animateColorAsState(
        targetValue = if (isSelected || isSelectionMode) TextHighlightPurple else Color.Transparent,
        label = "border"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            Color.Black.copy(alpha = 0.7f) // Antes 0.6f, lo subimos un poco
        else
            Color.Black.copy(alpha = 0.2f),
        label = "container"
    )

    // ...


    // ESTRUCTURA: Una "Tarjeta" flotante en lugar de una fila plana
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp) // Espacio entre tarjetas
            .clip(RoundedCornerShape(16.dp)) // Forma de cápsula/tarjeta
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (isSelected || isSelectedInMode) borderColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(8.dp) // Padding interno de la tarjeta
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. IMAGEN (Un poco más pequeña para compensar la tarjeta)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(50.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.coverUri)
                        .crossfade(true)
                        .error(android.R.drawable.ic_menu_help)
                        .build(),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp)) // Muy redondeada
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                )

                if (song.coverUri == null) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                    MiniVisualizer()
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. INFO
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    // Si está seleccionada usamos el morado, si no blanco puro
                    color = if (isSelected) TextHighlightPurple else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Pequeño detalle: Artista con opacidad
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. ACCIONES
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelectedInMode,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = TextHighlightPurple,
                        checkmarkColor = Color.White,
                        uncheckedColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            } else {
                // El botón de menú solo aparece sutilmente
                IconButton(onClick = onMoreClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.White.copy(alpha = 0.5f) // Sutil
                    )
                }
            }
        }
    }
}

/**
 * Las barritas animadas que te gustaron.
 * Usan TextHighlightPurple para combinar con el título seleccionado.
 */
@Composable
fun MiniVisualizer() {
    val transition = rememberInfiniteTransition(label = "visualizer")

    // Configuramos 3 animaciones con tiempos distintos para efecto aleatorio
    val height1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "bar1"
    )
    val height2 by transition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "bar2"
    )
    val height3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "bar3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .width(16.dp)
            .height(16.dp)
    ) {
        // Barra 1
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(height1)
                .clip(RoundedCornerShape(2.dp))
                .background(TextHighlightPurple)
        )
        // Barra 2
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(height2)
                .clip(RoundedCornerShape(2.dp))
                .background(TextHighlightPurple)
        )
        // Barra 3
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(height3)
                .clip(RoundedCornerShape(2.dp))
                .background(TextHighlightPurple)
        )
    }
}

// Me gusta mejora "icono reproduciendose, items mas pequeños"
//package com.jhovanny.musicplayback.ui.components
//
//import androidx.compose.animation.animateColorAsState
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.foundation.background
//import androidx.compose.foundation.combinedClickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.MoreVert
//import androidx.compose.material.icons.filled.MusicNote
//import androidx.compose.material3.Checkbox
//import androidx.compose.material3.CheckboxDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.hapticfeedback.HapticFeedbackType
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalHapticFeedback
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
//import coil.request.ImageRequest
//import com.jhovanny.musicplayback.data.Song
//import com.jhovanny.musicplayback.ui.theme.IconPrimary
//import com.jhovanny.musicplayback.ui.theme.TextGray
//import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun SongItem(
//    song: Song,
//    isSelected: Boolean,
//    isSelectionMode: Boolean,
//    isSelectedInMode: Boolean,
//    onClick: () -> Unit,
//    onLongClick: () -> Unit,
//    onMoreClick: () -> Unit
//) {
//    val haptics = LocalHapticFeedback.current
//
//    // 1. FONDO: Volvemos a tu color original (Blanco transparente)
//    // para que no choque con tus videos de fondo.
//    val backgroundColor by animateColorAsState(
//        targetValue = if (isSelected || isSelectedInMode)
//            Color.White.copy(alpha = 0.2f)
//        else Color.Transparent,
//        label = "selectionAnimation"
//    )
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(backgroundColor)
//            .combinedClickable(
//                onClick = onClick,
//                onLongClick = {
//                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
//                    onLongClick()
//                }
//            )
//            // 2. TAMAÑO: Volvemos a 8.dp para que sea más compacto
//            .padding(horizontal = 16.dp, vertical = 8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // --- SECCIÓN DE IMAGEN / VISUALIZADOR ---
//        Box(
//            contentAlignment = Alignment.Center,
//            modifier = Modifier.size(56.dp)
//        ) {
//            AsyncImage(
//                model = ImageRequest.Builder(LocalContext.current)
//                    .data(song.coverUri)
//                    .crossfade(true)
//                    .error(android.R.drawable.ic_menu_help)
//                    .build(),
//                contentDescription = "Cover",
//                contentScale = ContentScale.Crop,
//                modifier = Modifier
//                    .matchParentSize()
//                    .clip(RoundedCornerShape(8.dp)) // Mantenemos tu radio original
//                    .background(Color.DarkGray.copy(alpha = 0.3f))
//            )
//
//            // Si no hay portada, mostramos un icono bonito en lugar del gris vacío
//            if (song.coverUri == null) {
//                Surface(
//                    modifier = Modifier.matchParentSize(),
//                    shape = RoundedCornerShape(8.dp),
//                    color = Color.DarkGray.copy(alpha = 0.3f)
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.MusicNote,
//                        contentDescription = null,
//                        modifier = Modifier.padding(12.dp),
//                        tint = Color.White.copy(alpha = 0.5f)
//                    )
//                }
//            }
//
//            // 3. VISUALIZADOR: Solo aparece si está sonando (isSelected)
//            if (isSelected) {
//                // Fondo oscuro semitransparente para que resalten las barritas
//                Box(
//                    modifier = Modifier
//                        .matchParentSize()
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(Color.Black.copy(alpha = 0.5f))
//                )
//                // Las barritas animadas
//                MiniVisualizer()
//            }
//        }
//
//        Spacer(modifier = Modifier.width(16.dp))
//
//        // --- SECCIÓN DE TEXTO ---
//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = song.title,
//                style = MaterialTheme.typography.bodyLarge,
//                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
//                color = if (isSelected) TextHighlightPurple else MaterialTheme.colorScheme.onSurface,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = song.artist,
//                style = MaterialTheme.typography.bodySmall,
//                color = TextGray,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//        }
//
//        if (isSelectionMode) {
//            Checkbox(
//                checked = isSelectedInMode,
//                onCheckedChange = { onClick() },
//                colors = CheckboxDefaults.colors(
//                    checkedColor = TextHighlightPurple,
//                    checkmarkColor = Color.White
//                )
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//        }
//
//        if (!isSelectionMode) {
//            IconButton(onClick = onMoreClick) {
//                Icon(
//                    Icons.Default.MoreVert, contentDescription = "Opciones",
//                    tint = IconPrimary
//                )
//            }
//        }
//    }
//}
//
///**
// * Las barritas animadas que te gustaron.
// * Usan TextHighlightPurple para combinar con el título seleccionado.
// */
//@Composable
//fun MiniVisualizer() {
//    val transition = rememberInfiniteTransition(label = "visualizer")
//
//    // Configuramos 3 animaciones con tiempos distintos para efecto aleatorio
//    val height1 by transition.animateFloat(
//        initialValue = 0.3f, targetValue = 0.9f,
//        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "bar1"
//    )
//    val height2 by transition.animateFloat(
//        initialValue = 0.5f, targetValue = 1f,
//        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "bar2"
//    )
//    val height3 by transition.animateFloat(
//        initialValue = 0.4f, targetValue = 0.8f,
//        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "bar3"
//    )
//
//    Row(
//        verticalAlignment = Alignment.Bottom,
//        horizontalArrangement = Arrangement.spacedBy(3.dp),
//        modifier = Modifier
//            .width(16.dp)
//            .height(16.dp)
//    ) {
//        // Barra 1
//        Box(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxHeight(height1)
//                .clip(RoundedCornerShape(2.dp))
//                .background(TextHighlightPurple)
//        )
//        // Barra 2
//        Box(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxHeight(height2)
//                .clip(RoundedCornerShape(2.dp))
//                .background(TextHighlightPurple)
//        )
//        // Barra 3
//        Box(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxHeight(height3)
//                .clip(RoundedCornerShape(2.dp))
//                .background(TextHighlightPurple)
//        )
//    }
//}


// Código Base
//package com.jhovanny.musicplayback.ui.components
//
//import androidx.compose.animation.animateColorAsState
//import androidx.compose.foundation.background
//import androidx.compose.foundation.combinedClickable
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.MoreVert
//import androidx.compose.material3.Checkbox
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
//import coil.request.ImageRequest
//import com.jhovanny.musicplayback.data.Song
//import com.jhovanny.musicplayback.ui.theme.IconPrimary
//import com.jhovanny.musicplayback.ui.theme.TextGray
//import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple
//
//@Composable
//fun SongItem(
//    song: Song,
//    isSelected: Boolean, // Esto ya lo tenías (para indicar la canción sonando)
//    isSelectionMode: Boolean,
//    isSelectedInMode: Boolean,
//    onClick: () -> Unit,
//    onLongClick: () -> Unit,
//    onMoreClick: () -> Unit
//) {
//    println("SearchScreen SongItem isSelected: $isSelected")
//    val backgroundColor by animateColorAsState(
//        targetValue = if (isSelected || isSelectedInMode) Color.White.copy(alpha = 0.2f) else Color.Transparent,
//        label = "selectionAnimation"
//    )
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(backgroundColor)
//            // AQUÍ ESTÁ EL CAMBIO: combinedClickable maneja click y longClick
//            .combinedClickable(
//                onClick = onClick,       // Toque normal -> Reproducir
//                onLongClick = onLongClick // Mantener pulsado -> Abrir menú (igual que los 3 puntos)
//            )
//            .padding(horizontal = 16.dp, vertical = 8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // ... (El resto del contenido: AsyncImage, Textos, Icono, sigue IGUAL)
//        AsyncImage(
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(song.coverUri)
//                .crossfade(true)
//                .scale(coil.size.Scale.FILL)
//                .error(android.R.drawable.ic_menu_help)
//                .build(),
//            contentDescription = "Cover",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier
//                .size(56.dp)
//                .clip(RoundedCornerShape(8.dp))
//                .background(Color.DarkGray)
//                .background(Color.DarkGray.copy(alpha = 0.3f))
//        )
//
//        Spacer(modifier = Modifier.width(16.dp))
//
//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = song.title,
//                style = MaterialTheme.typography.bodyLarge,
//                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
//                color = if (isSelected) TextHighlightPurple else MaterialTheme.colorScheme.onSurface,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = song.artist,
//                style = MaterialTheme.typography.bodySmall,
//                color = TextGray,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//        }
//
//        if (isSelectionMode) {
//            Checkbox(
//                checked = isSelectedInMode,
//                onCheckedChange = { onClick() } // Al tocar el check, es como tocar la fila
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//        }
//
//        // 4. BOTÓN MENÚ (Solo visible en Modo Normal, o según prefieras)
//        if (!isSelectionMode) {
//            IconButton(onClick = onMoreClick) {
//                Icon(
//                    Icons.Default.MoreVert, contentDescription = "Opciones",
//                    tint = IconPrimary
//                )
//            }
//        }
//    }
//}