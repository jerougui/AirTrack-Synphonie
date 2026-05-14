## 1. Object Selection Implementation

- [ ] 1.1 Modify the overlay view to handle touch events (MotionEvent)
- [ ] 1.2 Implement hit-testing to detect taps within bounding boxes
- [ ] 1.3 Track the selected object's ID and update its bounding box color to green
- [ ] 1.4 Ensure tapping outside any bounding box clears selection (or leaves unchanged)

## 2. Tracking Data Display

- [ ] 2.1 Store the last known position (x, y, z) of the selected object for velocity calculation
- [ ] 2.2 Compute velocity (vx, vy, vz) as (current_position - last_position) / time_delta
- [ ] 2.3 Render position and velocity text in a corner of the screen (e.g., top-left)
- [ ] 2.4 Update the displayed tracking data with each new detection frame
- [ ] 2.5 Hide tracking information when no object is selected

## 3. Tracking Control UI

- [ ] 3.1 Add a stop tracking button to the overlay (e.g., top-right corner)
- [ ] 3.2 Implement button press handler to clear selected object ID
- [ ] 3.3 Revert the previously selected object's bounding box to normal color
- [ ] 3.4 Hide tracking data when stop button is pressed
- [ ] 3.5 Ensure button press has no effect when no object is selected

## 4. Integration and Refinement

- [ ] 4.1 Verify that tracking data updates in real-time with detection frames
- [ ] 4.2 Test edge cases: rapid taps, object leaving frame, multiple quick selections
- [ ] 4.3 Optimize hit-testing performance if necessary (e.g., early exit)
- [ ] 4.4 Confirm that existing detection rendering remains unaffected
- [ ] 4.5 Test on device to ensure touch responsiveness and display clarity