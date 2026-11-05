"""Regressao de foco do painel deslizante de autenticacao.

Execute com a aplicacao local em http://localhost:8080. O teste usa apenas
Chromium/Playwright e nao altera dados persistentes (o profile dev usa H2 em
memoria e as respostas do fluxo de verificacao sao interceptadas).
"""

from playwright.sync_api import Page, sync_playwright


BASE_URL = "http://localhost:8080/login"
AUTH_REGIONS = ".form-container, .toggle-panel"
THEME_SURFACES = {"dark": "#151922", "light": "#FFFFFF"}


def apply_theme(page: Page, theme: str) -> None:
    page.add_init_script(f"localStorage.setItem('rendo-theme', '{theme}')")


def assert_theme(page: Page, theme: str) -> str:
    expected = "light" if theme == "light" else None
    assert page.locator("html").get_attribute("data-theme") == expected
    surface = page.locator("html").evaluate(
        "el => getComputedStyle(el).getPropertyValue('--surface').trim()"
    )
    assert surface.upper() == THEME_SURFACES[theme].upper(), (theme, surface)
    return surface


def active_snapshot(page: Page) -> dict:
    return page.evaluate(
        """(regions) => {
            const active = document.activeElement;
            return {
                tag: active?.tagName || null,
                id: active?.id || null,
                name: active?.getAttribute('name') || null,
                insideAuthRegion: Boolean(active?.closest(regions)),
            };
        }""",
        AUTH_REGIONS,
    )


def assert_transition_locked(page: Page, expected_active: bool) -> None:
    state = page.locator("#container").evaluate(
        """(container) => ({
            active: container.classList.contains('active'),
            transitioning: container.dataset.authTransition === 'true',
            signInInert: container.querySelector('.sign-in').inert,
            signUpInert: container.querySelector('.sign-up').inert,
            leftInert: container.querySelector('.toggle-left').inert,
            rightInert: container.querySelector('.toggle-right').inert,
        })"""
    )
    assert state == {
        "active": expected_active,
        "transitioning": True,
        "signInInert": True,
        "signUpInert": True,
        "leftInert": True,
        "rightInert": True,
    }, state


def assert_focus_outside_auth(page: Page) -> None:
    snapshot = active_snapshot(page)
    assert not snapshot["insideAuthRegion"], snapshot


def assert_immediate_stable_accessibility(page: Page, signup: bool) -> None:
    """Valida o estado sincrono; o foco logico usa dois animation frames."""
    state = page.locator("#container").evaluate(
        """(container) => ({
            active: container.classList.contains('active'),
            transitionMarker: container.getAttribute('data-auth-transition'),
            signInInert: container.querySelector('.sign-in').inert,
            signUpInert: container.querySelector('.sign-up').inert,
            leftInert: container.querySelector('.toggle-left').inert,
            rightInert: container.querySelector('.toggle-right').inert,
        })"""
    )
    assert state == {
        "active": signup,
        "transitionMarker": None,
        "signInInert": signup,
        "signUpInert": not signup,
        "leftInert": not signup,
        "rightInert": signup,
    }, state


def assert_stable_state(page: Page, signup: bool) -> None:
    expected_focus = "nome" if signup else "username"
    page.wait_for_function(
        """({ signup, focus }) => {
            const container = document.querySelector('#container');
            return container.dataset.authTransition !== 'true'
                && container.classList.contains('active') === signup
                && document.activeElement?.getAttribute('name') === focus;
        }""",
        arg={"signup": signup, "focus": expected_focus},
    )
    state = page.locator("#container").evaluate(
        """(container) => ({
            signInInert: container.querySelector('.sign-in').inert,
            signUpInert: container.querySelector('.sign-up').inert,
            leftInert: container.querySelector('.toggle-left').inert,
            rightInert: container.querySelector('.toggle-right').inert,
            signInHidden: container.querySelector('.sign-in').getAttribute('aria-hidden'),
            signUpHidden: container.querySelector('.sign-up').getAttribute('aria-hidden'),
        })"""
    )
    assert state == {
        "signInInert": signup,
        "signUpInert": not signup,
        "leftInert": not signup,
        "rightInert": signup,
        "signInHidden": str(signup).lower(),
        "signUpHidden": str(not signup).lower(),
    }, state


def exercise_desktop_transition(page: Page) -> None:
    page.goto(BASE_URL)
    page.wait_for_load_state("networkidle")
    assert active_snapshot(page)["tag"] == "BODY", active_snapshot(page)

    page.locator("#btn-cadastro").click(no_wait_after=True)
    assert_transition_locked(page, expected_active=True)
    page.keyboard.press("Tab")
    assert_focus_outside_auth(page)
    # A amostra intermediaria impede um falso positivo que apenas confira o
    # lock no mesmo tick do clique e depois pule direto ao estado final.
    page.wait_for_timeout(100)
    assert_transition_locked(page, expected_active=True)
    assert_focus_outside_auth(page)
    assert_stable_state(page, signup=True)

    page.locator("#btn-login").click(no_wait_after=True)
    assert_transition_locked(page, expected_active=False)
    page.keyboard.press("Tab")
    assert_focus_outside_auth(page)
    page.wait_for_timeout(100)
    assert_transition_locked(page, expected_active=False)
    assert_focus_outside_auth(page)
    assert_stable_state(page, signup=False)


