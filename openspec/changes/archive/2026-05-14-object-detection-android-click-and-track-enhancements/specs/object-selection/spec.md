## ADDED Requirements

### Requirement: Select object on tap
The system SHALL allow the user to select a detected object by tapping on its bounding box.

#### Scenario: Tap inside bounding box selects object
- **WHEN** the user taps on a point within the bounding box of a detected object
- **THEN** the system marks that object as selected and changes its bounding box color to green

#### Scenario: Tap outside bounding box does not select
- **WHEN** the user taps on a point that is not within any detected object's bounding box
- **THEN** the system does not change the selection and no bounding box turns green