// Overlay de loading (fragments/loading.html) fica visivel por padrao no
// HTML -- isso resolve o splash de carregamento inicial: some assim que a
// pagina termina de carregar. login.js reusa o mesmo #loadingOverlay
// (mostrando/escondendo a classe "hidden") pra dar feedback durante o
// submit de login/cadastro.
window.addEventListener('load', () => {
    document.getElementById('loadingOverlay')?.classList.add('hidden');
});
