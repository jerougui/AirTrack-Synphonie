## Why

AirTrack Synphonie addresses the gap between intuitive aerial object tracking and creative musical expression. Current solutions require expensive equipment and complex software to capture motion data and convert it to sound. This app enables anyone with an Android smartphone to transform the flight of a kite, drone, balloon, or bird into a real-time musical performance—completely offline and at no cost.

## What Changes

- Create a new Android application with CameraX integration for real-time video capture
- Integrate ML Kit Object Detection & Tracking to detect and follow aerial objects
- Implement a tracking module with Kalman Filter to compute normalized position, velocity, and direction
- Build a mapping engine that converts motion data into MusicEvents (notes, instruments, effects)
- Add audio synthesis via Android MIDI API and SoundPool for sample playback
- Design an overlay UI system to display bounding boxes, trajectories, and realtime indicators
- Define an external JSON configuration schema for instruments, scales, notes, and mapping rules
- Ensure 30–60 FPS performance on Android 8+ devices with no cloud dependencies
- Document installation, configuration, and usage; add unit tests for tracking and mapping

## Capabilities

### New Capabilities

- `camera-capture`: CameraX-based frame capture, preview management, and InputImage generation
- `object-detection`: ML Kit object detection and tracking with bounding box extraction and tracking ID management
- `motion-tracking`: Position normalization, velocity computation, direction calculation, and Kalman Filter prediction
- `music-mapping`: Configuration-driven conversion of motion data (speed, direction, acceleration) into musical events
- `audio-playback`: MIDI note synthesis and SoundPool sample playback with dynamic instrument selection
- `ui-overlay`: Real-time rendering of bounding boxes, trajectories, velocity indicators, and active notes
- `config-system`: JSON configuration file loading and validation for instruments, scales, notes, and mapping parameters

### Modified Capabilities

*None* — this is a new application with no existing specs to modify.

## Impact

- New Android project structure (Gradle modules for camera, detection, tracking, mapping, audio, ui)
- AndroidManifest.xml with CAMERA permission, ML Kit dependencies, and MIDI features
- Runtime permissions flow for camera access
- External configuration file placement and discovery logic
- Device performance and camera capabilities detection
- Documentation and sample configuration provided to users
