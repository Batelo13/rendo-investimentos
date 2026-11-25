/*
 * Diferenciais da pagina inicial: microdemonstracoes com numeros ilustrativos.
 *   - Cambio: alterna US$/R$ contando do valor exibido ate o novo (600 ms).
 *   - Historico: um input range sobre o grafico percorre 30 dias de cotacao.
 * O preco medio usa data-contador (motion.js).
 */
(() => {
    const botao = document.getElementById('difCambioBotao');
    if (!botao) return;

    const reduzMovimento = window.RendoMotion.reduzMovimento;
    const formatos = {
        USD: new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'USD' }),
        BRL: new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }),
    };

    /* -------------------------------- Cambio --------------------------------- */
    const DOLAR = 5.40, TOTAL_USD = 10 * 228;
    const valor = document.getElementById('difCambioValor');
    const status = document.getElementById('difCambioStatus'); // anuncia so o valor final, nao a contagem
    let moeda = 'USD', exibido = TOTAL_USD, quadro = 0;

    function escrever(v) { exibido = v; valor.textContent = formatos[moeda].format(v); }

    botao.addEventListener('click', () => {
        moeda = moeda === 'USD' ? 'BRL' : 'USD';
        const alvo = moeda === 'USD' ? TOTAL_USD : TOTAL_USD * DOLAR;
        status.textContent = `10 ações da AAPL: ${formatos[moeda].format(alvo)}`;
        botao.textContent = moeda === 'USD' ? 'Ver em reais' : 'Ver em dólares';
        cancelAnimationFrame(quadro);
        if (reduzMovimento) { escrever(alvo); return; }
        // Recomeca do valor exibido: cliques no meio da contagem terminam no alvo exato.
        const de = exibido, inicio = performance.now();
        const passo = (agora) => {
            const t = Math.min(1, (agora - inicio) / 600);
            escrever(t < 1 ? de + (alvo - de) * (1 - (1 - t) ** 3) : alvo);
            quadro = t < 1 ? requestAnimationFrame(passo) : 0;
        };
        quadro = requestAnimationFrame(passo);
    });

    /* ------------------------------- Historico -------------------------------- */
    const SVG = 'http://www.w3.org/2000/svg';
    // Tendencia lenta + oscilacao curta (as duas valem 0 no dia 1: comeca em R$ 38,50).
    const PONTOS = Array.from({ length: 30 }, (_, i) => 38.5 + 1.6 * Math.sin(i / 4.2) + 0.05 * i + 0.35 * Math.sin(i * 2.3));
    const grafico = document.getElementById('difHistoricoGrafico');
    const entrada = document.getElementById('difHistorico');
    const legenda = document.getElementById('difHistoricoTexto');
    const min = Math.min(...PONTOS) - 0.4, max = Math.max(...PONTOS) + 0.4;
    const x = (i) => 8 + (i * 584) / 29;
    const y = (v) => 150 - ((v - min) / (max - min)) * 140;
    const svg = (tag, attrs) => {
        const el = document.createElementNS(SVG, tag);
        for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v);
        grafico.appendChild(el);
        return el;
    };
    const pontos = PONTOS.map((v, i) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`);
    svg('path', { class: 'dif-area', d: `M8,160 L${pontos.join(' L')} L592,160 Z` });
    svg('polyline', { class: 'dif-linha', points: pontos.join(' ') });
    const guia = svg('line', { class: 'dif-guia', y1: 0, y2: 160 });
    const marcador = svg('circle', { r: 5, fill: '#0A0C0D', stroke: '#5DE2BA', 'stroke-width': 2.5 });

    function mostrarDia() {
        const i = Number(entrada.value);
        guia.setAttribute('x1', x(i)); guia.setAttribute('x2', x(i));
        marcador.setAttribute('cx', x(i)); marcador.setAttribute('cy', y(PONTOS[i]));
        const texto = `dia ${i + 1} · ${formatos.BRL.format(PONTOS[i])}`;
        legenda.textContent = texto;
        entrada.setAttribute('aria-valuetext', texto);
    }
    entrada.addEventListener('input', mostrarDia);

    // Mouse percorre so passando; toque percorre ao tocar/arrastar. O range nativo
    // continua para teclado e leitor de tela (no iOS ele so responde ao polegar).
    const area = entrada.parentElement;
    let pendente = 0, ultimoX = 0;
    function seguir(e) {
        if (e.pointerType === 'mouse' && e.type === 'pointerdown') return;
        ultimoX = e.clientX;
        if (pendente) return;
        pendente = requestAnimationFrame(() => {
            pendente = 0;
            const r = area.getBoundingClientRect();
            const i = Math.round(Math.min(1, Math.max(0, (ultimoX - r.left) / r.width)) * 29);
            if (String(i) !== entrada.value) { entrada.value = String(i); mostrarDia(); }
        });
    }
    area.addEventListener('pointermove', seguir);
    area.addEventListener('pointerdown', seguir);
    mostrarDia();
})();
