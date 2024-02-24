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

        val trackFileDescriptor = assets.openFd("mp3/${item.fileName}")
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
        TODO("Not yet implemented")
    }

    override fun previousTrack() {
        TODO("Not yet implemented")
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