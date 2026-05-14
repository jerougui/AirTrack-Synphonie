## ADDED Requirements

### Requirement: Separate display of position and velocity data
The system SHALL display the position (x, y) and velocity (vx, vy) of the selected object in separate, clearly labeled sections.

#### Scenario: Position and velocity displayed separately
- **WHEN** an object is selected via tap
- **THEN** the system displays the position data (x, y) and velocity data (vx, vy) in distinct sections with clear labels

#### Scenario: Position data clearly labeled
- **WHEN** viewing the tracking display for a selected object
- **THEN** the position data is prefixed with "Position:" or similar clear label

#### Scenario: Velocity data clearly labeled
- **WHEN** viewing the tracking display for a selected object
- **THEN** the velocity data is prefixed with "Velocity:" or similar clear label

#### Scenario: Data updates independently
- **WHEN** the selected object's position or velocity changes
- **THEN** only the changed data section is updated (or both sections update but remain visually separate)