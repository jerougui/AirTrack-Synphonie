# Spec : ui-overlay

## Introduction

Affiche l'overlay graphique superposé à la preview CameraX : bounding box de l'objet suivi, trajectoire historique, indicateurs de performance (FPS, position normalisée), note MIDI en cours.

## Architecture

`TrackingOverlayView` existe déjà (TrackingOverlayView.kt). Il faut l'étendre.

**État actuel** :
- Dessine rectangle vert (bounding box)
- Dessine point rouge (centre)
- Affiche texte debug : `x=%.2f y=%.2f FPS: %d`

**À ajouter** :
- **Trajectoire** : ligne polyligne des N dernières positions (60 points max)
- **Indicateur de note** : texte gros, couleur selon note
- **Velocity bar** : barre horizontale ou cercle indiquant speed relative à min/max
- **Direction arrow** : flèche indiquant angle (optionnel)

## Composants

### TrackingOverlayView (extension)

**Nouvelles propriétés** :

```kotlin
private val trajectoryPoints = ArrayDeque<PointF>(maxTrajectoryLength)
private var currentNote: String? = null
private var currentVelocity: Float = 0f
private var currentInstrument: Int = 0
```

**Méthodes ajoutées** :

```kotlin
fun setMusicInfo(note: String, velocity: Float, instrument: Int)
fun clearTrajectory()
```

**onDraw() enrichi** :

1. Dessiner trajectoire (Path à partir de trajectoryPoints)
2. Dessiner velocity bar (arc ou barre horizontale)
3. Afficher note (texte, taille 48sp, couleur dynamique)
4. Optionnel : flèche direction (Canvas.drawLine ou drawPath)

### Optimisations

- Éviter allocations dans `onDraw()` : réutiliser `Paint`, `Path`, `RectF`
- `trajectoryPoints` : buffer circulaire, pas de GC
- `postInvalidateOnAnimation()` pour synchronisation Vsync

## Interface avec les autres modules

- **Reçoit** :
  - `TrackingData` (position, velocity, direction) → updateTracking()
  - `MusicEvent` (note, instrument) → setMusicInfo()
- **Consommateur** : `MainActivity` (appel des méthodes)

## Configuration

Aucune configuration externe. Taille trajectoire : constante (60) ou paramètre dans `ConfigManager`.

## Tests

### Visuels (manuels)
- Trajectoire visible, fluide, pas de lag
- Note s'affiche et change avec la musique
- FPS stable en HUD
- Overlay ne bloque pas preview CameraX

### Performance
- Profiler : moins de 5 ms par frame pour onDraw()
- Pas de jank (vsync drops) sur 60 FPS

## Dépendances

Aucune nouvelle.

## Statut par rapport au code existant

`TrackingOverlayView.kt` existe (94 lignes). À **étendre** avec les nouvelles fonctionnalités.
