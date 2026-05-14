## 1. Improved Object Selection Implementation

- [ ] 1.1 Modify touch event handling to check if touch point is within bounding box rectangle
- [ ] 1.2 Update hit-testing logic to accept touches anywhere inside the bounding box (not just near center)
- [ ] 1.3 Ensure selection works when tapping on edges or corners of bounding box
- [ ] 1.4 Maintain ability to clear selection by tapping outside any bounding box

## 2. Separated Tracking Display

- [ ] 2.1 Modify tracking data rendering to show position (x, y) and velocity (vx, vy) in separate sections
- [ ] 2.2 Add clear labels for position data (e.g., "Position:") and velocity data (e.g., "Velocity:")
- [ ] 2.3 Position and velocity sections should be visually distinct but related
- [ ] 2.4 Ensure both sections update independently when their respective values change
- [ ] 2.5 Maintain proper spacing and alignment for readability on different screen densities

## 3. Enhanced Stop Button Styling

- [ ] 3.1 Apply rounded corners (12px radius) to the stop tracking button
- [ ] 3.2 Implement color change feedback when button is pressed (use mp_variant color)
- [ ] 3.3 Ensure button has clear "Stop Tracking" label with proper text centering
- [ ] 3.4 Set appropriate button dimensions for easy touch targeting (minimum 48dp touch target)
- [ ] 3.5 Maintain button positioning in top-right corner with proper margins

## 4. Integration and Testing

- [ ] 4.1 Verify that improved object selection works reliably across different object sizes and positions
- [ ] 4.2 Test that position and velocity display remains clear and readable during rapid updates
- [ ] 4.3 Confirm that enhanced button styling provides good visual feedback on press
- [ ] 4.4 Ensure all existing functionality remains intact (object detection rendering, etc.)
- [ ] 4.5 Test on device to confirm touch responsiveness and display clarity