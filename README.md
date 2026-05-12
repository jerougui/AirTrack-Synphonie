# AirTrack-Synphonie - Object Detection with Interactive Tracking

This Android application enhances the MediaPipe Object Detection example with interactive capabilities for selecting and tracking objects in real-time.

## Features

- **Object Selection**: Tap on any detected object to select it (highlighted in green)
- **Real-time Tracking**: Displays the selected object's position (x, y) and velocity (vx, vy) in the top-left corner
- **Stop Tracking**: Button in the top-right corner to deselect the object and hide tracking data
- **Visual Feedback**: Rounded corner displays with semi-transparent backgrounds for better readability

## How It Works

1. Launch the application and point the camera at objects
2. Tap on any detected object to select it (it will turn green)
3. Position and velocity data will appear in the top-left corner
4. Tap the "Stop Tracking" button in the top-right corner to deselect
5. Tap outside any object to clear selection

## Building and Running

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 21+
- Git

### Build Commands
```bash
# Clean and build debug APK
./gradlew :app:clean :app:assembleDebug

# Install on connected device
adb install ".\app\build\outputs\apk\debug\app-debug.apk"

# Launch the application
adb shell am start -n com.google.mediapipe.examples.objectdetection/.MainActivity
```

### Development
- Open the project in Android Studio
- Build and run on emulator or physical device
- The main functionality is implemented in `app/src/main/java/com/google/mediapipe/examples/objectdetection/OverlayView.kt`

## Implementation Details

The enhancement modifies the `OverlayView` class to:
- Handle touch events for object selection
- Track selected object state and calculate velocity
- Render tracking data and UI controls
- Maintain compatibility with existing detection pipeline

## Files Modified
- `app/src/main/java/com/google/mediapipe/examples/objectdetection/OverlayView.kt` - Core tracking implementation
- `app/src/main/res/values/colors.xml` - Added green color for selection

## Specifications (French)

See `openspec/changes/archive/2026-05-12-object-detection-android-click-and-track-enhancements/specs_fr/` for French specifications:
- `object-selection/spec.md` - Sélection d'objet par toucher
- `object-tracking-display/spec.md` - Affichage des données de suivi
- `tracking-control/spec.md` - Contrôle d'arrêt du suivi

## Repository
Original code: https://github.com/google/mediapipe/tree/master/examples/object_detection/android
Enhanced version: https://github.com/jerougui/AirTrack-Synphonie.git