package dev.aston.intensiv.nikolay.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.aston.intensiv.nikolay.R

class TrackLibraryAdapter(
    private val onTrackClicked: (track: TrackItem) -> Unit
) : ListAdapter<TrackItem, TrackViewHolder>(TRACK_DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return TrackViewHolder(view, this::onTrackItemClicked)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    private fun onTrackItemClicked(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            onTrackClicked(currentList[position])
        }
    }

    companion object {

        private val TRACK_DIFF_CALLBACK = object : DiffUtil.ItemCallback<TrackItem>() {
            override fun areItemsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean =
                oldItem == newItem
        }
    }
}