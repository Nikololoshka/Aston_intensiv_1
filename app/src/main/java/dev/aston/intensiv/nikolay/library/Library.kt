package dev.aston.intensiv.nikolay.library

import android.content.Context
import android.content.res.AssetFileDescriptor
import java.io.FileDescriptor

object Library {
    fun allTracks(context: Context): List<TrackItem> {
        return context.assets.list("mp3")
            .orEmpty()
            .map { fileName ->
                TrackItem(
                    name = fileName.substringBefore(".mp3"),
                    fileName = fileName
                )
            }
    }
    fun loadTrack(context: Context, track: TrackItem): AssetFileDescriptor {
        return context.assets.openFd("mp3/${track.fileName}")
    }
}