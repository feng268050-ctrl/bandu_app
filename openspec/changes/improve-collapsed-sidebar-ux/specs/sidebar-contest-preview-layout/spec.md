## ADDED Requirements

### Requirement: Sidebar contest preview uses a reduced image footprint
The system SHALL render the activity preview image in the sidebar contest section at 65% of its current displayed size while preserving the image aspect ratio.

#### Scenario: Sidebar contest preview image is reduced
- **WHEN** the sidebar contest section is rendered in the desktop sidebar layout
- **THEN** the activity preview image SHALL appear at 65% of the previous sidebar image footprint

#### Scenario: Reduced preview remains layout-safe
- **WHEN** the reduced contest preview image is rendered in the sidebar contest section
- **THEN** the surrounding contest content SHALL remain visible without clipping, overflow, or sidebar width expansion

#### Scenario: Locale changes do not break the compact preview layout
- **WHEN** the sidebar contest section is viewed in either Chinese or English
- **THEN** the reduced preview image and its adjacent content SHALL remain aligned within the sidebar card
