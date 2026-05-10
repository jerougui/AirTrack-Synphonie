package com.example.camerobjecttracking.model

/**
 * Événement musical généré par le mapper.
 *
 * @param note Numéro MIDI (0–127)
 * @param velocity Vélocité MIDI (0–127, typical 64–127)
 * @param instrument Numéro de programme General MIDI (0–127)
 * @param durationMs Durée de la note en millisecondes (pour NOTE_OFF automatique)
 * @param effect Effet spécial optionnel (vibrato, etc.)
 */
data class MusicEvent(
    val note: Int,
    val velocity: Int,
    val instrument: Int,
    val durationMs: Int = 200,
    val effect: MusicEffect? = null
) {
    init {
        require(note in 0..127) { "MIDI note must be 0–127" }
        require(velocity in 0..127) { "MIDI velocity must be 0–127" }
        require(instrument in 0..127) { "MIDI program must be 0–127" }
        require(durationMs > 0) { "Duration must be positive" }
    }
}

/** Effets audio pouvant être appliqués à une note. */
enum class MusicEffect {
    VIBRATO,    // Modulation de hauteur
    TREMOLO,    // Modulation de volume
    FILTER_LOW, // Filter cutoff bas
    FILTER_HIGH // Filter cutoff aigu
}
