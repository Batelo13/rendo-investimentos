## 1. Bug fix — overflow do filtro em "Ações"

- [x] 1.1 `min-width: 0` + `flex: 1 1 160px` em `.filter-input`, `flex-wrap: wrap` em `.panel-head`
- [x] 1.2 Verificado no navegador em 1568px, 900px e 480px (iframe com largura fixa)

## 2. Login/cadastro — paleta e estrutura

- [x] 2.1 Bloco `.login-page { --background/--surface/--border/--text-primary/--text-secondary: ...; color: var(--text-primary); }` redeclarando os tokens já usados no resto do CSS
- [x] 2.2 Corrigido bug de contraste do `<h1>` do formulário (herdava `color` computado no `body`, fora do escopo `.login-page`)
- [x] 2.3 Cabeçalho "Já tem uma conta?/Ainda não tem uma conta?" no topo de cada painel, reaproveitando os botões/IDs `#btn-login`/`#btn-cadastro` já lidos por `login.js`
- [x] 2.4 Hero com quebra de linha e palavra em destaque mint
- [x] 2.5 Checklist ampliado de 3 para 4 itens, com selo circular verde

## 3. Painel deslizante — proporção 45/55

- [x] 3.1 `.sign-in`/`.sign-up`/`.toggle-container` migrados de `transform: translateX(±100%)` para `left` em % relativa ao container
- [x] 3.2 Testado manualmente as duas direções da transição (login→cadastro e cadastro→login) no navegador

## 4. Widgets decorativos e senha

- [x] 4.1 Widgets estáticos (Patrimônio total + sparkline, card PETR4, card de segurança) — sem chamada nova a API/serviço
- [x] 4.2 Toggle de mostrar/ocultar senha nos campos de senha do cadastro e do login, sem interferir na validação ao vivo
- [x] 4.3 Confirmado (grep em `SecurityConfig`/`pom.xml`) que não existe OAuth2 configurado — nenhum botão de login social adicionado

## 5. Responsivo

- [x] 5.1 Breakpoint <900px esconde os widgets decorativos
- [x] 5.2 Breakpoint <700px: painel vira faixa compacta no topo, troca de estado via exibição direta em vez de slide
- [x] 5.3 Verificado via `<iframe>` de largura fixa (390px) apontando pra mesma origem — `resize_window` da ferramenta de automação não reflete no `window.innerWidth` real nesta sessão

## 6. Verificação end-to-end

- [x] 6.1 `mvnw compile` sem erros (nenhum `.java` alterado)
- [x] 6.2 Fluxo completo no navegador: cadastro → mensagem de sucesso → login → dashboard
- [x] 6.3 Validação ao vivo (bordas verde/vermelho) continua funcionando com a marcação nova
- [x] 6.4 Console do navegador sem erros da aplicação

## 7. Dashboard — refinamentos (rodada 2)

- [x] 7.1 Tile "Patrimônio total" (saldo disponível + valor das posições convertido, dado real já carregado)
- [x] 7.2 Ícone em todos os stat-cards da Visão Geral
- [x] 7.3 Rótulos de eixo X/Y no gráfico de evolução patrimonial
- [x] 7.4 Pill de período ativo com estilo outlined em vez de preenchido
- [x] 7.5 Testado com dados sintéticos injetados via console (sem posições reais na conta de teste) para confirmar o cálculo e os rótulos de eixo
- [x] 7.6 Deliberadamente não implementado: widget de índice IBOV, sparkline do "Resultado não realizado", menu de três pontos por linha — todos exigiriam dado externo ou ação de backend inexistente
