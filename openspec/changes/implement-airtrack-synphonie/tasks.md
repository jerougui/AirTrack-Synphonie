# Tâches d'Implémentation : AirTrack Synphonie

**Changement** : implement-airtrack-synphonie  
**Schéma** : spec-driven  
**Spécifications** : camera-capture, object-detection, motion-tracking, music-mapping, audio-playback, ui-overlay, config-system

---

## Phase 1 — Refactor Architecture & Modèles (Jour 1)

- [x] **Tâche 1.1** : Créer le package `com.example.camerobjecttracking.model` (ou module `core` si modularisation) avec les data classes :
  - `TrackingData` (normalizedX, normalizedY, velocityX, velocityY, speed, direction, timestamp)
  - `MusicEvent` (note, velocity, instrument, durationMs, effect)
  - `AppConfig` (audio, mapping, detection, tracker, ui)
  - `TrackedObject` (id, boundingBox, label, confidence)

- [x] **Tâche 1.2** : Créer l'interface `ConfigManager` et implémentation `ConfigManagerImpl` :
  - Charger JSON depuis `filesDir/airtrack_config.json`
  - Si fichier absent, copier `assets/default_config.json`
  - Validation des champs (plages, enum)
  - Exposer `Flow<AppConfig>` pour observer changements
  - Migration depuis `SharedPreferences` (UDP settings) si première exécution

- [x] **Tâche 1.3** : Refactoriser `ObjectDetector` :
  - Créer interface `ObjectDetectorInterface` avec méthode `detect()`
  - Renommer classe actuelle en `TFLiteObjectDetector` implémentant l'interface
  - Préparer pour futur `MLKitObjectDetector` (interface unique)

- [x] **Tâche 1.4** : Extraire `CameraCapture` depuis `MainActivity` :
  - Créer `CameraCaptureImpl` avec logique actuelle de `startCamera()`
  - Exposer callback `onFrame: (ImageProxy) -> Unit`
  - `MainActivity` devient simple consommateur

- [x] **Tâche 1.5** : Déplacer l'extension `ImageProxy.toBitmap()` dans un fichier utilitaire (`ImageUtils.kt`)

## Phase 2 — Module Tracking avec Kalman Filter (Jour 2)

- [x] **Tâche 2.1** : Implémenter `KalmanFilter` (classe générique 1D ou spécialisée 2D) :
  - State : `[position, velocity]`
  - Matrices F, H, Q, R paramétrables via constructeur
  - Méthodes `predict(dt)` et `correct(measurement)`
  - Tests unitaires (convergence, réponse staircase, sinus)

- [x] **Tâche 2.2** : Créer `TrackingEngine` :
  - `start()`, `stop()`, `update(detections): TrackingData?`
  - Workflow : matching IoU → update Kalman → compute velocity/direction
  - Gérer perte d'objet (retour `null` si perdu > `maxLostFrames`)
  - Exposer `TrackingData` à chaque frame (même si prédiction pure)

- [x] **Tâche 2.3** : Intégrer dans `MainActivity` :
  - Remplacer logique actuelle de matching par appel à `TrackingEngine.update()`
  - Recevoir `TrackingData` complet (position, velocity, direction)
  - Mettre à jour `TrackingOverlayView` avec nouvelles données
  - Arrêter d'envoyer coordonnées brutes à `UdpSender` (pour l'instant, garder UDP en parallèle si besoin de debug)

- [ ] **Tâche 2.4** : Écrire tests unitaires `KalmanFilterTest` et `TrackingEngineTest` (dossier `app/src/test/java/...`)

## Phase 3 — Module Music Mapping (Jour 3)

- [x] **Tâche 3.1** : Implémenter `ScaleHelper` :
  - `getScaleNotes(scaleName: String): List<Int>` (demi-tons depuis tonique)
  - `midiNoteFromBase(base: Int, degree: Int): Int`
  - Gammes : pentatonic major/minor, major, natural minor, blues

- [x] **Tâche 3.2** : Créer `MusicMapperImpl` :
  - Lire `MappingConfig` depuis `ConfigManager`
  - Convertir `TrackingData` → `MusicEvent` :
    * speed → pitch (via gamme)
    * direction → instrument (sector-based si activé)
    * acceleration → effect (si activé)
  - Debounce : stocker `lastNoteTime`, `lastNote`, ignorer doublons < `noteDebounceMs`
  - Retourner `null` si speed < minSpeed (silence)

