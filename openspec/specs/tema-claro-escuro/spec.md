# tema-claro-escuro Specification

## Purpose

Lets the user choose between a light and dark appearance for the whole app, defaulting to the existing dark identity, with the choice remembered across visits.

## Requirements

### Requirement: Dark is the default theme
On a first visit with no stored preference, the system SHALL render in the existing dark theme.

#### Scenario: First visit
- **WHEN** a user with no stored theme preference opens any page
- **THEN** the page renders using the current dark palette

### Requirement: Theme is user-togglable and persists
The system SHALL let the user switch between light and dark from the dashboard, and SHALL remember that choice on later visits and other pages.

#### Scenario: User switches theme
- **WHEN** the user clicks the theme toggle in the dashboard
- **THEN** the page's colors switch immediately to the other theme, and the choice is saved

#### Scenario: Returning visit
- **WHEN** a user who previously chose light (or dark) opens the app again, on any page (login, dashboard, etc.)
- **THEN** the app renders in their previously chosen theme, not the default

### Requirement: No flash of the wrong theme
Pages SHALL apply the stored theme before first paint, not after.

#### Scenario: Reload with light theme stored
- **WHEN** a page loads for a user who has light theme stored
- **THEN** the page never visibly renders in dark theme first before switching to light

### Requirement: Content on fixed-color backgrounds stays legible in both themes
Any text or graphic sitting on a background that does not change with the theme (a solid brand-colored button, or the login page's sliding panel gradient) SHALL use a color that also does not change with the theme, so it stays legible regardless of which theme is active.

#### Scenario: Light theme active — dashboard buttons
- **WHEN** the light theme is active
- **THEN** "Cadastrar"/"Comprar"/"Vender" button text remains clearly legible against its colored background, not washed out

#### Scenario: Light theme active — login sliding panel
- **WHEN** the light theme is active
- **THEN** the login page's colored sliding panel (heading, paragraph, "Entrar"/"Criar conta" button, wallet watermark) remains as legible as it is in dark theme, since that panel's background stays dark by design in both themes
