## Why

The user asked for a light/dark mode toggle on the dashboard. The design system already centralizes every color in CSS custom properties (`tokens.css`), which makes this a token-swap problem rather than a rewrite.

## What Changes

- A second set of token values under `:root[data-theme="light"]` in `tokens.css`, alongside the existing dark values.
- A `data-theme` attribute on `<html>`, toggled by a new sidebar button (dashboard only) and persisted in `localStorage`.
- An inline script in the `<head>` of every template (`index.html`, `login.html`, `dashboard.html`) that reads the stored preference and applies `data-theme` before first paint, so switching pages or reloading never flashes the wrong theme.
- **Fix, not just a new feature**: several rules set text color to a token that follows the theme (`--rendo-color-text` or `--rendo-color-bg`) while sitting on a background that does *not* follow the theme (a solid brand color, or the login page's fixed dark gradient panel). That only ever read correctly because every background used to be dark. Found by auditing every `color: var(--rendo-color-text)`/`var(--rendo-color-bg)` usage against its actual background, not guessed:
  - `dashboard.css`: `.btn-primary`, `.btn-buy`, `.btn-sell` (colored button backgrounds)
  - `login.css`: `.login-container button[type="submit"]` (primary-colored bg), `.login-container button.ghost` + its hover border (lives inside the fixed dark toggle panel), `.toggle`/`.toggle-panel p`/`.toggle-watermark` (the sliding panel's own gradient is fixed dark regardless of site theme, by design)
  All of these switch to a new fixed token, `--rendo-color-on-accent` (defined once, never redefined per theme), instead of a theme-following token.

## Capabilities

### New Capabilities
- `tema-claro-escuro`: a persisted, user-togglable light/dark theme applied consistently across all pages.

### Modified Capabilities
(none — no existing capability's requirements change; the button-text fix is a latent-bug correction inside this new capability's own scope, not a behavior change to anything previously specified)

## Impact

- `tokens.css`: new light-theme variable block + new `--rendo-color-on-accent` token + `color-scheme: light` for the light theme (so native form controls/scrollbars match).
- `dashboard.css`: `.btn-primary`/`.btn-buy`/`.btn-sell` color source changed; new toggle button styles.
- `login.css`: `.login-container button[type="submit"]`, `.login-container button.ghost` (+hover), `.toggle`, `.toggle-panel p`, `.toggle-watermark` color source changed to the fixed token.
- `dashboard.html`: new toggle button in the sidebar.
- `index.html`, `login.html`, `dashboard.html`: new inline flash-prevention script in `<head>`.
- New small shared script (e.g. `static/js/theme.js`) for the toggle logic, included on `dashboard.html`.
- No backend changes.
