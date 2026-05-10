package com.example.camerobjecttracking.detection

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.camerobjecttracking.model.TrackedObject

/**
 * Interface abstraite pour la détection d'objets.
 * Permet de swap entre TFLite, ML Kit, ou autres backends.
 */
interface ObjectDetectorInterface : Closeable {
    /**
     * Détecte tous les objets dans l'image.
     * @param bitmap Image en entrée (RGB)
     * @return Liste des détections dont la confiance > threshold interne
     */
    fun detect(bitmap: Bitmap): List<Detection>

    /**
     * Trouve la détection contenant le point (x,y) dans les coordonnées normalisées [0,1].
     * Retourne la détection de plus haute confiance contenant le point.
     */
    fun findDetectionAtPoint(x: Float, y: Float, detections: List<Detection>): Detection?

    /**
     * Trouve la détection correspondant à une boîte précédente (IoU-based matching).
     * @param selected Boîte précédente (normalisée)
     * @param detections Détections courantes
     * @param threshold Seuil IoU minimal pour considérer un match (défaut 0.5)
     */
    fun matchDetection(selected: RectF, detections: List<Detection>, threshold: Float = 0.5f): Detection?

    /** Données brutes d'une détection. */
    data class Detection(
        val boundingBox: RectF, // coordonnées normalisées [0,1]
        val label: String,
        val confidence: Float
    ) {
        /** Convertit en TrackedObject (avec ID généré ou fourni). */
        fun toTrackedObject(id: Int): TrackedObject = TrackedObject(
            id = id,
            boundingBox = boundingBox,
            label = label,
            confidence = confidence
        )
    }
}
