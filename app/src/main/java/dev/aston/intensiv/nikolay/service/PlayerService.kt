package dev.aston.intensiv.nikolay.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.aston.intensiv.nikolay.R
import dev.aston.intensiv.nikolay.library.TrackItem


class PlayerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }
    private val binder = LocalBinder()

    private var player: MediaPlayer? = null

    var currentTrack: TrackItem? = null
        private set

    var isPlaying: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()

        Log.d("PlayerService", "onCreate")
        startForegroundService()
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("PlayerService", "onDestroy")

        val currentPlayer = player
        if (currentPlayer != null && currentPlayer.isPlaying) {
            currentPlayer.stop()
            currentPlayer.release()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun playTrack(item: TrackItem) {
        val currentPlayer = player
        if (currentPlayer != null) {
            if (currentTrack == item && currentPlayer.isPlaying) {
                return
            }
            currentPlayer.stop()
            currentPlayer.release()
        }

        val trackFileDescriptor = assets.openFd("mp3/${item.fileName}")
        player = MediaPlayer().apply {
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
}