package com.example.camerobjecttracking.tracking

import android.graphics.RectF
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.camerobjecttracking.detection.ObjectDetectorInterface
import com.example.camerobjecttracking.model.TrackingData

/**
 * Moteur de suivi qui combine détection, matching IoU et filtre de Kalman.
 *
 * Workflow:
 * 1. Reçoit une liste de détections (déjà normalisées)
 * 2. Essaye de matcher avec l'objet suivi précédent (IoU threshold)
 * 3. Si match trouvé: correction du Kalman avec la position mesurée
 *    Sinon: prédiction seulement
 * 4. Émet TrackingData (position lissée, vélocité, direction)
 *
 * @param detector Détecteur d'objets (pour matching)
 * @param config Configuration du tracker (IoU threshold, paramètres Kalman)
 */
class TrackingEngine(
    private val detector: ObjectDetectorInterface,
    private val config: com.example.camerobjecttracking.model.TrackerConfig
) {
    private var kalmanX = KalmanFilter1D(processNoise = config.kalmanProcessNoise, measurementNoise = config.kalmanMeasurementNoise)
    private var kalmanY = KalmanFilter1D(processNoise = config.kalmanProcessNoise, measurementNoise = config.kalmanMeasurementNoise)

    private var lastTimestamp: Long = 0L
    private var lostFramesCount = 0

    // Dernière position (pour calcul dt)
    private var lastDetection: ObjectDetectorInterface.Detection? = null

    // État actuel exposé (LiveData)
    private val _trackingData = MutableLiveData<TrackingData?>(null)
    val trackingData: LiveData<TrackingData?> = _trackingData

    /** Indique si un objet est actuellement suivi. */
    var isTracking: Boolean = false
        private set

    /**
     * Traite une nouvelle frame de détections.
     * @param detections Liste des objets détectés (normalisés)
     * @param timestamp Epoque ms de l'image
     * @return TrackingData si suivi actif, null sinon
     */
    fun update(detections: List<ObjectDetectorInterface.Detection>, timestamp: Long): TrackingData? {
        val dt = if (lastTimestamp > 0) (timestamp - lastTimestamp) / 1000f else 0f
        lastTimestamp = timestamp

        // 1. Tentative de matching avec la détection précédente
        val previousBox = lastDetection?.boundingBox
        val matched = if (previousBox != null) {
            detector.matchDetection(previousBox, detections, config.iouThreshold)
        } else null

        if (matched != null) {
            // Match trouvé : correction Kalman avec le centre
            val cx = matched.centerX
            val cy = matched.centerY

            kalmanX.predict(dt)
            kalmanY.predict(dt)
            kalmanX.correct(cx)
            kalmanY.correct(cy)

            lastDetection = matched
            lostFramesCount = 0
            isTracking = true

            val data = buildTrackingData(timestamp, dt)
            _trackingData.postValue(data)
            return data
        } else {
            // Aucun match : prédiction pure
            lostFramesCount++
            if (lostFramesCount > config.maxLostFrames) {
                isTracking = false
                _trackingData.postValue(null)
                return null
            }

            kalmanX.predict(dt)
            kalmanY.predict(dt)

            // On n'a pas de mesure, mais on peut émettre une prédiction
            val predictedX = kalmanX.position
            val predictedY = kalmanY.position
            val vx = kalmanX.velocity
            val vy = kalmanY.velocity

            val data = TrackingData(
                normalizedX = predictedX.coerceIn(0f, 1f),
                normalizedY = predictedY.coerceIn(0f, 1f),
                velocityX = vx,
                velocityY = vy,
                speed = sqrt(vx * vx + vy * vy),
                direction = atan2(vy, vx).toFloat(),
                timestamp = timestamp,
                boundingBox = null
            )
            _trackingData.postValue(data)
            return data
        }
    }

    /** Construit TrackingData à partir des états Kalman. */
    private fun buildTrackingData(timestamp: Long, dt: Float): TrackingData {
        val x = kalmanX.position
        val y = kalmanY.position
        val vx = kalmanX.velocity
        val vy = kalmanY.velocity
        val speed = sqrt(vx * vx + vy * vy)
        val dir = atan2(vy, vx).toFloat()

        return TrackingData(
            normalizedX = x,
            normalizedY = y,
            velocityX = vx,
            velocityY = vy,
            speed = speed,
            direction = dir,
            timestamp = timestamp,
            boundingBox = lastDetection?.boundingBox
        )
    }

    /** Réinitialise le moteur (perte d'objet volontaire). */
    fun reset() {
        kalmanX.reset()
        kalmanY.reset()
        lastDetection = null
        lastTimestamp = 0L
        lostFramesCount = 0
        isTracking = false
        _trackingData.postValue(null)
    }

    /** Retourne la dernière position suivie (si tracking actif). */
    fun getCurrentPosition(): Pair<Float, Float>? {
        val data = _trackingData.value ?: return null
        return data.normalizedX to data.normalizedY
    }
}

/** Helper pour obtenir le centre d'une RectF normalisée. */
private val ObjectDetectorInterface.Detection.centerX: Float
    get() = (boundingBox.left + boundingBox.right) / 2f

private val ObjectDetectorInterface.Detection.centerY: Float
    get() = (boundingBox.top + boundingBox.bottom) / 2f
