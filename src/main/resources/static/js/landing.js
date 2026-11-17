/*
 * Pagina inicial: jornada controlada pelo scroll.
 *
 * Um progresso p (0..1) sai da posicao da pagina dentro de #jornada e e
 * interpolado a cada quadro (requestAnimationFrame so roda enquanto ha
 * movimento). Esse mesmo p dirige:
 *   - o video, quando configurado: currentTime = p * duration;
 *   - a cena ilustrada em SVG (fallback sem video): barras, personagem,
 *     linha de crescimento, ceu e cidade;
 *   - os quatro momentos de texto.
 * Com prefers-reduced-motion nada e sincronizado: a cena fica no estado final
 * e o CSS mostra os momentos como texto em fluxo normal.
 */
(() => {
    const secao = document.getElementById('jornada');
    if (!secao) return;

    const SVG = 'http://www.w3.org/2000/svg';
    const reduzMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const celular = window.matchMedia('(max-width: 767px)');
    const header = document.getElementById('lpHeader');
    const dica = document.getElementById('jornadaDica');
    const barraProgresso = document.getElementById('jornadaBarra');
    const video = document.getElementById('jornadaVideo');
    const momentos = [...secao.querySelectorAll('.momento')].map((el) => ({
        el,
        inicio: Number(el.dataset.inicio),
        fim: Number(el.dataset.fim),
    }));

    const limitar = (v, min = 0, max = 1) => Math.min(max, Math.max(min, v));
    const suave = (a, b, v) => { const t = limitar((v - a) / (b - a)); return t * t * (3 - 2 * t); };
    const misturar = (a, b, t) => a + (b - a) * t;

    /* ------------------------------ Cena SVG ------------------------------ */
    const cena = (() => {
        const $ = (id) => document.getElementById(id);
        const svg = $('jornadaCena');
        const criar = (tag, attrs, pai) => {
            const el = document.createElementNS(SVG, tag);
            for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v);
            pai.appendChild(el);
            return el;
        };

        // Degraus: barras de um grafico, cada vez mais altas.
        const BASE = 820, LARGURA = 64, PASSO = 86, X0 = 800;
        const alturas = [70, 118, 172, 232, 298, 368, 444, 524];
        const degraus = alturas.map((h, i) => {
            const x = X0 + i * PASSO;
            const g = criar('g', { class: 'degrau' }, $('cenaBarras'));
            criar('rect', { x: x + LARGURA, y: BASE - h + 7, width: 11, height: h - 7, fill: 'url(#barraLado)' }, g);
            criar('rect', { x, y: BASE - h, width: LARGURA, height: h, rx: 2, fill: 'url(#barraFrente)' }, g);
            criar('rect', { x, y: BASE - h, width: LARGURA, height: 2.5, fill: '#9CF2D5', opacity: 0.8 }, g);
            return { g, cx: x + LARGURA / 2, topo: BASE - h, h };
        });

        const pontosLinha = [...degraus.map((d) => `${d.cx},${d.topo}`), '1560,200', '1720,150'].join(' ');
        $('cenaLinha').setAttribute('points', pontosLinha);
        $('cenaLinhaBrilho').setAttribute('points', pontosLinha);

        // Grade discreta com marcadores dourados (momento "evolucao").
        const grade = $('cenaGrade');
        [260, 400, 540, 680].forEach((y, i) => {
            criar('line', { x1: 720, x2: 1580, y1: y, y2: y, stroke: '#DCE6E2', 'stroke-opacity': 0.07, 'stroke-dasharray': '2 8' }, grade);
            criar('circle', { cx: 760 + i * 230, cy: y, r: 2.5, fill: '#E9C27A', opacity: 0.8 }, grade);
        });

        // Cidade no horizonte, com janelas acesas.
        const cidade = $('cenaCidade');
        let semente = 7;
        const aleatorio = () => ((semente = (semente * 9301 + 49297) % 233280) / 233280);
        const predios = [];
        for (let x = 640; x < 1700; ) {
            const w = 34 + aleatorio() * 46;
            const h = 120 + aleatorio() * 260 * (0.55 + 0.45 * ((x - 640) / 1060));
            predios.push({ x, w, h });
            x += w + 4 + aleatorio() * 10;
        }
        predios.forEach(({ x, w, h }) => {
            criar('rect', { x, y: 900 - h, width: w, height: h, fill: '#0C1314', stroke: 'rgba(233,194,122,0.18)', 'stroke-width': 1 }, cidade);
            for (let wy = 900 - h + 14; wy < 880; wy += 18) {
                for (let wx = x + 7; wx < x + w - 8; wx += 12) {
                    if (aleatorio() > 0.72) criar('rect', { x: wx, y: wy, width: 4, height: 6, fill: '#F2CF8A', opacity: (0.35 + aleatorio() * 0.5).toFixed(2) }, cidade);
                }
            }
        });

        const estrelas = $('cenaEstrelas');
        for (let i = 0; i < 70; i++) {
            criar('circle', { cx: (aleatorio() * 1800 - 100).toFixed(0), cy: (aleatorio() * 560).toFixed(0), r: (0.6 + aleatorio() * 1.3).toFixed(1), opacity: (0.2 + aleatorio() * 0.6).toFixed(2) }, estrelas);
        }

        const mundo = $('cenaMundo'), pessoa = $('cenaPessoa');
        const pernaFrente = $('pernaFrente'), pernaTras = $('pernaTras');
        const bracoFrente = $('bracoFrente'), bracoTras = $('bracoTras');
        const linha = $('cenaLinha'), linhaBrilho = $('cenaLinhaBrilho');
        const aurora = $('cenaAurora'), sol = $('cenaSol');

        // Retrato/celular (mesma regra do CSS): a tela mostra so uma faixa
        // estreita da cena, entao a camera segue o personagem o tempo todo e o
        // texto fica na parte de baixo.
        const retratoMq = window.matchMedia('(max-width: 767px), (max-aspect-ratio: 9/10)');
        let retrato = retratoMq.matches;
        function enquadrar() {
            retrato = retratoMq.matches;
        }

        // Personagem: figura clara na noite, silhueta escura contra o amanhecer.
        const corPessoa = (t) => {
            const de = [226, 231, 228], ate = [11, 14, 16];
            return `rgb(${de.map((c, k) => Math.round(misturar(c, ate[k], t))).join(',')})`;
        };
        const figura = svg.querySelector('.pessoa');

        function atualizar(p) {
            // Subida: degraus 0 -> 7 entre 6% e 76% do progresso.
            const s = limitar((p - 0.06) / 0.70) * (degraus.length - 1);
            const i = Math.min(Math.floor(s), degraus.length - 2);
            const f = s - i;
            const passo = f < 1 ? f * f * (3 - 2 * f) : 1;
            const a = degraus[i], b = degraus[i + 1];
            const x = misturar(a.cx, b.cx, passo);
            const y = misturar(a.topo, b.topo, passo) - Math.sin(Math.PI * f) * 30;

            // Degraus surgem um pouco a frente do personagem.
            degraus.forEach((d, k) => {
                const cresce = k < 3 ? 1 : suave(k - 2.8, k - 1.6, s);
                d.g.setAttribute('transform', `translate(0 ${BASE}) scale(1 ${Math.max(cresce, 0.001)}) translate(0 ${-BASE})`);
                d.g.setAttribute('opacity', (misturar(1, 0.16, suave(0.8, 0.95, p)) * (0.25 + 0.75 * cresce)).toFixed(3));
            });

            const ciclo = Math.sin(f * Math.PI * 2) * (p < 0.06 || p > 0.76 ? 0 : 1);
            pessoa.setAttribute('transform', `translate(${x.toFixed(1)} ${y.toFixed(1)})`);
            pernaFrente.setAttribute('transform', `rotate(${(ciclo * 26).toFixed(1)} 0 -28)`);
            pernaTras.setAttribute('transform', `rotate(${(-ciclo * 26).toFixed(1)} 0 -28)`);
            bracoFrente.setAttribute('transform', `rotate(${(-ciclo * 22).toFixed(1)} 0 -53)`);
            bracoTras.setAttribute('transform', `rotate(${(ciclo * 22).toFixed(1)} 0 -53)`);

            // Escada vira linha de crescimento ate a cidade.
            const traco = (1 - suave(0.76, 0.93, p)).toFixed(4);
            linha.setAttribute('stroke-dashoffset', traco);
            linhaBrilho.setAttribute('stroke-dashoffset', traco);

            const amanhecer = suave(0.6, 1, p);
            figura.setAttribute('fill', corPessoa(suave(0.7, 0.9, p)));
            aurora.setAttribute('opacity', amanhecer.toFixed(3));
            sol.setAttribute('opacity', (amanhecer * 0.9).toFixed(3));
            sol.setAttribute('cy', misturar(760, 600, amanhecer).toFixed(1));
            estrelas.setAttribute('opacity', (1 - amanhecer).toFixed(3));
            grade.setAttribute('opacity', (suave(0.42, 0.52, p) * (1 - suave(0.74, 0.84, p))).toFixed(3));
            cidade.setAttribute('opacity', suave(0.7, 0.92, p).toFixed(3));
            cidade.setAttribute('transform', `translate(0 ${misturar(70, 0, suave(0.7, 0.95, p)).toFixed(1)})`);

            // Camera: aproxima e acompanha na subida, afasta no final para
            // revelar a cidade. (tx, ty) = ponto da tela onde fica o foco.
            let escala, fx, fy, tx, ty;
            if (retrato) {
                const fim = suave(0.78, 1, p);
                escala = misturar(1, 0.62, fim);
                fx = misturar(x + 40, 1250, fim);
                fy = misturar(y - 30, 560, fim);
                tx = 800; ty = 360;
            } else {
                const foco = suave(0.08, 0.3, p) * (1 - suave(0.7, 0.95, p));
                escala = misturar(1, 1.22, foco) * misturar(1, 0.94, suave(0.82, 1, p));
                fx = misturar(1150, x, foco);
                fy = misturar(470, y - 40, foco);
                tx = 1150; ty = 470;
            }
            mundo.setAttribute('transform', `translate(${tx} ${ty}) scale(${escala.toFixed(4)}) translate(${(-fx).toFixed(1)} ${(-fy).toFixed(1)})`);
        }

        enquadrar();
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

    /* ------------------------------- Video -------------------------------- */
    let duracao = 0;
    let usarVideo = false;

    function carregarVideo() {
        const desktop = secao.dataset.video;
        const mobile = secao.dataset.videoMobile;
        const economia = navigator.connection && navigator.connection.saveData;
        if (!desktop || economia || reduzMovimento || typeof video.canPlayType !== 'function') return;

        const fontes = celular.matches && mobile ? [mobile, desktop] : [desktop];
        let indice = 0;
        const tentar = () => {
            video.src = fontes[indice];
            video.preload = 'metadata';
            video.load();
        };
        video.addEventListener('loadedmetadata', () => {
            if (!Number.isFinite(video.duration) || video.duration <= 0) return;
            duracao = video.duration;
            usarVideo = true;
            video.preload = 'auto';
            secao.classList.add('com-video');
            pedirQuadro();
        });
        video.addEventListener('error', () => {
            indice += 1;
            if (indice < fontes.length) tentar();
            else { usarVideo = false; secao.classList.remove('com-video'); }
        });
        tentar();
    }

    function posicionarVideo(p) {
        if (!usarVideo || !duracao) return false;
        const alvo = Math.min(p * duracao, duracao - 0.05);
        if (video.seeking) return true; // tenta de novo no proximo quadro
        if (Math.abs(video.currentTime - alvo) > 1 / 60) video.currentTime = alvo;
        return false;
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
        return posicionarVideo(p);
    }

    function passo() {
        quadro = 0;
        atual += (alvo - atual) * 0.14;
        if (Math.abs(alvo - atual) < 0.0004) atual = alvo;
        const pendente = desenhar(atual);
        if (atual !== alvo || pendente) quadro = requestAnimationFrame(passo);
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
    // Video so depois da pagina pronta: nao disputa banda com o carregamento inicial.
    window.addEventListener('rendo:page-ready', carregarVideo, { once: true });
    if (document.documentElement.classList.contains('page-ready')) carregarVideo();
})();
