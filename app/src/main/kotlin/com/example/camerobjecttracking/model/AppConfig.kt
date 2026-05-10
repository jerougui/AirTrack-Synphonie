package com.example.camerobjecttracking.model

/**
 * Configuration globale de l'application, chargée depuis JSON.
 * Toutes les plages et valeurs par défaut sont validées à la lecture.
 */
data class AppConfig(
    val audio: AudioConfig,
    val mapping: MappingConfig,
    val detection: DetectionConfig,
    val tracker: TrackerConfig,
    val ui: UiConfig
) {
    /** Retourne une copie avec une section modifiée (pour immutabilité). */
    fun copyWith(
        audio: AudioConfig? = null,
        mapping: MappingConfig? = null,
        detection: DetectionConfig? = null,
        tracker: TrackerConfig? = null,
        ui: UiConfig? = null
    ): AppConfig = AppConfig(
        audio = audio ?: this.audio,
        mapping = mapping ?: this.mapping,
        detection = detection ?: this.detection,
        tracker = tracker ?: this.tracker,
        ui = ui ?: this.ui
    )
}

/** Configuration du sous-système audio. */
data class AudioConfig(
    val backend: AudioBackend,
    val defaultInstrument: Int, // MIDI program number 0–127
    val volume: Float, // 0.0 – 1.0
    val soundpoolPath: String? = null, // relatif à filesDir si backend = SOUNDPOOL
    val midiDeviceId: Int = -1 // -1 = auto-select
) {
    init {
        require(volume in 0.0f..1.0f) { "Volume must be 0.0–1.0" }
        require(defaultInstrument in 0..127) { "MIDI program must be 0–127" }
    }
}

enum class AudioBackend {
    MIDI,
    SOUNDPOOL
}

/** Configuration du mapping mouvement → musique. */
data class MappingConfig(
    val minSpeed: Float,
    val maxSpeed: Float,
    val speedToPitch: Boolean,
    val directionToInstrument: Boolean,
    val accelerationToEffect: Boolean,
    val noteDebounceMs: Int,
    val scale: String, // clé de gamme : "pentatonic", "major", "minor", "blues"
    val baseNote: Int, // MIDI note de la tonique (0–127)
    val instruments: List<String> // noms ou numéros MIDI comme chaînes
) {
    init {
        require(minSpeed >= 0f) { "minSpeed must be >= 0" }
        require(maxSpeed > minSpeed) { "maxSpeed must be > minSpeed" }
        require(noteDebounceMs >= 0) { "noteDebounceMs must be >= 0" }
        require(baseNote in 0..127) { "baseNote must be 0–127" }
    }
}

/** Configuration du module de détection d'objets. */
data class DetectionConfig(
    val confidenceThreshold: Float, // 0.0 – 1.0
    val maxDetections: Int // nombre max d'objets retournés par le modèle
) {
    init {
        require(confidenceThreshold in 0.0f..1.0f) { "confidenceThreshold must be 0.0–1.0" }
        require(maxDetections > 0) { "maxDetections must be > 0" }
    }
}

/** Configuration du module de tracking (Kalman + IoU). */
data class TrackerConfig(
    val kalmanProcessNoise: Float,
    val kalmanMeasurementNoise: Float,
    val iouThreshold: Float,
    val maxLostFrames: Int
) {
    init {
        require(kalmanProcessNoise > 0f) { "kalmanProcessNoise must be > 0" }
        require(kalmanMeasurementNoise > 0f) { "kalmanMeasurementNoise must be > 0" }
        require(iouThreshold in 0f..1f) { "iouThreshold must be 0.0–1.0" }
        require(maxLostFrames > 0) { "maxLostFrames must be > 0" }
    }
}

/** Configuration de l'interface utilisateur. */
data class UiConfig(
    val showTrajectory: Boolean,
    val trajectoryLength: Int,
    val showFps: Boolean,
    val showNote: Boolean
) {
    init {
        require(trajectoryLength > 0) { "trajectoryLength must be > 0" }
    }
}
