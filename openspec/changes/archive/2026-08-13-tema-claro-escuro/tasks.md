## 1. Tokens

- [x] 1.1 In `tokens.css`, add `--rendo-color-on-accent: #EDEBF5;` at `:root` (fixed, never redefined)
- [x] 1.2 In `tokens.css`, add a `:root[data-theme="light"] { ... }` block with the light values from design.md (bg, surface, surface-alt, text, text-muted, border, `color-scheme: light`)

## 2. Fix theme-coupled text-on-fixed-background bug

- [x] 2.1 In `dashboard.css`, switch `.btn-primary`, `.btn-buy`, `.btn-sell` text color to `var(--rendo-color-on-accent)`
- [x] 2.2 In `login.css`, switch `.login-container button[type="submit"]` text color to `var(--rendo-color-on-accent)`
- [x] 2.3 In `login.css`, switch `.login-container button.ghost` text color (and its hardcoded `rgba(237,235,245,0.5)` border / hover border-color) to `var(--rendo-color-on-accent)` (with alpha via `color-mix` or an equivalent fixed rgba — no theme dependency)
- [x] 2.4 In `login.css`, switch `.toggle`, `.toggle-panel p`, `.toggle-watermark` color/fill/stroke to `var(--rendo-color-on-accent)`

## 3. Toggle mechanism

- [x] 3.1 Create `static/js/theme.js`: reads/writes `localStorage['rendo-theme']`, applies/removes `data-theme="light"` on `<html>`, exposes an init for the toggle button's click handler and current-state icon
- [x] 3.2 Add the same small inline flash-prevention script (design.md) to the `<head>` of `index.html`, `login.html`, `dashboard.html`, before their stylesheet `<link>`s
- [x] 3.3 Add a theme toggle button in `dashboard.html`'s sidebar (`.sidebar-foot`, above "Sair"), sun/moon inline SVG icons swapped by `theme.js` based on current state
- [x] 3.4 Include `theme.js` on `dashboard.html` and wire the button's click handler

## 4. Verification

- [x] 4.1 Run the existing test suite — no backend changes, expect no impact
- [x] 4.2 Manually verify in a real browser: default is dark; toggling switches immediately; reloading `/dashboard` keeps the choice; navigating to `/login` (a different page) also reflects the stored choice with no flash; "Comprar"/"Vender"/primary buttons and the login toggle panel stay legible in light theme
