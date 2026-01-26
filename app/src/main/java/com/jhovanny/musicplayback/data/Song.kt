package com.jhovanny.musicplayback.data

import android.net.Uri

/**
 * A data class representing a single audio track in the application.
 *
 * This class serves as the primary model for a song, holding all its essential metadata.
 * It is designed to be immutable, which makes it safe to use across different
 * threads and with Jetpack Compose's state management system.
 *
 * @property id The unique identifier for the song, typically from [MediaStore].
 * @property title The title of the song.
 * @property artist The name of the artist.
 * @property duration The total duration of the song in milliseconds.
 * @property coverUri A string representation of the URI for the album art. Can be null if no cover is available.
 * @property uri The [Uri] pointing to the actual audio file on the device.
 * @property dateAdded The timestamp (in milliseconds) when the song was added to the device's media library. Defaults to 0.
 * @property playCount The number of times the song has been played. This value is typically populated from local database statistics. Defaults to 0.
 */
data class Song(
  val id: Long,
  val title: String,
  val artist: String,
  val duration: Long,
  val coverUri: String?,
  val uri: Uri,
  val dateAdded: Long = 0L,
  val playCount: Int = 0
)
