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
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.RecyclerView
import dev.aston.intensiv.nikolay.library.TrackItem
import dev.aston.intensiv.nikolay.library.TrackLibraryAdapter
import dev.aston.intensiv.nikolay.service.PlayerNotification
import dev.aston.intensiv.nikolay.service.PlayerService

class MainActivity : AppCompatActivity() {

    private var playerService: PlayerService? = null
    private var isBound = false

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = (service as PlayerService.LocalBinder).getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupNotificationChannel()

        val trackAdapter = TrackLibraryAdapter(this::onTrackItemClicked)
        val trackList: RecyclerView = findViewById(R.id.track_library_list)
        trackList.adapter = trackAdapter

        val tracks = assets.list("mp3")
            .orEmpty()
            .map { fileName ->
                TrackItem(
                    name = fileName.substringBefore(".mp3"),
                    fileName = fileName
                )
            }
        trackAdapter.submitList(tracks)
    }

    private fun onTrackItemClicked(track: TrackItem) {
        Log.d("MainActivity", "onTrackItemClicked: $track")

        playerService?.playTrack(track)
        updateTrackInformation(track)
    }

    private fun updateTrackInformation(track: TrackItem) {
        val currentTrackView: TextView = findViewById(R.id.current_track_label)
        currentTrackView.text = track.name
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
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE)

        Log.d("MainActivity", "onStart: bindService")
    }

    override fun onStop() {
        super.onStop()

        if (isBound) {
            unbindService(playerServiceConnection)
            isBound = false

            Log.d("MainActivity", "onStop: unbindService")
        }
    }
}