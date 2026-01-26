package com.jhovanny.musicplayback.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.copy
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoBackground(
    videoResId: Int,
    shouldDarken: Boolean = true // <--- NUEVO PARÁMETRO
) {
    val context = LocalContext.current

    // 1. Animación suave de la opacidad
    // Si shouldDarken es true, usamos 0.4f (oscuro). Si es false, 0f (transparente).
    val overlayAlpha by animateFloatAsState(
        targetValue = if (shouldDarken) 0.4f else 0f,
        animationSpec = tween(durationMillis = 600), // Transición de 0.6 segundos
        label = "VideoOverlayAlpha"
    )

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            val uri = Uri.parse("android.resource://${context.packageName}/$videoResId")
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Usamos un Box para apilar: Video abajo, Capa oscura arriba
    Box(modifier = Modifier.fillMaxSize()) {

        // A. El Video (Abajo)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // B. La Capa Oscura (Arriba)
        // Esta capa siempre está ahí, pero su alfa cambia
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )
    }
}

//@OptIn(UnstableApi::class)
//@Composable
//fun VideoBackground(videoResId: Int) {
//    val context = LocalContext.current
//    val exoPlayer = remember {
//        ExoPlayer.Builder(context).build().apply {
//            repeatMode = Player.REPEAT_MODE_ALL
//            volume = 0f
//            val uri = Uri.parse("android.resource://${context.packageName}/$videoResId")
//            setMediaItem(MediaItem.fromUri(uri))
//            prepare()
//            playWhenReady = true
//        }
//    }
//
//    DisposableEffect(Unit) {
//        onDispose { exoPlayer.release() }
//    }
//
//    AndroidView(
//        factory = { ctx ->
//            PlayerView(ctx).apply {
//                player = exoPlayer
//                useController = false
//                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
//            }
//        },
//        modifier = Modifier.fillMaxSize()
//    )
//}