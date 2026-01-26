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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.jhovanny.musicplayback.ui.theme.TextHighlightPurple

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItemPersonalize(
    song: Song,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isSelectedInMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    dragModifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    // ANIMACIONES DE COLOR (Estilo Glass)
    val borderColor by animateColorAsState(
        targetValue = if (isSelected || isSelectionMode) TextHighlightPurple else Color.White.copy(alpha = 0.1f),
        label = "border"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            Color.Black.copy(alpha = 0.7f) // Más oscuro al seleccionar
        else
            Color.Black.copy(alpha = 0.2f), // "Ahumado" para mejor contraste
        label = "container"
    )

    // ESTRUCTURA DE TARJETA FLOTANTE
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp) // Espacio entre tarjetas
            .clip(RoundedCornerShape(16.dp))
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
            .padding(8.dp) // Padding interno
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. ASA DE ARRASTRE (DRAG HANDLE) ---
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reordenar",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = dragModifier
                    .padding(end = 8.dp)
                    .size(24.dp)
            )

            // --- 2. IMAGEN + VISUALIZER ---
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                )

                // Fallback icon si no hay portada
                if (song.coverUri == null) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // --- AQUÍ ESTÁ EL CAMBIO: VISUALIZADOR ANIMADO ---
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f)) // Fondo oscuro para que resalte
                    )
                    MiniVisualizerPersonalize() // Usamos la animación
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // --- 3. TEXTOS ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) TextHighlightPurple else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // --- 4. CHECKBOX O MENÚ ---
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
                IconButton(onClick = onMoreClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Copia local del MiniVisualizer para que funcione en este archivo.
 * (Si ya lo tienes en un archivo separado Utils.kt o similar, puedes borrar esto)
 */
@Composable
private fun MiniVisualizerPersonalize() {
    val transition = rememberInfiniteTransition(label = "visualizer")

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
        Box(modifier = Modifier.weight(1f).fillMaxHeight(height1).clip(RoundedCornerShape(2.dp)).background(TextHighlightPurple))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(height2).clip(RoundedCornerShape(2.dp)).background(TextHighlightPurple))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(height3).clip(RoundedCornerShape(2.dp)).background(TextHighlightPurple))
    }
}
