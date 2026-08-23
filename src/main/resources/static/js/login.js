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
            loginEmailInput.value = dto.email;
            loginMensagem.textContent = 'Conta criada! Faça login.';
            loginMensagem.className = 'form-mensagem sucesso';
            cadastroForm.reset();
            container.classList.remove('active');
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

// Login e um form nativo -- o browser navega pra fora da pagina ao submeter,
// entao nao ha "fim" client-side pra esconder o overlay de novo (ou a
// navegacao troca a pagina, ou o Spring Security recarrega o login com erro).
document.querySelector('.sign-in form').addEventListener('submit', () => {
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
