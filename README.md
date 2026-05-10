# AirTrack Synphonie

Application Android gratuite qui transforme le mouvement d'un objet aérien (cerf-volant, drone, ballon, oiseau) en musique en temps réel via la caméra du smartphone.

## Fonctionnalités

- **Détection d'objets** : MobileNet SSD v2 (TensorFlow Lite) pour détecter automatiquement les objets
- **Suivi en temps réel** : Filtre de Kalman pour un suivi fluide et prédictif
- **Génération musicale** : Conversion mouvement → notes MIDI (General MIDI)
- **Configuration JSON** : Personnalisez instruments, gammes, seuils de vitesse sans recompiler
- **Overlay visuel** : Affichage de la boîte englobante, trajectoire, vitesse et note en cours
- **100% hors-ligne** : Aucun service cloud, aucun coût d'utilisation
- **UDP optionnel** : Envoi des coordonnées sur réseau local pour traitement externe

## Prérequis

- **Android Studio** (version 2023.1.1 ou ultérieure)
- **SDK Android** : API 26 (Android 8.0) minimum, cible API 34
- **Appareil Android** avec caméra (physical device recommandé pour tests)
- **Java 17** (configuré automatiquement par Android Studio)

## Structure du projet

```
AirTrack Synphonie/
├── app/
│   ├── build.gradle                  // Configuration Gradle de l'app
│   ├── src/main/
│   │   ├── AndroidManifest.xml       // Permissions et composants
│   │   ├── assets/
│   │   │   ├── default_config.json   // Configuration par défaut
│   │   │   └── mobilenet_ssd_v2.tflite  // Modèle TFLite
│   │   ├── java/com/example/camerobjecttracking/
│   │   │   ├── MainActivity.kt       // Activity principale
│   │   │   ├── SettingsActivity.kt   // Écran de paramètres
│   │   │   ├── ObjectDetector.kt     // Détecteur TFLite
│   │   │   ├── TrackingOverlayView.kt // Overlay graphique
│   │   │   ├── UdpSender.kt          // Envoi UDP
│   │   │   ├── camera/               // Module CameraX
│   │   │   ├── detection/            // Module détection
│   │   │   ├── tracking/             // Module tracking (Kalman)
│   │   │   ├── mapping/              // Module musique
│   │   │   ├── audio/                // Lecture MIDI/SoundPool
│   │   │   ├── config/               // Gestion config JSON
│   │   │   ├── model/                // Data classes
│   │   │   └── utils/                // Utilitaires
│   │   └── res/                      // Ressources UI
├── build.gradle                      // Configuration racine
├── settings.gradle                   // Modules (si modularisation future)
├── gradlew / gradlew.bat             // Wrapper Gradle
└── README.md                         // Ce fichier
```

## Compilation

### Avec Android Studio (recommandé)

1. Ouvrez le projet : `File → Open…` → sélectionnez le dossier `AirTrack Synphonie`
2. Laissez Android Studio télécharger les dépendances (Gradle sync automatique)
3. Build : `Build → Make Project` (ou `Ctrl+F9`)
4. L'APK de debug est généré dans `app/build/outputs/apk/debug/app-debug.apk`

### En ligne de commande

```bash
# Sur Windows (PowerShell)
./gradlew assembleDebug

# Sur macOS/Linux
./gradlew assembleDebug

# APK de sortie : app/build/outputs/apk/debug/app-debug.apk
```

### Signature (release)

Pour une version release signée :
1. Générez un keystore (une fois) :
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
   ```
2. Configurez `app/build.gradle` avec les signingConfigs
3. Build release :
   ```bash
   ./gradlew assembleRelease
   ```

## Installation

### Sur un appareil physique

1. Activez le **mode développeur** et **USB Debugging** sur votre appareil
2. Connectez l'appareil via USB
3. Installez l'APK via ADB :
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
4. Lancez l'app depuis le launcher : **AirTrack Synphonie**

### Via Android Studio

- Cliquez sur le bouton **Run** (triangle vert) après compilation
- Sélectionnez votre appareil dans la liste des devices connectés

## Test et utilisation

### 1. Première utilisation

- Accordez la permission **CAMERA** lorsque demandé
- L'application s'ouvre sur la preview caméra

### 2. Sélection d'un objet

- Pointez la caméra vers des objets (personne, animal, vélo, etc.)
- Les détections apparaissent automatiquement (boîtes vertes en overlay)
- **Touchez** un objet dans l'écran pour le sélectionner et démarrer le suivi

### 3. Génération musicale

- Une note MIDI est jouée à chaque changement de vitesse significatif
- La hauteur de la note dépend de la vitesse de l'objet (gamme configurable)
- Le volume (velocity) varie avec la vitesse
- Par défaut : gamme pentatonique, instrument "flute" (GM 73)

### 4. Configuration

- Cliquez sur l'icône **Settings** (engrenage)
- **UDP** : configurez IP/Port pour envoyer les coordonnées sur le réseau (pour debug externe)
- **Fichier JSON** : la configuration musicale est dans `airtrack_config.json` (créé automatiquement dans `filesDir` de l'app)

#### Éditer la configuration manuellement

Connectez-vous en ADB et récupérez/modifiez le fichier :

```bash
# Récupérer le fichier depuis l'appareil
adb pull /data/data/com.example.camerobjecttracking/files/airtrack_config.json .

