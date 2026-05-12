# AirTrack-Synphonie - Détection d'objets avec suivi interactif

Cette application Android améliore l'exemple de détection d'objets de MediaPipe avec des capacités interactives pour sélectionner et suivre des objets en temps réel.

## Fonctionnalités

- **Sélection d'objet** : Appuyez sur n'importe quel objet détecté pour le sélectionner (surligné en vert)
- **Suivi en temps réel** : Affiche la position (x, y) et la vitesse (vx, vy) de l'objet sélectionné dans le coin supérieur gauche
- **Arrêt du suivi** : Bouton dans le coin supérieur droit pour désélectionner l'objet et masquer les données de suivi
- **Retour visuel** : Affichages avec coins arrondis et arrière-plans semi-transparents pour une meilleure lisibilité

## Fonctionnement

1. Lancez l'application et pointez la caméra vers des objets
2. Appuyez sur n'importe quel objet détecté pour le sélectionner (il deviendra vert)
3. Les données de position et de vitesse apparaîtront dans le coin supérieur gauche
4. Appuyez sur le bouton "Arrêter le suivi" dans le coin supérieur droit pour désélectionner
5. Appuyez en dehors de n'importe quel objet pour effacer la sélection

## Compilation et Exécution

### Prérequis
- Android Studio Arctic Fox ou version ultérieure
- SDK Android 21+
- Git

### Commandes de compilation
```bash
# Nettoyer et construire l'APK de débogage
./gradlew :app:clean :app:assembleDebug

# Installer sur l'appareil connecté
adb install ".\app\build\outputs\apk\debug\app-debug.apk"

# Lancer l'application
adb shell am start -n com.google.mediapipe.examples.objectdetection/.MainActivity
```

### Développement
- Ouvrez le projet dans Android Studio
- Compiler et exécuter sur un émulateur ou un appareil physique
- La fonctionnalité principale est implémentée dans `app/src/main/java/com/google/mediapipe/examples/objectdetection/OverlayView.kt`

## Détails d'implémentation

L'amélioration modifie la classe `OverlayView` pour :
- Gérer les événements tactiles pour la sélection d'objet
- Suivre l'état de l'objet sélectionné et calculer la vitesse
- Afficher les données de suivi et les contrôles UI
- Maintenir la compatibilité avec le pipeline de détection existant

## Fichiers Modifiés
- `app/src/main/java/com/google/mediapipe/examples/objectdetection/OverlayView.kt` - Implémentation principale du suivi
- `app/src/main/res/values/colors.xml` - Ajout de la couleur verte pour la sélection

## Spécifications (Français)

Voir `openspec/changes/archive/2026-05-12-object-detection-android-click-and-track-enhancements/specs_fr/` pour les spécifications en français :
- `object-selection/spec.md` - Sélection d'objet par toucher
- `object-tracking-display/spec.md` - Affichage des données de suivi
- `tracking-control/spec.md` - Contrôle d'arrêt du suivi

## Dépôt
Code original : https://github.com/google/mediapipe/tree/master/examples/object_detection/android
Version améliorée : https://github.com/jerougui/AirTrack-Synphonie.git