/*
 * Tour "Como funciona": marca o passo que ocupa o meio da tela (classe "ativo"
 * no artigo e aria-current no indice). O clique no indice e a ancora normal do
 * navegador (scroll-margin-top no CSS). Com movimento reduzido, tudo fica ativo.
 */
(() => {
    const passos = [...document.querySelectorAll('.tour-passo')];
    if (!passos.length) return;
    const links = new Map([...document.querySelectorAll('.tour-indice a')].map((a) => [a.getAttribute('href').slice(1), a]));

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || !('IntersectionObserver' in window)) {
        passos.forEach((p) => p.classList.add('ativo'));
        return;
    }

    function ativar(id) {
        passos.forEach((p) => p.classList.toggle('ativo', p.id === id));
        links.forEach((a, alvo) => {
            if (alvo === id) a.setAttribute('aria-current', 'true');
            else a.removeAttribute('aria-current');
        });
    }

    const observador = new IntersectionObserver((entradas) => {
        entradas.forEach((e) => { if (e.isIntersecting) ativar(e.target.id); });
    }, { rootMargin: '-45% 0px -50% 0px' });
    passos.forEach((p) => observador.observe(p));
})();
