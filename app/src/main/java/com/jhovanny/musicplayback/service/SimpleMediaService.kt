package com.jhovanny.musicplayback.service

import android.content.Intent
import androidx.activity.result.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jhovanny.musicplayback.data.local.AppDatabase
import com.jhovanny.musicplayback.data.local.entities.SongStatsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


/**
 * A [MediaSessionService] that manages media playback in the background.
 *
 * This service is responsible for:
 * 1. Creating and managing the [ExoPlayer] instance and the [MediaSession].
 * 2. Handling playback errors gracefully by skipping to the next track.
 * 3. Tracking song playback to update statistics for the "Most Played" feature.
 * 4. Ensuring playback can continue even if the main app UI is closed.
 */
class SimpleMediaService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: Player? = null

    // States for most listened to songs "Más escuchadas"
    private val songStatsDao by lazy {
        AppDatabase.getDatabase(applicationContext).songStatsDao()
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var validationJob: kotlinx.coroutines.Job? = null
    private var currentSongCounted = false

    override fun onCreate() {
        println("(onCreate) service")
        super.onCreate()

        // 1. Create Player and Sesión
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()

        // 2. ERROR PROTECTION (Corrupted files)
        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                println("(onPlayerError) service")
                error.printStackTrace()
                if (player?.hasNextMediaItem() == true) {
                    player?.seekToNextMediaItem()
                    player?.prepare()
                    player?.play()
                } else {
                    player?.stop()
                }
            }

            // New Song
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                validationJob?.cancel()
                currentSongCounted = false

                if (mediaItem != null) {
                    // Cancel always when you change the song
                    validationJob?.cancel()

                    // We only start the counter IF the music is playing
                    // (Sometimes onMediaItemTransition occurs before playback begins, but usually isPlaying is already true or will be true soon.)
                    if (mediaItem != null && player?.isPlaying == true) {
                        startValidationTimer(mediaItem.mediaId)
                    }
                }
            }

            // User active Play/Pause
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                if (isPlaying) {
                    val currentId = player?.currentMediaItem?.mediaId
                    if (currentId != null && validationJob?.isActive != true && !currentSongCounted) {
                        startValidationTimer(currentId)
                    }
                } else {
                    // IF PAUSE: cancel the timer
                    // This requires the user to listen for 60 seconds STRAIGHT without pausing for it to count.
                    validationJob?.cancel()
                }
            }

        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        println("(onGetSession) service")
        return mediaSession
    }

    // Register Current Song
    private fun registerSongPlay(mediaId: String) {
        val songId = mediaId.toLongOrNull() ?: return

        serviceScope.launch {
            try {
                val currentStat = songStatsDao.getStat(songId)
                val newCount = (currentStat?.playCount ?: 0) + 1
                songStatsDao.insertOrUpdate(SongStatsEntity(songId, newCount))
                println("SERVICIO: Canción $songId registrada. Total: $newCount")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Start Timer
    private fun startValidationTimer(mediaId: String) {
        validationJob?.cancel()

        validationJob = serviceScope.launch {
            kotlinx.coroutines.delay(60000)

            if (!currentSongCounted) {
                registerSongPlay(mediaId)
                currentSongCounted = true
            }
            validationJob = null
        }
    }

    override fun onDestroy() {
        println("(onDestroy) service")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}