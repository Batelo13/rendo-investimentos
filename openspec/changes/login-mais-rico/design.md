## Context

See proposal.md - Why. `.login-page`'s current background is two static `radial-gradient(circle at X% Y%, ...)` layers. `radial-gradient`'s `at X% Y%` position is baked into the gradient function itself, not `background-position`, so it can't be animated with a plain `background-position` transition/keyframe.

## Goals / Non-Goals

**Goals:**
- Keep every addition reversible/tunable from the user's reaction (this is a "show and iterate" page per established practice).
- Stay within `login.html`/`login.css`/`login.js` — no shared component changes, no risk to the dashboard.

**Non-Goals:**
- A generic "app-wide" animated background — scoped to the login page only, per the ask.
- Real (server-checked) validation on the client — the existing server-side checks remain the source of truth; this is purely a perceived-speed nicety.

## Decisions

**Background motion via absolutely-positioned blurred blobs, not animated gradients.** Since gradient position can't be keyframed directly, use two `.login-page::before`/`::after` pseudo-elements: large circles with the same mint/primary colors at low opacity, `filter: blur(60-80px)`, `position: absolute`, animated via `@keyframes` on `transform: translate(...)` only (compositor-friendly, no layout thrash). `.login-page` gets `position: relative; overflow: hidden` to contain them. 24-30s duration, `ease-in-out infinite alternate`, small travel distance (a few percent of viewport) — matches "subtle," not a moving-background effect that competes with the form.

**Highlights are static content, not dynamic/data-driven** — three short lines per side, hand-written copy, not pulled from any config. Simplest option for content that never needs to change.

**Validation: per-field checks, not a form-level validity object.** Each field's `input` listener runs its own tiny check function and toggles `.valido`/`.invalido` on its `.input-group` wrapper (already the existing DOM structure — no markup restructuring needed, just add/remove classes). No dependency, no schema library — the checks are one-liners (regex test, length compare, non-empty).

## Risks / Trade-offs

- **Subjective "how subtle is subtle"** for both the background motion and the highlight copy → mitigation: this is explicitly a first pass to react to, per the established pattern on this page (show, then iterate on the user's reaction), not a final answer to get exactly right up front.