def exercise_initial_query(page: Page) -> None:
    page.goto(f"{BASE_URL}?criarConta=1&nome=Ana&email=ana%40example.com")
    page.wait_for_load_state("networkidle")
    assert page.locator("#container").evaluate("el => el.classList.contains('active')")
    assert active_snapshot(page)["tag"] == "BODY", active_snapshot(page)
    assert page.locator("#cadastro-form [name=nome]").input_value() == "Ana"
    assert page.locator("#cadastro-form [name=email]").input_value() == "ana@example.com"


def exercise_rapid_switch(page: Page) -> None:
    page.goto(BASE_URL)
    page.wait_for_load_state("networkidle")
    page.evaluate(
        """() => {
            const container = document.querySelector('#container');
            const regions = {
                signIn: container.querySelector('.sign-in'),
                signUp: container.querySelector('.sign-up'),
                left: container.querySelector('.toggle-left'),
                right: container.querySelector('.toggle-right'),
            };
            const startedAt = performance.now();
            let phase = 'opening-signup';
            const sample = () => ({
                elapsed: performance.now() - startedAt,
                phase,
                active: container.classList.contains('active'),
                transitioning: container.dataset.authTransition === 'true',
                signInInert: regions.signIn.inert,
                signUpInert: regions.signUp.inert,
                leftInert: regions.left.inert,
                rightInert: regions.right.inert,
            });
            window.__authRapidSamples = [sample()];
            window.__authRapidInterval = setInterval(
                () => window.__authRapidSamples.push(sample()),
                5,
            );
            sincronizarEstadoAuth(true, { moverFoco: true });
            setTimeout(() => {
                phase = 'returning-login';
                sincronizarEstadoAuth(false, { moverFoco: true });
                window.__authRapidSamples.push(sample());
            }, 40);
        }"""
    )
    assert_stable_state(page, signup=False)
    # Mantem o observador alem dos dois fallbacks potenciais. Se o callback
    # antigo escapar do cancelamento, alguma amostra libera cadastro/left.
    page.wait_for_timeout(700)
    samples = page.evaluate(
        """() => {
            clearInterval(window.__authRapidInterval);
            return window.__authRapidSamples;
        }"""
    )
    returning = [sample for sample in samples if sample["phase"] == "returning-login"]
    assert len(returning) >= 50, len(returning)
    for sample in returning:
        assert sample["active"] is False, sample
        if sample["transitioning"]:
            assert all(
                sample[key]
                for key in ("signInInert", "signUpInert", "leftInert", "rightInert")
            ), sample
        else:
            assert sample["signInInert"] is False, sample
            assert sample["signUpInert"] is True, sample
            assert sample["leftInert"] is True, sample
            assert sample["rightInert"] is False, sample
    assert active_snapshot(page)["name"] == "username", active_snapshot(page)


def exercise_verification_return(page: Page) -> None:
    page.route(
        "**/usuarios",
        lambda route: route.fulfill(status=200, content_type="application/json", body="{}"),
    )
    page.route(
        "**/usuarios/verificar-email",
        lambda route: route.fulfill(status=200, content_type="application/json", body="{}"),
    )
    page.goto(f"{BASE_URL}?criarConta=1")
    page.wait_for_load_state("networkidle")
    page.locator("#cadastro-form [name=nome]").fill("Ana Silva")
    page.locator("#cadastro-form [name=email]").fill("ana@example.com")
    page.locator("#cadastro-form [name=cpf]").fill("12345678901")
    page.locator("#cadastro-form [name=senha]").fill("senha-segura")
    page.locator("#cadastro-form button[type=submit]").click()
    page.locator("#verificacao-form [name=codigo]").fill("123456")
    page.locator("#verificacao-form button[type=submit]").click(no_wait_after=True)
    page.wait_for_function(
        """() => !document.querySelector('#container').classList.contains('active')
            && document.activeElement?.getAttribute('name') === 'username'"""
    )


def run() -> None:
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        computed_surfaces = {}

        for theme in ("dark", "light"):
            desktop = browser.new_page(
                viewport={"width": 1440, "height": 900},
            )
            apply_theme(desktop, theme)
            exercise_desktop_transition(desktop)
            computed_surfaces[theme] = assert_theme(desktop, theme)
            desktop.close()
        assert computed_surfaces["dark"] != computed_surfaces["light"], computed_surfaces

        for theme in ("dark", "light"):
            page = browser.new_page(
                viewport={"width": 390, "height": 844},
            )
            apply_theme(page, theme)
            page.goto(BASE_URL)
            page.wait_for_load_state("networkidle")
            assert_theme(page, theme)
            page.locator("#btn-cadastro").click()
            assert_immediate_stable_accessibility(page, signup=True)
            assert_stable_state(page, signup=True)
            page.locator("#btn-login").click()
            assert_immediate_stable_accessibility(page, signup=False)
            assert_stable_state(page, signup=False)
            page.close()

        reduced = browser.new_page(
            viewport={"width": 1440, "height": 900},
            reduced_motion="reduce",
        )
        reduced.goto(BASE_URL)
        reduced.wait_for_load_state("networkidle")
        reduced.locator("#btn-cadastro").click()
        assert_immediate_stable_accessibility(reduced, signup=True)
        assert_stable_state(reduced, signup=True)
        reduced.locator("#btn-login").click()
        assert_immediate_stable_accessibility(reduced, signup=False)
        assert_stable_state(reduced, signup=False)
        reduced.close()

        initial = browser.new_page(viewport={"width": 1440, "height": 900})
        exercise_initial_query(initial)
        exercise_verification_return(initial)
        exercise_rapid_switch(initial)
        initial.close()

        browser.close()
    print("auth focus regression: PASS")


if __name__ == "__main__":
    run()
