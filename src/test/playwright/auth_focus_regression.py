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


def assert_theme(page: Page, theme: str) -> dict:
    expected = "light" if theme == "light" else None
    assert page.locator("html").get_attribute("data-theme") == expected
    styles = page.locator("#container").evaluate(
        """(container) => {
            const root = getComputedStyle(document.documentElement);
            const surface = getComputedStyle(container);
            const signIn = getComputedStyle(container.querySelector('.sign-in'));
            return {
                token: root.getPropertyValue('--surface').trim(),
                background: surface.backgroundColor,
                border: surface.borderTopColor,
                color: signIn.color,
            };
        }"""
    )
    assert styles["token"].upper() == THEME_SURFACES[theme].upper(), (theme, styles)
    return styles


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


def wait_for_auth_entrance(page: Page) -> None:
    """Isola o slide da animacao finita de entrada do proprio card auth."""
    page.wait_for_function(
        """() => document.querySelector('#container').getAnimations()
            .every((animation) => animation.playState === 'finished')"""
    )


def install_desktop_lifecycle_audit(page: Page, trigger_selector: str) -> None:
    """Instala listeners antes do clique e reconstrui mutacoes no mesmo ciclo."""
    page.evaluate(
        """(triggerSelector) => {
            const container = document.querySelector('#container');
            const toggle = container.querySelector('.toggle');
            const regions = [...container.querySelectorAll('.form-container, .toggle-panel')];
            const regionName = (element) => {
                if (element.classList.contains('sign-in')) return 'signIn';
                if (element.classList.contains('sign-up')) return 'signUp';
                if (element.classList.contains('toggle-left')) return 'left';
                if (element.classList.contains('toggle-right')) return 'right';
                return null;
            };
            const events = [];
            const auditStartedAt = performance.now();
            let started = false;
            let transitionEnded = false;

            const processMutations = (records) => {
                records.forEach((record, index) => {
                    let newValue;
                    for (let next = index + 1; next < records.length; next += 1) {
                        const candidate = records[next];
                        if (candidate.target === record.target
                            && candidate.attributeName === record.attributeName) {
                            newValue = candidate.oldValue;
                            break;
                        }
                    }
                    if (newValue === undefined) {
                        newValue = record.target.getAttribute(record.attributeName);
                    }
                    events.push({
                        type: 'attribute',
                        region: regionName(record.target),
                        attribute: record.attributeName,
                        oldValue: record.oldValue,
                        newValue,
                        started,
                        transitionEnded,
                        elapsed: performance.now() - auditStartedAt,
                    });
                });
            };
            const observer = new MutationObserver(processMutations);
            regions.forEach((region) => observer.observe(region, {
                attributes: true,
                attributeOldValue: true,
                attributeFilter: ['inert', 'aria-hidden'],
            }));
            observer.observe(container, {
                attributes: true,
                attributeOldValue: true,
                attributeFilter: ['class', 'data-auth-transition'],
            });

            const onClickCapture = (event) => {
                if (event.target.closest(triggerSelector)) started = true;
            };
            const onFocus = (event) => {
                const region = event.target.closest('.form-container, .toggle-panel');
                events.push({
                    type: 'focus',
                    region: region ? regionName(region) : null,
                    started,
                    transitionEnded,
                    elapsed: performance.now() - auditStartedAt,
                });
            };
            const onTransitionEnd = (event) => {
                if (event.target === toggle && event.propertyName === 'transform') {
                    transitionEnded = true;
                    events.push({
                        type: 'transitionend',
                        property: 'transform',
                        elapsed: performance.now() - auditStartedAt,
                    });
                }
            };
            document.addEventListener('click', onClickCapture, true);
            document.addEventListener('focusin', onFocus, true);
            // Instalado antes do clique: antecede o listener que o produto cria
            // no handler de clique e marca o fim antes da liberacao estavel.
            toggle.addEventListener('transitionend', onTransitionEnd, true);

            window.__finishAuthLifecycleAudit = () => {
                processMutations(observer.takeRecords());
                observer.disconnect();
                document.removeEventListener('click', onClickCapture, true);
                document.removeEventListener('focusin', onFocus, true);
                toggle.removeEventListener('transitionend', onTransitionEnd, true);
                delete window.__finishAuthLifecycleAudit;
                return { events, started, transitionEnded };
            };
        }""",
        trigger_selector,
    )


