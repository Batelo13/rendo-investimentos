// Alterna data-theme na <html> e persiste em localStorage. O flash de tema
// errado ja e evitado por um script inline no <head> de cada pagina (roda
// antes deste arquivo, antes do primeiro paint) -- este arquivo so cuida do
// botao de troca, que existe apenas no dashboard.
const THEME_KEY = "rendo-theme";

function temaAtual() {
    return document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
}

function aplicarTema(tema) {
    if (tema === "light") {
        document.documentElement.setAttribute("data-theme", "light");
    } else {
        document.documentElement.removeAttribute("data-theme");
    }
    localStorage.setItem(THEME_KEY, tema);
    atualizarIconeBotao();
}

function atualizarIconeBotao() {
    const btn = document.getElementById("btnTema");
    if (!btn) return;
    const claro = temaAtual() === "light";
    btn.querySelector(".icone-sol").classList.toggle("hidden", !claro);
    btn.querySelector(".icone-lua").classList.toggle("hidden", claro);
    btn.setAttribute("aria-label", claro ? "Mudar para tema escuro" : "Mudar para tema claro");
}

const btnTema = document.getElementById("btnTema");
if (btnTema) {
    btnTema.addEventListener("click", () => {
        aplicarTema(temaAtual() === "light" ? "dark" : "light");
    });
    atualizarIconeBotao();
}
