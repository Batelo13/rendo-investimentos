"""Regressao da pagina inicial (abertura, sistema de movimento e cabecalho).

Execute com a aplicacao rodando (padrao http://localhost:8080; use RENDO_URL
para outra porta, ex.: a instancia descartavel em 8081). Usa so Chromium/
Playwright e nao grava nada: a landing e publica e nao chama a API.
"""

import os
import time

from playwright.sync_api import Browser, Page, sync_playwright


BASE_URL = os.environ.get("RENDO_URL", "http://localhost:8080")
CAPTURA_CSP = (
    "document.addEventListener('securitypolicyviolation', "
    "e => console.error('CSP ' + e.violatedDirective + ' ' + e.blockedURI))"
)


def texto(page: Page, seletor: str) -> str:
    # Intl.NumberFormat usa espaco nao separavel depois de "R$".
    return page.locator(seletor).first.inner_text().replace("\xa0", " ").strip()


def abrir(browser: Browser, **contexto) -> Page:
    page = browser.new_context(**contexto).new_page()
    page.erros = []
    page.on("console", lambda m: m.type == "error" and page.erros.append(m.text))
    page.on("pageerror", lambda e: page.erros.append(str(e)))
    page.add_init_script(CAPTURA_CSP)
    page.goto(BASE_URL + "/")
    page.wait_for_load_state("networkidle")
    time.sleep(0.4)
    return page


def sem_erros(page: Page) -> None:
    assert not page.erros, f"Erros no console: {page.erros}"
    page.context.close()


# --------------------------- Sistema de movimento ---------------------------

def contador_anima_ate_valor_final(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    fato = "#visao-geral dt[data-contador]"
    assert texto(page, fato) == "R$ 0", f"Contador fora da tela deveria partir de R$ 0, veio {texto(page, fato)!r}"
    page.locator(fato).scroll_into_view_if_needed()
    time.sleep(1.6)
    assert texto(page, fato) == "R$ 100.000", f"Contador deveria terminar em R$ 100.000, veio {texto(page, fato)!r}"
    sem_erros(page)


def contador_respeita_movimento_reduzido(browser: Browser) -> None:
    page = abrir(browser, reduced_motion="reduce")
    assert texto(page, "#visao-geral dt[data-contador]") == "R$ 100.000", "Movimento reduzido deve mostrar o valor final"
    sem_erros(page)


def sem_js_mostra_valores_finais(browser: Browser) -> None:
    page = browser.new_context(java_script_enabled=False).new_page()
    page.goto(BASE_URL + "/")
    assert texto(page, "#visao-geral dt[data-contador]") == "R$ 100.000", "Sem JS o contador deve mostrar o valor final"
    page.context.close()


TESTES = [
    contador_anima_ate_valor_final,
    contador_respeita_movimento_reduzido,
    sem_js_mostra_valores_finais,
]


def main() -> None:
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="chrome")
        try:
            for teste in TESTES:
                teste(browser)
                print(f"ok  {teste.__name__}")
        finally:
            browser.close()
    print(f"{len(TESTES)} testes da landing passaram")


if __name__ == "__main__":
    main()
