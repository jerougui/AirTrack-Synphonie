## ADDED Requirements

### Requirement: Stop tracking selected object
The system SHALL provide a button to stop tracking the currently selected object, which clears the selection and hides the tracking data.

#### Scenario: Stop tracking button clears selection
- **WHEN** the user presses the stop tracking button while an object is selected
- **THEN** the system deselects the object, reverts its bounding box to the normal color, and hides the position and velocity display

#### Scenario: Stop tracking button has no effect when no object selected
- **WHEN** the user presses the stop tracking button and no object is selected
- **THEN** the system does nothing (no change in state)