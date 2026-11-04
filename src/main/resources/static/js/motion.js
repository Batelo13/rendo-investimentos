(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const animated = new WeakSet();

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

    function startMotion() {
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
