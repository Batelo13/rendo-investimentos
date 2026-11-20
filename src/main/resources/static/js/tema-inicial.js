// Aplica o tema salvo antes do primeiro paint (script sincrono no <head>, antes
// dos stylesheets). Arquivo externo em vez de inline: a CSP usa script-src 'self'.
if (localStorage.getItem('rendo-theme') === 'light') {
    document.documentElement.setAttribute('data-theme', 'light');
}
