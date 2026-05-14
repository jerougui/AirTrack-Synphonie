## ADDED Requirements

### Requirement: Select object anywhere inside bounding box
The system SHALL allow the user to select a detected object by tapping on any point inside its bounding box rectangle.

#### Scenario: Tap anywhere inside bounding box selects object
- **WHEN** the user taps on any point that is inside the bounding box rectangle of a detected object
- **THEN** the system marks that object as selected and changes its bounding box color to green

#### Scenario: Tap on bounding box edge selects object
- **WHEN** the user taps on a point that is exactly on the edge of the bounding box rectangle of a detected object
- **THEN** the system marks that object as selected and changes its bounding box color to green

#### Scenario: Tap outside bounding box does not select
- **WHEN** the user taps on a point that is outside the bounding box rectangle of all detected objects
- **THEN** the system does not change the selection and no bounding box turns green