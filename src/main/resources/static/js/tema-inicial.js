// Aplica o tema salvo antes do primeiro paint (script sincrono no <head>, antes
// dos stylesheets). Arquivo externo em vez de inline: a CSP usa script-src 'self'.
// "js" no <html>: a landing so esconde o estado final do cartao (para animar)
// quando ha JavaScript; sem JS o HTML ja mostra o cartao pronto.
document.documentElement.classList.add('js');
if (localStorage.getItem('rendo-theme') === 'light') {
    document.documentElement.setAttribute('data-theme', 'light');
}
