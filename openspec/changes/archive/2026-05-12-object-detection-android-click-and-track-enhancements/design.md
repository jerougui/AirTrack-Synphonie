## Context

The current object detection Android application processes video frames, runs object detection models, and draws bounding boxes with labels on the screen. The user interface is passive: it shows what the model detects but does not allow user interaction with the detections. The proposal introduces three new capabilities: object selection, tracking display, and tracking control. To implement these, we need to modify the UI overlay to handle touch events, track the selected object, and render additional information.

## Goals / Non-Goals

**Goals:**
- Enable users to select a detected object by tapping its bounding box.
- Visually indicate the selected object by changing its bounding box color to green.
- Display the selected object's 3D position (x, y, z) and velocity (vx, vy, vz) in real-time.
- Provide a button to stop tracking, which clears the selection and hides the tracking data.
- Ensure the tracking data updates with each new frame of detection results.
- Maintain compatibility with the existing object detection pipeline and rendering.

**Non-Goals:**
- Changing the underlying object detection model or its output format.
- Adding support for multiple simultaneous object selections.
- Persisting tracking data across application sessions.
- Implementing advanced tracking algorithms (e.g., Kalman filters) beyond simple frame-to-frame velocity estimation.
- Supporting other interaction methods (e.g., voice commands).

## Decisions

### Touch Event Handling
**Decision:** Use `MotionEvent` processing in the custom view that draws the overlay to detect taps within bounding boxes.
**Rationale:** The overlay view already has access to the detection results and canvas for drawing. Handling touch events there keeps the logic co-located with the rendering, avoiding the need to propagate events through multiple layers.
**Alternatives Considered:** 
- Handling touch events in the Activity/Fragment and then communicating to the overlay view. This would require additional interfaces and state management, increasing complexity without clear benefit.

### Selection State
**Decision:** Maintain the currently selected object's ID and its last known position (for velocity calculation) as member variables in the overlay view.
**Rationale:** The overlay view is the natural place to hold UI-related state. Storing the ID allows us to highlight the correct object each frame, and storing the last position enables velocity computation.
**Alternatives Considered:**
- Using a ViewModel or other architecture component. This would be overkill for simple UI state and would require crossing the boundary between UI and business logic layers unnecessarily.

### Velocity Calculation
**Decision:** Estimate velocity as the difference in position between the current and previous frame, divided by the time delta.
**Rationale:** This provides a simple, real-time approximation of velocity that is sufficient for display purposes. The object detection model may already provide velocity vectors, but if not, this method works with the available position data.
**Alternatives Considered:**
- Using a more sophisticated filter (e.g., Kalman) to smooth velocity estimates. This would increase complexity and is not required for the goal of displaying basic movement information.
- Relying solely on the model's output if it provides velocity. We cannot assume the model does, so we compute our own.

### UI Layout for Tracking Data and Button
**Decision:** Render the tracking data (position and velocity) as text in a corner of the screen, and place the stop button in the opposite corner.
**Rationale:** This keeps the information visible without obstructing the main view. Using corners is a common pattern for HUD elements in augmented reality applications.
**Alternatives Considered:**
- Overlaying the data near the selected object. This could clutter the view if the object is near other detections or screen edges.
- Using a dialog or separate screen. This would take the user out of the live camera view, which is contrary to the goal of real-time tracking.

## Risks / Trade-offs

**[Risk]** Touch event handling may interfere with other gestures if the application later adds pinch-to-zoom or pan.
**Mitigation:** Implement touch handling to only consume events when a tap occurs within a bounding box, allowing other gestures to be processed by the underlying view or parent.

**[Risk]** Velocity estimation based on successive frames may be noisy if the detection position jumps due to model variability.
**Mitigation:** Apply a simple low-pass filter or only update velocity if the position change is above a threshold. For now, we note that the display may show jitter and accept it as a trade-off for simplicity.

**[Risk]** The selected object may leave the frame or be occluded, causing tracking to be lost.
**Mitigation:** If the object is not detected in a frame, we retain the selection but do not update the position. We can clear the selection after a timeout if desired, but for simplicity we leave it to the user to stop tracking manually.

**[Risk]** Adding touch handling and extra drawing may impact UI performance on lower-end devices.
**Mitigation:** The operations are minimal (a hit test per detection and a bit more text drawing). We will monitor performance and optimize if necessary.

## Open Questions

- Does the object detection model provide z-depth (position) and velocity, or must we compute everything from 2D bounding boxes over time? If only 2D is available, we may need to adjust what we display (e.g., only x, y and speed magnitude).
- What coordinate system should be used for the displayed position and velocity (e.g., screen coordinates, world coordinates from ARCore, or normalized)? We will need to check what the current detection output provides.