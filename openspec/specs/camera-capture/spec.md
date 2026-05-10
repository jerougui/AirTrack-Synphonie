# Spec : camera-capture

## Introduction

Ce module gère l'acquisition vidéo en temps réel via CameraX. L'application existante (mainActivity.kt) utilise déjà CameraX avec Preview et ImageAnalysis. Ce module doit être extrait en composant réutilisable.

## Architecture

Le module `camera-capture` expose une interface simple :

```kotlin
interface CameraCapture {
    fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView, onFrame: (ImageProxy) -> Unit)
    fun stop()
    fun setTargetResolution(size: Size)
    fun useFrontCamera(enable: Boolean)
}
```

## Composants

### CameraCaptureImpl

Implémentation concrète utilisant CameraX :

- **Initialisation** : `ProcessCameraProvider.getInstance(context)`
- **Use cases** :
  - `Preview` → attache à `previewView.surfaceProvider`
  - `ImageAnalysis` → exécuteur dédié, callback `onFrame`
- **Configuration** :
  - Résolution cible : 640×480 par défaut (configurable)
  - Backpressure : `STRATEGY_KEEP_ONLY_LATEST`
  - Caméra : arrière par défaut, option avant
- **Gestion d'erreur** : toast + log si échec de binding

### Extensions

Extension `ImageProxy.toBitmap()` déjà existante dans MainActivity.kt — à déplacer dans un fichier dédié.

## Interface avec les autres modules

- **Consommateur principal** : `ObjectDetector` (module detection)
- ** flux de données** : `ImageProxy` → conversion en `Bitmap` → inférence TFLite
- **Cycle de vie** : contrôlé par `MainActivity` (start/stop)

## Configuration

Aucune configuration externe nécessaire pour ce module. Résolution fixe ou via `setTargetResolution()`.

## Performance

- Cible : 30–60 FPS
- Mesure : timestamp sur chaque frame reçue
- Optimisation : `STRATEGY_KEEP_ONLY_LATEST` évite l'accumulation de frames

## Tests

### Unitaires
- Mock `ProcessCameraProvider` → vérifier `bindToLifecycle` appelé avec Preview + ImageAnalysis

### Instrumentés (manuel)
- Preview s'affiche sans lag
- FPS mesuré ≥ 30 sur device de référence (Android 8+)

## Dépendances

```gradle
implementation "androidx.camera:camera-core:1.3.0-alpha02"
implementation "androidx.camera:camera-camera2:1.3.0-alpha02"
implementation "androidx.camera:camera-lifecycle:1.3.0-alpha02"
implementation "androidx.camera:camera-view:1.3.0-alpha02"
```

## Statut par rapport au code existant

Le code existe déjà dans `MainActivity.kt` (fonction `startCamera()` et extension `toBitmap()`). Le travail consiste à **extraire** dans un module dédié et à injecter via interface.
