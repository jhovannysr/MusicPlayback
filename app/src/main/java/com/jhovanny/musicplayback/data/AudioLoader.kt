package com.jhovanny.musicplayback.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * A utility class responsible for querying the Android [MediaStore] to fetch audio files.
 *
 * This class abstracts the logic of interacting with the device's [ContentResolver]
 * to find all music files and map them to a list of [Song] objects that the
 * application can use.
 *
 * @param context The application context, required to access the `contentResolver`.
 */
class AudioLoader(private val context: Context) {

    /**
     * Scans the device's external storage for all music files and returns them as a list of [Song] objects.
     *
     * The query is configured to:
     * - Work on both modern (Android 10+) and older Android versions.
     * - Filter out non-music files and very short audio clips (less than 1 second).
     * - Sort the results alphabetically by title.
     *
     * @return A `List<Song>` containing all discovered audio tracks. Returns an empty list if
     *         the query fails or no music is found.
     */
    fun getAllAudioFiles(): List<Song> {
        val audioList = mutableListOf<Song>()

        // Define the correct content URI based on the Android version (Scoped Storage).
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // Define the columns to retrieve from the MediaStore.
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID, // Needed to construct the album art URI.
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Filter results to include only music files longer than 1 second.
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 1000"

        // Sort the results alphabetically by song title.
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // Execute the query using the ContentResolver.
        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )

        // Process the results from the cursor safely.
        query?.use { cursor ->
            // Get column indices once to improve performance inside the loop.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                // Convert date from seconds to milliseconds.
                val dateAddedMillis = cursor.getLong(dateAddedColumn) * 1000L

                // Construct the URI for the audio file itself.
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Construct the URI for the album art.
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                // Create a Song object and add it to the list.
                audioList.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        coverUri = artworkUri,
                        uri = contentUri,
                        dateAdded = dateAddedMillis
                    )
                )
            }
        }

        return audioList
    }
}