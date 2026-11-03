## ADDED Requirements

### Requirement: Loading exit coordinates with content reveal
The loading experience SHALL coordinate its exit with a short transition from
light blur to sharp page content, without changing when each existing loading
state is shown.

#### Scenario: Initial splash exits on index or dashboard
- **WHEN** the initial loading overlay on `index.html` or `dashboard.html` is dismissed after the page finishes loading
- **THEN** the overlay fades out while the page content becomes sharp, visible and interactive

#### Scenario: Login submission starts loading feedback
- **WHEN** the login form is submitted
- **THEN** the existing loading feedback appears immediately and does not delay or replace the browser's native form navigation

#### Scenario: Cadastro remains without loading overlay
- **WHEN** the cadastro form is submitted
- **THEN** no loading overlay is introduced and the existing panel transition plus inline message remain the feedback

