/*
 * Sistema de movimento compartilhado (landing, login e dashboard).
 *
 * Comportamentos por atributo, todos com prefers-reduced-motion respeitado:
 *   data-motion                 revela ao entrar na tela.
 *   data-contador="100000"      conta de 0 ate o valor ao entrar na tela. O HTML ja
 *     traz o valor final (SEO e sem JS); data-formato = brl | brl0 | inteiro.
 *   data-luz                    reflexo que segue o ponteiro (--luz-x/--luz-y, CSS em glass.css).
 *   data-inclina="6"            inclinacao maxima em graus na direcao do ponteiro;
 *     data-inclina-area="#id"   area que escuta o ponteiro (padrao: o proprio elemento),
 *     que recebe --mx/--my (-1..1) para parallax em CSS.
 * Luz e inclinacao so com mouse (hover + pointer fine): toque nao inclina nada.
 */
(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const ponteiroFino = window.matchMedia('(hover: hover) and (pointer: fine)');
    const animated = new WeakSet();

    const formatos = {
        brl: new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }),
        // minimumFractionDigits explicito: sem ele, engines sem NumberFormat v3 lancam RangeError.
        brl0: new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 0, maximumFractionDigits: 0 }),
        inteiro: new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }),
    };
    const formatar = (valor, formato = 'inteiro') => (formatos[formato] || formatos.inteiro).format(valor);

    window.RendoMotion = { formatar, reduzMovimento: reducedMotion.matches };

    function reveal(element, index) {
        if (animated.has(element)) return;
        animated.add(element);
        if (reducedMotion.matches || typeof element.animate !== 'function') return;

        element.animate(
            [
                { opacity: 0.35, transform: 'translateY(12px)', filter: 'blur(6px)' },
                { opacity: 1, transform: 'translateY(0)', filter: 'blur(0)' }
            ],
            {
                duration: 520,
                delay: Math.min(index, 6) * 45,
                easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
                fill: 'both'
            }
        );
    }

    /* ------------------------------ Contador ------------------------------ */
    function contar(element) {
        const final = Number(element.dataset.contador);
        const formato = element.dataset.formato;
        const inicio = performance.now();
        const quadro = (agora) => {
            const t = Math.min(1, (agora - inicio) / 1200);
            const suave = 1 - (1 - t) ** 3; // easeOutCubic
            element.textContent = formatar(final * suave, formato);
            if (t < 1) requestAnimationFrame(quadro);
        };
        requestAnimationFrame(quadro);
    }

    function prepararContadores() {
        const contadores = [...document.querySelectorAll('[data-contador]')];
        if (!contadores.length || reducedMotion.matches || !('IntersectionObserver' in window)) return;
        contadores.forEach((el) => { el.textContent = formatar(0, el.dataset.formato); });
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) return;
                observer.unobserve(entry.target);
                contar(entry.target);
            });
        }, { threshold: 0.6 });
        contadores.forEach((el) => observer.observe(el));
    }

    /* ------------------------ Luz e inclinacao ---------------------------- */
    // Agrupa eventos do ponteiro num unico quadro por elemento.
    function porQuadro(fn) {
        let pendente = 0, ultimo = null;
        return (evento) => {
            ultimo = evento;
            if (!pendente) pendente = requestAnimationFrame(() => { pendente = 0; fn(ultimo); });
        };
    }

    function ligarLuz(el) {
        el.addEventListener('pointermove', porQuadro((e) => {
            const r = el.getBoundingClientRect();
            el.style.setProperty('--luz-x', `${(e.clientX - r.left).toFixed(0)}px`);
            el.style.setProperty('--luz-y', `${(e.clientY - r.top).toFixed(0)}px`);
            el.classList.add('com-luz');
        }));
        el.addEventListener('pointerleave', () => el.classList.remove('com-luz'));
    }

    function ligarInclinacao(el) {
        const maximo = Number(el.dataset.inclina) || 4;
        const area = (el.dataset.inclinaArea && document.querySelector(el.dataset.inclinaArea)) || el;
        const aplicar = (mx, my) => {
            area.style.setProperty('--mx', mx.toFixed(3));
            area.style.setProperty('--my', my.toFixed(3));
            el.style.transform = `perspective(1200px) rotateX(${(-my * maximo).toFixed(2)}deg) rotateY(${(mx * maximo).toFixed(2)}deg)`;
        };
        area.addEventListener('pointermove', porQuadro((e) => {
            const r = area.getBoundingClientRect();
            const mx = Math.max(-1, Math.min(1, ((e.clientX - r.left) / r.width) * 2 - 1));
            const my = Math.max(-1, Math.min(1, ((e.clientY - r.top) / r.height) * 2 - 1));
            aplicar(mx, my);
        }));
        area.addEventListener('pointerleave', () => aplicar(0, 0));
    }

    function prepararPonteiro() {
        if (reducedMotion.matches || !ponteiroFino.matches) return;
        document.querySelectorAll('[data-luz]').forEach(ligarLuz);
        document.querySelectorAll('[data-inclina]').forEach(ligarInclinacao);
    }

    let iniciado = false;
    function startMotion() {
        if (iniciado) return;
        iniciado = true;
        prepararContadores();
        prepararPonteiro();

        const targets = [...document.querySelectorAll('[data-motion]')];
        if (reducedMotion.matches || !('IntersectionObserver' in window)) {
            targets.forEach((element, index) => reveal(element, index));
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) return;
                const index = targets.indexOf(entry.target);
                reveal(entry.target, index);
                observer.unobserve(entry.target);
            });
        }, { threshold: 0.08, rootMargin: '0px 0px -4% 0px' });

        targets.forEach((element) => observer.observe(element));
    }

    window.addEventListener('rendo:page-ready', startMotion, { once: true });
    if (document.readyState === 'complete') startMotion();
})();
