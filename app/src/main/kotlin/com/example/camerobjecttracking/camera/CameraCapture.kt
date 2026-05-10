package com.example.camerobjecttracking.camera

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

/**
 * Interface d'abstraction pour l'acquisition caméra.
 * Encapsule CameraX et fournit un flux d'images analysées.
 */
interface CameraCapture {
    /**
     * Démarre la caméra.
     * @param lifecycleOwner Propriétaire du cycle de vie (généralement Activity)
     * @param previewView Vue d'aperçu (PreviewView) où afficher le flux
     * @param onFrame Callback appelé pour chaque frame analysée (hors thread UI)
     */
    fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView, onFrame: (ImageProxy) -> Unit)

    /** Arrête la caméra et libère les ressources. */
    fun stop()

    /** Définit la résolution cible (doit être appelé avant start). */
    fun setTargetResolution(size: android.util.Size)

    /** Sélectionne la caméra avant (true) ou arrière (false). */
    fun useFrontCamera(enable: Boolean)

    /** Retourne le FPS courant mesuré (approximation). */
    fun getCurrentFps(): Int
}
