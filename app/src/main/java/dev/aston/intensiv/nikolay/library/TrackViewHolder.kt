package dev.aston.intensiv.nikolay.library

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.aston.intensiv.nikolay.R

class TrackViewHolder(
    itemView: View,
    private val onTrackClicked: (position: Int) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val trackNameView: TextView = itemView.findViewById(R.id.track_item_name)

    init {
        itemView.setOnClickListener { onTrackClicked(adapterPosition) }
    }

    fun bind(track: TrackItem) {
        trackNameView.text = track.name
    }
}