def assert_desktop_lifecycle_audit(page: Page) -> None:
    audit = page.evaluate("window.__finishAuthLifecycleAudit()")
    assert audit["started"] is True, audit
    assert audit["transitionEnded"] is True, audit
    transition_events = [
        event for event in audit["events"] if event["type"] == "transitionend"
    ]
    assert len(transition_events) == 1, audit
    violations = []
    for event in audit["events"]:
        before_end = event.get("started") and not event.get("transitionEnded")
        released = (
            event["type"] == "attribute"
            and (
                (event["attribute"] == "inert" and event["newValue"] is None)
                or (event["attribute"] == "aria-hidden" and event["newValue"] == "false")
            )
        )
        focused_region = event["type"] == "focus" and event["region"] is not None
        if before_end and (released or focused_region):
            violations.append(event)
    assert violations == [], audit


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
    wait_for_auth_entrance(page)
    assert active_snapshot(page)["tag"] == "BODY", active_snapshot(page)

    install_desktop_lifecycle_audit(page, "#btn-cadastro")
    page.locator("#btn-cadastro").click(no_wait_after=True)
    page.keyboard.press("Tab")
    assert_stable_state(page, signup=True)
    assert_desktop_lifecycle_audit(page)

    install_desktop_lifecycle_audit(page, "#btn-login")
    page.locator("#btn-login").click(no_wait_after=True)
    page.keyboard.press("Tab")
    assert_stable_state(page, signup=False)
    assert_desktop_lifecycle_audit(page)


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
    wait_for_auth_entrance(page)
    audit_window = page.evaluate(
        """() => {
            const container = document.querySelector('#container');
            const regions = {
                signIn: container.querySelector('.sign-in'),
                signUp: container.querySelector('.sign-up'),
                left: container.querySelector('.toggle-left'),
                right: container.querySelector('.toggle-right'),
            };
            let phase = 'opening-signup';
            const events = [];
            const names = new Map(Object.entries(regions).map(([name, element]) => [element, name]));
            const processMutations = (records) => {
                records.forEach((record, index) => {
                    let newValue;
                    for (let next = index + 1; next < records.length; next += 1) {
                        const candidate = records[next];
                        if (candidate.target === record.target
                            && candidate.attributeName === record.attributeName) {
                            newValue = candidate.oldValue;
                            break;
                        }
                    }
                    if (newValue === undefined) {
                        newValue = record.target.getAttribute(record.attributeName);
                    }
                    events.push({
                        type: 'attribute',
                        phase,
                        region: names.get(record.target),
                        attribute: record.attributeName,
                        oldValue: record.oldValue,
                        newValue,
                    });
                });
            };
            const observer = new MutationObserver(processMutations);
            Object.values(regions).forEach((region) => observer.observe(region, {
                attributes: true,
                attributeOldValue: true,
                attributeFilter: ['inert', 'aria-hidden'],
            }));
            const onFocus = (event) => {
                const region = event.target.closest('.form-container, .toggle-panel');
                events.push({
                    type: 'focus',
                    phase,
                    region: region ? names.get(region) : null,
                });
            };
            document.addEventListener('focusin', onFocus, true);

            sincronizarEstadoAuth(true, { moverFoco: true });
            setTimeout(() => {
                phase = 'returning-login';
                sincronizarEstadoAuth(false, { moverFoco: true });
            }, 40);

            window.__finishAuthRapidAudit = () => {
                processMutations(observer.takeRecords());
                observer.disconnect();
                document.removeEventListener('focusin', onFocus, true);
                delete window.__finishAuthRapidAudit;
                return events;
            };

            const style = getComputedStyle(container.querySelector('.toggle'));
            const toMs = (value) => value.endsWith('ms')
                ? Number.parseFloat(value)
                : Number.parseFloat(value) * 1000;
            const durations = style.transitionDuration.split(',').map(toMs);
            const delays = style.transitionDelay.split(',').map(toMs);
            const movement = durations.reduce(
                (max, duration, index) => Math.max(max, duration + (delays[index % delays.length] || 0)),
                0,
            );
            return movement * 2 + 300;
        }"""
    )
    assert_stable_state(page, signup=False)
    # Cobre os fallbacks antigo e novo; MutationObserver preserva oldValue e
    # detecta ate uma remocao/reinsercao ocorrida dentro do mesmo ciclo.
    page.wait_for_timeout(audit_window)
    events = page.evaluate("window.__finishAuthRapidAudit()")
    returning = [event for event in events if event["phase"] == "returning-login"]
    assert returning, events
    violations = []
    for event in returning:
        protected_region = event["region"] in ("signUp", "left")
        removed_inert = (
            event["type"] == "attribute"
            and event["attribute"] == "inert"
            and event["newValue"] is None
        )
        exposed_region = (
            event["type"] == "attribute"
            and event["attribute"] == "aria-hidden"
            and event["newValue"] == "false"
        )
        focused_region = event["type"] == "focus"
        if protected_region and (removed_inert or exposed_region or focused_region):
            violations.append(event)
    assert violations == [], violations
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
    page.locator("#cadastro-form [name=senha]").fill("Senha-segura")
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
        assert computed_surfaces["dark"]["token"] != computed_surfaces["light"]["token"]
        visible_style_keys = ("background", "border", "color")
        assert any(
            computed_surfaces["dark"][key] != computed_surfaces["light"][key]
            for key in visible_style_keys
        ), computed_surfaces

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
