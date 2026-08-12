## Why

The user finds the login/cadastro page too plain and wants more visual interest, plus real-time field feedback. This is a deliberate, explicit loosening of the original visual brief's "no exaggeration" constraint for this specific page — the user asked for more, not less, restraint here.

## What Changes

- Two to three icon+text highlights added to each side of the sliding toggle panel (currently just a heading + one paragraph + a button).
- The static two-blob radial-gradient background becomes a slow, subtle drifting animation (still restrained — same colors/opacity as today, just no longer frozen).
- Real-time inline validation feedback (border color) on cadastro's nome/email/senha and login's email, based on client-side checks — visual feedback only, does not block submission or replace server-side validation.

## Capabilities

### New Capabilities
- `login-mais-rico`: richer visual presentation and real-time field feedback on the login/cadastro page.

### Modified Capabilities
(none — purely additive presentation layer; the actual login/cadastro submission logic and server-side validation are unchanged)

## Impact

- `login.html`: new highlight list markup in both `.toggle-panel`s.
- `login.css`: highlight list styles, background drift animation, `.valido`/`.invalido` input-group border styles.
- `login.js`: new `input` event listeners for live validation; existing submit handlers untouched.
- No backend changes, no new dependency.
