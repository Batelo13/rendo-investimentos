## Context

Ver proposal.md - Why. Detalhamento completo do design original (perguntas de brainstorming, alternativas descartadas passo a passo): `docs/superpowers/specs/2026-08-18-cadastro-cpf-design.md`. Este documento resume as decisões técnicas para o registro OpenSpec.

## Goals / Non-Goals

**Goals:**
- Validar CPF (formato + dígito verificador) sem depender de nenhum serviço externo.
- Reaproveitar exatamente os padrões já estabelecidos no projeto para `email` (unicidade, DTO de validação declarativa, exposição na resposta).

**Non-Goals:**
- Lookup externo de CPF (não existe API pública equivalente à BrasilAPI para pessoa física).
- Máscara de input formatando enquanto digita (mesmo padrão do campo CNPJ hoje: texto livre, dígitos extraídos no submit).
- Campo opcional/nullable — decisão explícita de deixar obrigatório desde já, sem dado legado a migrar.

## Decisions

**Validação via anotação Bean Validation customizada (`@CPF`) em vez de checagem manual no Service.** Diferente de `CvmValidador` (que vive em `integration/` porque opera sobre dado já buscado de uma API externa), a validação de CPF só precisa da própria string — encaixa como uma constraint declarativa no DTO, no mesmo espírito de `@Email`/`@Pattern` que os outros campos já usam. Alternativa descartada: classe estática chamada manualmente no `UsuarioService`, rejeitada por adicionar um passo manual sem necessidade.

**Sem máscara de input no frontend.** Segue a convenção já usada pelo campo CNPJ no dashboard (`onlyDigits`/`fmtCnpj`): texto livre, dígitos extraídos no submit. Evita introduzir um padrão de UI novo só para este campo.

**Validação client-side é só de comprimento (11 dígitos), nunca replica o dígito verificador.** O dígito verificador de verdade é responsabilidade exclusiva do backend — mesma filosofia que os outros campos do formulário de cadastro já seguem (feedback visual, não substitui a validação real).

**Fixture de teste gera CPFs sintéticos únicos por chamada** (`CpfTestFixtures.proximoCpfValido()`, contador `AtomicInteger` + mesmo algoritmo de dígito verificador reaproveitado de `CpfValidator`), em vez de um único CPF fixo — necessário porque `cpf` é `unique` no banco e múltiplos testes de integração precisam persistir `Usuario` distintos na mesma execução.

## Risks / Trade-offs

[CPF sintético de teste colide com um CPF de outro teste] → Mitigado pelo contador monotônico compartilhado (`AtomicInteger`), que garante base de 9 dígitos única por chamada dentro da mesma JVM de teste.

[Nenhuma validação real de que o CPF pertence à pessoa cadastrada] → Aceito conscientemente: é uma simulação acadêmica, e não existe API pública para essa verificação. O rigor aplicado (formato + dígito verificador + unicidade) é o máximo tecnicamente viável sem serviço de terceiro.
