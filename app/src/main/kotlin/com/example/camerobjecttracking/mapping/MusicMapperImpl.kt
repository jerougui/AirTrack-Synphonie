package com.example.camerobjecttracking.mapping

import android.util.Log
import com.example.camerobjecttracking.config.ConfigManager
import com.example.camerobjecttracking.model.MusicEvent
import com.example.camerobjecttracking.model.MusicEffect
import com.example.camerobjecttracking.model.MappingConfig
import com.example.camerobjecttracking.model.TrackingData

/**
 * Mappe les données de suivi (mouvement) vers des événements musicaux (notes).
 * La configuration est lue depuis ConfigManager (via StateFlow).
 *
 * Workflow:
 * 1. Si speed < minSpeed → silence (retour null)
 * 2. Si speedToPitch → calcule note selon gamme et vitesse normalisée
 * 3. Si directionToInstrument → sélectionne instrument selon angle
 * 4. Si accelerationToEffect → ajoute effet si accélération brutale
 * 5. Debounce → ne pas envoyer plus d'une note toutes les noteDebounceMs
 */
class MusicMapperImpl(private val configManager: ConfigManager) {

    private var lastNote: Int? = null
    private var lastNoteTime: Long = 0L

    /**
     * Convertit TrackingData en MusicEvent.
     * @return MusicEvent ou null si silence (vitesse trop faible)
     */
    fun map(tracking: TrackingData): MusicEvent? {
        val config = configManager.getCurrentConfig().mapping

        // 1. Seuil de vitesse (silence si trop lent)
        if (tracking.speed < config.minSpeed) {
            lastNote = null
            return null
        }

        // 2. Speed → Pitch
        val speedProgress = ((tracking.speed - config.minSpeed) / (config.maxSpeed - config.minSpeed))
            .coerceIn(0f, 1f)

        val scaleIntervals = ScaleHelper.getScaleIntervals(config.scale)
        val degree = (speedProgress * (scaleIntervals.size - 1)).roundToInt()
        val note = ScaleHelper.midiNoteFromBase(config.baseNote, degree, scaleIntervals)

        // Debounce: si même note et pas assez de temps écoulé, ne rien envoyer
        val now = System.currentTimeMillis()
        if (note == lastNote && (now - lastNoteTime) < config.noteDebounceMs) {
            return null
        }

        lastNote = note
        lastNoteTime = now

        // 3. Direction → Instrument (optionnel, pas implémenté dans MusicEvent pour l'instant, car l'instrument est global dans audio)
        // On pourrait changer l'instrument dans AudioPlayer de manière persistante, mais MusicEvent a un champ instrument.
        // Pour l'instant, on utilise l'instrument par défaut de config.
        val instrument = configManager.getCurrentConfig().audio.defaultInstrument

        // 4. Acceleration → Effect (non implémenté car MusicEvent.effect optionnel, on pourrait ajouter si accel brutale)
        val effect = if (config.accelerationToEffect) {
            // TODO: calculer acceleration à partir de tracking.velocity difference.
            // Pour l'instant, null.
            null
        } else null

        // Velocity MIDI: 0–127, mapper depuis speed ou fixe
        val midiVelocity = (speedProgress * 127).toInt().coerceIn(0, 127)

        return MusicEvent(
            note = note,
            velocity = midiVelocity,
            instrument = instrument,
            durationMs = 200, // valeur fixe pour l'instant
            effect = effect
        )
    }

    /** Réinitialise l'état interne (dernière note) pour éviter notes collées lors du démarrage. */
    fun reset() {
        lastNote = null
        lastNoteTime = 0L
    }
}