- [x] **Tâche 3.3** : Intégrer dans `MainActivity` :
  - Flux : `TrackingEngine` → `MusicMapper.map()` → `AudioPlayer.play()`
  - Afficher note courante dans `TrackingOverlayView` (`setMusicInfo()`)

- [ ] **Tâche 3.4** : Tests unitaires `MusicMapperTest` (speed→pitch, direction→instrument, debounce)

## Phase 4 — Module Audio Playback (Jour 4)

- [x] **Tâche 4.1** : Définir interface `AudioPlayer` avec méthodes `play(event: MusicEvent)`, `stop()`, `setVolume()`

- [x] **Tâche 4.2** : Implémenter `MidiAudioPlayer` :
  - Obtenir `MidiManager` → `openMidiDevice()` (device par défaut)
  - Créer `MidiOutputPort`
  - Envoyer `NOTE_ON` (channel 0, note, velocity)
  - Programmer `NOTE_OFF` après `durationMs` (Handler/postDelayed)
  - Gérer `PROGRAM_CHANGE` si instrument change
  - Libérer ressources dans `close()`

- [x] **Tâche 4.3** : Implémenter `SoundPoolAudioPlayer` (optionnel, pour samples) :
  - `SoundPool.Builder().setMaxStreams(8).build()`
  - Charger samples depuis `assets/samples/` ou `filesDir/samples/`
  - Mapper instrument → soundId
  - `play(soundId, rate=1.0)`

- [x] **Tâche 4.4** : Intégration dans `MainActivity` :
  - Initialiser `AudioPlayer` (selon backend config) dans `onCreate()`
  - Recevoir `MusicEvent` de `MusicMapper` → `audioPlayer.play()`
  - Gérer `onDestroy()` : `audioPlayer.close()`

- [ ] **Tâche 4.5** : Tests manuels :
  - MIDI : brancher phone → haut-parleurs, vérifier notes jouées
  - Vérifier pas de latence audible (>100ms)
  - Changement d'instruments fonctionne

## Phase 5 — Système de Configuration JSON (Jour 5)

- [x] **Tâche 5.1** : Créer `assets/default_config.json` avec schema complet (voir spec)

- [x] **Tâche 5.2** : Implémenter `ConfigManager` complet :
  - `load()` : lire fichier ou copier default, parsing JSON, validation
  - `save(config)` : écriture fichier
  - `reset()` : restore defaults
  - `config: LiveData<AppConfig>` pour observer changements

- [x] **Tâche 5.3** : Migration SharedPreferences :
  - Dans `ConfigManager.load()`, détecter anciennes prefs (`udp_ip`, etc.)
  - Si présentes, créer `AppConfig` avec valeurs migrées + defaults pour manquants
  - Écrire nouveau JSON, supprimer anciennes prefs (optionnel)

- [ ] **Tâche 5.4** : Mettre à jour `SettingsActivity` :
  - Lire `ConfigManager` au lieu de `PreferenceManager` directement
  - Ajouter écran "Configuration JSON" (boutons : Exporter, Importer, Reset)
  - Conserver écran UDP existant (lié à config.network si on ajoute section network)

- [ ] **Tâche 5.5** : Tester :
  - Première launch → default_config.json créé
  - Modifier fichier via adb → appli le prend en compte (nécessite peut-être reload)
  - Valeurs invalides → fallback defaults + log warning

## Phase 6 — Amélioration UI Overlay (Jour 6)

- [ ] **Tâche 6.1** : Étendre `TrackingOverlayView` :
  - Ajouter `trajectoryPoints: ArrayDeque<PointF>(maxLength=60)`
  - Méthode `addPoint(x: Float, y: Float)` (appelée par MainActivity chaque frame)
  - Dessiner polyligne (Path) dans `onDraw()`
  - Afficher note actuelle (texte gros, couleur par plage)

- [ ] **Tâche 6.2** : Ajouter indicateur velocity :
  - Arc de cercle ou barre horizontale
  - Échelle : 0–maxSpeed configuré

- [ ] **Tâche 6.3** : Optimisations drawing :
  - Réutiliser objets Paint, Path, RectF
  - Éviter allocations dans `onDraw`
  - Vérifier VSync (pas de jank)

