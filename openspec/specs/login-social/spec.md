# login-social Specification

## Purpose

Permite autenticar no Rendo com uma conta Google, Microsoft ou Apple, sem nunca exibir um provedor que não esteja realmente configurado e sem contornar a exigência de CPF no cadastro.

## Requirements

### Requirement: Login social só ativa com credenciais reais
O sistema SHALL só disponibilizar login social (Google/Microsoft/Apple) para provedores cujo client-id e client-secret estejam configurados via variável de ambiente. Sem nenhuma credencial configurada, a aplicação SHALL subir normalmente e a UI SHALL não exibir nenhum botão de login social.

#### Scenario: Nenhuma credencial configurada
- **WHEN** a aplicação inicia sem nenhuma variável de ambiente de OAuth2 configurada
- **THEN** ela sobe normalmente e a tela de login não mostra nenhum botão de login social

#### Scenario: Só um provedor configurado
- **WHEN** apenas as credenciais do Google estão configuradas
- **THEN** somente o botão do Google aparece na tela de login

### Requirement: Login social autentica apenas contas existentes
O sistema SHALL autenticar via login social somente quando já existir uma conta Rendo com o mesmo email do provedor. Nenhuma conta SHALL ser criada automaticamente a partir de um login social, porque CPF é obrigatório e nenhum provedor o fornece.

#### Scenario: Email do provedor já tem conta Rendo
- **WHEN** o usuário conclui o login social e o email retornado corresponde a uma conta Rendo existente e ativa
- **THEN** o usuário é autenticado normalmente e redirecionado ao dashboard

#### Scenario: Email do provedor não tem conta Rendo
- **WHEN** o usuário conclui o login social e o email retornado não corresponde a nenhuma conta Rendo
- **THEN** o sistema redireciona para a tela de cadastro com nome e email pré-preenchidos, sem criar nenhuma conta, e o usuário precisa informar CPF e senha pra concluir

### Requirement: Conta social requer CPF completo antes de existir
Nenhum registro na tabela de usuários SHALL ser criado sem CPF e senha, independentemente de o cadastro ter sido iniciado por login social ou pelo formulário tradicional.

#### Scenario: Usuário completa o cadastro pré-preenchido
- **WHEN** o usuário chega ao cadastro via redirecionamento de login social e preenche CPF e senha
- **THEN** a conta é criada exatamente pelas mesmas regras e validações do cadastro tradicional
