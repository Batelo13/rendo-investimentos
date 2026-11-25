/*
 * Tour "Como funciona": marca o passo cujo topo ja passou do meio da tela (o
 * ultimo deles), com classe "ativo" no artigo e aria-current no indice. Regra
 * por posicao, recalculada a cada quadro de rolagem: funciona descendo, subindo
 * e em saltos (a faixa estreita do IntersectionObserver marcava o passo errado
 * quando caia na divisa entre dois passos). O clique no indice e a ancora normal
 * do navegador (scroll-margin-top no CSS). Com movimento reduzido, tudo fica ativo.
 */
(() => {
    const passos = [...document.querySelectorAll('.tour-passo')];
    if (!passos.length) return;
    const links = new Map([...document.querySelectorAll('.tour-indice a')].map((a) => [a.getAttribute('href').slice(1), a]));

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        passos.forEach((p) => p.classList.add('ativo'));
        return;
    }

    let ativoAtual = null;
    function ativar(id) {
        if (id === ativoAtual) return;
        ativoAtual = id;
        passos.forEach((p) => p.classList.toggle('ativo', p.id === id));
        links.forEach((a, alvo) => {
            if (alvo === id) a.setAttribute('aria-current', 'true');
            else a.removeAttribute('aria-current');
        });
    }

    function atualizar() {
        const meio = window.innerHeight / 2;
        let atual = passos[0];
        for (const p of passos) {
            if (p.getBoundingClientRect().top <= meio) atual = p;
        }
        ativar(atual.id);
    }

    let pendente = 0;
    const agendar = () => { if (!pendente) pendente = requestAnimationFrame(() => { pendente = 0; atualizar(); }); };
    window.addEventListener('scroll', agendar, { passive: true });
    window.addEventListener('resize', agendar);
    atualizar();
})();