- [ ] **Tâche 6.4** : Tests visuels sur device (60 FPS stable)

## Phase 7 — Modularisation Gradle (Jour 7)

- [ ] **Tâche 7.1** : Créer structure de modules dans `settings.gradle` :
  ```
  include ':app', ':core', ':camera', ':detection', ':tracking', ':mapping', ':audio', ':config'
  ```
- [ ] **Tâche 7.2** : Créer `build.gradle` pour chaque module :
  - `:core` : pas de dépendances Android (Kotlin only)
  - `:camera` : dépend de `:core`, dépendances CameraX
  - `:detection` : dépend de `:core`, TFLite
  - `:tracking` : dépend de `:detection`, `:core`
  - `:mapping` : dépend de `:tracking`, `:core`
  - `:audio` : dépend de `:core`
  - `:config` : dépend de `:core` (Moshi optionnel)
  - `:app` : dépend de tous les feature modules

- [ ] **Tâche 7.3** : Déplacer les fichiers Kotlin dans les dossiers modules correspondants :
  - `CameraCapture.kt` → `camera/src/main/kotlin/...`
  - `ObjectDetector*.kt` → `detection/`
  - `KalmanFilter.kt`, `TrackingEngine.kt` → `tracking/`
  - `ScaleHelper.kt`, `MusicMapper.kt` → `mapping/`
  - `AudioPlayer*.kt` → `audio/`
  - `ConfigManager.kt` → `config/`
  - `TrackingOverlayView.kt` → `ui/` (ou reste dans `:app` car View Android)

- [ ] **Tâche 7.4** : Mettre à jour imports dans tout le projet
- [ ] **Tâche 7.5** : Vérifier compilation (./gradlew assembleDebug) et lancement

## Phase 8 — Finitions & Documentation (Jour 8)

- [ ] **Tâche 8.1** : Mettre à jour `AndroidManifest.xml` :
  - `android.hardware.camera` required (déjà fait)
  - Optionnel : `android.hardware.camera.autofocus` (non requis)
  - Supprimer `INTERNET` permission si non utilisé (UDP suffit, pas besoin internet) — sauf si ML Kit cloud *(note: ML Kit on-device ne nécessite pas INTERNET)*

- [ ] **Tâche 8.2** : Créer `assets/default_config.json` avec valeurs par défaut documentées

- [ ] **Tâche 8.3** : Créer `assets/samples/` (optionnel SoundPool samples) — au moins un sample de démo (flute note)

- [ ] **Tâche 8.4** : Rédiger documentation :
  - `README.md` (français) : installation, usage, configuration JSON, exemples mapping
  - `ARCHITECTURE.md` : diagramme modules, flux données
  - `CONTRIBUTING.md` : style code (Kotlin), comment ajouter une gamme, tester

- [ ] **Tâche 8.5** : Ajouter tests unitaires manquants :
  - `KalmanFilterTest` (couverture 80%+)
  - `TrackingEngineTest`
  - `MusicMapperTest`
  - `ConfigManagerTest`

- [ ] **Tâche 8.6** : Vérification performance :
  - Mesurer FPS moyen (Device référence : Pixel 6a ou équivalent)
  - Mesurer latence audio (timestamp événement → son)
  - Si FPS < 30 : ajuster résolution caméra ou intervalle détection

- [ ] **Tâche 8.7** : Préparer release :
  - `build.gradle` : versionName "1.0", versionCode 1
  - Générer signed APK/AAB
  - Note : pas de Google Play requis pour ce projet (distribution directe)

---

## Critères d'Acceptation (rappel)

- Détection et suivi stable (30–60 FPS)
- Latence audio < 100ms
- Config JSON modifie instruments/notes sans recompiler
- Tests unitaires passent (tracking, mapping, config)
- Code modulaire, chaque module compilable séparément
- Documentation complète en français

---

## Notes d'Implémentation

- **Priorité** : suivi fonctionnel avant audio (même sans son, vérifier TrackingData correct)
- **Debug** : garder `UdpSender` actif durant développement pour visualiser coordonnées sur PC (ex: Python receiver)
- **Sécurité** : ne jamais logger secrets ; pas de network sauf UDP local
- **Compatibilité** : tester sur Android 8 (API 26) minimum
