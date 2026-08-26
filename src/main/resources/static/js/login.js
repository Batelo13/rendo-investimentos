const container = document.getElementById('container');
const btnCadastro = document.getElementById('btn-cadastro');
const btnLogin = document.getElementById('btn-login');
const loadingOverlay = document.getElementById('loadingOverlay');

const onlyDigits = (s) => (s || '').replace(/\D/g, '');

btnCadastro.addEventListener('click', () => container.classList.add('active'));
btnLogin.addEventListener('click', () => container.classList.remove('active'));

// Login (POST /login) e um form comum -- o proprio Spring Security processa.
// Cadastro (POST /usuarios) espera JSON no corpo (@RequestBody), entao um
// post de formulario nativo (application/x-www-form-urlencoded) nao seria
// aceito -- por isso via fetch aqui, unico lugar desta pagina que precisa de
// JavaScript de verdade.
const cadastroForm = document.getElementById('cadastro-form');
const cadastroMensagem = document.getElementById('cadastro-mensagem');
const loginMensagem = document.getElementById('login-mensagem');
const loginEmailInput = document.getElementById('login-email');
const verificacaoForm = document.getElementById('verificacao-form');
const verificacaoMensagem = document.getElementById('verificacao-mensagem');
const verificacaoSubtitulo = document.getElementById('verificacao-subtitulo');
const btnReenviarCodigo = document.getElementById('btn-reenviar-codigo');
let emailEmVerificacao = '';

cadastroForm.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    cadastroMensagem.textContent = '';
    cadastroMensagem.className = 'form-mensagem';

    const dto = {
        nome: cadastroForm.nome.value,
        email: cadastroForm.email.value,
        cpf: onlyDigits(cadastroForm.cpf.value),
        senha: cadastroForm.senha.value,
    };

    try {
        const resposta = await fetch('/usuarios', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dto),
        });

        if (resposta.ok) {
            emailEmVerificacao = dto.email;
            verificacaoSubtitulo.textContent = `Enviamos um código de 6 dígitos pra ${dto.email}`;
            verificacaoMensagem.textContent = '';
            verificacaoMensagem.className = 'form-mensagem';
            cadastroForm.classList.add('hidden');
            verificacaoForm.classList.remove('hidden');
            verificacaoForm.codigo.focus();
            return;
        }

        const erro = await resposta.json();
        cadastroMensagem.textContent = erro.message || 'Não foi possível criar a conta.';
        cadastroMensagem.classList.add('erro');
    } catch (falha) {
        cadastroMensagem.textContent = 'Não foi possível conectar ao servidor.';
        cadastroMensagem.classList.add('erro');
    }
});

verificacaoForm.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    verificacaoMensagem.textContent = '';
    verificacaoMensagem.className = 'form-mensagem';

    try {
        const resposta = await fetch('/usuarios/verificar-email', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: emailEmVerificacao, codigo: onlyDigits(verificacaoForm.codigo.value) }),
        });

        const corpo = await resposta.json();

        if (resposta.ok) {
            loginEmailInput.value = emailEmVerificacao;
            loginMensagem.textContent = 'E-mail verificado! Faça login.';
            loginMensagem.className = 'form-mensagem sucesso';
            verificacaoForm.reset();
            verificacaoForm.classList.add('hidden');
            cadastroForm.classList.remove('hidden');
            cadastroForm.reset();
            container.classList.remove('active');
            return;
        }

        verificacaoMensagem.textContent = corpo.message || 'Não foi possível confirmar o código.';
        verificacaoMensagem.classList.add('erro');
    } catch (falha) {
        verificacaoMensagem.textContent = 'Não foi possível conectar ao servidor.';
        verificacaoMensagem.classList.add('erro');
    }
});

async function reenviarCodigo(email, elementoMensagem, botao) {
    if (!email) {
        elementoMensagem.textContent = 'Informe seu email pra reenviar o código.';
        elementoMensagem.className = 'form-mensagem erro';
        return;
    }

    botao.disabled = true;
    try {
        const resposta = await fetch('/usuarios/reenviar-codigo', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email }),
        });
        const corpo = await resposta.json();
        elementoMensagem.textContent = corpo.message;
        elementoMensagem.className = 'form-mensagem ' + (resposta.ok ? 'sucesso' : 'erro');
    } catch (falha) {
        elementoMensagem.textContent = 'Não foi possível conectar ao servidor.';
        elementoMensagem.className = 'form-mensagem erro';
    } finally {
        // Cooldown de reenvio no servidor e de 60s -- so evita cliques
        // repetidos acidentais, a regra de verdade continua no backend.
        setTimeout(() => { botao.disabled = false; }, 60000);
    }
}

