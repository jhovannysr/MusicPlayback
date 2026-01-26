package com.jhovanny.musicplayback.ui.components

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jhovanny.musicplayback.ui.theme.IconPrimary

@Composable
fun MiniPlayer(
    song: com.jhovanny.musicplayback.data.Song,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClick: () -> Unit
) {
    // Calculamos el porcentaje (0.0 a 1.0) de forma segura
    val progress = if (duration > 0) {
        currentPosition.toFloat() / duration.toFloat()
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
//                .background(Color(0xFF252525)) // Gris sólido oscuro
                .background(Color.Black.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Carátula
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.DarkGray.copy(alpha = 0.6f),
                    modifier = Modifier.size(42.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song.coverUri)
                            .crossfade(true)
                            .error(R.drawable.ic_menu_help)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
//                AsyncImage(
//                    model = ImageRequest.Builder(LocalContext.current)
//                        .data(song.coverUri)
//                        .crossfade(true)
//                        .error(R.drawable.ic_menu_help)
//                        .build(),
//                    contentDescription = null,
//                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
//                    modifier = Modifier
//                        .size(42.dp)
//                        .clip(RoundedCornerShape(6.dp))
//                        .background(Color.Gray)
//                )

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Texto (Título y Artista)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                // 3. Controles (Anterior - Play - Siguiente)

                // Botón ANTERIOR (Nuevo)
                IconButton(onClick = onPrev) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.7f) // Mismo estilo que Next
                    )
                }

                // Botón PLAY/PAUSE (Destacado)
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp) // Un poco más grande
                    )
                }

                // Botón SIGUIENTE
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress }, // <--- AQUÍ USAMOS LA VARIABLE CALCULADA
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(2.dp),
                color = Color.White,
                trackColor = Color.Transparent, // Fondo transparente para que quede elegante
            )
        }
    }
}
