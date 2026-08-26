## 1. Dependências e configuração

- [x] 1.1 Adicionar `spring-boot-starter-mail` ao `pom.xml`
- [x] 1.2 Adicionar `spring.mail.*` (via `${MAIL_HOST}`, `${MAIL_PORT}`, `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`) e `app.mail.enabled=${MAIL_ENABLED:true}` em `application.properties`/`application-docker.properties`
- [x] 1.3 Adicionar `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_ENABLED` ao `environment:` do serviço `aplicacao` em `compose.yaml`
- [x] 1.4 Adicionar as mesmas chaves (sem valores reais) ao `.env.example`

## 2. Modelo de dados

- [x] 2.1 Adicionar campo `emailVerified` (Boolean) à entidade `Usuario` com getter/setter, seguindo o padrão dos campos existentes
- [x] 2.2 Criar enum `TipoCodigo` (`VERIFICACAO_EMAIL`) em `model`
- [x] 2.3 Criar entidade `EmailVerificationCode` (`usuario`, `codigoHash`, `tipo`, `expiraEm`, `criadoEm`, `usado`, `tentativas`)
- [x] 2.4 Criar `EmailVerificationCodeRepository` (`findFirstByUsuarioAndTipoAndUsadoFalseOrderByCriadoEmDesc`, ou equivalente)

## 3. Envio de email

- [x] 3.1 Criar `EmailService` com `enviarCodigoVerificacao(destinatario, nome, codigo)`, respeitando `app.mail.enabled`
- [x] 3.2 Criar template HTML (Thymeleaf) do email de verificação: assunto "Confirme seu e-mail", código destacado, aviso de expiração em 10 minutos

## 4. Geração e validação de código

- [x] 4.1 Criar `EmailVerificationService` com geração de código (`SecureRandom`, 6 dígitos com zero à esquerda), hash via `PasswordEncoder`, persistência e disparo do email
- [x] 4.2 Implementar confirmação de código (`@Transactional`): busca código ativo, valida expiração (10 min), valida tentativas (máx. 5), compara hash, marca `usado=true` e `usuario.emailVerified=true`
- [x] 4.3 Implementar reenvio de código: valida cooldown de 60s desde o código anterior, invalida o anterior (`usado=true` sem verificar a conta), gera e envia um novo
- [x] 4.4 Criar exceções específicas reaproveitando os tipos existentes (`RecursoNaoEncontradoException`, `RegraDeNegocioException`) para: conta não encontrada, conta já verificada, código inexistente/incorreto, código expirado, tentativas excedidas, cooldown de reenvio ativo

## 5. Endpoints

- [x] 5.1 Criar `VerificarEmailRequestDTO(String email, String codigo)` e `ReenviarCodigoRequestDTO(String email)`
- [x] 5.2 Criar `CadastroResponseDTO(String message, boolean emailVerificationRequired)` e `MensagemResponseDTO(String message)` (ou reaproveitar um DTO simples equivalente já existente)
- [x] 5.3 Adicionar `POST /usuarios/verificar-email` e `POST /usuarios/reenviar-codigo` ao `UsuarioController`
- [x] 5.4 Liberar as duas novas rotas em `SecurityConfig` (`permitAll`, mesma linha de `POST /usuarios`)

## 6. Cadastro

- [x] 6.1 Atualizar `UsuarioService.cadastrar`: definir `emailVerified=false` explicitamente, gerar/enviar código após salvar o usuário, devolver `CadastroResponseDTO`
- [x] 6.2 Atualizar `UsuarioController.cadastrar` para devolver `202 Accepted` com o novo corpo (sem `Location`, já que não há mais um recurso "pronto")

## 7. Bloqueio de login por email não verificado

- [x] 7.1 Criar `EmailNaoVerificadoException extends DisabledException` (pacote `security`)
- [x] 7.2 Atualizar `UsuarioDetailsService.loadUserByUsername`: lançar `EmailNaoVerificadoException` quando `ativo=true` e `emailVerified=false` explicitamente (tratar `null` como verificado); manter `disabled` para `ativo=false` como está hoje
- [x] 7.3 Criar `LoginFormFailureHandler` (`AuthenticationFailureHandler`) que redireciona para `/login?erro=email-nao-verificado` em `EmailNaoVerificadoException`, e delega para `/login?error` nos demais casos
- [x] 7.4 Registrar o novo failure handler no `formLogin` em `SecurityConfig`, sem alterar o comportamento já testado de bloqueio por `ativo`

## 8. Login social marca email como verificado

- [x] 8.1 Atualizar `RendoOidcUserService.loadUser`: após validar `ativo`, marcar e salvar `emailVerified=true` quando ainda não estava

## 9. Frontend

- [x] 9.1 Adicionar ao painel de cadastro (`login.html`) um passo de confirmação de código (input de 6 dígitos + botão confirmar + link "reenviar código"), oculto por padrão
- [x] 9.2 Atualizar `login.js`: ao cadastrar com sucesso, mostrar o passo de confirmação em vez de ir direto para a mensagem de sucesso do login; `fetch` para `/usuarios/verificar-email` e `/usuarios/reenviar-codigo`; ao confirmar com sucesso, seguir para a mensagem "Conta criada! Faça login."
- [x] 9.3 Atualizar `login.html` (tela de login) para mostrar a mensagem de "confirme seu email" quando a URL tiver `?erro=email-nao-verificado`, com um atalho para reenviar o código

## 10. Testes

- [x] 10.1 Atualizar `UsuarioAuthIntegrationTest`: ajustar `deveCadastrarUsuarioPublicamenteSemAutenticacao` para o novo contrato (202, `emailVerificationRequired=true`); ajustar `cadastrarUsuario` (helper usado pelos testes de login) para setar `emailVerified=true`, preservando os testes de login já existentes
- [x] 10.2 Criar `EmailVerificationServiceTest` (mockando `EmailService`, `UsuarioRepository`, `EmailVerificationCodeRepository`, `PasswordEncoder`): código correto verifica a conta; código errado não verifica e incrementa tentativa; código expirado é rejeitado; código já usado é rejeitado; conta já verificada é rejeitada; limite de 5 tentativas é respeitado; reenvio gera novo código e invalida o anterior; cooldown de 60s é respeitado
- [x] 10.3 Adicionar teste de integração: cadastro gera código pendente; login com conta não verificada é bloqueado com o redirect distinto de `/login?error`
- [x] 10.4 Garantir que nenhum teste dependa de envio real de email (mock de `EmailService`/`JavaMailSender`)

## 11. Validação final

- [x] 11.1 `mvn test` (ou equivalente do projeto) passando
- [x] 11.2 `docker compose build` e `docker compose up` continuam subindo a aplicação e o Postgres normalmente
- [x] 11.3 Teste manual: cadastro → email chega (ou log, se `MAIL_ENABLED=false`) → confirmação → login liberado
