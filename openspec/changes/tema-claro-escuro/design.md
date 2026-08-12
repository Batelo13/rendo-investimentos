## Context

See proposal.md - Why. Every color in the app already flows through CSS custom properties in `tokens.css` (`--rendo-color-*`, plus the Bootstrap `--bs-*` overrides which are themselves defined as `var(--rendo-color-*)` references, not literal values — so they automatically follow whichever theme block is in scope, no duplication needed there). `color-scheme: dark` is currently hardcoded at `:root`, which forces native controls (scrollbars, `<select>` popups) to dark styling always — needs to flip too.

## Goals / Non-Goals

**Goals:**
- Reuse the existing token system; no parallel styling mechanism.
- Fix the latent "text color coupled to a token that happens to always have been dark" bug properly (a new fixed token), not patch around it per-instance with hardcoded hex.

**Non-Goals:**
- Per-component theme overrides beyond the token layer — if a component needs a different look per theme beyond swapping tokens, that's future scope.
- Auto-following the OS theme (`prefers-color-scheme`) — explicitly decided against; dark is always the default until the user manually switches.
- Polishing every low-contrast micro-detail uncovered while auditing (e.g. `dashboard.css`'s `.nav-item:hover` uses a hardcoded `rgba(237,235,245,0.05)` tint that will be barely visible on a light background) — noted below as a deferred follow-up, not a legibility-breaking bug like the button-text issue.

## Decisions

**Mechanism: `data-theme` attribute + CSS variable block, not a class or separate stylesheet.** `:root[data-theme="light"] { --rendo-color-bg: ...; ... }` redefines the same variable names used everywhere else, so no selector in any existing CSS file needs to change to become theme-aware — they already read `var(--rendo-color-*)`.

**Light palette values** (primary/accent/warning/danger unchanged across themes — only neutrals flip):
```
--rendo-color-bg: #F3F2F8;        /* was #1B1A24 */
--rendo-color-surface: #FFFFFF;   /* was #24232F */
--rendo-color-surface-alt: #EDEBF5; /* was #242530 — reuses the old dark-theme text color */
--rendo-color-text: #1B1A24;      /* was #EDEBF5 — reuses the old dark-theme bg color */
--rendo-color-text-muted: #6B6980; /* was #B7B5C4, darkened for AA contrast on a light bg */
--rendo-color-border: #DEDCE8;    /* was #34333f */
color-scheme: light;              /* was implicitly dark via the :root default */
```
`--rendo-color-primary`/`--rendo-color-accent`/`--rendo-color-warning`/`--rendo-color-danger` are not redefined — they were already chosen to work against both a dark panel and as button fills, and stay the brand's fixed identity colors in both themes.

**New fixed token `--rendo-color-on-accent: #EDEBF5;`** — defined once at `:root`, never redefined inside `[data-theme="light"]`. Used wherever text/graphics sit on a background that is itself a fixed brand color rather than a theme-following neutral:
- `dashboard.css`: `.btn-primary`, `.btn-buy`, `.btn-sell`
- `login.css`: `.login-container button[type="submit"]`, `.login-container button.ghost` (+ its hover border, currently a hardcoded `rgba(237,235,245,0.5)` — also switched to reference the fixed token's color via a resolved rgba, see tasks), `.toggle`, `.toggle-panel p`, `.toggle-watermark`

**Flash prevention: inline synchronous script in `<head>`, before the stylesheet link that needs it to matter.** External CSS/JS is fetched/executed after HTML parsing reaches it; an inline `<script>` placed before first paint runs synchronously and can set the `data-theme` attribute on `<html>` before the browser has anything to paint yet. Same tiny script duplicated in `index.html`, `login.html`, `dashboard.html` heads (not a shared external file — an external file would itself suffer the same load-order problem it's meant to solve):
```html
<script>
(function () {
  var t = localStorage.getItem('rendo-theme');
  if (t === 'light') document.documentElement.setAttribute('data-theme', 'light');
})();
</script>
```
Absence of the attribute (or any value other than `'light'`) falls through to the default dark `:root` block — matches "dark is default" from the spec without needing an explicit `'dark'` stored value.

**Toggle button lives only in the dashboard sidebar** (per user's own scoping) but changes a global, page-independent value (`localStorage` + the `<html>` attribute), so the effect is felt on every page via the shared inline script — not a dashboard-only theme.

## Risks / Trade-offs

- **Contrast of the new light-muted-text value** (`#6B6980`) against `#F3F2F8` background → should meet AA for normal text at typical sizes used here (13-14px body text); if the user finds it too light/dark on review, it's a one-line token tweak, not a structural change.
- **Deferred**: `.nav-item:hover`'s hardcoded low-alpha rgba tint (barely visible on light bg) — cosmetic only, not a legibility break like the button-text issue; revisit if it reads as flat in practice.
