package dev.aston.intensiv.nikolay

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import dev.aston.intensiv.nikolay.library.TrackItem
import dev.aston.intensiv.nikolay.library.TrackLibraryAdapter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val trackAdapter = TrackLibraryAdapter(this::onTrackItemClicked)
        val trackList: RecyclerView = findViewById(R.id.track_library_list)
        trackList.adapter = trackAdapter

        val tracks = assets.list("mp3")
            .orEmpty()
            .map { TrackItem(it.substringBefore(".mp3")) }
        trackAdapter.submitList(tracks)
    }

    private fun onTrackItemClicked(track: TrackItem) {
        Log.d("MainActivity", "onTrackItemClicked: $track")
    }
}