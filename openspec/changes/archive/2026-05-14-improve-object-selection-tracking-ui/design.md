## Context

The current object detection Android application processes video frames, runs object detection models, and draws bounding boxes with labels on the screen. The current implementation allows object selection but has limitations in the hit detection area (only works near center of bounding box), combines position and velocity data in a single display block, and uses basic styling for UI controls.

The proposal introduces three improved capabilities: improved object selection (anywhere inside bounding box), separated tracking display (position and velocity shown separately), and enhanced stop button styling. To implement these improvements, we need to modify the touch handling logic to accept touches anywhere within the bounding box rectangle, separate the rendering of position and velocity data into distinct UI elements, and improve the visual design of the stop button.

## Goals / Non-Goals

**Goals:**
- Improve object selection reliability by accepting touches anywhere inside the bounding box rectangle
- Separate position and velocity display into clearly distinct, labeled sections for better readability
- Enhance stop button styling with improved colors, rounded corners, and visual feedback for pressed state
- Maintain backward compatibility with existing detection pipeline and rendering
- Keep performance impact minimal

**Non-Goals:**
- Changing the underlying object detection model or its output format
- Adding support for multiple simultaneous object selections
- Persisting tracking data across application sessions
- Implementing advanced touch gestures (multi-touch, long press, etc.)
- Changing the coordinate system or units used for position/velocity display

## Decisions

### Hit Detection Improvement
**Decision:** Modify the touch event handling to check if the touch point lies within the entire bounding box rectangle (using simple boundary comparison) rather than relying on potentially inconsistent hit detection.
**Rationale:** The current implementation may have inconsistent hit detection due to transformation matrices or other factors. By using a direct rectangle containment check, we ensure reliable selection anywhere within the visual bounding box.
**Alternatives Considered:**
- Using the existing hit detection but improving the transformation calculations - this would be more complex and might not solve the core issue
- Adding a margin of error around the bounding box - this could lead to accidental selections of nearby objects

### Separated Display
**Decision:** Render position and velocity data as separate text elements with distinct labels and potentially different styling, positioned vertically stacked in the top-left corner.
**Rationale:** Separating the data makes it easier for users to read individual values at a glance. Vertical stacking maintains a clean layout while providing clear visual separation.
**Alternatives Considered:**
- Side-by-side horizontal layout - this would require more horizontal space and might not fit well on narrower screens
- Using icons or symbols to differentiate the values - this adds complexity without significant readability improvement
- Combining the data but with better spacing - this still requires users to parse which value is which

### Enhanced Button Styling
**Decision:** Improve the stop button with:
- Rounded corners (12px radius)
- Color change on pressed state (using mp_variant color)
- Better proportions (180x50 dp)
- Improved text centering and sizing
**Rationale:** These improvements follow Material Design principles for button styling, providing better visual feedback and touch target size.
**Alternatives Considered:**
- Using elevation changes for pressed state - this requires API level 21+ and might not be consistent with existing UI
- Using ripple effects - while good, the current implementation doesn't use Material Components library
- Keeping the basic styling but improving only the colors - this misses opportunity for better touch feedback

## Risks / Trade-offs

**[Risk]** Improved hit detection might accidentally select objects when tapping near edges due to finger touch imprecision.
**Mitigation:** The hit detection remains strictly within the bounding box bounds, so selection only occurs when the finger is actually over the visible object area. Users can still tap outside to clear selection.

**[Risk]** Separated display might take slightly more vertical space, potentially overlapping with other UI elements on small screens.
**Mitigation:** The display remains in the top-left corner with reasonable padding, and the application primarily uses landscape mode where vertical space is less constrained.

**[Risk]** Enhanced button styling might not match the exact visual language of the existing application.
**Mitigation:** The styling uses existing color resources (mp_primary, mp_variant) and follows common Android button design patterns to maintain consistency.

**[Risk]** More precise hit detection logic might have slight performance impact.
**Mitigation:** The additional computation is minimal (4 boundary checks per detection per touch event) and negligible compared to the object detection processing itself.

## Open Questions

- Should the hit detection use the exact transformed bounding box coordinates or apply a small tolerance for touch accuracy?
- What is the optimal spacing between the separated position and velocity display elements for readability on various screen densities?
- Should the enhanced button include any iconography (like a square symbol) to clearly indicate its stop function?