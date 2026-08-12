## Purpose

Gives the user visual feedback during two moments that previously had none: the initial render of a server-rendered page, and the in-flight period of a login or cadastro submission.

## ADDED Requirements

### Requirement: Initial page load splash
The system SHALL display a full-screen loading overlay, built around the wallet icon, as the first visible content of `index.html` and `dashboard.html`, and SHALL remove it once the page has finished loading. `login.html` SHALL NOT show this splash on a plain page load — it is a static form with nothing to load, and the overlay there is reserved for actual login/cadastro submission feedback (see below), so the user only sees loading when they've taken an action, not on every visit to the login screen.

#### Scenario: Page finishes loading
- **WHEN** `index.html` or `dashboard.html` finishes loading in the browser
- **THEN** the overlay fades out and the page content becomes fully visible and interactive

#### Scenario: Slow network
- **WHEN** `index.html` or `dashboard.html` resources are still loading
- **THEN** the overlay remains visible instead of showing a partially-rendered or unstyled page

#### Scenario: Plain visit to the login page
- **WHEN** the user opens `login.html` without submitting a form (fresh visit, or a redirect back with an error)
- **THEN** no loading overlay is shown

### Requirement: Cadastro has no loading overlay
The system SHALL NOT show the loading overlay for a cadastro submission. The login/cadastro page is a single sliding panel, and cadastro success is already communicated by the panel sliding back to the login side plus the existing inline success/error message — adding the loading overlay on top would compete with that motion instead of supporting it.

#### Scenario: Cadastro submitted
- **WHEN** the user submits the cadastro form
- **THEN** no loading overlay appears; the existing slide-back-to-login and inline message behavior is the only feedback

### Requirement: Login submission feedback
The system SHALL show a loading state when the login form is submitted, to indicate the request is in progress until the browser navigates away.

#### Scenario: Login form submitted
- **WHEN** the user submits the login form
- **THEN** a loading state is shown immediately, and the browser's native form navigation proceeds unmodified
