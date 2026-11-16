## ADDED Requirements

### Requirement: Light and dark themes have equivalent glass depth
Both themes SHALL provide equivalent visual hierarchy, surface depth, focus
visibility and interaction feedback across dashboard, authentication, landing
and loading, while using values appropriate to each palette.

#### Scenario: Compare the same component between themes
- **WHEN** the user views the same card, form control, modal or navigation element in light and dark themes
- **THEN** both variants provide equivalent depth, contrast and interaction feedback without requiring identical opacity or shadow values

#### Scenario: Stored theme applies before authentication page paint
- **WHEN** a user with a stored theme opens the authentication page
- **THEN** the correct liquid-glass theme variant is applied before the first visible paint without a flash of the other theme

