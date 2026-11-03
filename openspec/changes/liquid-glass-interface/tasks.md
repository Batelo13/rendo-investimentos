## 1. Contratos e testes de integração visual

- [ ] 1.1 Criar `LiquidGlassMarkupTest` cobrindo carregamento de `glass.css`/`motion.js`, aplicação antecipada do tema e preservação dos IDs funcionais nos três templates.
- [ ] 1.2 Executar o novo teste isoladamente e confirmar que falha antes da criação dos recursos compartilhados.

## 2. Fundação compartilhada

- [ ] 2.1 Adicionar em `tokens.css` os tokens semânticos de atmosfera, densidade de vidro, borda, brilho, sombra, blur, duração e easing para claro e escuro.
- [ ] 2.2 Criar `glass.css` com superfícies compartilhadas, fundo atmosférico, estados de interação, fallback opaco, responsividade e `prefers-reduced-motion`.
- [ ] 2.3 Criar `motion.js` como melhoria progressiva, mantendo HTML visível por padrão, usando `IntersectionObserver` e limitando o stagger de listas.
- [ ] 2.4 Integrar `glass.css` e `motion.js` nos templates e fazer os testes de marcação passarem.

## 3. Landing e loading

- [ ] 3.1 Reorganizar `index.html` em uma composição glass de apresentação sem alterar sua rota ou fragmento de logo.
- [ ] 3.2 Atualizar `loading.css`, `loading.js` e o fragmento de loading para coordenar a saída do overlay com o estado de página pronta.
- [ ] 3.3 Verificar que index/dashboard exibem loading inicial, login não exibe loading em visita simples e cadastro continua sem overlay.

## 4. Autenticação nos dois temas

- [ ] 4.1 Aplicar o tema persistido antes do paint em `login.html` e carregar os recursos compartilhados na ordem definida pelo design.
- [ ] 4.2 Migrar `login.css` para neutros temáticos e superfícies glass, preservando menta, roxo-ardósia, fluxo deslizante, validação e responsividade abaixo de 700 px.
- [ ] 4.3 Refinar somente as classes visuais necessárias em `login.html`/`login.js`, sem alterar nomes de campos, endpoints ou regras de submissão.
- [ ] 4.4 Validar login, cadastro, verificação de email, show/hide de senha e troca de estado nos temas claro e escuro.

## 5. Dashboard em camadas

- [ ] 5.1 Reorganizar `dashboard.html` em planos de atmosfera, navegação e conteúdo, preservando todos os IDs, `data-*` e regiões atualizadas pelo JavaScript.
- [ ] 5.2 Migrar sidebar, topbar, resumo financeiro, gráficos, painéis, filtros e tabelas em `dashboard.css` para a hierarquia glass responsiva.
- [ ] 5.3 Atualizar modal, menus, alertas, estados vazios e loading/skeleton para o mesmo sistema visual sem deslocar linhas de dados no hover.
- [ ] 5.4 Ajustar classes geradas em `dashboard.js` somente onde componentes dinâmicos precisarem das primitives compartilhadas.
- [ ] 5.5 Validar navegação, filtros, períodos do gráfico, tooltip, modais e ações de corretora/ação/operação/transferência nos dois temas.

## 6. Acessibilidade, compatibilidade e desempenho

- [ ] 6.1 Validar foco por teclado, contraste, zoom, viewport móvel, dispositivo sem hover e ausência de overflow horizontal.
- [ ] 6.2 Validar `prefers-reduced-motion` e confirmar que o conteúdo permanece visível quando `motion.js` está ausente.
- [ ] 6.3 Validar fallback sem `backdrop-filter` e limitar blur/stagger em viewport móvel e listas extensas.

## 7. Verificação e rastreabilidade

- [ ] 7.1 Executar a suíte Maven completa e confirmar ausência de regressões funcionais.
- [ ] 7.2 Executar inspeção visual automatizada/manual de landing, login e dashboard nos temas claro/escuro e em desktop/mobile, verificando também o console.
- [ ] 7.3 Executar `openspec validate liquid-glass-interface --strict --no-interactive` e marcar as tarefas concluídas.
- [ ] 7.4 Executar `graphify update .`, consultar o impacto final e registrar qualquer dependência inesperada antes do arquivamento da mudança.
