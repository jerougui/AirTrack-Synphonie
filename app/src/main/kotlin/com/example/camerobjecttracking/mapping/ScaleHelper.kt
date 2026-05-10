package com.example.camerobjecttracking.mapping

/**
 * Helper pour le calcul des gammes musicales et conversion notes MIDI.
 */
object ScaleHelper {

    /**
     * Retourne les écarts en demi-tons depuis la tonique pour une gamme donnée.
     * @param scaleName Nom de la gamme (ex: "pentatonic", "major", "minor", "blues")
     * @return Liste des offsets en demi-tons (ex: pentatonic major → [0,2,4,7,9])
     */
    fun getScaleIntervals(scaleName: String): List<Int> {
        return when (scaleName.lowercase()) {
            "pentatonic" -> listOf(0, 2, 4, 7, 9)      // Pentatonic major
            "pentatonic_minor", "pentatonic minor" -> listOf(0, 3, 5, 7, 10) // Pentatonic minor
            "major", "ionian" -> listOf(0, 2, 4, 5, 7, 9, 11)
            "minor", "aeolian" -> listOf(0, 2, 3, 5, 7, 8, 10)
            "blues" -> listOf(0, 3, 5, 6, 7, 10)
            "chromatic" -> (0..12).toList()
            else -> {
                // Fallback: pentatonic
                listOf(0, 2, 4, 7, 9)
            }
        }
    }

    /**
     * Calcule la note MIDI à partir de la note de base et d'un degré dans la gamme.
     * @param baseNote MIDI note de la tonique (ex: 60 = C4)
     * @param degree Index dans la liste des intervalles (0 = tonique)
     * @return Numéro MIDI (0–127)
     */
    fun midiNoteFromBase(baseNote: Int, degree: Int, scaleIntervals: List<Int>): Int {
        val offset = scaleIntervals.getOrElse(degree) { 0 }
        var note = baseNote + offset
        // Assurer dans la范围 0–127 (clamp)
        if (note > 127) note = 127
        if (note < 0) note = 0
        return note
    }

    /**
     * Calcule la fréquence MIDI en Hz (pour affichage ou synthèse externe).
     * @param midiNote Numéro MIDI 0–127
     * @return Fréquence en Hz (A4 = 440Hz, A4 MIDI = 69)
     */
    fun midiToFrequency(midiNote: Int): Double {
        return 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)
    }
}
