# Spec : audio-playback

## Introduction

Génère le son à partir des `MusicEvent`. Deux backends supportés :
1. **MIDI API** : synthèse General MIDI via `android.media.midi` (faible latence, pas de samples)
2. **SoundPool** : lecture de samples audio (WAV/OGG) pour instruments personnalisés

## Architecture

```kotlin
interface AudioPlayer : Closeable {
    fun play(event: MusicEvent)
    fun stop()
    fun setVolume(volume: Float)  // 0.0–1.0
}
```

**Deux implémentations** :

```kotlin
class MidiAudioPlayer(context: Context) : AudioPlayer
class SoundPoolAudioPlayer(context: Context) : AudioPlayer
```

## Composants

### MidiAudioPlayer

Utilise `android.media.midi` (API 23+) :

1. **Initialisation** :
   - `MidiManager` → `openMidiDevice()` sur device par défaut (synthé GM intégré)
   - `MidiOutputPort` pour envoyer messages

2. **Messages MIDI** :
   - `NOTE_ON` : canal 0, note, velocity (quand `MusicEvent` arrive)
   - `NOTE_OFF` : après `durationMs` (timer) ou immédiat si nouvelle note diffère
   - `PROGRAM_CHANGE` : si instrument change entre events

3. **Canaux** :
   - Canal 0 : instrument principal
   - Canal 1–15 : reserves pour effets futurs

4. **Latence** :
   - Envoi direct sur thread audio (ou handler dédié)
   - Pas de buffer excessif

**Gestion des ressources** :
- Fermer `MidiOutputPort` et `MidiDevice` dans `close()`
- Gérer `onDeviceStatusChanged` (device déconnecté) → fallback vers SoundPool si disponible

### SoundPoolAudioPlayer

Pour samples personnalisés :

1. **Chargement** :
   - Lire fichiers WAV/OGG depuis `assets/samples/` ou `filesDir/samples/`
   - `SoundPool.Builder().setMaxStreams(8).build()`
   - `load(fd, priority)` → retourne `soundId`
   - Mapper `soundId` → instrument

2. **Lecture** :
   - `play(soundId, volume, volume, priority, loop, rate)`
   - `rate` peut être ajusté selon pitch désiré (1.0 = normal)

3. **Pitch shifting** :
   - SoundPool supporte `playbackRate` (0.5–2.0)
   - Mapping note → sample + rate approximation

4. **Limitations** :
   - Plusieurs samples nécessaires pour couvrir gamme (ou pitch shift)
   - Latence légèrement supérieure à MIDI (acceptable)

## Interface avec les autres modules

- **Entrée** : `MusicEvent` (depuis `music-mapping`)
- **Appel** : `audioPlayer.play(event)` sur le thread principal ou dédié
- **Cycle de vie** : démarré par `MainActivity.onCreate()`, arrêté dans `onDestroy()`

## Configuration JSON (section audio)

```json
{
  "audio": {
    "backend": "midi",           // "midi" ou "soundpool"
    "defaultInstrument": 0,      // General MIDI program number (0=Acoustic Grand Piano)
    "volume": 0.8,
    "soundpoolPath": "samples/", // relatif à filesDir
    "midiDeviceId": -1           // -1 = auto-select
  }
}
```

## Performance

- **Latence cible** : < 50 ms de l'événement au son
- **Mesure** : timestamp avant envoi → après appel `play()` (approximatif)
- **Thread** : appels sur thread audio séparé (HandlerThread) pour éviter blocage UI

## Tests

### Unitaires (limités)
- Mock `MidiManager` → vérifier `send()` appelé avec bons messages
- Mock `SoundPool` → vérifier `play()` appelé

### Instrumentés (obligatoire)
- **MIDI** : brancher phone → synthé externe ou écouter haut-parleurs, chaque mouvement déclenche note
- **SoundPool** : sample se joue sans craquements

## Dépendances

```gradle
// Aucune dépendance supplémentaire pour MIDI (déjà dans Android SDK)
// SoundPool fait partie de Android SDK
```

## Statut par rapport au code existant

Aucun code audio n'existe actuellement dans l'application. Module entièrement **nouveau**.

## Notes

- General MIDI : 128 instruments (0–127). Mapping : 0=Acoustic Grand Piano, 40=violin, 73=flute, etc.
- Pour un instrument "flute" configuré, utiliser program 73 (vérifier correspondance)
- Futur : supporte `MidiSynth` USB externe (optionnel)
