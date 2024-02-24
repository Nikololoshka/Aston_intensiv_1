package dev.aston.intensiv.nikolay.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
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

    private val tracks by lazy { Library.allTracks(this) }

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }


    override fun onCreate() {
        super.onCreate()

        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.getStringExtra(EXTRA_PLAYER_ACTION)) {
                EXTRA_PLAYER_STOP -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                EXTRA_PLAY_TRACK -> {
                    val trackFileName = intent.getStringExtra(EXTRA_PLAY_TRACK_NAME)
                    if (trackFileName != null) {
                        val track = Library.allTracks(this)
                            .find { it.fileName == trackFileName }
                        if (track != null) {
                            playTrack(track)
                        }
                    }
                }
                EXTRA_PLAYER_PREV -> {
                    Log.d("PlayerService", "onStartCommand: EXTRA_PLAYER_PREV")
                    previousTrack()
                }
                EXTRA_PLAYER_NEXT -> {
                    Log.d("PlayerService", "onStartCommand: EXTRA_PLAYER_NEXT")
                    nextTrack()
                }
                EXTRA_PLAYER_PLAY_PAUSE -> {
                    Log.d("PlayerService", "onStartCommand: EXTRA_PLAYER_PLAY_PAUSE")
                    if (isPlaying) pausePlaying() else resumePlaying()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        val currentPlayer = mediaPlayer
        if (currentPlayer != null) {
            if (currentPlayer.isPlaying) {
                currentPlayer.stop()
            }
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

        val notification = buildTrackNotification(item)
        notificationManager.notify(PlayerNotification.PLAYER_SERVICE_ID, notification)
    }

    private fun buildNotificationPanel(
        title: String,
        isPlaying: Boolean
    ): RemoteViews {
        val previousPendingIntent = PendingIntent.getService(
            this,
            REQUEST_PREV,
            Intent(this, PlayerService::class.java).apply {
                putExtra(EXTRA_PLAYER_ACTION, EXTRA_PLAYER_PREV)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPendingIntent = PendingIntent.getService(
            this,
            REQUEST_NEXT,
            Intent(this, PlayerService::class.java).apply {
                putExtra(EXTRA_PLAYER_ACTION, EXTRA_PLAYER_NEXT)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playOrPausePendingIntent = PendingIntent.getService(
            this,
            REQUEST_PLAY,
            Intent(this, PlayerService::class.java).apply {
                putExtra(EXTRA_PLAYER_ACTION, EXTRA_PLAYER_PLAY_PAUSE)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        return RemoteViews(packageName, R.layout.player_notification).apply {
            setTextViewText(R.id.notification_title, title)
            if (isPlaying) {
                setImageViewResource(R.id.play_pause_button, R.drawable.ic_pause)
            }
            setOnClickPendingIntent(R.id.prev_track_button, previousPendingIntent)
            setOnClickPendingIntent(R.id.play_pause_button, playOrPausePendingIntent)
            setOnClickPendingIntent(R.id.next_track_button, nextPendingIntent)
        }
    }

    private fun buildTrackNotification(track: TrackItem): Notification {
        return NotificationCompat.Builder(this, PlayerNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(
                buildNotificationPanel(
                    title = track.name,
                    isPlaying = true
                )
            )
            .setShowWhen(false)
            .setOngoing(true)
            .build()
    }

    private fun buildCancelNotification(track: TrackItem? = null): Notification {
        val deletePendingIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, PlayerService::class.java).apply {
                putExtra(EXTRA_PLAYER_ACTION, EXTRA_PLAYER_STOP)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PlayerNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(
                buildNotificationPanel(
                    title = track?.name ?: "Player is running",
                    isPlaying = false
                )
            )
            .setOngoing(false)
            .setDeleteIntent(deletePendingIntent)
            .setShowWhen(false)
            .build()
    }

    private fun startForegroundService() {
        val notification = buildCancelNotification()
        startForeground(PlayerNotification.PLAYER_SERVICE_ID, notification)
    }

    override fun pausePlaying() {
        mediaPlayer?.pause()
        isPlaying = false

        val notification = buildCancelNotification(currentTrack)
        notificationManager.notify(PlayerNotification.PLAYER_SERVICE_ID, notification)
    }

    override fun resumePlaying() {
        mediaPlayer?.start()
        isPlaying = true

        val notification = currentTrack?.let { buildTrackNotification(it) }
        notificationManager.notify(PlayerNotification.PLAYER_SERVICE_ID, notification)
    }

    override fun stop() {
        stopSelf()
    }

    override fun nextTrack() {
        val index = tracks.indexOf(currentTrack)
        if (index != -1) {
            playTrack(tracks[(index + 1) % tracks.size])
        }
    }

    override fun previousTrack() {
        val index = tracks.indexOf(currentTrack)
        if (index != -1) {
            val newIndex = if (index - 1 < 0) tracks.size - 1 else index - 1
            playTrack(tracks[newIndex])
        }
    }

    companion object {

        private const val REQUEST_STOP = 1
        private const val REQUEST_PLAY = 2
        private const val REQUEST_NEXT = 3
        private const val REQUEST_PREV = 4

        private const val EXTRA_PLAYER_ACTION = "extra_player_action"

        private const val EXTRA_PLAYER_PLAY_PAUSE = "extra_player_play_pause"
        private const val EXTRA_PLAYER_NEXT= "extra_player_next"
        private const val EXTRA_PLAYER_PREV = "extra_player_prev"
        private const val EXTRA_PLAYER_STOP = "extra_player_stop"

        private const val EXTRA_PLAY_TRACK = "extra_play_track"
        private const val EXTRA_PLAY_TRACK_NAME = "extra_play_track_name"


        fun createPlayTrackIntent(context: Context, track: TrackItem): Intent {
            return Intent(context, PlayerService::class.java).apply {
                putExtra(EXTRA_PLAYER_ACTION, EXTRA_PLAY_TRACK)
                putExtra(EXTRA_PLAY_TRACK_NAME, track.fileName)
            }
        }
    }
}