/*
 * Pagina inicial: jornada controlada pelo scroll.
 *
 * Um progresso p (0..1) sai da posicao da pagina dentro de #jornada e e
 * interpolado a cada quadro (requestAnimationFrame so roda enquanto ha
 * movimento). Esse mesmo p dirige:
 *   - a cena da carteira demonstrativa: saldo, alocacao, linha de evolucao
 *     e, no fim, os cartoes de posicoes e operacoes;
 *   - os quatro momentos de texto.
 * Com prefers-reduced-motion nada e sincronizado: a cena fica no estado final
 * e o CSS mostra os momentos como texto em fluxo normal.
 */
(() => {
    const secao = document.getElementById('jornada');
    if (!secao) return;

    const SVG = 'http://www.w3.org/2000/svg';
    const reduzMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const header = document.getElementById('lpHeader');
    const dica = document.getElementById('jornadaDica');
    const barraProgresso = document.getElementById('jornadaBarra');
    const momentos = [...secao.querySelectorAll('.momento')].map((el) => ({
        el,
        inicio: Number(el.dataset.inicio),
        fim: Number(el.dataset.fim),
    }));

    const limitar = (v, min = 0, max = 1) => Math.min(max, Math.max(min, v));
    const suave = (a, b, v) => { const t = limitar((v - a) / (b - a)); return t * t * (3 - 2 * t); };
    const misturar = (a, b, t) => a + (b - a) * t;

    /* ------------------------------- Cena -------------------------------- */
    // Carteira demonstrativa: todos os numeros sao ilustrativos e o cartao
    // mostra o selo "Dados demonstrativos".
    const cena = (() => {
        const $ = (id) => document.getElementById(id);
        const criar = (tag, attrs, pai) => {
            const el = document.createElementNS(SVG, tag);
            for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v);
            pai.appendChild(el);
            return el;
        };
        const formatar = (v) => window.RendoMotion.formatar(v, 'brl');

        const ALOCACAO = [['PETR4', 0.35], ['VALE3', 0.25], ['WEGE3', 0.20], ['AAPL', 0.20]];
        const SERIE = [100000, 101200, 99800, 102600, 104100, 103000, 105800,
                       107200, 106100, 108900, 110300, 109500, 112400]; // ilustrativa, 12 meses
        const OPERACOES = [[1, 'compra'], [6, 'compra'], [9, 'venda']]; // indice do mes
        const CORES = ['#5DE2BA', '#2E9E7B', '#D4B07A', '#8FA79F'];

        // Grafico: degraus (uma barra por mes) sob a linha de evolucao.
        const X0 = 16, X1 = 464, Y_BASE = 190, Y_MIN = 178, Y_MAX = 26, V_MIN = 98000, V_MAX = 114000;
        const ultimo = SERIE.length - 1;
        const xs = SERIE.map((_, i) => X0 + (i * (X1 - X0)) / ultimo);
        const ys = SERIE.map((v) => Y_MIN - ((v - V_MIN) / (V_MAX - V_MIN)) * (Y_MIN - Y_MAX));
        const degraus = SERIE.map((_, i) => criar('rect', {
            x: (xs[i] - 10).toFixed(1), y: ys[i].toFixed(1), width: 20, height: (Y_BASE - ys[i]).toFixed(1),
            rx: 3, fill: '#5DE2BA', opacity: 0.15,
        }, $('cenaDegraus')));
        const linha = $('cenaLinha');
        linha.setAttribute('points', xs.map((x, i) => `${x.toFixed(1)},${ys[i].toFixed(1)}`).join(' '));
        const marcadores = OPERACOES.map(([i, tipo]) => ({
            i,
            el: criar('circle', {
                cx: xs[i].toFixed(1), cy: ys[i].toFixed(1), r: 5, class: `marcador marcador-${tipo}`,
            }, $('cenaMarcadores')),
        }));

        // Alocacao: fatias do anel e chips com a mesma cor.
        const fatias = [...document.querySelectorAll('#cenaAnel .anel-fatia')];
        const chips = [...document.querySelectorAll('#cenaChips li')];
        let acumulado = 0;
        const inicioFatia = ALOCACAO.map(([, pct]) => { const a = acumulado; acumulado += pct; return a; });
        fatias.forEach((f, i) => { f.style.stroke = CORES[i]; f.style.strokeDashoffset = String(-inicioFatia[i]); });
        chips.forEach((c, i) => c.style.setProperty('--cor', CORES[i]));

        // Horizonte: grade em perspectiva convergindo para o centro da cena.
        const grade = $('fundoGrade');
        for (let k = -12; k <= 12; k++) criar('line', { x1: 1000 + k * 22, y1: 560, x2: 1000 + k * 260, y2: 900 }, grade);
        for (let k = 0; k < 9; k++) {
            const y = 560 + 340 * (k / 8) ** 1.8;
            criar('line', { x1: 0, y1: y.toFixed(1), x2: 1600, y2: y.toFixed(1) }, grade);
        }

        const valor = $('cenaValor'), camera = $('cenaCamera'), amanhecer = $('fundoAmanhecer');
        const satelites = [...document.querySelectorAll('.cena-satelite')];
        // Mesma regra do CSS: satelites so aparecem em telas largas (nao retrato).
        const comSatelites = window.matchMedia('(min-width: 1130px) and (min-aspect-ratio: 9/10)');
        let textoAtual = valor.textContent;

        function atualizar(p) {
            // Valor: parado no planejamento, segue a serie na evolucao.
            const evolucao = suave(0.45, 0.75, p);
            const pos = evolucao * ultimo;
            const i = Math.min(Math.floor(pos), ultimo - 1);
            const v = misturar(SERIE[i], SERIE[i + 1], pos - i);
            const texto = formatar(Math.round(v / 10) * 10);
            if (texto !== textoAtual) { valor.textContent = texto; textoAtual = texto; }

            chips.forEach((c, k) => {
                const t = suave(0.2 + k * 0.04, 0.28 + k * 0.04, p);
                c.style.opacity = t.toFixed(3);
                c.style.transform = `translate3d(0, ${((1 - t) * 24).toFixed(1)}px, 0)`;
            });
            fatias.forEach((f, k) => {
                const t = suave(0.25 + k * 0.04, 0.33 + k * 0.04, p);
                f.style.strokeDasharray = `${(ALOCACAO[k][1] * t).toFixed(4)} 1`;
            });

            linha.style.strokeDashoffset = (1 - evolucao).toFixed(4);
            degraus.forEach((d, k) => {
                d.setAttribute('opacity', (0.15 + 0.5 * suave(0.45 + k * 0.023, 0.5 + k * 0.023, p)).toFixed(3));
            });
            marcadores.forEach((m) => m.el.classList.toggle('aceso', pos >= m.i));

            const fim = suave(0.78, 0.95, p);
            satelites.forEach((s, k) => {
                s.style.opacity = fim.toFixed(3);
                s.style.transform = `translate3d(${((1 - fim) * (k ? -40 : 40)).toFixed(1)}px, 0, 0)`;
            });
            // Fim: o cartao recua; com satelites, abre espaco para a coluna deles.
            const recuo = suave(0.78, 1, p);
            camera.style.transform = comSatelites.matches
                ? `translate3d(${(-60 * recuo).toFixed(1)}px, ${(-30 * recuo).toFixed(1)}px, 0) scale(${misturar(1, 0.8, recuo).toFixed(4)})`
                : `scale(${misturar(1, 0.9, recuo).toFixed(4)})`;
            amanhecer.setAttribute('opacity', (0.35 * suave(0.7, 1, p)).toFixed(3));
        }

        // Layout e todo em CSS; mantido pela interface do laco (resize).
        function enquadrar() {}

        return { atualizar, enquadrar };
    })();

    /* -------------------------- Momentos de texto ------------------------- */
    const FADE = 0.045;
    function opacidadeMomento(m, p, primeiro, ultimo) {
        const entra = primeiro ? 1 : suave(m.inicio - FADE, m.inicio + FADE, p);
        const sai = ultimo ? 1 : 1 - suave(m.fim - FADE, m.fim + FADE, p);
        return Math.min(entra, sai);
    }

    function aplicarMomentos(p) {
        momentos.forEach((m, i) => {
            const o = opacidadeMomento(m, p, i === 0, i === momentos.length - 1);
            const saindo = p > (m.inicio + m.fim) / 2;
            m.el.style.opacity = o.toFixed(3);
            m.el.style.transform = `translate3d(0, ${((1 - o) * (saindo ? -28 : 28)).toFixed(1)}px, 0)`;
            m.el.style.filter = o > 0.99 ? 'none' : `blur(${((1 - o) * 8).toFixed(1)}px)`;
            m.el.classList.toggle('ativo', o > 0.5);
        });
    }

    /* ------------------------------- Laco --------------------------------- */
    let alvo = 0, atual = 0, quadro = 0;

    function progressoDoScroll() {
        const total = secao.offsetHeight - window.innerHeight;
        if (total <= 0) return 0;
        return limitar(-secao.getBoundingClientRect().top / total);
    }

    function desenhar(p) {
        cena.atualizar(p);
        aplicarMomentos(p);
        barraProgresso.style.transform = `scaleX(${p.toFixed(4)})`;
    }

    function passo() {
        quadro = 0;
        atual += (alvo - atual) * 0.14;
        if (Math.abs(alvo - atual) < 0.0004) atual = alvo;
        desenhar(atual);
        if (atual !== alvo) quadro = requestAnimationFrame(passo);
    }

    function pedirQuadro() {
        if (!quadro) quadro = requestAnimationFrame(passo);
    }

    function aoRolar() {
        alvo = progressoDoScroll();
        header.classList.toggle('rolado', window.scrollY > 24);
        dica.classList.toggle('oculta', window.scrollY > 40);
        pedirQuadro();
    }

    function aoRedimensionar() {
        cena.enquadrar();
        aoRolar();
    }

    /* ------------------------------ Cabecalho ----------------------------- */
    // Marca o link da secao que ocupa o meio da tela (vale com movimento reduzido).
    const links = [...header.querySelectorAll('.lp-nav a[href^="#"]')];
    if ('IntersectionObserver' in window && links.length) {
        const alvos = [secao, ...links.map((a) => document.querySelector(a.getAttribute('href'))).filter(Boolean)];
        const observador = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) return;
                const id = `#${entry.target.id}`;
                links.forEach((a) => {
                    if (a.getAttribute('href') === id) a.setAttribute('aria-current', 'true');
                    else a.removeAttribute('aria-current');
                });
            });
        }, { rootMargin: '-45% 0px -50% 0px' });
        alvos.forEach((alvo) => observador.observe(alvo));
    }

    if (reduzMovimento) {
        cena.atualizar(1);
        header.classList.add('rolado');
        return;
    }

    // Teclado: focar um botao de um momento leva a pagina ate a faixa dele,
    // entao nada fica focado e invisivel.
    momentos.forEach((m) => m.el.addEventListener('focusin', () => {
        const total = secao.offsetHeight - window.innerHeight;
        const meio = (m.inicio + m.fim) / 2;
        const destino = secao.offsetTop + total * (m.inicio === 0 ? 0 : meio);
        if (Math.abs(window.scrollY - destino) > 4) window.scrollTo({ top: destino, behavior: 'auto' });
    }));

    const ligar = () => {
        window.addEventListener('scroll', aoRolar, { passive: true });
        window.addEventListener('resize', aoRedimensionar, { passive: true });
    };
    const desligar = () => {
        window.removeEventListener('scroll', aoRolar);
        window.removeEventListener('resize', aoRedimensionar);
        cancelAnimationFrame(quadro);
        quadro = 0;
    };
    ligar();
    window.addEventListener('pagehide', desligar);
    // Voltar pelo cache do navegador (bfcache) restaura a pagina sem recarregar.
    window.addEventListener('pageshow', (e) => { if (e.persisted) { ligar(); aoRolar(); } });

    atual = alvo = progressoDoScroll();
    desenhar(atual);
    aoRolar();
})();
