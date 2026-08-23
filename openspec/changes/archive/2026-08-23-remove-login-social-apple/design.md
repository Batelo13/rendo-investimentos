## Context

Ver proposal.md - Why. A Apple nunca teve credenciais configuradas; o bean `ClientRegistrationRepository` já era condicional (`@ConditionalOnExpression`) sobre a presença de client-id de QUALQUER provedor, então bastava remover a Apple da checagem e da lista de registros.

## Goals / Non-Goals

**Goals:** remover todo código/config específico da Apple sem afetar Google/Microsoft.
**Non-Goals:** mudar o mecanismo condicional em si (continua o mesmo padrão pra Google/Microsoft).

## Decisions

- `AppleAuthorizationRequestResolver` foi deletado (não só desativado) porque sua única razão de existir era o `response_mode=form_post` exigido pela Apple — sem ela, o resolver customizado do `.authorizationEndpoint()` também some, voltando ao resolver padrão do Spring Security pra Google/Microsoft (que nunca precisaram de customização).

## Risks / Trade-offs

- Nenhum: Apple nunca esteve configurada em nenhum ambiente, então não há credencial "quebrada" por essa remoção.
