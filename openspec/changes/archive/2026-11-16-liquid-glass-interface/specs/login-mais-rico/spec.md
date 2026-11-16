## ADDED Requirements

### Requirement: Auth screen follows the active theme while preserving brand identity
The login/cadastro page SHALL provide equivalent liquid-glass variants for the
active light and dark themes while preserving the mint accent, slate-purple
brand panel, logo, sliding flow and form behavior.

#### Scenario: Authentication page with dark theme active
- **WHEN** a user whose stored preference is dark opens the authentication page
- **THEN** the page renders a dark liquid-glass composition with the existing brand colors and readable form controls

#### Scenario: Authentication page with light theme active
- **WHEN** a user whose stored preference is light opens the authentication page
- **THEN** the page renders an equally detailed light liquid-glass composition while the brand panel and all controls remain legible

#### Scenario: Switching between login and cadastro
- **WHEN** the user activates the existing state-toggle control
- **THEN** the same login/cadastro forms and validation behavior are preserved while the panel transition uses the refined motion language

## REMOVED Requirements

### Requirement: Auth screen uses a fixed color identity
**Reason**: A single fixed palette conflicts with the approved requirement for equally elaborate light and dark liquid-glass experiences across all screens.

**Migration**: The authentication screen now derives neutral glass surfaces from the persisted theme while keeping its fixed mint and slate-purple brand accents.

