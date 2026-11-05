"""Regressao de foco do painel deslizante de autenticacao.

Execute com a aplicacao local em http://localhost:8080. O teste usa apenas
Chromium/Playwright e nao altera dados persistentes (o profile dev usa H2 em
memoria e as respostas do fluxo de verificacao sao interceptadas).
"""

from playwright.sync_api import Page, sync_playwright


BASE_URL = "http://localhost:8080/login"
AUTH_REGIONS = ".form-container, .toggle-panel"


def apply_theme(page: Page, theme: str) -> None:
    page.add_init_script(f"localStorage.setItem('rendo-theme', '{theme}')")


def assert_theme(page: Page, theme: str) -> None:
    expected = "light" if theme == "light" else None
    assert page.locator("html").get_attribute("data-theme") == expected


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
    assert not active_snapshot(page)["insideAuthRegion"], active_snapshot(page)
    assert_stable_state(page, signup=True)

    page.locator("#btn-login").click(no_wait_after=True)
    assert_transition_locked(page, expected_active=False)
    page.keyboard.press("Tab")
    assert not active_snapshot(page)["insideAuthRegion"], active_snapshot(page)
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
            sincronizarEstadoAuth(true, { moverFoco: true });
            setTimeout(() => sincronizarEstadoAuth(false, { moverFoco: true }), 40);
        }"""
    )
    assert_stable_state(page, signup=False)
    page.wait_for_timeout(550)
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

        for theme in ("dark", "light"):
            desktop = browser.new_page(
                viewport={"width": 1440, "height": 900},
            )
            apply_theme(desktop, theme)
            exercise_desktop_transition(desktop)
            assert_theme(desktop, theme)
            desktop.close()

        for theme in ("dark", "light"):
            page = browser.new_page(
                viewport={"width": 390, "height": 844},
            )
            apply_theme(page, theme)
            page.goto(BASE_URL)
            page.wait_for_load_state("networkidle")
            assert_theme(page, theme)
            page.locator("#btn-cadastro").click()
            assert_stable_state(page, signup=True)
            page.locator("#btn-login").click()
            assert_stable_state(page, signup=False)
            page.close()

        reduced = browser.new_page(
            viewport={"width": 1440, "height": 900},
            reduced_motion="reduce",
        )
        reduced.goto(BASE_URL)
        reduced.wait_for_load_state("networkidle")
        reduced.locator("#btn-cadastro").click()
        assert_stable_state(reduced, signup=True)
        reduced.locator("#btn-login").click()
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
