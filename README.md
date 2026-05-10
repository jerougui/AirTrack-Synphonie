# Kite Tracker Simple

A simple Android application that tracks a kite (or any selected object) via camera and sends position data over UDP for real-time music generation.

## Features

- Real-time object detection using TensorFlow Lite (MobileNet SSD)
- Touch-based object selection
- IoU-based object tracking
- UDP coordinate transmission (normalized x, y coordinates + timestamp)
- Configurable target IP and port via Settings

## Requirements

- Android Studio (latest recommended)
- Android SDK 26+ (minSdk 26, targetSdk 34)
- Device with camera (physical device recommended)

## Build Instructions

```bash
# Clone and open in Android Studio
./gradlew assembleDebug    # Build debug APK
./gradlew assembleRelease  # Build release APK
```

## Usage

1. Launch the app - grant camera permission when prompted
2. Point camera at objects - detections appear automatically
3. Touch an object to select it for tracking
4. Open Settings (gear icon) to configure UDP destination:
   - IP address (default: 192.168.1.100)
   - Port (default: 5005)
   - Send frequency (default: 30 Hz)
5. Tracking sends JSON packets: `{"x":0.5,"y":0.3,"t":1234567890}`

## Python Receiver Example

```python
import socket, json

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("0.0.0.0", 5005))

while True:
    data, addr = sock.recvfrom(1024)
    coords = json.loads(data)
    print(f"x={coords['x']:.2f}, y={coords['y']:.2f} @ {coords['t']}")
    # Use coords for music synthesis...
```

## Architecture

- `MainActivity.kt` - CameraX setup, UI orchestration
- `ObjectDetector.kt` - TensorFlow Lite inference
- `UdpSender.kt` - UDP packet transmission
- `TrackingOverlayView.kt` - Visual tracking indicator
- `SettingsActivity.kt` - Configuration screen

## License

MIT