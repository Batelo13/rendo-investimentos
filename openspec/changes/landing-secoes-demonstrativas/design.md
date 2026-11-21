## Decisões

1. **Três blocos de diferenciais**: câmbio, histórico e preço médio. A confirmação de ordem fica só no passo 3 do tour, para não repetir a réplica.
2. **Linha "Também na Rendo:"** com os recursos que saíram dos blocos (histórico de operações, evolução patrimonial, tema claro/escuro), para não perder informação.
3. **Seção "Acompanhamento" removida**: o passo 4 do tour mostra o mesmo conteúdo.
4. **Cabeçalho** com A Rendo, Experimente, Como funciona e Segurança (seção 0 do design). O rodapé fica como está.
5. **Tour sem mover o DOM**: cada passo é um `article` com texto e réplica.
   - **Desktop:** réplica `sticky` dentro do próprio passo e índice fixo à esquerda.
   - **Celular:** passos empilhados, sem sticky.
6. **Comportamentos genéricos** viram atributos no `motion.js` (`data-resolve`, `data-ima`). Os específicos ficam em arquivos próprios.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Sem autenticação nova. |
| Tampering | Nada é enviado ao servidor; as demonstrações vivem no navegador. |
| Repudiation | Não se aplica. |
| Information disclosure | Nenhum dado pessoal. A lista de segurança cita só proteções implementadas, sem afirmar o que o sistema não faz. |
| Denial of service | Animações de `transform`/`opacity`, ponteiro agrupado em rAF. |
| Elevation of privilege | CSP `script-src 'self'` mantida: todo JS em arquivo próprio. |
