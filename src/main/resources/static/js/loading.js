window.addEventListener('load', () => {
    document.documentElement.classList.add('page-ready');
    document.getElementById('loadingOverlay')?.classList.add('hidden');
    window.dispatchEvent(new CustomEvent('rendo:page-ready'));
}, { once: true });
