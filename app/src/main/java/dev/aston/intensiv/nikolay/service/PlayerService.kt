package dev.aston.intensiv.nikolay.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.aston.intensiv.nikolay.R
import dev.aston.intensiv.nikolay.library.Library
import dev.aston.intensiv.nikolay.library.TrackItem


class PlayerService : Service(), Player {

    inner class LocalBinder : Binder() {
        fun getPlayer(): Player = this@PlayerService
    }
    private val binder = LocalBinder()

    private var mediaPlayer: MediaPlayer? = null

    override var currentTrack: TrackItem? = null
        private set

    override var isPlaying: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()

        Log.d("PlayerService", "onCreate")
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val trackFileName = intent.getStringExtra(EXTRA_PLAY_TRACK)
            if (trackFileName != null) {
                val track = Library.allTracks(this).find { it.fileName == trackFileName }
                if (track != null) {
                    playTrack(track)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        val currentPlayer = mediaPlayer
        if (currentPlayer != null && currentPlayer.isPlaying) {
            currentPlayer.stop()
            currentPlayer.release()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun playTrack(item: TrackItem) {
        val currentPlayer = mediaPlayer
        if (currentPlayer != null) {
            if (currentTrack == item && currentPlayer.isPlaying) {
                return
            }
            currentPlayer.stop()
            currentPlayer.release()
        }

        val trackFileDescriptor = Library.loadTrack(this, item)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(
                trackFileDescriptor.fileDescriptor,
                trackFileDescriptor.startOffset,
                trackFileDescriptor.length
            )
            prepare()
            start()
        }

        currentTrack = item
        isPlaying = true
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, PlayerNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("PlayerService started")
            .setContentText("PlayerService is running")
            .setOngoing(true)
            .build()

        startForeground(PlayerNotification.PLAYER_SERVICE_ID, notification)
    }

    override fun pausePlaying() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    override fun resumePlaying() {
        mediaPlayer?.start()
        isPlaying = true
    }

    override fun stop() {
        stopSelf()
    }

    override fun nextTrack() {
        val allTracks = Library.allTracks(this)
        val index = allTracks.indexOf(currentTrack)
        if (index != -1) {
            playTrack(allTracks[(index + 1) % allTracks.size])
        }
    }

    override fun previousTrack() {
        val allTracks = Library.allTracks(this)
        val index = allTracks.indexOf(currentTrack)
        if (index != -1) {
            val newIndex = if (index - 1 < 0) allTracks.size - 1 else index - 1
            playTrack(allTracks[newIndex])
        }
    }

    companion object {

        private const val EXTRA_PLAY_TRACK = "extra_play_track"

        fun createPlayTrackIntent(context: Context, track: TrackItem) : Intent {
            return Intent(context, PlayerService::class.java).apply {
                putExtra(EXTRA_PLAY_TRACK, track.fileName)
            }
        }
    }
}