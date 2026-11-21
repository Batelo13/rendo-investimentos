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
    // O valor fica visivel no <output> (silenciado) e e lido pelo proprio slider
    // via aria-valuetext; quem anuncia o resultado e so o #simResumo.
    function rotular(saida, entrada, ticker) {
        const v = estado.alocado.get(ticker);
        saida.textContent = porTicker.get(ticker).mercado === 'EUA'
            ? `${brl0(v)} ≈ ${usd.format(v / DOLAR)}`
            : brl0(v);
        entrada.setAttribute('aria-valuetext', saida.textContent);
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
        saida.setAttribute('aria-live', 'off'); // <output> e regiao viva por padrao
        entrada.addEventListener('input', () => {
            // Nunca passa do saldo livre: o proprio slider e reescrito com o limite.
            const livre = TOTAL - soma(ticker);
            let v = Number(entrada.value);
            if (v > livre) { v = livre; entrada.value = String(v); }
            estado.alocado.set(ticker, v);
            rotular(saida, entrada, ticker);
            atualizarControles();
            redesenhar();
        });
        rotular(saida, entrada, ticker);
        linha.append(rotulo, entrada, saida);
        return linha;
    }

    function montarSliders() {
        areaSliders.replaceChildren(...[...estado.alocado.keys()].map(criarSlider));
    }

    /* --------------------------------- Resultado --------------------------------- */
    const SVG = 'http://www.w3.org/2000/svg';
    const CORES = ['#5DE2BA', '#2E9E7B', '#D4B07A', '#8FA79F'];
    const reduzMovimento = window.RendoMotion.reduzMovimento;
    const svg = (tag, attrs, pai) => {
        const el = document.createElementNS(SVG, tag);
        for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v);
        if (pai) pai.appendChild(el);
        return el;
    };

    // Modelo ilustrativo: cada acao segue o indice do cenario na medida do seu beta.
    function serie() {
        const caixaLivre = TOTAL - soma();
        return MERCADO[estado.cenario].map((m, t) => {
            let v = caixaLivre;
            estado.alocado.forEach((aloc, ticker) => {
                const { beta, k } = porTicker.get(ticker);
                v += aloc * (1 + beta * (m - 1) + 0.006 * Math.sin(t * (1.1 + 0.3 * k)));
            });
            return v;
        });
    }

    // Grafico 560x220, eixo Y fixo (85.000 a 120.000) para comparar cenarios.
    const grafico = $('simGrafico');
    const X0 = 24, X1 = 544, Y_TOPO = 16, Y_BASE = 188, V_MIN = 85000, V_MAX = 120000;
    const xDe = (i) => X0 + (i * (X1 - X0)) / 12;
    const yDe = (v) => Y_BASE - ((Math.min(V_MAX, Math.max(V_MIN, v)) - V_MIN) / (V_MAX - V_MIN)) * (Y_BASE - Y_TOPO);
    svg('line', { class: 'sim-referencia', x1: X0, x2: X1, y1: yDe(TOTAL), y2: yDe(TOTAL) }, grafico);
    svg('text', { class: 'sim-eixo', x: X0, y: yDe(TOTAL) + 16 }, grafico).textContent = 'R$ 100 mil';
    const area = svg('path', { class: 'sim-area' }, grafico);
    const linha = svg('polyline', { class: 'sim-linha' }, grafico);
    svg('text', { class: 'sim-eixo', x: X0, y: 212 }, grafico).textContent = 'mês 0';
    svg('text', { class: 'sim-eixo', x: X1, y: 212, 'text-anchor': 'end' }, grafico).textContent = 'mês 12';

    function desenharGrafico(valores) {
        const pontos = valores.map((v, i) => `${xDe(i).toFixed(1)},${yDe(v).toFixed(1)}`);
        linha.setAttribute('points', pontos.join(' '));
        area.setAttribute('d', `M${X0},${Y_BASE} L${pontos.join(' L')} L${X1},${Y_BASE} Z`);
    }

    const anel = $('simAnel');
    function desenharAnel() {
        const fatias = [...estado.alocado].map(([, v], i) => [v / TOTAL, CORES[i]]);
        fatias.push([(TOTAL - soma()) / TOTAL, 'rgba(255,255,255,0.12)']);
        let inicio = 0;
        anel.replaceChildren(...fatias.filter(([f]) => f > 0).map(([f, cor]) => {
            const c = svg('circle', {
                cx: 60, cy: 60, r: 48, pathLength: 1, stroke: cor,
                'stroke-dasharray': `${f.toFixed(4)} 1`, 'stroke-dashoffset': (-inicio).toFixed(4),
            });
            inicio += f;
            return c;
        }));
        const ordem = [...estado.alocado.keys()];
        chips.forEach((b, ticker) => {
            const i = ordem.indexOf(ticker);
            b.style.setProperty('--cor', i >= 0 ? CORES[i] : 'transparent');
        });
    }

    // Transicao: parte do desenho atual (mesmo no meio de outra) ate o alvo, 500 ms.
    const valor = $('simValor');
    let desenhado = serie(), alvo = desenhado, de = desenhado, inicioAnim = 0, quadro = 0;
    function mostrar(valores) {
        desenhado = valores;
        desenharGrafico(valores);
        valor.textContent = brl0(valores[12]); // sem centavos: e ilustracao, nao calculo de retorno
    }
    function animar(agora) {
        const t = Math.min(1, (agora - inicioAnim) / 500);
        const e = 1 - (1 - t) ** 3;
        mostrar(t < 1 ? de.map((v, i) => v + (alvo[i] - v) * e) : alvo);
        quadro = t < 1 ? requestAnimationFrame(animar) : 0;
    }

    // Leitor de tela: anuncia so depois que o visitante para de mexer.
    const resumo = $('simResumo');
    const NOME_CENARIO = { alta: 'de alta', lateral: 'lateral', queda: 'de queda' };
    let espera = 0;
    function anunciar() {
        clearTimeout(espera);
        espera = setTimeout(() => {
            const n = estado.alocado.size;
            resumo.textContent = `Cenário ${NOME_CENARIO[estado.cenario]}: ${brl0(alvo[12])} após 12 meses, com ${n} ${n === 1 ? 'ação' : 'ações'} e ${brl0(TOTAL - soma())} em caixa.`;
        }, 400);
    }

    function redesenhar() {
        alvo = serie();
        desenharAnel();
        anunciar();
        if (reduzMovimento) { cancelAnimationFrame(quadro); quadro = 0; mostrar(alvo); return; }
        de = desenhado;
        inicioAnim = performance.now();
        if (!quadro) quadro = requestAnimationFrame(animar);
    }

    const cenarios = $('simCenarios');
    cenarios.addEventListener('change', (e) => {
        if (e.target.name !== 'cenario') return;
        estado.cenario = e.target.value;
        redesenhar();
    });
    const marcado = cenarios.querySelector('input:checked');
    if (marcado) estado.cenario = marcado.value;
    mostrar(serie());

    montarSliders();
    atualizarControles();
    redesenhar();
})();
