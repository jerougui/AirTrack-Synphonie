## ADDED Requirements

### Requirement: Display tracking data for selected object
The system SHALL show the estimated position (x, y, z) and velocity (vx, vy, vz) of the selected object on screen.

#### Scenario: Tracking data visible when object is selected
- **WHEN** an object is selected via tap
- **THEN** the system displays the object's current position and velocity in real-time

#### Scenario: Tracking data updates each frame
- **WHEN** a new detection frame is available and an object is selected
- **THEN** the displayed position and velocity are updated to reflect the latest estimates

#### Scenario: No tracking data when no object selected
- **WHEN** no object has been selected
- **THEN** the system does not display any position or velocity information