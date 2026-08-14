# login-mais-rico Specification

## Purpose

Makes the login/cadastro page feel less bare and gives users faster feedback while filling the cadastro form, without changing what's actually required to log in or sign up.

## Requirements

### Requirement: Toggle panels show feature highlights
Each side of the sliding toggle panel SHALL display two or three short icon+text highlights in addition to its existing heading, paragraph, and button.

#### Scenario: Viewing either panel
- **WHEN** the user views the login-side or cadastro-side toggle panel
- **THEN** they see 2-3 short highlight lines, each with a small icon, consistent with the page's existing icon style

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
