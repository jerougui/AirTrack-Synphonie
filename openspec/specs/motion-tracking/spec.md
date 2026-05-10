# Spec : motion-tracking

## Introduction

Calcule position normalisée, vitesse et direction à partir des détections d'objets. Intègre un filtre de Kalman pour lissage et prédiction, stabilisant le suivi même en cas de détections manquantes ou bruitées.

## Architecture

```kotlin
interface TrackingEngine {
    fun start()
    fun stop()
    fun update(detections: List<ObjectDetectorInterface.Detection>): TrackingData?
}
```

**Data classes** :

```kotlin
data class TrackingData(
    val normalizedX: Float,     // 0.0–1.0 (relatif à la largeur image)
    val normalizedY: Float,     // 0.0–1.0 (relatif à la hauteur image)
    val velocityX: Float,       // Δx / Δt (unités normalisés/sec)
    val velocityY: Float,       // Δy / Δt
    val speed: Float,           // √(vx²+vy²)
    val direction: Float,       // angle en radians, 0 = droite, π/2 = bas, etc.
    val timestamp: Long         // ms
)
```

## Composants

### KalmanFilter (1D ou 2D)

**Modèle** : vitesse constante (constant velocity)

State vector : `[position, velocity]` (2D si on suit x et y séparément, ou 4D pour 2D combined)

Matrices :
- F (state transition) : `[[1, Δt], [0, 1]]`
- H (measurement) : `[1, 0]` (on ne mesure que la position)
- Q (process noise) : à accorder selon le mouvement attendu
- R (measurement noise) : bruit de détection (pixels → normalisés)

**Fonctions** :
- `predict(dt: Float): PredictedState`
- `correct(measurement: Float): CorrectedState`

**Paramétrage** :
- `processNoise` : 0.01–0.1 (ajustable)
- `measurementNoise` : 0.05–0.2 (selon confiance détection)

### TrackingEngineImpl

Workflow :

1. **Détection reçue** → center = middle of bounding box (normalisé)
2. **Matching** : utiliser `ObjectDetector.matchDetection()` pour associer à l'objet suivi précédent (IoU threshold configurable)
3. **Kalman** :
   - Si match trouvé : `correct(measuredCenter)` → position lissée
   - Sinon : `predict(dt)` seulement (utiliser prédiction, pas de correction)
4. **Calcul dérivés** :
   - `velocity = (position - previousPosition) / dt`
   - `direction = atan2(velocityY, velocityX)`
   - `speed = sqrt(vx² + vy²)`
5. **Émission** : émettre `TrackingData` à chaque frame (même si prédiction pure)

### Perte d'objet

- Si aucune détection ne correspond après N frames (N=5?), statut = `LOST`
- `MainActivity` peut alors rafraîchir la sélection par touch

## Interface avec les autres modules

- **Entrée** : `List<ObjectDetector.Detection>` (depuis detection)
- **Sortie** : `TrackingData` (vers mapping)
- **Side channel** : `TrackingOverlayView.updateTracking()` pour affichage

## Configuration

Paramètres (depuis Config JSON ou SharedPreferences) :
- `kalmanProcessNoise` : Float (défaut 0.05)
- `kalmanMeasurementNoise` : Float (défaut 0.1)
- `iouThreshold` : Float (défaut 0.3) pour matching
- `maxLostFrames` : Int (défaut 5) avant marquer perdu

## Performance

- Calcul Kalman : O(1) par frame
- Aucune allocation excessive (réutiliser objets si possible)
- Cible : 60 Hz mise à jour

## Tests

### Unitaires
- `KalmanFilterTest` :
  - Position constante → converge, variance diminue
  - Position linéaire → suit sans lag
  - Saut brusque → réagit progressivement
- `TrackingEngineTest` :
  - Séquence de détections avec bruit → vérifier stabilité
  - Perte d'objet → prédiction seule
  - Re-acquisition après perte

### Instrumentés
- Suivi fluide d'un objet en mouvement réel (drone, oiseau)
- Velocity stable (pas de sauts brutaux)

## Dépendances

Aucune nouvelle dépendance ( maths pures Kotlin). Possible `kotlin.math` pour `atan2`, `sqrt`.

## Statut par rapport au code existant

Le code actuel (`MainActivity.kt:231–270`) fait un matching IoU simple et transmet les coordonnées brutes. Remplacer par pipeline : détection → Kalman → TrackingData.
