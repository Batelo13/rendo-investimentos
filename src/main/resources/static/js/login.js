const container = document.getElementById('container');
const btnCadastro = document.getElementById('btn-cadastro');
const btnLogin = document.getElementById('btn-login');

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
