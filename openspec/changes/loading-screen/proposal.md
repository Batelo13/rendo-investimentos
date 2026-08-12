## Why

The login/cadastro page and dashboard were built without any loading feedback: the initial page render can flash unstyled/incomplete content, and the cadastro fetch / login POST give no visual feedback while in flight, so a slow network makes the app look frozen or double-clickable. The wallet-icon loading animation was already decided during the identity-visual phase but deferred until a page actually needed it — this is that page.

## What Changes

- New reusable Thymeleaf fragment (`fragments/loading.html`) rendering a full-screen overlay built around the existing wallet SVG icon, animated via CSS (no JS-driven animation). Takes a `startHidden` fragment parameter so callers control whether it's a passive splash or an action-only overlay.
- New `static/css/loading.css` with the overlay + wallet animation (restrained motion, consistent with the existing "no exaggeration" visual brief).
- New `static/js/loading.js` with the initial-load splash behavior: overlay hidden via a `window.load` listener with a CSS fade-out. Only wired up where the overlay starts visible (`index.html`, `dashboard.html`).
- `login.js` reuses the same `#loadingOverlay` for login-only feedback (overlay starts hidden on this page, `startHidden=true`): turned on in the login form's `submit` event handler and deliberately never turned off client-side — the browser navigation replaces the page. Cadastro submission is explicitly excluded: the page is a single sliding panel, and cadastro already has its own feedback (slide back to the login side + inline success/error message) — the loading overlay would compete with that motion, not support it.
- Fragment included in `index.html` and `dashboard.html` as a splash (`startHidden=false`), and in `login.html` as action-only feedback (`startHidden=true`, no splash on a plain page visit).

## Capabilities

### New Capabilities
- `loading-screen`: full-screen splash on initial page load and inline loading feedback during login/cadastro form submission, built on the existing wallet icon.

### Modified Capabilities
(none — no existing spec'd capability changes behavior, this only adds new UI feedback)

## Impact

- Affected templates: `index.html`, `login.html`, `dashboard.html` (fragment include only).
- Affected JS: `login.js` (wrap existing fetch/submit handlers with loading toggles).
- New files only otherwise (fragment, CSS, JS) — no backend/API changes, no new dependencies.
