## ADDED Requirements

### Requirement: O problema que a Rendo resolve
A página SHALL apresentar três problemas de quem quer começar a investir, cada um com um estado "antes" que SHALL se resolver no estado "Rendo" quando o card entra na tela. Com movimento reduzido, os cards SHALL já aparecer resolvidos.

#### Scenario: Card entra na tela
- **WHEN** o visitante rola até a seção "O problema"
- **THEN** os três cards passam do estado "antes" para o estado resolvido

### Requirement: Tour do produto
A seção "Como funciona" SHALL mostrar os quatro passos (conta, corretoras, ordens, acompanhamento), cada um com uma réplica ilustrativa da tela real. O passo no meio da tela SHALL ficar marcado como ativo no índice, e clicar num passo do índice SHALL levar a ele.

#### Scenario: Clique no indice
- **WHEN** o visitante clica no passo 4 do índice enquanto vê o passo 1
- **THEN** a página vai ao passo 4 e só ele fica ativo

#### Scenario: Celular
- **WHEN** a página é aberta num celular de 390 px
- **THEN** os passos aparecem empilhados, sem réplica fixa cobrindo o texto e sem scroll horizontal

### Requirement: Diferenciais demonstrados
A seção de recursos SHALL ter três blocos de tamanhos diferentes com microdemonstrações: câmbio entre US$ e R$ com cotação demonstrativa, histórico de cotação ilustrativo percorrível por mouse, toque ou teclado, e preço médio de duas compras.

#### Scenario: Cambio
- **WHEN** o visitante alterna a moeda do bloco de câmbio
- **THEN** o valor conta até "R$ 12.312,00" e, ao alternar de novo, volta a "US$ 2.280,00"

#### Scenario: Historico pelo teclado
- **WHEN** o visitante foca o gráfico de cotação e aperta End
- **THEN** o texto mostra o dia 30 e o preço ilustrativo desse dia

### Requirement: Seguranca verificavel
A seção de segurança SHALL listar apenas proteções implementadas (verificação de e-mail, limite de tentativas, senha com BCrypt, CPF protegido, dados isolados, cabeçalhos de segurança e verificação semanal) e SHALL afirmar que nenhum dinheiro real circula na Rendo.

#### Scenario: Visitante le a secao
- **WHEN** o visitante chega à seção de segurança
- **THEN** vê as sete proteções e a frase "Nenhum dinheiro real circula na Rendo."

### Requirement: Chamada final com contador
A chamada final SHALL mostrar "Comece com R$ 100.000 fictícios" com o valor como contador. O botão principal SHALL ter uma atração magnética sutil (no máximo 6 px) só com mouse e sem movimento reduzido.

#### Scenario: Toque no botao
- **WHEN** o visitante toca o botão num celular
- **THEN** o botão não se desloca
