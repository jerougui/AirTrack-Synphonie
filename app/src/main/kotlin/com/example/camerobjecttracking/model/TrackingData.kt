package com.example.camerobjecttracking.model

import android.graphics.RectF

/**
 * Données de suivi d'un objet, calculées à partir des détections.
 *
 * @param normalizedX Position X normalisée [0.0, 1.0] (0 = gauche, 1 = droite)
 * @param normalizedY Position Y normalisée [0.0, 1.0] (0 = haut, 1 = bas)
 * @param velocityX Vitesse en pixels/s (ou unités normalisées/s si image cube)
 * @param velocityY Vitesse en pixels/s
 * @param speed Norme de la vélocité √(vx² + vy²)
 * @param direction Angle en radians, 0 = droite, π/2 = bas, π = gauche, -π/2 = haut
 * @param timestamp Epoch ms de la dernière mise à jour
 * @param boundingBox Boîte englobante de l'objet détecté (optionnelle, pour affichage)
 */
data class TrackingData(
    val normalizedX: Float,
    val normalizedY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val speed: Float,
    val direction: Float,
    val timestamp: Long,
    val boundingBox: RectF? = null
) {
    companion object {
        /** Crée un TrackingData à partir d'une position normalisée et d'un timestamp. */
        fun fromPosition(x: Float, y: Float, ts: Long, box: RectF? = null): TrackingData {
            return TrackingData(x, y, 0f, 0f, 0f, 0f, ts, box)
        }
    }
}
