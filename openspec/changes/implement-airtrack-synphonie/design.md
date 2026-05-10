## Context

AirTrack Synphonie is a new Android application that transforms aerial object motion into real-time music using the smartphone camera. The app must process ~30–60 FPS video streams, detect and track objects, compute motion metrics (position, velocity, direction), and convert those metrics into musical events—entirely offline. Key constraints: Android 8+, zero cost (only free technologies), modular and testable codebase.

## Goals / Non-Goals

**Goals:**
- Achieve stable object detection and tracking under varied lighting and motion speeds
- Maintain 30–60 FPS end-to-end latency from capture to audio feedback
- Expose a simple JSON configuration system for instruments, scales, and mapping rules
- Support MIDI synthesis and sample-based playback with low latency
- Provide clear visual feedback via overlay (bounding box, trajectory, current note)
- Structure code into modular, testable layers (camera, detection, tracking, mapping, audio, UI)

**Non-Goals:**
- Cloud-based processing or backend services
- Multi-user collaboration or social features
- Recording/exporting performances (may be considered later)
- Support for Android versions below API 26 (Android 8.0)
- Custom ML model training (use ML Kit pretrained object detection)

## Decisions

### Architecture: Clean MVVM with module separation

**Decision:** Adopt a modular Gradle structure with feature modules (`camera`, `detection`, `tracking`, `mapping`, `audio`, `ui`) and a shared `core` module for data models. Use a unidirectional data flow (Camera → Detection → Tracking → Mapping → Audio) with coroutines and SharedFlow for event propagation.

**Alternatives:** Monolithic single-module app (rejected due to testability and separation concerns); MVC (rejected in favor of modern Jetpack architecture).

### Camera: CameraX with ImageAnalysis

**Decision:** Use CameraX `ImageAnalysis` use case to pull frames at desired FPS, convert to ML Kit `InputImage`, and forward to detection. Disable preview stabilization initially; add optional stabilization later if needed.

**Alternatives:** Camera2 API (more control but significantly more complex); SurfaceView-based manual capture (not compatible with ML Kit easily).

### Object Detection: ML Kit Object Detection & Tracking (STREAM_MODE)

**Decision:** Use ML Kit's `ObjectDetection` with `ObjectDetectorOptions` in `STREAM_MODE` to get continuous tracking IDs. Configure for single-object focus (largest bounding box) to simplify mapping.

**Alternatives:** Custom TensorFlow Lite model (rejected—more work, not needed for generic object tracking); OpenCV optical flow alone (insufficient for object re-acquisition).

### Tracking: Kalman Filter for smooth motion estimation

**Decision:** Implement a 1D/2D Kalman Filter (constant velocity model) to smooth position and predict velocity/direction. Use bounding box center as measurement. Position normalized to [0,1] relative to image dimensions.

**Alternatives:** Raw delta without filtering (noisy at high FPS); Complementary filter (simpler but less accurate); Particle filter (overkill for single object).

### Mapping: Configuration-driven state machine

**Decision:** Mapping reads config JSON (scale, note range, speed-to-pitch mapping, direction-to-instrument toggles). On each tracking update, compute a `MusicEvent` (note, velocity, instrument, effect). Debounce rapid note changes (min 100ms between distinct notes).

**Alternatives:** Hardcoded mapping (inflexible); Real-time audio analysis (out of scope).

### Audio: MIDI API + SoundPool

**Decision:** Use Android MIDI API (`MidiManager`, `MidiOutputPort`) for synthesized instruments (General MIDI). Use `SoundPool` for custom instrument samples if user provides them. Latency goal: <50ms from MusicEvent to sound.

**Alternatives:** `MediaPlayer` (too high latency); ExoPlayer (not suited for low-latency note triggering); Third-party audio engine (avoid external dependencies).

### UI: Jetpack Compose with Canvas overlay

**Decision:** MainActivity hosts Compose UI; use `AndroidView` to embed `PreviewView` from CameraX. Overlay Compose `Canvas` on top for real-time drawing of bounding box, trajectory polyline, and HUD text (speed, active note).

**Alternatives:** XML layouts (legacy approach); Custom View for entire overlay (possible, but Compose preferred for consistency).

### Configuration: JSON schema with validation

**Decision:** Single config file `airtrack_config.json` in app's files directory or external storage (if permitted). Supported fields: instrument (GM number or sample name), scale (name or semitone array), min/maxSpeed thresholds, mapping flags. Validate at startup; fall back to defaults if invalid.

**Alternatives:** ProtoBuf or XML (overkill); Remote config (violates offline constraint).

## Risks / Trade-offs

- [Risk] ML Kit object detection may struggle with very fast objects (motion blur). **Mitigation:** Increase camera FPS; tune detector sensitivity; fallback to frame differencing as secondary detector.
- [Risk] MIDI API latency varies by device manufacturer. **Mitigation:** Benchmark on reference devices; allow user to adjust "trigger debounce" in config.
- [Risk] Kalman Filter tuning (process/measurement noise) may need per-device adjustment. **Mitigation:** Expose noise parameters in advanced config section; provide sensible defaults.
- [Risk] Camera permissions denied by user. **Mitigation:** Clear permission rationale UI; graceful degradation with educational message.
- [Risk] 30–60 FPS target may not be met on older devices. **Mitigation:** Adaptive quality: reduce detector resolution dynamically; offer "performance mode" in settings.

## Migration Plan

This is a greenfield application with no existing user base. Deployment steps:
1. Build a signed APK/AAB with minimum SDK 26.
2. Distribute via Google Play (free) and direct APK download.
3. On first launch, copy default config to app's files directory; guide user to edit if desired.
4. Collect anonymous performance metrics (FPS, detection success rate) via Firebase Analytics (free, opt-out) to guide optimizations.

**Rollback:** N/A for new release; monitor crash reports and performance dashboards.

## Open Questions

- Should we support multi-object tracking and let user select target? (Postpone to v2)
- Should we offer more audio backends beyond MIDI/SoundPool (e.g., Oboe for ultra-low latency)? (Consider for future)
- File format for custom instrument samples (WAV/OGG? mono/stereo?) – decide during audio module implementation.
- Exact JSON schema validation approach (Moshi + Kotlin data classes vs manual parsing) – align with existing project conventions if any.
