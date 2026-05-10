package com.example.camerobjecttracking.audio

import com.example.camerobjecttracking.model.MusicEvent

/**
 * Interface de lecture audio (sons MIDI ou samples).
 * Doit être thread-safe et à faible latence.
 */
interface AudioPlayer : AutoCloseable {

    /**
     * Joue un événement musical.
     * L'implémentation doit gérer NOTE_ON immédiatement et NOTE_OFF après durationMs.
     */
    fun play(event: MusicEvent)

    /** Arrête tous les sons en cours. */
    fun stop()

    /** Règle le volume global (0.0 – 1.0). */
    fun setVolume(volume: Float)
}
