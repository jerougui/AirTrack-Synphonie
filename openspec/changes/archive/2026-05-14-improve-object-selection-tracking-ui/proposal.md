## Why

The current object selection implementation in the Android object detection example only allows selecting objects by tapping near their center or has inconsistent hit detection. Users need a more reliable way to select objects anywhere within their bounding box. Additionally, the position and velocity display are currently combined in a single block, making it harder to read individual values, and the stop tracking button has basic styling that could be improved for better user experience.

## What Changes

- Improve object selection to work reliably when tapping anywhere inside the bounding box (entire rectangle area)
- Separate the position and velocity display into distinct, clearly labeled sections for better readability
- Enhance the stop tracking button with improved styling including better colors, rounded corners, and pressed state feedback
- Maintain all existing functionality while improving the user interface and interaction experience

## Capabilities

### New Capabilities
- `improved-object-selection`: Enhanced object selection that works anywhere inside the bounding box
- `separated-tracking-display`: Separate display of position and velocity data for improved readability
- `enhanced-stop-button`: Improved styling for the stop tracking button with better visual feedback

### Modified Capabilities
- `object-selection`: Updated requirement to specify selection works anywhere inside the bounding box
- `object-tracking-display`: Updated requirement to specify separate position and velocity display
- `tracking-control`: Updated requirement to specify enhanced button styling

## Impact

- **UI Layer**: Modifications to the overlay view's touch handling logic and drawing routines
- **Interaction Model**: More forgiving object selection that improves user experience
- **Visual Presentation**: Clearer separation of tracking data and more polished button appearance
- **Dependencies**: No new external libraries required; uses existing Android Canvas and MotionEvent APIs
- **Systems**: Affects the main activity/fragment where detection results are rendered; no changes to core object detection model