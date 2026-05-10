# Spec : config-system

## Introduction

Système de configuration par fichier JSON externe pour personnaliser instruments, gammes, règles de mapping et paramètres audio. Remplacer/augmenter les `SharedPreferences` actuelles (UDP settings).

## Architecture

```kotlin
data class AppConfig(
    val audio: AudioConfig,
    val mapping: MappingConfig,
    val detection: DetectionConfig,
    val tracker: TrackerConfig,
    val ui: UiConfig
)

data class AudioConfig(
    val backend: AudioBackend,      // MIDI ou SOUNDPOOL
    val defaultInstrument: Int,     // program MIDI 0–127
    val volume: Float,              // 0.0–1.0
    val soundpoolPath: String?      // null si MIDI
)

data class MappingConfig(
    val minSpeed: Float,
    val maxSpeed: Float,
    val speedToPitch: Boolean,
    val directionToInstrument: Boolean,
    val accelerationToEffect: Boolean,
    val noteDebounceMs: Int,
    val scale: String,              // "pentatonic", "major", etc.
    val baseNote: Int,              // MIDI note 0–127
    val instruments: List<String>   // noms ou numéros
)

data class DetectionConfig(
    val confidenceThreshold: Float, // 0.0–1.0
    val maxDetections: Int
    // autres...
)

data class TrackerConfig(
    val kalmanProcessNoise: Float,
    val kalmanMeasurementNoise: Float,
    val iouThreshold: Float,
    val maxLostFrames: Int
)

data class UiConfig(
    val showTrajectory: Boolean,
    val trajectoryLength: Int,
    val showFps: Boolean
)
```

**Interface principale** :

```kotlin
class ConfigManager(context: Context) {
    fun load(): AppConfig        // lit fichier ou crée defaults
    fun save(config: AppConfig)  // écrit fichier
    fun reset()                  // restore defaults
    val configFlow: Flow<AppConfig>  // observe changements
}
```

## Fichier de configuration

**Chemin** :
1. `context.filesDir/airtrack_config.json` (interne, persistant)
2. Si absent : copier `assets/default_config.json` → `filesDir/`

**Schema JSON** :

```json
{
  "audio": {
    "backend": "midi",
    "defaultInstrument": 73,
    "volume": 0.8
  },
  "mapping": {
    "minSpeed": 0.0,
    "maxSpeed": 3.0,
    "speedToPitch": true,
    "directionToInstrument": false,
    "accelerationToEffect": true,
    "noteDebounceMs": 100,
    "scale": "pentatonic",
    "baseNote": 60,
    "instruments": ["flute", "violin"]
  },
  "detection": {
    "confidenceThreshold": 0.5,
    "maxDetections": 10
  },
  "tracker": {
    "kalmanProcessNoise": 0.05,
    "kalmanMeasurementNoise": 0.1,
    "iouThreshold": 0.3,
    "maxLostFrames": 5
  },
  "ui": {
    "showTrajectory": true,
    "trajectoryLength": 60,
    "showFps": true
  }
}
```

## Validation

- Plages numériques (min→max, 0–127 pour MIDI, 0.0–1.0 pour volumes/confiance)
- Enum validation : `backend` ∈ {"midi","soundpool"}, `scale` ∈ gammes supportées
- Si JSON invalide : logger erreur, retourner defaults, proposer reset

## Migration depuis SharedPreferences

- Lire anciennes préférences (`UDP_IP`, `UDP_PORT`, etc.)
- Si config JSON absent, migrer automatiquement :
  - `UDP settings` → section `network` (nouvelle section optionnelle, non dans spec mais utile)
  - Valeurs par défaut pour le reste
- Une fois migration faite, supprimer anciennes préfs (ou laisser coexistence)

## Interface UI Settings

- Ajouter dans `SettingsActivity` un écran "Configuration JSON"
- Options :
  - Afficher chemin fichier
  - Bouton "Exporter config" (share fichier)
  - Bouton "Importer config" (pick file)
  - Bouton "Reset defaults"
  - Éventuellement éditeur texte simple (EditText multiline) pour modifier directement (avancé)

## Dépendances

```gradle
implementation "com.squareup.moshi:moshi:1.15.0"
implementation "com.squareup.moshi:moshi-kotlin:1.15.0"
// ou bien org.json (pas d'autres dépendances si préfère manuel)
```

Préférence : **Moshi** pour parsing type-safe, mais `org.json` suffit si on veut éviter dépendance.

## Tests

### Unitaires
- `ConfigManagerTest` :
  - Lecture fichier valide → AppConfig correct
  - JSON invalide → fallback defaults
  - Écriture puis re-lecture → roundtrip
  - Migration préférences → config complète

### Instrumentés
- Lancer app sans config → fichier defaults créé automatiquement
- Modifier fichier (via adb push) → config rechargée après redémarrage (ou hot-reload si Flow implémenté)

## Statut par rapport au code existant

`SettingsActivity` utilise `SharedPreferences` (preferences.xml). À migrer progressivement :
- Lire depuis `ConfigManager` au lieu de préfs directement
- Conserve les préfs pour rétrocompatibilité (fallback)
- UI Settings : mélange préfs (UDP) + config JSON (mapping) — pour simplifier v1, tout centraliser dans JSON

## Futur

- Support de thèmes (dark/light) dans config
- Profils multiples (import/export)
- validation schema JSON Schema (optionnel)