# Éditer avec votre éditeur
# Puis re-upload
adb push airtrack_config.json /data/data/com.example.camerobjecttracking/files/
```

**Exemple de `airtrack_config.json`** :
```json
{
  "audio": {
    "backend": "MIDI",
    "defaultInstrument": 73,
    "volume": 0.8
  },
  "mapping": {
    "minSpeed": 0.0,
    "maxSpeed": 3.0,
    "speedToPitch": true,
    "directionToInstrument": false,
    "accelerationToEffect": false,
    "noteDebounceMs": 100,
    "scale": "pentatonic",
    "baseNote": 60,
    "instruments": ["flute"]
  },
  "tracker": {
    "kalmanProcessNoise": 0.05,
    "kalmanMeasurementNoise": 0.1,
    "iouThreshold": 0.3,
    "maxLostFrames": 5
  }
}
```

### 5. Test avec récepteur UDP (optionnel)

Si vous avez configuré IP/Port dans les settings, vous pouvez recevoir les coordonnées en Python :

```python
# udp_receiver.py
import socket, json, time

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("0.0.0.0", 5005))
print("Listening on UDP port 5005...")

while True:
    data, addr = sock.recvfrom(1024)
    try:
        coords = json.loads(data.decode())
        print(f"x={coords['x']:.3f}, y={coords['y']:.3f}, t={coords['t']}")
    except Exception as e:
        print(f"Error: {e}")
```

Lancez-le sur votre PC (même réseau WiFi) :
```bash
python udp_receiver.py
```

## Tests unitaires (à venir)

Le projet inclut des squelettes de tests pour KalmanFilter, TrackingEngine, MusicMapper et ConfigManager. Pour exécuter :

```bash
./gradlew testDebugUnitTest
```

Rapports : `app/build/reports/tests/testDebugUnitTest/index.html`

## Résolution de problèmes

### L'APK ne s'installe pas

- Vérifiez que `minSdkVersion=26` dans `app/build.gradle` (compatible Android 8+)
- Activez `Install unknown apps` si nécessaire (pour APK manuel)

### Pas de son MIDI

- Certains dispositifs Android n'ont pas de synthétiseur MIDI natif
- Vérifiez les logs (Logcat) pour messages `MidiAudioPlayer`
- Solution de repli : configurer `backend: "soundpool"` et ajouter des samples

### Détection instable

- Ajustez `iouThreshold` dans `airtrack_config.json` (0.3 par défaut)
- Augmentez `kalmanProcessNoise` pour plus de réactivité, ou `kalmanMeasurementNoise` pour plus de lissage
- Assurez-vous d'un bon éclairage et d'un objet contrasté

### Latence audio élevée

- Mesurez avec chronomètre : déclenchement → son
- Si > 100ms, essayez de réduire `noteDebounceMs` (min 50ms)
- Utilisez un device performant (Android 10+ recommandé)

## Architecture technique

### Flux de données

```
CameraX (ImageAnalysis) 
    → ImageProxy → Bitmap
    → TFLiteObjectDetector (détections)
    → TrackingEngine (Kalman + IoU matching)
    → MusicMapper (speed → note, gamme)
    → AudioPlayer (MIDI NOTE_ON/OFF)
    → Sortie audio
```

### Modules

| Module | Rôle | Technologies |
|--------|------|--------------|
| `camera` | Capture CameraX | androidx.camera |
| `detection` | Détection TFLite | TensorFlow Lite |
| `tracking` | Suivi lissé | Filtre de Kalman 1D |
| `mapping` | Conversion musique | Scalaires MIDI |
| `audio` | Lecture son | android.media.midi |
| `config` | Configuration | JSON + LiveData |

## Performances cibles

- **FPS** : 30–60 images par seconde (suivi fluide)
- **Latence audio** : < 100 ms (détection → son)
- **Précision vitesse** : erreur < 10% par rapport à la vitesse réelle
- **Stabilité** : suivi maintenu en mouvement rapide, même avec occlusion brève

## Roadmap

- [ ] Interface Jetpack Compose (remplacement XML)
- [ ] Trajectoire historique dans l'overlay (buffer circulaire)
- [ ] Samples audio personnalisés (SoundPool complet)
- [ ] Tests unitaires et instrumentation
- [ ] Modularisation Gradle complète (modules indépendants)
- [ ] Mode multi-objet (sélection multiple)
- [ ] Recording des performances (export MIDI)

## Licence

MIT – Utilisation libre, modification et redistribution autorisées.

## Contact & contribution

Ce projet est développé dans le cadre de l'initiative **AirTrack Synphonie**.  
Les spécifications techniques détaillées se trouvent dans `openspec/`.

Pour soumettre un problème ou une suggestion :  
https://github.com/jerougui/AirTrack-Synphonie/issues

---

**Note** : Ce projet est expérimental. Il fonctionne mieux sur appareil Android physique avec bonne caméra. L'émulateur peut ne pas supporter CameraX correctement.
