## ADDED Requirements

### Requirement: Politica de conteudo restrita nas paginas
As páginas da aplicação SHALL responder com uma política de segurança de conteúdo que só permite scripts da própria origem, sem script inline nem `eval`, e que restringe o envio de formulários à própria origem. As páginas SHALL NOT conter script inline nem atributos de evento inline.

#### Scenario: Pagina publica com politica restrita
- **WHEN** um visitante pede `GET /`
- **THEN** o sistema responde 200 com uma política de conteúdo que tem `default-src 'self'`, `script-src 'self'` sem `'unsafe-inline'` e `form-action 'self'`, além de `Cross-Origin-Opener-Policy: same-origin`

#### Scenario: Tema aplicado antes do primeiro paint sem script inline
- **WHEN** qualquer página é carregada com o tema claro salvo
- **THEN** o tema é aplicado por um script da própria origem carregado antes das folhas de estilo, sem violação da política de conteúdo

### Requirement: Dados de cadastro nunca na URL
Os formulários de cadastro e de verificação de email SHALL usar o método POST com token CSRF, mesmo quando enviados sem JavaScript.

#### Scenario: Envio sem JavaScript
- **WHEN** o formulário de cadastro é enviado sem o JavaScript da página
- **THEN** nome, email, CPF e senha não aparecem na URL; a requisição é POST e o sistema responde com erro 4xx sem criar conta

### Requirement: DAST reproduzivel
O projeto SHALL ter um comando de DAST que executa o ZAP baseline contra uma instância descartável e SHALL reprovar (código de saída 1) se surgir um alerta não triado ou se o scan não puder rodar.

#### Scenario: Scan sem alertas novos
- **WHEN** o comando de DAST roda com Docker disponível
- **THEN** ele termina com 0 e registra relatório HTML e JSON, com cada alerta aceito justificado no arquivo de regras
