# login-mais-rico Specification

## Purpose

Makes the login/cadastro page feel less bare and gives users faster feedback while filling the cadastro form, without changing what's actually required to log in or sign up.
## Requirements
### Requirement: Toggle panels show feature highlights
Each side of the sliding toggle panel SHALL display four short icon+text highlights in addition to its existing heading, paragraph, and button.

#### Scenario: Viewing either panel
- **WHEN** the user views the login-side or cadastro-side toggle panel
- **THEN** they see 4 short highlight lines, each with a small circular check badge, consistent with the page's icon style

### Requirement: Background has subtle motion
The page background SHALL animate slowly and continuously rather than remain static, using the same colors/opacity already established, at a pace subtle enough not to distract from the form.

#### Scenario: Page is open
- **WHEN** the login page is open for more than a few seconds
- **THEN** the background's glow visibly, slowly shifts position, looping indefinitely

### Requirement: Real-time field feedback
Cadastro's nome/email/senha fields and login's email field SHALL show visual feedback (valid/invalid) as the user types, based on client-side checks, without blocking form submission.

#### Scenario: Typing an invalid email
- **WHEN** the user types a value in an email field that doesn't match a basic email shape
- **THEN** that field shows an invalid visual state

#### Scenario: Typing a valid value
- **WHEN** the user's input in a field meets its client-side check (valid email shape; senha length >= 8; nome non-empty)
- **THEN** that field shows a valid visual state

#### Scenario: Untouched field
- **WHEN** a field has not been interacted with yet
- **THEN** it shows neither valid nor invalid state (no premature error on page load)

#### Scenario: Submitting with client-side-invalid input
- **WHEN** a field shows an invalid client-side state but the user submits anyway
- **THEN** the submission still proceeds to the server exactly as before this change (client-side feedback is advisory, not a gate)

### Requirement: State-toggle trigger available at the top of the dark panel
Each side of the sliding dark panel SHALL show a compact header ("Já tem uma conta? Entrar" / "Ainda não tem uma conta? Criar conta") whose button triggers the same slide transition as before, without a duplicate trigger below the form.

#### Scenario: Switching from cadastro to login via the header
- **WHEN** the user is on the cadastro state and clicks "Entrar" in the dark panel's header
- **THEN** the panel slides to the login state, identically to the previous trigger location

#### Scenario: No duplicate link below the cadastro form
- **WHEN** the user views the cadastro form
- **THEN** there is no second "Entrar" link/button below the form itself

### Requirement: Password fields support show/hide toggle
Both the cadastro and login password fields SHALL have a show/hide toggle button that switches the field between masked and plain text, without altering existing client-side validation.

#### Scenario: Revealing a typed password
- **WHEN** the user types a password and clicks the show/hide toggle
- **THEN** the field's content becomes visible as plain text, and clicking again re-masks it

#### Scenario: Toggling visibility does not affect validation
- **WHEN** the user toggles password visibility
- **THEN** the field's valid/invalid state (from real-time validation) is unaffected

### Requirement: Decorative financial widgets use only static data
The cadastro-state dark panel MAY show illustrative financial widgets (e.g. total balance, a sample stock quote, a security note), but any values shown SHALL be static/illustrative and SHALL NOT originate from a new API, service, or database call.

#### Scenario: Viewing the decorative widgets
- **WHEN** the user views the cadastro-state dark panel
- **THEN** any financial figures shown are fixed illustrative values, not fetched from the backend

### Requirement: No social login controls without backend support
The page SHALL NOT display Google/Apple/Microsoft (or any other) social login buttons unless the backend has real OAuth2 support configured for them.

#### Scenario: Backend has no OAuth2 configuration
- **WHEN** the backend has no social login provider configured
- **THEN** the login/cadastro page shows no social login section at all

### Requirement: Responsive layout below 700px
Below 700px viewport width, the dark panel SHALL collapse into a compact top area (not a 45%-width side panel) showing only its heading, short paragraph and state-toggle header, and the state switch SHALL happen as an immediate display swap rather than a horizontal slide.

#### Scenario: Narrow viewport, switching state
- **WHEN** the viewport is narrower than 700px and the user taps the state-toggle button
- **THEN** the dark panel and the form swap to the other state without a horizontal slide animation, and the decorative widgets and highlight checklist are not shown

#### Scenario: Narrow viewport, initial render
- **WHEN** the viewport is narrower than 700px
- **THEN** the dark panel appears above the form as a compact band, and the form remains the primary, unobstructed focus of the page

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

