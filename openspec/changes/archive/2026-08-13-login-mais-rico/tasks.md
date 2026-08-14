## 1. Toggle panel highlights

- [x] 1.1 In `login.html`, add a short highlight list (2-3 items, small inline SVG icon + text) to `.toggle-left` (login side) after its paragraph, before the button
- [x] 1.2 Same for `.toggle-right` (cadastro side)
- [x] 1.3 Style the list in `login.css` — compact, consistent with `.toggle-panel p`'s existing look, icons using `var(--rendo-color-on-accent)` (same fixed-token fix as the theme work, since this panel is always dark)

## 2. Animated background

- [x] 2.1 In `login.css`, replace `.login-page`'s static radial-gradient background with `position: relative; overflow: hidden` + two blurred pseudo-element blobs (`::before`/`::after`)
- [x] 2.2 Add `@keyframes` drifting the blobs' `transform: translate(...)` slowly (24-30s, ease-in-out, infinite, alternate)

## 3. Real-time validation

- [x] 3.1 In `login.js`, add small pure check functions: email shape (regex), senha length >= 8, nome non-empty
- [x] 3.2 Wire `input` listeners on cadastro's nome/email/senha and login's email fields, toggling `.valido`/`.invalido` on the parent `.input-group` (no class until the field has been interacted with)
- [x] 3.3 Add `.input-group.valido input` / `.input-group.invalido input` border-color styles in `login.css`

## 4. Verification

- [x] 4.1 Manually verify in a real browser: highlights render on both panel sides; background visibly drifts over ~10s of observation; typing an invalid email shows the invalid border, fixing it shows valid; empty/untouched fields show neither; submitting with an invalid-looking field still hits the server exactly as before
- [x] 4.2 Confirm no console errors introduced
