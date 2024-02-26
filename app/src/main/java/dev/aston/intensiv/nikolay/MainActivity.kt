package dev.aston.intensiv.nikolay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.RecyclerView
import dev.aston.intensiv.nikolay.library.Library
import dev.aston.intensiv.nikolay.library.TrackItem
import dev.aston.intensiv.nikolay.library.TrackLibraryAdapter
import dev.aston.intensiv.nikolay.service.Player
import dev.aston.intensiv.nikolay.service.PlayerNotification
import dev.aston.intensiv.nikolay.service.PlayerService

class MainActivity : AppCompatActivity() {

    private var playerService: Player? = null
    private var isBound = false

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = (service as PlayerService.LocalBinder).getPlayer()
            isBound = true
            updateTrackInformation()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
            isBound = false
            updateTrackInformation()
        }
    }

    private lateinit var currentTrackView: TextView
    private lateinit var playTrackButton: ImageButton
    private lateinit var previousTrackButton: ImageButton
    private lateinit var nextTrackButton: ImageButton

    private val trackAdapter = TrackLibraryAdapter(this::onTrackItemClicked)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNotificationChannel()

        setContentView(R.layout.activity_main)

        currentTrackView = findViewById(R.id.current_track_label)
        playTrackButton = findViewById(R.id.play_pause_button)
        playTrackButton.setOnClickListener { onPlayOrPauseTrackClicked() }
        previousTrackButton = findViewById(R.id.prev_track_button)
        previousTrackButton.setOnClickListener { onPreviousTrackClicked() }
        nextTrackButton = findViewById(R.id.next_track_button)
        nextTrackButton.setOnClickListener { onNextTrackClicked() }

        val trackList: RecyclerView = findViewById(R.id.track_library_list)
        trackList.adapter = trackAdapter

        val tracks = Library.allTracks(this)
        trackAdapter.submitList(tracks)
    }

    private fun onTrackItemClicked(track: TrackItem) {
        val player = playerService

        if (player == null) {
            val intent = PlayerService.createPlayTrackIntent(this, track)
            ContextCompat.startForegroundService(this, intent)
            bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE)
        } else {
            player.playTrack(track)
            updateTrackInformation()
        }
    }

    private fun updateTrackInformation() {
        val player = playerService

        currentTrackView.text =
            player?.currentTrack?.name ?: getString(R.string.select_track_to_play)

        playTrackButton.setImageResource(
            if (player != null && player.isPlaying) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play_arrow
            }
        )
    }

    private fun onPlayOrPauseTrackClicked() {
        val player = playerService ?: return

        if (player.isPlaying) player.pausePlaying() else player.resumePlaying()
        updateTrackInformation()
    }

    private fun onPreviousTrackClicked() {
        playerService?.previousTrack()
        updateTrackInformation()
    }

    private fun onNextTrackClicked() {
        playerService?.nextTrack()
        updateTrackInformation()
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PlayerNotification.CHANNEL_ID,
                getString(R.string.player_notification),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService<NotificationManager>()
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, PlayerService::class.java)
        bindService(intent, playerServiceConnection, 0)

        updateTrackInformation()
    }

    override fun onStop() {
        super.onStop()

        if (isBound) {
            unbindService(playerServiceConnection)
            playerService = null
            isBound = false
        }
    }
}