/*
 * Simulador demonstrativo da pagina inicial (secao #experimente).
 * Tudo roda no navegador: nada e enviado nem salvo. Os numeros sao
 * ilustrativos e a secao mostra os rotulos "Dados demonstrativos" e
 * "Serie ilustrativa: nao e historico real nem previsao."
 */
(() => {
    const secao = document.getElementById('experimente');
    if (!secao) return;

    /* --------------------------- Dados demonstrativos --------------------------- */
    const ACOES = [ // [ticker, nome, mercado, preco demo, beta]
        ['PETR4', 'Petrobras PN', 'BR', 38.50, 1.3],
        ['VALE3', 'Vale ON', 'BR', 61.20, 1.2],
        ['ITUB4', 'Itaú Unibanco PN', 'BR', 33.80, 0.9],
        ['WEGE3', 'WEG ON', 'BR', 41.70, 0.8],
        ['AAPL', 'Apple', 'EUA', 228.00, 1.1],
        ['MSFT', 'Microsoft', 'EUA', 415.00, 1.0],
    ];
    const DOLAR = 5.40;
    const MERCADO = { // indice ilustrativo do mercado, 13 pontos (mes 0..12)
        alta: [1, 1.012, 1.004, 1.026, 1.041, 1.035, 1.058, 1.072, 1.066, 1.089, 1.103, 1.097, 1.124],
        lateral: [1, 1.008, 0.994, 1.006, 1.012, 0.998, 1.004, 1.015, 1.001, 0.992, 1.006, 1.011, 1.003],
        queda: [1, 0.991, 0.978, 0.986, 0.962, 0.955, 0.941, 0.948, 0.929, 0.918, 0.927, 0.909, 0.896],
    };
    const INICIAL = { PETR4: 40000, VALE3: 25000, WEGE3: 20000 }; // caixa 15.000
    const TOTAL = 100000;
    const MAX_ACOES = 4;

    const $ = (id) => document.getElementById(id);
    const brl0 = (v) => window.RendoMotion.formatar(v, 'brl0');
    const usd = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 });
    const porTicker = new Map(ACOES.map((a, k) => [a[0], { nome: a[1], mercado: a[2], beta: a[4], k }]));
    const criar = (tag, classe, texto) => {
        const el = document.createElement(tag);
        if (classe) el.className = classe;
        if (texto !== undefined) el.textContent = texto;
        return el;
    };

    // Map preserva a ordem de escolha (usada para as cores do anel na Task 3).
    const estado = { alocado: new Map(Object.entries(INICIAL)), cenario: 'alta' };
    const soma = (exceto) => [...estado.alocado].reduce((t, [k, v]) => (k === exceto ? t : t + v), 0);

    const areaAcoes = $('simAcoes'), areaSliders = $('simSliders'), caixa = $('simCaixa'), aviso = $('simAviso');

    /* ------------------------------ Escolha das acoes ------------------------------ */
    const chips = new Map();
    ACOES.forEach(([ticker, nome, mercado]) => {
        const b = criar('button', 'sim-chip');
        b.type = 'button';
        b.dataset.ticker = ticker;
        b.append(criar('b', '', ticker), criar('span', '', nome), criar('small', '', mercado));
        b.addEventListener('click', () => alternar(ticker));
        areaAcoes.append(b);
        chips.set(ticker, b);
    });

    function alternar(ticker) {
        if (estado.alocado.has(ticker)) estado.alocado.delete(ticker);
        else if (estado.alocado.size < MAX_ACOES) estado.alocado.set(ticker, 0);
        montarSliders();
        atualizarControles();
        redesenhar();
    }

    function atualizarControles() {
        const cheio = estado.alocado.size >= MAX_ACOES;
        chips.forEach((b, ticker) => {
            const escolhido = estado.alocado.has(ticker);
            b.setAttribute('aria-pressed', String(escolhido));
            b.disabled = cheio && !escolhido;
        });
        aviso.hidden = !cheio;
        caixa.textContent = `Caixa: ${brl0(TOTAL - soma())}`;
    }

    /* ------------------------------- Distribuicao -------------------------------- */
    function rotular(saida, ticker) {
        const v = estado.alocado.get(ticker);
        saida.textContent = porTicker.get(ticker).mercado === 'EUA'
            ? `${brl0(v)} ≈ ${usd.format(v / DOLAR)}`
            : brl0(v);
    }

    function criarSlider(ticker) {
        const linha = criar('div', 'sim-slider');
        const id = `simSlider${ticker}`;
        const rotulo = criar('label', '', ticker);
        rotulo.htmlFor = id;
        const entrada = criar('input');
        Object.assign(entrada, { type: 'range', id, min: 0, max: TOTAL, step: 1000, value: estado.alocado.get(ticker) });
        entrada.dataset.ticker = ticker;
        const saida = criar('output');
        saida.dataset.valorDe = ticker;
        saida.htmlFor = id;
        entrada.addEventListener('input', () => {
            // Nunca passa do saldo livre: o proprio slider e reescrito com o limite.
            const livre = TOTAL - soma(ticker);
            let v = Number(entrada.value);
            if (v > livre) { v = livre; entrada.value = String(v); }
            estado.alocado.set(ticker, v);
            rotular(saida, ticker);
            atualizarControles();
            redesenhar();
        });
        rotular(saida, ticker);
        linha.append(rotulo, entrada, saida);
        return linha;
    }

    function montarSliders() {
        areaSliders.replaceChildren(...[...estado.alocado.keys()].map(criarSlider));
    }

    /* --------------------------------- Resultado --------------------------------- */
    function redesenhar() {}

    montarSliders();
    atualizarControles();
    redesenhar();
})();
