## 1. Contratos e testes de integração visual

- [x] 1.1 Criar `LiquidGlassMarkupTest` cobrindo carregamento de `glass.css`/`motion.js`, aplicação antecipada do tema e preservação dos IDs funcionais nos três templates.
- [x] 1.2 Executar o novo teste isoladamente e confirmar que falha antes da criação dos recursos compartilhados.

## 2. Fundação compartilhada

- [x] 2.1 Adicionar em `tokens.css` os tokens semânticos de atmosfera, densidade de vidro, borda, brilho, sombra, blur, duração e easing para claro e escuro.
- [x] 2.2 Criar `glass.css` com superfícies compartilhadas, fundo atmosférico, estados de interação, fallback opaco, responsividade e `prefers-reduced-motion`.
- [x] 2.3 Criar `motion.js` como melhoria progressiva, mantendo HTML visível por padrão, usando `IntersectionObserver` e limitando o stagger de listas.
- [x] 2.4 Integrar `glass.css` e `motion.js` nos templates e fazer os testes de marcação passarem.

## 3. Landing e loading

- [x] 3.1 Reorganizar `index.html` em uma composição glass de apresentação sem alterar sua rota ou fragmento de logo.
- [x] 3.2 Atualizar `loading.css`, `loading.js` e o fragmento de loading para coordenar a saída do overlay com o estado de página pronta.
- [x] 3.3 Verificar que index/dashboard exibem loading inicial, login não exibe loading em visita simples e cadastro continua sem overlay.

## 4. Autenticação nos dois temas

- [x] 4.1 Aplicar o tema persistido antes do paint em `login.html` e carregar os recursos compartilhados na ordem definida pelo design.
- [x] 4.2 Migrar `login.css` para neutros temáticos e superfícies glass, preservando menta, roxo-ardósia, fluxo deslizante, validação e responsividade abaixo de 700 px.
- [x] 4.3 Refinar somente as classes visuais necessárias em `login.html`/`login.js`, sem alterar nomes de campos, endpoints ou regras de submissão.
- [x] 4.4 Validar login, cadastro, verificação de email, show/hide de senha e troca de estado nos temas claro e escuro.

## 5. Dashboard em camadas

- [x] 5.1 Reorganizar `dashboard.html` em planos de atmosfera, navegação e conteúdo, preservando todos os IDs, `data-*` e regiões atualizadas pelo JavaScript.
- [x] 5.2 Migrar sidebar, topbar, resumo financeiro, gráficos, painéis, filtros e tabelas em `dashboard.css` para a hierarquia glass responsiva.
- [x] 5.3 Atualizar modal, menus, alertas, estados vazios e loading/skeleton para o mesmo sistema visual sem deslocar linhas de dados no hover.
- [x] 5.4 Ajustar classes geradas em `dashboard.js` somente onde componentes dinâmicos precisarem das primitives compartilhadas.
- [x] 5.5 Validar navegação, filtros, períodos do gráfico, tooltip, modais e ações de corretora/ação/operação/transferência nos dois temas.

## 6. Acessibilidade, compatibilidade e desempenho

- [x] 6.1 Validar foco por teclado, contraste, zoom, viewport móvel, dispositivo sem hover e ausência de overflow horizontal.
- [x] 6.2 Validar `prefers-reduced-motion` e confirmar que o conteúdo permanece visível quando `motion.js` está ausente.
- [x] 6.3 Validar fallback sem `backdrop-filter` e limitar blur/stagger em viewport móvel e listas extensas.

## 7. Verificação e rastreabilidade

- [x] 7.1 Executar a suíte Maven completa e confirmar ausência de regressões funcionais.
- [x] 7.2 Executar inspeção visual automatizada/manual de landing, login e dashboard nos temas claro/escuro e em desktop/mobile, verificando também o console.
- [x] 7.3 Executar `openspec validate liquid-glass-interface --strict --no-interactive` e marcar as tarefas concluídas.
- [x] 7.4 Executar `graphify update .`, consultar o impacto final e registrar qualquer dependência inesperada antes do arquivamento da mudança.
