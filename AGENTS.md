# Rendo -- instrucoes para agentes

- Stack: Java 17, Spring Boot 4.1, Spring Security 7, JPA, Thymeleaf + JS vanilla. Pacote `com.curso.gestaoinvestimentos`.
- Antes de abrir arquivos: `graphify query "<pergunta>"` / `graphify explain "<simbolo>"`; depois de alterar codigo: `graphify update .`.
- Mudanca de comportamento passa por OpenSpec (`openspec/changes/<nome>`): proposal, design (STRIDE quando tocar autenticacao, dados pessoais ou contrato publico), specs e tasks.
- Testes antes do codigo; `./mvnw test` verde antes de commitar. Commits pequenos, em portugues.
- Segredos so em `.env` (nunca versionado); variavel nova vai para `.env.example` com placeholder.
- Ao terminar: `./mvnw test`, `scripts/security_gate.sh` e revisao do diff; triagem de achados por `.agents/prompts/triagem-seguranca.md`.
- Nunca: desligar CSRF, testes ou regras do portao para "passar"; logar codigo de verificacao, senha, token ou email completo.
