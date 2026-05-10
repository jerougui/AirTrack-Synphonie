package com.example.camerobjecttracking.model

import android.graphics.RectF

/**
 * Objet détecté et suivi par le système.
 *
 * @param id Identifiant unique (peut venir du tracking ID ML Kit, sinon généré)
 * @param boundingBox Boîte englobante dans les coordonnées normalisées de l'image (0–1)
 * @param label Label de la classe (ex: "person", "kite")
 * @param confidence Confiance de la détection [0.0, 1.0]
 */
data class TrackedObject(
    val id: Int,
    val boundingBox: RectF,
    val label: String,
    val confidence: Float
) {
    /** Centre de la boîte englobante (normalisé). */
    val centerX: Float get() = (boundingBox.left + boundingBox.right) / 2f
    val centerY: Float get() = (boundingBox.top + boundingBox.bottom) / 2f
}
