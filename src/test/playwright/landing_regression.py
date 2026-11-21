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


def rotacao_x(page: Page) -> float:
    t = page.locator("#cenaCarteira").evaluate("e => e.style.transform")
    return float(t.split("rotateX(")[1].split("deg")[0])


def ponteiro_vertical_segue_mouse(browser: Browser) -> None:
    # Mouse no centro vertical da tela: sem inclinacao em X, no topo e no fim da jornada.
    page = abrir(browser, viewport={"width": 1280, "height": 720})
    for p in (0.0, 1.0):
        rolar_para(page, p)
        page.mouse.move(1000, 300)
        page.mouse.move(1000, 360, steps=4)
        time.sleep(0.3)
        assert abs(rotacao_x(page)) < 0.6, f"rotateX deveria ser ~0 com o mouse no centro (p={p}), veio {rotacao_x(page)}"
    page.mouse.move(1000, 100, steps=4)
    time.sleep(0.3)
    # rotateX positivo: a borda de cima (perto do mouse) recua.
    assert rotacao_x(page) > 3, f"Mouse no alto deveria fazer a borda de cima recuar, veio {rotacao_x(page)}"
    sem_erros(page)


INTL_ANTIGO = """
(() => { const Original = Intl.NumberFormat;
  // Engines sem NumberFormat v3: maximumFractionDigits < minimo da moeda lanca RangeError.
  Intl.NumberFormat = function (locale, opcoes = {}) {
    if (opcoes.style === 'currency' && opcoes.maximumFractionDigits === 0 && opcoes.minimumFractionDigits === undefined)
      throw new RangeError('maximumFractionDigits value is out of range.');
    return new Original(locale, opcoes);
  };
})();"""


def abertura_funciona_em_intl_antigo(browser: Browser) -> None:
    context = browser.new_context(viewport={"width": 1280, "height": 720})
    context.add_init_script(INTL_ANTIGO)
    page = context.new_page()
    page.erros = []
    page.on("pageerror", lambda e: page.erros.append(str(e)))
    page.goto(BASE_URL + "/")
    page.wait_for_load_state("networkidle")
    time.sleep(0.6)
    assert opacidade(page, ".momento") > 0.9, "Titulo da abertura invisivel com Intl antigo"
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


# -------------------------------- Simulador ---------------------------------

def ir_para_simulador(page: Page) -> None:
    page.evaluate("document.getElementById('experimente').scrollIntoView({block: 'start'})")
    time.sleep(0.6)


def slider(page: Page, ticker: str, valor: int) -> None:
    page.locator(f'#simSliders input[data-ticker="{ticker}"]').evaluate(
        "(e, v) => { e.value = v; e.dispatchEvent(new Event('input', { bubbles: true })); }", valor
    )


def valor_slider(page: Page, ticker: str) -> int:
    return int(page.locator(f'#simSliders input[data-ticker="{ticker}"]').input_value())


def chip(page: Page, ticker: str):
    return page.locator(f'#simAcoes button[data-ticker="{ticker}"]')


