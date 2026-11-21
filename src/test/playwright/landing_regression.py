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


# --------------------------------- Abertura ---------------------------------

def rolar_para(page: Page, p: float, espera: float = 1.4) -> None:
    page.evaluate(
        """p => { const s = document.getElementById('jornada');
                  scrollTo(0, s.offsetTop + (s.offsetHeight - innerHeight) * p); }""",
        p,
    )
    time.sleep(espera)


def opacidade(page: Page, seletor: str, indice: int = 0) -> float:
    return float(page.locator(seletor).nth(indice).evaluate("e => getComputedStyle(e).opacity"))


def estado_inicial(page: Page) -> None:
    assert texto(page, "#cenaValor") == "R$ 100.000,00", f"Comeco deveria mostrar R$ 100.000,00, veio {texto(page, '#cenaValor')!r}"
    assert opacidade(page, "#cenaChips li") < 0.1, "Chips nao deveriam aparecer no comeco"


def estado_final(page: Page) -> None:
    assert texto(page, "#cenaValor") == "R$ 112.400,00", f"Fim deveria mostrar R$ 112.400,00, veio {texto(page, '#cenaValor')!r}"
    for i in range(2):
        assert opacidade(page, ".cena-satelite", i) > 0.9, f"Satelite {i} deveria estar visivel no fim"


def cena_acompanha_scroll(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    rolar_para(page, 0.02)
    estado_inicial(page)
    rolar_para(page, 0.62)
    traco = float(page.locator("#cenaLinha").evaluate("e => getComputedStyle(e).strokeDashoffset").replace("px", ""))
    assert traco < 0.7, f"Linha deveria estar mais de 30% desenhada em p=0.62 (dashoffset {traco})"
    for i in range(4):
        assert opacidade(page, "#cenaChips li", i) > 0.9, f"Chip {i} deveria estar visivel em p=0.62"
    rolar_para(page, 0.98)
    estado_final(page)
    rolar_para(page, 0.02)
    estado_inicial(page)
    sem_erros(page)


def scroll_brusco_mantem_cena_coerente(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    rolar_para(page, 1, espera=1.5)
    estado_final(page)
    page.evaluate("scrollTo(0, 0)")
    time.sleep(1.5)
    estado_inicial(page)
    sem_erros(page)


def redimensionar_sem_overflow(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    rolar_para(page, 0.6)
    page.set_viewport_size({"width": 390, "height": 844})
    time.sleep(0.8)
    largura = page.evaluate("[document.documentElement.scrollWidth, innerWidth]")
    assert largura[0] <= largura[1], f"Scroll horizontal apos redimensionar: {largura}"
    caixa = page.locator("#cenaCarteira").bounding_box()
    assert caixa and caixa["x"] >= 0 and caixa["x"] + caixa["width"] <= 390 and caixa["y"] >= 0, f"Cartao cortado: {caixa}"
    sem_erros(page)


def ponteiro_inclina_cartao(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    page.mouse.move(640, 360)
    page.mouse.move(1200, 200, steps=5)
    time.sleep(0.3)
    transform = page.locator("#cenaCarteira").evaluate("e => e.style.transform")
    assert "rotateY(" in transform and "rotateY(0.00deg)" not in transform, f"Cartao deveria inclinar com o mouse: {transform!r}"
    sem_erros(page)


def toque_nao_inclina(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 390, "height": 844}, has_touch=True, is_mobile=True)
    page.locator("#cenaCarteira").tap()
    time.sleep(0.3)
    transform = page.locator("#cenaCarteira").evaluate("e => e.style.transform")
    assert transform == "", f"Toque nao deveria inclinar o cartao: {transform!r}"
    sem_erros(page)


def teclado_foca_botoes_visiveis(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    for alvo in ("Começar agora", "Construir meu futuro"):
        for _ in range(30):
            page.keyboard.press("Tab")
            if alvo in page.evaluate("document.activeElement.textContent || ''"):
                break
        else:
            raise AssertionError(f"Tab nao chegou em {alvo!r}")
        time.sleep(1.4)
        foco = page.evaluate(
            """() => { const e = document.activeElement, r = e.getBoundingClientRect();
                       return { dentro: r.top >= 0 && r.bottom <= innerHeight,
                                opacidade: +getComputedStyle(e.closest('.momento')).opacity }; }"""
        )
        assert foco["dentro"] and foco["opacidade"] > 0.9, f"{alvo!r} focado mas invisivel: {foco}"
    sem_erros(page)


def movimento_reduzido_mostra_estado_final(browser: Browser) -> None:
    page = abrir(browser, reduced_motion="reduce")
    assert texto(page, "#cenaValor") == "R$ 112.400,00", "Movimento reduzido deve mostrar o estado final"
    page.mouse.move(900, 300)
    assert page.locator("#cenaCarteira").evaluate("e => e.style.transform") == "", "Sem inclinacao com movimento reduzido"
    sem_erros(page)


def screenshots(browser: Browser) -> None:
    pasta = os.path.join("target", "landing-screens")
    os.makedirs(pasta, exist_ok=True)
    for largura, altura in ((1280, 720), (768, 1024), (390, 844)):
        page = abrir(browser, viewport={"width": largura, "height": altura})
        for p in (0.02, 0.35, 0.62, 0.98):
            rolar_para(page, p)
            page.screenshot(path=os.path.join(pasta, f"abertura-{largura}-{int(p * 100):02d}.png"))
        sem_erros(page)


# -------------------------------- Desempenho --------------------------------

def desempenho_scroll(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    page.evaluate(
        """() => { window.__longas = []; window.__cls = 0;
                   new PerformanceObserver(l => l.getEntries().forEach(e => __longas.push(Math.round(e.duration))))
                       .observe({ type: 'longtask', buffered: false });
                   new PerformanceObserver(l => l.getEntries().forEach(e => { if (!e.hadRecentInput) __cls += e.value; }))
                       .observe({ type: 'layout-shift', buffered: true }); }"""
    )
    for i in range(61):  # 0 -> 1 em ~3 s
        rolar_para(page, i / 60, espera=0.05)
    time.sleep(1)
    longas, cls = page.evaluate("[__longas, __cls]")
    print(f"    tarefas longas no scroll: {longas}  CLS: {cls:.4f}")
    assert len(longas) <= 1, f"Tarefas longas (>50 ms) recorrentes no scroll: {longas}"
    assert cls < 0.1, f"CLS {cls:.4f} >= 0.1"
    sem_erros(page)


# -------------------------------- Cabecalho ---------------------------------

def cabecalho_marca_secao_ativa(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    page.locator("#como-funciona").scroll_into_view_if_needed()
    page.evaluate("document.getElementById('como-funciona').scrollIntoView({block: 'center'})")
    time.sleep(0.6)
    ativos = page.evaluate(
        "[...document.querySelectorAll('.lp-nav a[aria-current=\"true\"]')].map(a => a.getAttribute('href'))"
    )
    assert ativos == ["#como-funciona"], f"So o link de #como-funciona deveria estar ativo, veio {ativos}"
    sem_erros(page)


TESTES = [
    contador_anima_ate_valor_final,
    contador_respeita_movimento_reduzido,
    sem_js_mostra_valores_finais,
    cena_acompanha_scroll,
    scroll_brusco_mantem_cena_coerente,
    redimensionar_sem_overflow,
    ponteiro_inclina_cartao,
    toque_nao_inclina,
    teclado_foca_botoes_visiveis,
    movimento_reduzido_mostra_estado_final,
    cabecalho_marca_secao_ativa,
    desempenho_scroll,
    screenshots,
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
