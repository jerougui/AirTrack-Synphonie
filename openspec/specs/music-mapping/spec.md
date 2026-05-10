# Spec : music-mapping

## Introduction

Convertit les données de mouvement (position, vitesse, direction, accélération) en événements musicaux (notes, instruments, effets). Le mapping est entièrement配置able via fichier JSON.

## Architecture

```kotlin
interface MusicMapper {
    fun map(tracking: TrackingData): MusicEvent?
}
```

**Data classes** :

```kotlin
data class MusicEvent(
    val note: Int,           // MIDI note number 0–127
    val velocity: Int,       // MIDI velocity 0–127
    val instrument: Int,     // MIDI program number 0–127
    val durationMs: Int,     // note duration (pour NoteOff)
    val effect: MusicEffect? // optional: VIBRATO, PITCH_BEND, etc.
)

enum class MusicEffect { VIBRATO, TREMOLO, FILTER_LOW, FILTER_HIGH }
```

## Composants

### MusicMapperImpl

Logique de mapping :

1. **Speed → Pitch** :
   - Si `config.speedToPitch` est true :
   - `speed` ∈ [minSpeed, maxSpeed] mappé sur une note dans la gamme
   - `progress = (speed - minSpeed) / (maxSpeed - minSpeed)`
   - `noteIndex = (progress * (scale.size - 1)).roundToInt()`
   - `note = baseNote + scale[noteIndex]` (ouMIDI direct si gamme fournie comme table)

2. **Direction → Instrument** :
   - Si `config.directionToInstrument` est true :
   - `angle = direction` (0 à 2π)
   - Diviser le cercle en N secteurs (N = nb d'instruments configurés)
   - Sélectionner instrument selon secteur

3. **Acceleration → Effect** :
   - Si `config.accelerationToEffect` est true :
   - Calcul `accel = (speed - previousSpeed) / dt`
   - Si |accel| > threshold → ajouter `MusicEffect` (ex : accel > 0 = VIBRATO)

4. **Debounce** :
   - Si même note déjà en cours, ne pas renvoyer nouvel événement avant `noteDebounceMs`
   - Si note différente : envoyer NOTE_OFF ancienne, NOTE_ON nouvelle

### ScaleHelper

Fonctions utilitaires :

- `fun getScaleNotes(scaleName: String): List<Int>` — retourne les écarts (en demi-tons) depuis la tonique pour diverses gammes (pentatonic major, pentatonic minor, major, minor, blues, etc.)
- `fun midiNoteFromBase(baseNote: Int, scaleDegree: Int): Int` — calcule le numéro MIDI

**Gammes supportées** :
- Pentatonic Major : [0, 2, 4, 7, 9]
- Pentatonic Minor : [0, 3, 5, 7, 10]
- Major (Ionien) : [0, 2, 4, 5, 7, 9, 11]
- Natural Minor (Aeolian) : [0, 2, 3, 5, 7, 8, 10]
- Blues : [0, 3, 5, 6, 7, 10]

## Interface avec les autres modules

- **Entrée** : `TrackingData` (depuis `tracking`)
- **Sortie** : `MusicEvent` (vers `audio-playback`)
- **État interne** : note actuelle, timestamp dernier événement (pour debounce)

## Configuration JSON (section mapping)

```json
{
  "mapping": {
    "minSpeed": 0.0,
    "maxSpeed": 3.0,
    "speedToPitch": true,
    "directionToInstrument": false,
    "accelerationToEffect": true,
    "noteDebounceMs": 100,
    "scale": "pentatonic",
    "baseNote": 60,          // C4 MIDI
    "instruments": ["flute", "violin"],
    "accelThreshold": 1.5    // unités normalisées/sec²
  }
}
```

## Tests

### Unitaires
- `MusicMapperTest` :
  - Speed=0 → silence ou note minimum ?
  - Speed min → note basse ; Speed max → note haute
  - Direction change → instrument change (si activé)
  - Debounce : deux événements rapprochés ignore le second

### Instrumentés
- Mouvement lent → notes basses ; rapide → notes aigues
- Rotation → changement d'instrument (si activé)
- Accélération brusque → effet audible (vibrato)

## Dépendances

Aucune en dehors de Kotlin stdlib. Audio MIDI viendra plus tard via `audio-playback`.

## Statut

À implémenter. Pas de code préexistant dédié au mapping.
