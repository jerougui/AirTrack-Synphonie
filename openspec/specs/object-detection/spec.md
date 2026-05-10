# Spec : object-detection

## Introduction

Module de détection d'objets. L'application existante utilise déjà TensorFlow Lite avec MobileNet SSD v2 (`mobilenet_ssd_v2.tflite`) via la classe `ObjectDetector`. Ce module doit être formalisé en interface pour faciliter les tests et un éventuel remplacement par ML Kit.

## Architecture

```kotlin
interface ObjectDetectorInterface : Closeable {
    data class Detection(val boundingBox: RectF, val label: String, val confidence: Float)
    fun detect(bitmap: Bitmap): List<Detection>
    fun findDetectionAtPoint(x: Float, y: Float, detections: List<Detection>): Detection?
    fun matchDetection(selected: RectF, detections: List<Detection>, threshold: Float = 0.5f): Detection?
}
```

**Implémentation par défaut** : `TFLiteObjectDetector` (wrapper autour de `Interpreter` TensorFlow Lite).

## Composants

### TFLiteObjectDetector

- Charge le modèle `mobilenet_ssd_v2.tflite` depuis les assets
- Prétraitement : redimensionne bitmap à 300×300, convertit en `ByteBuffer` (UINT8 ou FLOAT32 selon modèle)
- Inférence : `interpreter.runForMultipleInputsOutputs()`
- Post-traitement : extrait boxes, classes, scores, filtre confiance > 0.5
- Labels : liste COCO de 80 classes (inclut "kite", "bird", "person", etc.)

### ObjectDetector (actuel)

La classe existante sera refactorée pour implémenter l'interface.

## Interface avec les autres modules

- **Reçoit** : `Bitmap` depuis `camera-capture`
- **Retourne** : `List<Detection>` → consommé par `tracking` pour suivi
- **Sélection** : `findDetectionAtPoint()` appelé par UI (touch event)

## Configuration

Aucune configuration externe. Modèle fixe dans `assets/mobilenet_ssd_v2.tflite`.

**Future** : pour ML Kit, créer `MLKitObjectDetector` implémentant la même interface.

## Performance

- Latence d'inférence : cible < 100 ms sur Android 8+
- Optimisations :
  - Réutilisation du `Interpreter` (pas de rechargement)
  - Taille d'entrée 300×300 pour vitesse
  - Exécution sur thread d'analyse (pas UI)

## Tests

### Unitaires
- `detect()` avec image fixe → vérifier nombre de détections,置信度
- `matchDetection()` avec rectangles simulés → vérifier IoU correct
- `findDetectionAtPoint()` → point dans boîte retourne bonne détection

### Instrumentés
- Détection en temps réel : objets visibles (personne, animal, vélo, etc.)
- Suivi après sélection tactile

## Dépendances

```gradle
implementation "org.tensorflow:tensorflow-lite:2.16.1"
implementation "org.tensorflow:tensorflow-lite-support:0.4.4"
```

## Statut par rapport au code existant

Code existant : `ObjectDetector.kt` (151 lignes). À **refactoriser** :
- Extraire interface `ObjectDetectorInterface`
- Renommer classe actuelle en `TFLiteObjectDetector`
- Injecter via DI (constructor injection)