def simulador_estado_inicial(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    for t in ("PETR4", "VALE3", "WEGE3"):
        assert chip(page, t).get_attribute("aria-pressed") == "true", f"{t} deveria comecar selecionada"
    for t in ("ITUB4", "AAPL", "MSFT"):
        assert chip(page, t).get_attribute("aria-pressed") == "false", f"{t} nao deveria comecar selecionada"
    assert texto(page, "#simCaixa") == "Caixa: R$ 15.000", f"Caixa inicial errado: {texto(page, '#simCaixa')!r}"
    sem_erros(page)


def simulador_limita_soma_em_100_mil(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    slider(page, "PETR4", 100000)
    assert valor_slider(page, "PETR4") == 55000, f"PETR4 deveria parar em 55.000, ficou {valor_slider(page, 'PETR4')}"
    assert texto(page, "#simCaixa") == "Caixa: R$ 0", f"Caixa deveria zerar: {texto(page, '#simCaixa')!r}"
    sem_erros(page)


def simulador_teclado_respeita_limite(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    page.locator('#simSliders input[data-ticker="VALE3"]').focus()
    page.keyboard.press("End")
    time.sleep(0.2)
    assert valor_slider(page, "VALE3") == 40000, f"VALE3 deveria parar em 40.000, ficou {valor_slider(page, 'VALE3')}"
    assert texto(page, "#simCaixa") == "Caixa: R$ 0"
    sem_erros(page)


def simulador_escolhe_ate_4_acoes(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    chip(page, "AAPL").click()
    assert chip(page, "ITUB4").is_disabled() and chip(page, "MSFT").is_disabled(), "Com 4 escolhidas as outras deveriam ficar indisponiveis"
    assert page.locator("#simAviso").is_visible(), "Aviso 'Escolha até 4 ações.' deveria aparecer"
    chip(page, "AAPL").click()
    assert chip(page, "ITUB4").is_enabled() and chip(page, "MSFT").is_enabled(), "Ao desmarcar, as outras deveriam voltar"
    assert not page.locator("#simAviso").is_visible(), "Aviso deveria sumir"
    sem_erros(page)


def simulador_desmarcar_devolve_ao_caixa(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    chip(page, "VALE3").click()
    assert page.locator('#simSliders input[data-ticker="VALE3"]').count() == 0, "Slider da VALE3 deveria sumir"
    assert texto(page, "#simCaixa") == "Caixa: R$ 40.000", f"Caixa errado: {texto(page, '#simCaixa')!r}"
    sem_erros(page)


def simulador_converte_acoes_eua(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    chip(page, "AAPL").click()
    slider(page, "AAPL", 10000)
    rotulo = texto(page, 'output[data-valor-de="AAPL"]')
    assert "R$ 10.000" in rotulo and "US$ 1.852" in rotulo, f"Rotulo da AAPL errado: {rotulo!r}"
    sem_erros(page)


def numero(t: str) -> float:
    return float(t.replace("R$", "").replace(".", "").replace(",", ".").strip())


def cenario(page: Page, nome: str, espera: float = 0.8) -> float:
    page.check(f'#simCenarios input[value="{nome}"]')
    time.sleep(espera)
    return numero(texto(page, "#simValor"))


def simulador_tudo_em_caixa_nao_varia(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    for t in ("PETR4", "VALE3", "WEGE3"):
        chip(page, t).click()
    for nome in ("alta", "lateral", "queda"):
        assert cenario(page, nome) == 100000.0, f"Tudo em caixa deveria dar R$ 100.000,00 em {nome}, veio {texto(page, '#simValor')!r}"
    sem_erros(page)


def simulador_cenarios_ordenados(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    alta, lateral, queda = (cenario(page, n) for n in ("alta", "lateral", "queda"))
    assert alta > lateral > queda, f"Esperado alta > lateral > queda, veio {alta}, {lateral}, {queda}"
    assert queda < 100000 < alta, f"Queda deveria ficar abaixo e alta acima de R$ 100.000: {queda}, {alta}"
    sem_erros(page)


def simulador_deterministico(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    primeiro = cenario(page, "alta")
    cenario(page, "queda")
    assert cenario(page, "alta") == primeiro, "Voltar ao cenario deveria mostrar o mesmo valor"
    sem_erros(page)


def simulador_troca_rapida_de_cenario(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    lateral = cenario(page, "lateral")
    cenario(page, "alta")
    page.check('#simCenarios input[value="queda"]')
    time.sleep(0.1)
    assert cenario(page, "lateral") == lateral, f"Troca no meio da animacao deveria terminar em lateral ({lateral}), veio {texto(page, '#simValor')!r}"
    sem_erros(page)


def simulador_resumo_aria_live(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    cenario(page, "queda")
    resumo = texto(page, "#simResumo")
    assert resumo.startswith("Cenário de queda: R$"), f"Resumo errado: {resumo!r}"
    sem_erros(page)


def simulador_movimento_reduzido(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900}, reduced_motion="reduce")
    ir_para_simulador(page)
    alta = numero(texto(page, "#simValor"))
    imediato = cenario(page, "queda", espera=0.05)
    time.sleep(1)
    assert imediato == numero(texto(page, "#simValor")) != alta, "Com movimento reduzido o valor deveria mudar na hora"
    sem_erros(page)


def simulador_arrastar_sem_travar(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 1280, "height": 900})
    ir_para_simulador(page)
    inicial = texto(page, "#simValor")
    page.evaluate(
        """() => { window.__longas = [];
                   new PerformanceObserver(l => l.getEntries().forEach(e => __longas.push(Math.round(e.duration))))
                       .observe({ type: 'longtask' }); }"""
    )
    page.locator('#simSliders input[data-ticker="PETR4"]').scroll_into_view_if_needed()
    time.sleep(0.3)
    caixa = page.locator('#simSliders input[data-ticker="PETR4"]').bounding_box()
    y = caixa["y"] + caixa["height"] / 2
    page.mouse.move(caixa["x"] + 2, y)
    page.mouse.down()
    page.mouse.move(caixa["x"] + caixa["width"] - 2, y, steps=30)
    page.mouse.up()
    time.sleep(0.8)
    longas = page.evaluate("__longas")
    assert len(longas) <= 1, f"Tarefas longas ao arrastar: {longas}"
    assert texto(page, "#simValor") != inicial, "O valor deveria acompanhar o slider"
    sem_erros(page)


def simulador_sem_overflow_no_celular(browser: Browser) -> None:
    page = abrir(browser, viewport={"width": 390, "height": 844})
    ir_para_simulador(page)
    largura = page.evaluate("[document.documentElement.scrollWidth, innerWidth]")
    assert largura[0] <= largura[1], f"Scroll horizontal no simulador: {largura}"
    for entrada in page.locator("#experimente input").all():
        caixa = entrada.bounding_box()
        assert caixa and caixa["x"] >= 0 and caixa["x"] + caixa["width"] <= 390, f"Controle fora da tela: {caixa}"
    sem_erros(page)


def screenshots_simulador(browser: Browser) -> None:
    pasta = os.path.join("target", "landing-screens")
    os.makedirs(pasta, exist_ok=True)
    for largura, altura in ((1280, 900), (768, 1024), (390, 844)):
        page = abrir(browser, viewport={"width": largura, "height": altura})
        ir_para_simulador(page)
        cenario(page, "alta")
        page.locator("#experimente").screenshot(path=os.path.join(pasta, f"simulador-{largura}.png"))
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
    ponteiro_vertical_segue_mouse,
    abertura_funciona_em_intl_antigo,
    toque_nao_inclina,
    teclado_foca_botoes_visiveis,
    movimento_reduzido_mostra_estado_final,
    cabecalho_marca_secao_ativa,
    simulador_estado_inicial,
    simulador_limita_soma_em_100_mil,
    simulador_teclado_respeita_limite,
    simulador_escolhe_ate_4_acoes,
    simulador_desmarcar_devolve_ao_caixa,
    simulador_converte_acoes_eua,
    simulador_tudo_em_caixa_nao_varia,
    simulador_cenarios_ordenados,
    simulador_deterministico,
    simulador_troca_rapida_de_cenario,
    simulador_resumo_aria_live,
    simulador_movimento_reduzido,
    simulador_arrastar_sem_travar,
    simulador_sem_overflow_no_celular,
    screenshots_simulador,
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
