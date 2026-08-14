## 1. Fragment and styles

- [x] 1.1 Create `templates/fragments/loading.html` — overlay markup around the existing wallet SVG (reuse the icon from `fragments/logo.html`, don't redraw it)
- [x] 1.2 Create `static/css/loading.css` — overlay layout (fixed, full-screen, above content), wallet keyframe animation (restrained, matches the identity brief), fade-out transition class

## 2. Splash behavior (initial page load)

- [x] 2.1 Include the loading fragment in `index.html`, `login.html`, `dashboard.html`
- [x] 2.2 Create `static/js/loading.js` with a `DOMContentLoaded`/`load` listener that adds the fade-out class and removes the overlay from the DOM afterward
- [x] 2.3 Include `loading.js` (and `loading.css`) on all three pages

## 3. Cadastro has no loading overlay

- [x] 3.1 Revert: `login.js` cadastro handler gets no loading-overlay/button-disable — the panel's existing slide-back-to-login + inline message is the only feedback (corrected after first pass wrongly added an overlay here)

## 4. Login submit feedback (native form POST)

- [x] 4.1 In `login.js`, add a `submit` listener on the login form that turns the loading state on (no `finally` — the browser navigation replaces the page)

## 5. Verification

- [x] 5.1 Manually verify in a real browser: splash shows/hides on `/` and `/dashboard`; `/login` shows no overlay on plain visits or cadastro; login shows loading immediately on submit and it carries through to the dashboard splash
- [x] 5.2 Check browser console/network for errors introduced by the new JS
