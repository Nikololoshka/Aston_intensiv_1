package dev.aston.intensiv.nikolay.service

import dev.aston.intensiv.nikolay.library.TrackItem

interface Player {

    val currentTrack: TrackItem?
    val isPlaying: Boolean

    fun playTrack(item: TrackItem)

    fun pausePlaying()

    fun resumePlaying()

    fun stop()

    fun nextTrack()

    fun previousTrack()
}