btnReenviarCodigo.addEventListener('click', () => {
    reenviarCodigo(emailEmVerificacao, verificacaoMensagem, btnReenviarCodigo);
});

// Login social sem conta existente guarda o ultimo email tentado (ver
// listener de submit do form de login mais abaixo) pra permitir reenviar o
// código direto da tela de login quando o redirect ?erro=email-nao-verificado
// acontece (o form nativo nao preserva o valor digitado apos o redirect).
const btnReenviarDoLogin = document.getElementById('btn-reenviar-do-login');
if (btnReenviarDoLogin) {
    // Usa o #login-mensagem (paragrafo separado) como feedback, nunca o
    // <p> que contem o proprio botao -- sobrescrever o textContent dele
    // apagaria o botao do DOM.
    btnReenviarDoLogin.addEventListener('click', () => {
        const emailSalvo = localStorage.getItem('rendo-ultimo-login-email') || '';
        reenviarCodigo(emailSalvo, loginMensagem, btnReenviarDoLogin);
    });
}

// Login e um form nativo -- o browser navega pra fora da pagina ao submeter,
// entao nao ha "fim" client-side pra esconder o overlay de novo (ou a
// navegacao troca a pagina, ou o Spring Security recarrega o login com erro).
document.querySelector('.sign-in form').addEventListener('submit', (evento) => {
    try {
        localStorage.setItem('rendo-ultimo-login-email', evento.target.username.value || '');
    } catch (falha) { /* localStorage indisponivel (ex.: modo privado) -- reenvio na tela de login so nao tera o email pre-preenchido */ }
    loadingOverlay?.classList.remove('hidden');
});

// Validacao em tempo real -- so feedback visual (cor da borda), a validacao
// de verdade continua sendo a do backend. So mostra estado depois que o
// campo foi tocado, pra nao nascer "invalido" numa pagina recem-carregada.
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const validadores = {
    nome: (v) => v.trim().length >= 2,
    email: (v) => EMAIL_REGEX.test(v.trim()),
    cpf: (v) => onlyDigits(v).length === 11,
    senha: (v) => v.length >= 8,
};

function ligarValidacaoAoVivo(input, checar) {
    const grupo = input.closest('.input-group');
    if (!checar || !grupo) return;

    input.addEventListener('input', () => {
        const valido = checar(input.value);
        grupo.classList.toggle('valido', valido);
        grupo.classList.toggle('invalido', !valido);
    });
}

ligarValidacaoAoVivo(cadastroForm.nome, validadores.nome);
ligarValidacaoAoVivo(cadastroForm.email, validadores.email);
ligarValidacaoAoVivo(cadastroForm.cpf, validadores.cpf);
ligarValidacaoAoVivo(cadastroForm.senha, validadores.senha);
if (loginEmailInput) ligarValidacaoAoVivo(loginEmailInput, validadores.email);

// Login social sem conta existente: LoginSocialFailureHandler redireciona pra
// cá com ?criarConta=1&nome=...&email=... -- so pre-preenche o cadastro (CPF e
// senha continuam sendo digitados pelo usuario, nenhuma conta e criada aqui).
(function preencherCadastroViaSocial() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('criarConta') !== '1') return;

    container.classList.add('active');
    if (params.get('nome')) cadastroForm.nome.value = params.get('nome');
    if (params.get('email')) cadastroForm.email.value = params.get('email');
    cadastroMensagem.textContent = 'Complete seu cadastro (CPF e senha) pra continuar.';
    cadastroMensagem.className = 'form-mensagem info';
})();

// Mostrar/ocultar senha -- so alterna o type do input irmao, nao interfere
// na validacao ao vivo (que escuta o evento 'input', nao 'click').
document.querySelectorAll('.input-toggle-senha').forEach((botao) => {
    botao.addEventListener('click', () => {
        const input = botao.closest('.input-group').querySelector('input');
        const mostrando = input.type === 'text';
        input.type = mostrando ? 'password' : 'text';
        botao.setAttribute('aria-label', mostrando ? 'Mostrar senha' : 'Ocultar senha');
    });
});
