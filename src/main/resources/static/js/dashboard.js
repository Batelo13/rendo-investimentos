/* ====================================================================
   Rendo — Dashboard
   Estrutura (sidebar, views, modal, toasts) inspirada no projeto antigo do
   usuario (Carteira-de-Acao), adaptada ao modelo de dados deste projeto:
   posicao e por (acao, corretora) -- nao por acao sozinha -- e compra/venda
   passam pelo mesmo endpoint POST /operacoes (tipo diferencia), nao por
   /acoes/{id}/comprar|vender como no projeto antigo.
   ==================================================================== */

const $ = (s, ctx = document) => ctx.querySelector(s);
const $$ = (s, ctx = document) => [...ctx.querySelectorAll(s)];

const state = {
    saldo: null,
    posicoes: [],
    acoes: [],
    operacoes: [],
    operacoesPagina: { numero: 0, totalPages: 1 },
    corretoras: [],
    rendimento: [],
    periodoChart: "TUDO",
};

// Pontos do grafico no espaco do viewBox (preenchido a cada render), usado
// pelo tooltip on-hover pra achar o ponto mais proximo do mouse sem
// recalcular a projecao a cada movimento.
let pontosChartAtual = [];

/* ----------------------------- HTTP ------------------------------ */
async function api(path, options = {}) {
    const res = await fetch(path, {
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        ...options,
    });

    let data = null;
    const text = await res.text();
    if (text) {
        try { data = JSON.parse(text); } catch { data = text; }
    }

    if (!res.ok) {
        const msg = (data && typeof data === "object" && data.message) || `Erro ${res.status} ao comunicar com o servidor.`;
        const err = new Error(msg);
        err.status = res.status;
        throw err;
    }
    return data;
}

/* --------------------------- Utils ------------------------------- */
const onlyDigits = (s) => (s || "").replace(/\D/g, "");
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[m]));

function moedaPorTicker(ticker) {
    const acao = state.acoes.find((a) => a.ticker === ticker);
    return acao ? acao.moeda : "BRL";
}

function nomeEmpresaPorTicker(ticker) {
    const acao = state.acoes.find((a) => a.ticker === ticker);
    return acao ? acao.nomeEmpresa : null;
}

function acaoPorTicker(ticker) {
    return state.acoes.find((a) => a.ticker === ticker) || null;
}

function fmtMoeda(valor, moeda) {
    if (valor == null) return "—";
    const cur = moeda === "USD" ? "USD" : "BRL";
    try {
        return new Intl.NumberFormat(cur === "USD" ? "en-US" : "pt-BR", { style: "currency", currency: cur }).format(Number(valor));
    } catch {
        return `${valor} ${moeda || ""}`.trim();
    }
}

function fmtNumero(valor) {
    if (valor == null) return "—";
    return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 8 }).format(Number(valor));
}

function fmtData(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    if (isNaN(d)) return iso;
    return d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function fmtCnpj(c) {
    const d = onlyDigits(c);
    if (d.length !== 14) return c || "—";
    return `${d.slice(0,2)}.${d.slice(2,5)}.${d.slice(5,8)}/${d.slice(8,12)}-${d.slice(12)}`;
}

function mercadoTag(m) {
    return m === "EUA" ? `<span class="tag">US EUA</span>` : `<span class="tag">BR Brasil</span>`;
}

function acaoLogoHTML(a, size = 24) {
    if (!a.logoUrl) return "";
    return `<img class="acao-logo" src="${esc(a.logoUrl)}" alt="" width="${size}" height="${size}" onerror="this.remove()">`;
}

/* ---------------------------- Icones (SVG) ---------------------------- */
function icone(nome, size = 14) {
    const paths = {
        refresh: '<path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/>',
        expand: '<polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" x2="14" y1="3" y2="10"/><line x1="3" x2="10" y1="21" y2="14"/>',
        badgeCheck: '<path d="M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z"/><path d="m9 12 2 2 4-4"/>',
        mapPin: '<path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"/><circle cx="12" cy="10" r="3"/>',
        kebab: '<circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/>',
        trash: '<path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/>',
    };
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="${size}" height="${size}" style="vertical-align:-2px">${paths[nome] || ""}</svg>`;
}

function fmtResultado(valor, moeda) {
    if (valor == null) return '<span class="pl pl-zero">—</span>';
    const n = Number(valor);
    if (n === 0) return `<span class="pl pl-zero">${esc(fmtMoeda(0, moeda))}</span>`;
    const cls = n > 0 ? "pl-pos" : "pl-neg";
    const sinal = n > 0 ? "+" : "-";
    return `<span class="pl ${cls}">${sinal}${esc(fmtMoeda(Math.abs(n), moeda))}</span>`;
}

/**
 * Deriva a taxa de cambio implicita do campo ja convertido que a API manda
 * pra acoes EUA (cotacaoAtualBRL) -- evita um endpoint dedicado de cambio,
 * a mesma taxa serve pra converter qualquer outro valor daquela acao
 * (preco medio, valor investido, valor de uma operacao) no frontend.
 */
function taxaCambioPorTicker(ticker) {
    const a = state.acoes.find((x) => x.ticker === ticker);
    if (!a || a.moeda !== "USD" || a.cotacaoAtualBRL == null || !a.cotacaoAtual) return null;
    return Number(a.cotacaoAtualBRL) / Number(a.cotacaoAtual);
}

function fmtConvertido(valorNaMoedaOriginal, ticker) {
    const taxa = taxaCambioPorTicker(ticker);
    if (taxa == null || valorNaMoedaOriginal == null) return "";
    return `<span class="valor-convertido">≈ ${esc(fmtMoeda(Number(valorNaMoedaOriginal) * taxa, "BRL"))}</span>`;
}

/* --------------------------- Toasts ------------------------------ */
function toast(titulo, msg = "", tipo = "ok") {
    const el = document.createElement("div");
    el.className = `rendo-toast ${tipo}`;
    const corpo = document.createElement("div");
    corpo.className = "t-body";
    const b = document.createElement("b");
    b.textContent = titulo;
    corpo.append(b, document.createTextNode(msg));
    el.append(corpo);
    $("#toasts").appendChild(el);
    setTimeout(() => {
        el.style.transition = "opacity .3s, transform .3s";
        el.style.opacity = "0";
        el.style.transform = "translateX(16px)";
        setTimeout(() => el.remove(), 320);
    }, tipo === "err" ? 6000 : 3800);
}

/* ----------------------- Navegacao de views ---------------------- */
const VIEW_META = {
    "visao-geral": ["Visão geral", "Resumo da sua carteira"],
    "acoes": ["Ações", "Catálogo de ações e cotações"],
    "posicoes": ["Minhas posições", "Suas posições por ação e corretora"],
    "operacoes": ["Operações", "Histórico de compras e vendas"],
    "corretoras": ["Corretoras", "Instituições validadas via CNPJ, CEP e CVM"],
};

function setView(view) {
    $$(".nav-item").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
    $$(".view").forEach((v) => v.classList.add("hidden"));
    $(`#view-${view}`).classList.remove("hidden");
    const [t, s] = VIEW_META[view] || VIEW_META["visao-geral"];
    $("#viewTitle").textContent = t;
    $("#viewSubtitle").textContent = s;
    $("#sidebar").classList.remove("open");
}

/* ---------------------------- Render ------------------------------ */
function renderVisaoGeral() {
    $("#statSaldoDisponivel").textContent = state.saldo ? fmtMoeda(state.saldo.saldoDisponivel, "BRL") : "—";
    $("#statSaldoInicial").textContent = state.saldo ? fmtMoeda(state.saldo.saldoInicial, "BRL") : "—";
    $("#statPosicoes").textContent = state.posicoes.length;
    $("#statCorretoras").textContent = state.corretoras.length;

    let naoRealizado = 0;
    let valorPosicoesBRL = 0;
    for (const p of state.posicoes) {
        if (p.valorAtual == null || p.valorInvestido == null) continue;
        const moeda = moedaPorTicker(p.acaoTicker);
        let taxa = 1;
        if (moeda === "USD") {
            taxa = taxaCambioPorTicker(p.acaoTicker);
            if (taxa == null) continue; // taxa indisponivel -- exclui em vez de tratar como 1:1
        }
        naoRealizado += (Number(p.valorAtual) - Number(p.valorInvestido)) * taxa;
        valorPosicoesBRL += Number(p.valorAtual) * taxa;
    }
    $("#statPatrimonioTotal").textContent = state.saldo
        ? fmtMoeda(Number(state.saldo.saldoDisponivel) + valorPosicoesBRL, "BRL")
        : "—";
    const realizado = state.operacoes
        .filter((o) => o.tipo === "VENDA" && o.status === "ATIVA")
        .reduce((s, o) => s + Number(o.lucroPrejuizoRealizado || 0) * Number(o.taxaCambio || 1), 0);

    $("#statNaoRealizado").innerHTML = fmtResultado(naoRealizado, "BRL");
    $("#statRealizado").innerHTML = fmtResultado(realizado, "BRL");

    renderRendimentoChart();

    const dash = $("#dashPosicoes");
    const recentes = state.posicoes.slice(0, 6);
    if (!recentes.length) {
        dash.innerHTML = `<div class="empty">Nenhuma posição ainda. Vá em <b>Ações</b> para comprar.</div>`;
        return;
    }
    dash.innerHTML = recentes.map((p, i) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        const acao = acaoPorTicker(p.acaoTicker);
        const resultado = p.valorAtual != null ? Number(p.valorAtual) - Number(p.valorInvestido) : null;
        return `
        <div class="mini-row">
            <span class="m-ativo">
                ${acao ? acaoLogoHTML(acao, 22) : ""}
                <span class="m-ativo-info">
                    <span class="m-ticker">${esc(p.acaoTicker)}</span>
                    ${acao && acao.nomeEmpresa ? `<span class="m-empresa">${esc(acao.nomeEmpresa)}</span>` : ""}
                </span>
            </span>
            <span class="m-corretora">
                <span class="m-corretora-badge">${esc((p.corretoraNome || "—").slice(0, 2).toUpperCase())}</span>
                ${esc(p.corretoraNome)}
            </span>
            <span class="m-qtd">${esc(fmtNumero(p.quantidade))} un.</span>
            <span class="m-preco-medio">${esc(fmtMoeda(p.precoMedio, moeda))}</span>
            <span class="m-preco-atual">${acao && acao.cotacaoAtual != null ? esc(fmtMoeda(acao.cotacaoAtual, moeda)) : "—"}</span>
            <span class="m-resultado">${fmtResultado(resultado, moeda)}</span>
            <span class="m-valor">${esc(fmtMoeda(p.valorAtual, moeda))}${fmtConvertido(p.valorAtual, p.acaoTicker)}</span>
            <span class="m-kebab-wrap">
                <button type="button" class="m-kebab" data-kebab="${i}" aria-label="Mais ações">${icone("kebab")}</button>
                <div class="row-menu" id="rowMenu${i}">
                    <button type="button" data-sell="${i}">Vender</button>
                </div>
            </span>
        </div>`;
    }).join("");

    renderSparklineNaoRealizado();
}

// Reaproveita a MESMA serie ja buscada pro grafico principal (state.rendimento
// = rendimento total realizado+nao-realizado ao longo do tempo) -- nao e uma
// decomposicao exata de "so nao realizado", mas e dado real, nao inventado; o
// rotulo do card ja deixa claro que e sobre a carteira como um todo.
function renderSparklineNaoRealizado() {
    const svg = $("#sparklineNaoRealizado");
    if (!svg) return;
    const pontos = state.rendimento.slice(-20);
    if (pontos.length < 2) { svg.innerHTML = ""; return; }

    const valores = pontos.map((p) => Number(p.rendimento));
    const min = Math.min(...valores), max = Math.max(...valores);
    const range = (max - min) || 1;
    const coords = valores.map((v, i) => {
        const x = (i / (valores.length - 1)) * 100;
        const y = 26 - ((v - min) / range) * 24 - 1;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
    });
    const cor = valores[valores.length - 1] >= valores[0] ? "var(--success)" : "var(--danger)";
    svg.innerHTML = `<polyline points="${coords.join(" ")}" fill="none" stroke="${cor}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></polyline>`;
}

/* ------------------------------- IBOV ------------------------------- */
// Widget decorativo da sidebar: dado real (brapi, mesmo provider ja usado
// pras acoes brasileiras). Falha silenciosamente (widget some) em vez de
// quebrar o dashboard -- e um extra, nao um dado essencial da carteira.
async function carregarIbovespa() {
    try {
        const r = await fetch("/mercado/ibovespa");
        if (!r.ok) throw new Error("ibovespa indisponivel");
        const indice = await r.json();
        $("#ibovNome").textContent = indice.nome;
        $("#ibovPontos").textContent = fmtNumero(indice.pontos) + " pts";
        const variacao = Number(indice.variacaoPercentual || 0);
        const el = $("#ibovVariacao");
        el.textContent = `${variacao >= 0 ? "+" : ""}${variacao.toFixed(2)}%`;
        el.style.color = variacao >= 0 ? "var(--success)" : "var(--danger)";
        $("#sidebarIbov").classList.remove("hidden");
    } catch {
        $("#sidebarIbov")?.classList.add("hidden");
    }
}

const PERIODO_DIAS = { "1D": 1, "1M": 30, "3M": 90, "6M": 180, "1A": 365, TUDO: null };

function filtrarPontosPorPeriodo(pontos, periodo) {
    const dias = PERIODO_DIAS[periodo];
    if (!dias) return pontos;
    const corte = Date.now() - dias * 24 * 60 * 60 * 1000;
    return pontos.filter((p) => new Date(p.timestamp).getTime() >= corte);
}

function renderRendimentoChart() {
    const wrap = $("#rendimentoChartWrap");
    const svg = $("#rendimentoChart");
    wrap.querySelector(".empty")?.remove();
    wrap.querySelector(".chart-skeleton")?.remove();
    wrap.querySelectorAll(".chart-axis-y, .chart-axis-x").forEach((el) => el.remove());

    const pontos = filtrarPontosPorPeriodo(state.rendimento, state.periodoChart);
    pontosChartAtual = [];

    if (pontos.length < 2) {
        svg.classList.add("hidden");
        $("#chartTooltip").classList.remove("show");
        const msg = state.rendimento.length < 2
            ? "Ainda não há pontos suficientes pro gráfico. Compre uma ação e atualize a cotação pra começar a ver o rendimento."
            : "Sem dados suficientes nesse período.";
        wrap.insertAdjacentHTML("beforeend", `<div class="empty">${msg}</div>`);
        return;
    }
    svg.classList.remove("hidden");

    const W = 600, H = 220, PAD_X = 8, PAD_Y = 16;
    const tempos = pontos.map((p) => new Date(p.timestamp).getTime());
    const valores = pontos.map((p) => Number(p.rendimento));

    const tMin = Math.min(...tempos), tMax = Math.max(...tempos);
    const tRange = tMax - tMin;
    const vMin = Math.min(0, ...valores), vMax = Math.max(0, ...valores);
    const vRange = (vMax - vMin) || 1;

    const xDe = (i) => tRange > 0 ? PAD_X + ((tempos[i] - tMin) / tRange) * (W - 2 * PAD_X) : (W / (pontos.length - 1)) * i;
    const yDe = (v) => H - PAD_Y - ((v - vMin) / vRange) * (H - 2 * PAD_Y);

    pontosChartAtual = pontos.map((p, i) => ({ x: xDe(i), y: yDe(valores[i]), valor: valores[i], data: p.timestamp }));
    const coords = pontosChartAtual.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`);

    const ultimo = valores[valores.length - 1];
    const cor = ultimo >= 0 ? "var(--success)" : "var(--danger)";
    const yZero = yDe(0);

    // Grid horizontal discreto: 3 linhas guia (25/50/75% da altura util).
    const grid = [0.25, 0.5, 0.75].map((f) => {
        const y = (PAD_Y + (H - 2 * PAD_Y) * f).toFixed(1);
        return `<line x1="0" y1="${y}" x2="${W}" y2="${y}" class="rendimento-grid"></line>`;
    }).join("");

    const areaId = "rendimentoAreaGrad";
    const area = `M${coords[0]} L${coords.join(" L")} L${pontosChartAtual[pontosChartAtual.length - 1].x.toFixed(1)},${H} L${pontosChartAtual[0].x.toFixed(1)},${H} Z`;

    svg.innerHTML = `
        <defs>
            <linearGradient id="${areaId}" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="${cor}" stop-opacity="0.5"></stop>
                <stop offset="100%" stop-color="${cor}" stop-opacity="0"></stop>
            </linearGradient>
        </defs>
        ${grid}
        <line x1="0" y1="${yZero.toFixed(1)}" x2="${W}" y2="${yZero.toFixed(1)}" class="rendimento-zero"></line>
        <path d="${area}" class="rendimento-area" fill="url(#${areaId})"></path>
        <line class="rendimento-guide hidden" id="rendimentoGuide" x1="0" y1="0" x2="0" y2="${H}"></line>
        <polyline points="${coords.join(" ")}" class="rendimento-linha" style="stroke:${cor}"></polyline>
        <circle class="rendimento-ponto-hover hidden" id="rendimentoPontoHover" style="stroke:${cor}" cx="0" cy="0"></circle>
    `;

    wrap.querySelectorAll(".chart-axis-y, .chart-axis-x").forEach((el) => el.remove());
    wrap.insertAdjacentHTML("beforeend", `
        <div class="chart-axis-y">
            <span style="top:${(PAD_Y / H * 100).toFixed(1)}%">${esc(fmtMoeda(vMax, "BRL"))}</span>
            <span style="top:${((H - PAD_Y) / H * 100).toFixed(1)}%">${esc(fmtMoeda(vMin, "BRL"))}</span>
        </div>
        <div class="chart-axis-x">
            <span>${esc(fmtData(pontos[0].timestamp))}</span>
            <span>${esc(fmtData(pontos[pontos.length - 1].timestamp))}</span>
        </div>
    `);
}

function moverTooltipChart(clientX, clientY) {
    if (!pontosChartAtual.length) return;
    const svg = $("#rendimentoChart");
    const wrap = $("#rendimentoChartWrap");
    const tooltip = $("#chartTooltip");
    const svgRect = svg.getBoundingClientRect();
    const wrapRect = wrap.getBoundingClientRect();
    if (clientX < svgRect.left || clientX > svgRect.right) { esconderTooltipChart(); return; }

    const escalaX = svgRect.width / 600;
    const xAlvo = (clientX - svgRect.left) / escalaX;
    let maisProximo = pontosChartAtual[0];
    for (const p of pontosChartAtual) {
        if (Math.abs(p.x - xAlvo) < Math.abs(maisProximo.x - xAlvo)) maisProximo = p;
    }

    const ponto = $("#rendimentoPontoHover");
    if (ponto) {
        ponto.setAttribute("cx", maisProximo.x.toFixed(1));
        ponto.setAttribute("cy", maisProximo.y.toFixed(1));
        ponto.classList.remove("hidden");
    }
    const guia = $("#rendimentoGuide");
    if (guia) {
        guia.setAttribute("x1", maisProximo.x.toFixed(1));
        guia.setAttribute("x2", maisProximo.x.toFixed(1));
        guia.classList.remove("hidden");
    }

    const pxX = (svgRect.left - wrapRect.left) + maisProximo.x * escalaX;
    const pxY = (svgRect.top - wrapRect.top) + maisProximo.y;
    tooltip.style.left = `${pxX}px`;
    tooltip.style.top = `${pxY}px`;
    tooltip.innerHTML = `<span class="tt-data">${esc(fmtData(maisProximo.data))}</span><span class="tt-valor">${esc(fmtMoeda(maisProximo.valor, "BRL"))}</span>`;
    tooltip.classList.add("show");
}

function esconderTooltipChart() {
    $("#chartTooltip")?.classList.remove("show");
    $("#rendimentoPontoHover")?.classList.add("hidden");
    $("#rendimentoGuide")?.classList.add("hidden");
}

function mudarPeriodoChart(periodo) {
    state.periodoChart = periodo;
    $$(".chart-periodo").forEach((b) => b.classList.toggle("active", b.dataset.periodo === periodo));
    renderRendimentoChart();
}

function renderAcoes() {
    const filtro = $("#filterAcoes").value.trim().toLowerCase();
    const tbody = $("#tableAcoes tbody");
    const lista = state.acoes.filter((a) =>
        !filtro || (a.ticker || "").toLowerCase().includes(filtro) || (a.nomeEmpresa || "").toLowerCase().includes(filtro));

    $("#countAcoes").textContent = state.acoes.length;
    $("#emptyAcoes").classList.toggle("hidden", state.acoes.length > 0);

    tbody.innerHTML = lista.map((a) => `
        <tr>
            <td><div class="acao-ticker-col">${acaoLogoHTML(a)}${esc(a.ticker)}</div></td>
            <td>${esc(a.nomeEmpresa || "—")}</td>
            <td>${mercadoTag(a.mercado)}</td>
            <td class="num">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}${a.cotacaoAtualBRL != null ? `<span class="valor-convertido">≈ ${esc(fmtMoeda(a.cotacaoAtualBRL, "BRL"))}</span>` : ""}</td>
            <td class="acoes-col">
                <button class="btn btn-buy btn-icon" data-buy="${a.id}">Comprar</button>
                <button class="btn btn-ghost btn-icon" title="Atualizar cotação" data-refresh-acao="${a.id}">${icone("refresh")}</button>
                <button class="btn btn-ghost btn-icon" title="Detalhes" data-detail-acao="${a.id}">${icone("expand")}</button>
                <button class="btn btn-ghost btn-icon" title="Excluir" data-delete-acao="${a.id}">${icone("trash")}</button>
            </td>
        </tr>`).join("");

    if (state.acoes.length && !lista.length) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty">Nenhum resultado para o filtro.</td></tr>`;
    }
}

function corretorasDasPosicoes() {
    return [...new Set(state.posicoes.map((p) => p.corretoraNome).filter(Boolean))].sort();
}

function popularFiltroCorretoras() {
    const select = $("#filterPosicoesCorretora");
    const atual = select.value;
    const opcoes = corretorasDasPosicoes();
    select.innerHTML = `<option value="">Todas as corretoras</option>` +
        opcoes.map((c) => `<option value="${esc(c)}">${esc(c)}</option>`).join("");
    if (opcoes.includes(atual)) select.value = atual;
}

function resultadoPosicao(p) {
    const resultado = Number(p.valorAtual) - Number(p.valorInvestido);
    if (resultado > 0) return "lucro";
    if (resultado < 0) return "prejuizo";
    return "neutro";
}

function renderPosicoes() {
    const filtro = $("#filterPosicoes").value.trim().toLowerCase();
    const mercado = $("#filterPosicoesMercado").value;
    const corretora = $("#filterPosicoesCorretora").value;
    const filtroResultado = $("#filterPosicoesResultado").value;
    const tbody = $("#tablePosicoes tbody");
    const lista = state.posicoes.filter((p) =>
        (!filtro || (p.acaoTicker || "").toLowerCase().includes(filtro) || (p.corretoraNome || "").toLowerCase().includes(filtro)) &&
        (!mercado || acaoPorTicker(p.acaoTicker)?.mercado === mercado) &&
        (!corretora || p.corretoraNome === corretora) &&
        (!filtroResultado || resultadoPosicao(p) === filtroResultado));

    $("#countPosicoes").textContent = lista.length;
    $("#emptyPosicoes").classList.toggle("hidden", state.posicoes.length > 0);

    tbody.innerHTML = lista.map((p, i) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        const resultado = p.valorAtual != null ? Number(p.valorAtual) - Number(p.valorInvestido) : null;
        return `
        <tr>
            <td>${esc(p.acaoTicker)}</td>
            <td>${esc(p.corretoraNome)}</td>
            <td class="num">${esc(fmtNumero(p.quantidade))}</td>
            <td class="num">${esc(fmtMoeda(p.precoMedio, moeda))}${fmtConvertido(p.precoMedio, p.acaoTicker)}</td>
            <td class="num">${esc(fmtMoeda(p.valorInvestido, moeda))}${fmtConvertido(p.valorInvestido, p.acaoTicker)}</td>
            <td class="num">${esc(fmtMoeda(p.valorAtual, moeda))}${fmtConvertido(p.valorAtual, p.acaoTicker)}</td>
            <td class="num">${fmtResultado(resultado, moeda)}${fmtConvertido(resultado, p.acaoTicker)}</td>
            <td class="acoes-col"><button class="btn btn-sell btn-icon" data-sell="${i}">Vender</button></td>
        </tr>`;
    }).join("");

    if (state.posicoes.length && !lista.length) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty">Nenhum resultado para o filtro.</td></tr>`;
    }
}

function tipoTag(tipo) {
    return tipo === "COMPRA" ? '<span class="tag tag-buy">COMPRA</span>' : '<span class="tag tag-sell">VENDA</span>';
}

function renderOperacoes() {
    const filtro = $("#filterOperacoes").value.trim().toLowerCase();
    const tbody = $("#tableOperacoes tbody");
    const lista = state.operacoes.filter((o) => !filtro || (o.acaoTicker || "").toLowerCase().includes(filtro));

    $("#countOperacoes").textContent = state.operacoes.length;
    $("#emptyOperacoes").classList.toggle("hidden", state.operacoes.length > 0);

    tbody.innerHTML = lista.map((o) => {
        const moeda = moedaPorTicker(o.acaoTicker);
        // Usa a taxa de cambio gravada na propria operacao (a taxa real, historica,
        // travada no momento da negociacao) -- NAO a taxa de agora. O dinheiro que
        // realmente entrou/saiu do saldo usou essa taxa historica (SaldoCalculator),
        // entao a exibicao precisa bater com isso, nao com a cotacao atual do cambio.
        const taxaHistorica = o.taxaCambio != null && Number(o.taxaCambio) !== 1 ? Number(o.taxaCambio) : null;
        const convertidoPreco = taxaHistorica != null
            ? `<span class="valor-convertido">≈ ${esc(fmtMoeda(Number(o.precoUnitario) * taxaHistorica, "BRL"))}</span>`
            : "";
        const convertidoResultado = taxaHistorica != null
            ? `<span class="valor-convertido">≈ ${esc(fmtMoeda(Number(o.lucroPrejuizoRealizado || 0) * taxaHistorica, "BRL"))}</span>`
            : "";
        return `
        <tr>
            <td>${esc(fmtData(o.dataHora))}</td>
            <td>${esc(o.acaoTicker)}</td>
            <td>${esc(o.corretoraNome)}</td>
            <td>${tipoTag(o.tipo)}</td>
            <td class="num">${esc(fmtNumero(o.quantidade))}</td>
            <td class="num">${esc(fmtMoeda(o.precoUnitario, moeda))}${convertidoPreco}</td>
            <td>${o.status === "CANCELADA" ? '<span class="tag">Cancelada</span>' : '<span class="tag tag-cvm">Ativa</span>'}</td>
            <td class="num">${o.tipo === "VENDA" ? fmtResultado(o.lucroPrejuizoRealizado, moeda) + convertidoResultado : '<span class="pl pl-zero">—</span>'}</td>
        </tr>`;
    }).join("");

    if (state.operacoes.length && !lista.length) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty">Nenhum resultado para o filtro.</td></tr>`;
    }

    const pag = state.operacoesPagina;
    $("#labelPaginaOperacoes").textContent = `Página ${pag.numero + 1} de ${Math.max(pag.totalPages, 1)}`;
    $("#btnOperacoesAnterior").disabled = pag.numero <= 0;
    $("#btnOperacoesProxima").disabled = pag.numero >= pag.totalPages - 1;
}

async function mudarPaginaOperacoes(delta) {
    const alvo = state.operacoesPagina.numero + delta;
    if (alvo < 0 || alvo >= state.operacoesPagina.totalPages) return;
    await carregarOperacoes(alvo);
    renderOperacoes();
}

function renderCorretoras() {
    const filtro = $("#filterCorretoras").value.trim().toLowerCase();
    const wrap = $("#cardsCorretoras");
    const lista = state.corretoras.filter((c) =>
        !filtro ||
        (c.razaoSocial || "").toLowerCase().includes(filtro) ||
        (c.nomeFantasia || "").toLowerCase().includes(filtro) ||
        onlyDigits(c.cnpj).includes(onlyDigits(filtro)));

    $("#countCorretoras").textContent = state.corretoras.length;
    $("#emptyCorretoras").classList.toggle("hidden", state.corretoras.length > 0);

    wrap.innerHTML = lista.map((c) => `
        <div class="ccard" data-detail-corretora="${c.id}">
            <div class="ccard-top">
                <div>
                    <h3>${esc(c.nomeFantasia || c.razaoSocial || "—")}</h3>
                    ${c.nomeFantasia && c.razaoSocial ? `<div class="fantasia">${esc(c.razaoSocial)}</div>` : ""}
                </div>
                ${c.validadaNaCvm ? `<span class="tag tag-cvm">${icone("badgeCheck", 12)} CVM</span>` : ""}
            </div>
            <div class="row">${esc(fmtCnpj(c.cnpj))}</div>
            <div class="row">${icone("mapPin", 13)} <b>${esc(c.cidade || "—")}${c.uf ? "/" + esc(c.uf) : ""}</b></div>
        </div>`).join("");

    if (state.corretoras.length && !lista.length) {
        wrap.innerHTML = `<div class="empty">Nenhum resultado para o filtro.</div>`;
    }
}

function renderTudo() {
    renderVisaoGeral();
    renderAcoes();
    popularFiltroCorretoras();
    renderPosicoes();
    renderOperacoes();
    renderCorretoras();
}

/* --------------------------- Carregar ------------------------------ */
async function carregarOperacoes(pagina = 0) {
    const resp = await api(`/carteiras/me/operacoes?page=${pagina}&size=20`);
    state.operacoes = resp.content || [];
    state.operacoesPagina = { numero: resp.number ?? pagina, totalPages: resp.totalPages ?? 1 };
}

function mostrarSkeletonChart() {
    const wrap = $("#rendimentoChartWrap");
    $("#rendimentoChart")?.classList.add("hidden");
    esconderTooltipChart();
    wrap.querySelector(".empty")?.remove();
    if (!wrap.querySelector(".chart-skeleton")) {
        wrap.insertAdjacentHTML("beforeend", `<div class="chart-skeleton"></div>`);
    }
}

async function carregarTudo() {
    mostrarSkeletonChart();
    try {
        // Ações e corretoras pedem uma página grande porque o dashboard as usa
        // como cache completo (dropdown do modal de compra, lookup de moeda por
        // ticker, botões de detalhe) -- não é só a tabela do catálogo.
        const [saldo, posicoes, acoes, corretoras, rendimento] = await Promise.all([
            api("/carteiras/me/saldo"),
            api("/carteiras/me"),
            api("/acoes?size=1000"),
            api("/corretoras?size=1000"),
            api("/carteiras/me/rendimento-historico"),
        ]);
        state.saldo = saldo;
        state.posicoes = posicoes || [];
        state.acoes = acoes.content || [];
        state.corretoras = corretoras.content || [];
        state.rendimento = rendimento || [];
        await carregarOperacoes(state.operacoesPagina.numero);
        renderTudo();
    } catch (e) {
        toast("Falha ao carregar dados", e.message, "err");
    }
    carregarIbovespa(); // widget decorativo -- nao bloqueia nem falha o resto do dashboard
}

/* --------------------------- Detalhes ------------------------------ */
function abrirModal(html) {
    $("#modalContent").innerHTML = html;
    $("#modal").classList.remove("hidden");
}
function fecharModal() { $("#modal").classList.add("hidden"); }

function detalheAcaoHTML(a) {
    return `
        <h2>${acaoLogoHTML(a, 32)}${esc(a.ticker)} ${mercadoTag(a.mercado)}</h2>
        <p class="sub">${esc(a.nomeEmpresa || "Empresa não informada")}</p>
        <div class="detail-grid">
            <div class="detail-item"><span class="k">Cotação atual</span><span class="v">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}${a.cotacaoAtualBRL != null ? `<span class="valor-convertido">≈ ${esc(fmtMoeda(a.cotacaoAtualBRL, "BRL"))}</span>` : ""}</span></div>
            <div class="detail-item"><span class="k">Atualizada em</span><span class="v">${esc(fmtData(a.dataHoraCotacao))}</span></div>
        </div>
        <div style="margin-top:20px; display:flex; gap:10px; flex-wrap:wrap">
            <button class="btn btn-buy" data-buy="${a.id}">Comprar</button>
            <button class="btn" data-refresh-acao="${a.id}">${icone("refresh")} Atualizar cotação</button>
        </div>`;
}

function detalheCorretoraHTML(c) {
    const endereco = [c.logradouro, c.numero, c.complemento].filter(Boolean).join(", ");
    return `
        <h2>${esc(c.nomeFantasia || c.razaoSocial)} ${c.validadaNaCvm ? `<span class="tag tag-cvm">${icone("badgeCheck", 12)} CVM</span>` : ""}</h2>
        <p class="sub">${esc(c.razaoSocial || "")}</p>
        <div class="detail-grid">
            <div class="detail-item"><span class="k">CNPJ</span><span class="v">${esc(fmtCnpj(c.cnpj))}</span></div>
            <div class="detail-item"><span class="k">Situação cadastral</span><span class="v">${esc(c.situacaoCadastral || "—")}</span></div>
            <div class="detail-item"><span class="k">E-mail</span><span class="v">${esc(c.email || "—")}</span></div>
            <div class="detail-item"><span class="k">Telefone</span><span class="v">${esc(c.telefone || "—")}</span></div>
            <div class="detail-item detail-full"><span class="k">Endereço</span><span class="v">${esc(endereco || "—")}</span></div>
            <div class="detail-item"><span class="k">Cidade / UF</span><span class="v">${esc(c.cidade || "—")}${c.uf ? " / " + esc(c.uf) : ""}</span></div>
        </div>`;
}

/* ----------------------- Modal de operacao -------------------------- */
function abrirCompraModal(acaoId) {
    const a = state.acoes.find((x) => x.id === acaoId);
    if (!a) return;
    const cotacao = a.cotacaoAtual != null ? Number(a.cotacaoAtual) : "";

    abrirModal(`
        <h2>Comprar ${esc(a.ticker)} ${tipoTag("COMPRA")}</h2>
        <p class="sub">${esc(a.nomeEmpresa || "")}</p>
        <form id="formOperacao" class="form" data-tipo="COMPRA">
            <label class="field">
                <span>Corretora</span>
                <select name="corretoraId" required>
                    <option value="">Selecione…</option>
                    ${state.corretoras.map((c) => `<option value="${c.id}">${esc(c.nomeFantasia || c.razaoSocial)} — ${esc(fmtCnpj(c.cnpj))}</option>`).join("")}
                </select>
            </label>
            <label class="field">
                <span>Quantidade</span>
                <input name="quantidade" type="number" min="0.000001" step="any" placeholder="Ex.: 100" required />
            </label>
            <label class="field">
                <span>Preço unitário</span>
                <input name="precoUnitario" type="number" min="0.01" step="0.01" value="${cotacao}" required />
            </label>
            <div id="opResumo"></div>
            <button class="btn btn-buy" type="submit" id="btnOperacao">Confirmar compra</button>
            <p class="form-hint">A compra recalcula o preço médio da posição nessa corretora.</p>
        </form>`);

    ligarResumoOperacao(a, null);
}

function abrirVendaModal(posicaoIndex) {
    const p = state.posicoes[posicaoIndex];
    if (!p) return;
    const a = state.acoes.find((x) => x.ticker === p.acaoTicker);
    const corretora = state.corretoras.find((c) => c.nomeFantasia === p.corretoraNome);
    if (!a || !corretora) {
        // ponytail: resolucao de id via ticker/nomeFantasia porque PosicaoDTO
        // nao expoe acaoId/corretoraId -- adicionar esses campos ao DTO se
        // nomeFantasia deixar de ser um identificador confiavel o bastante.
        toast("Não foi possível abrir a venda", "Ação ou corretora não encontrada no catálogo.", "err");
        return;
    }
    const cotacao = a.cotacaoAtual != null ? Number(a.cotacaoAtual) : "";

    abrirModal(`
        <h2>Vender ${esc(p.acaoTicker)} ${tipoTag("VENDA")}</h2>
        <p class="sub">${esc(p.corretoraNome)}</p>
        <div class="op-info">Posição atual: <b>${esc(fmtNumero(p.quantidade))} un.</b> a preço médio de <b>${esc(fmtMoeda(p.precoMedio, moedaPorTicker(p.acaoTicker)))}</b></div>
        <form id="formOperacao" class="form" data-tipo="VENDA" data-acao-id="${a.id}" data-corretora-id="${corretora.id}">
            <label class="field">
                <span>Quantidade (máx. ${esc(fmtNumero(p.quantidade))})</span>
                <input name="quantidade" type="number" min="0.000001" max="${p.quantidade}" step="any" placeholder="Ex.: 50" required />
            </label>
            <label class="field">
                <span>Preço unitário</span>
                <input name="precoUnitario" type="number" min="0.01" step="0.01" value="${cotacao}" required />
            </label>
            <div id="opResumo"></div>
            <button class="btn btn-sell" type="submit" id="btnOperacao">Confirmar venda</button>
            <p class="form-hint">A venda calcula o lucro/prejuízo: (preço de venda − preço médio) × quantidade.</p>
        </form>`);

    ligarResumoOperacao(a, p);
}

function ligarResumoOperacao(acao, posicao) {
    const form = $("#formOperacao");
    const moeda = acao.moeda;
    const calcResumo = () => {
        const qtd = Number(form.quantidade.value || 0);
        const preco = Number(form.precoUnitario.value || 0);
        const box = $("#opResumo");
        if (!qtd || qtd <= 0 || !preco) { box.innerHTML = ""; return; }
        const total = qtd * preco;
        let html = `<div class="op-resumo-row"><span>Valor total</span><b>${esc(fmtMoeda(total, moeda))}${fmtConvertido(total, acao.ticker)}</b></div>`;
        if (posicao) {
            const resultado = (preco - Number(posicao.precoMedio)) * qtd;
            html += `<div class="op-resumo-row"><span>Resultado estimado</span>${fmtResultado(resultado, moeda)}${fmtConvertido(resultado, acao.ticker)}</div>`;
        }
        box.innerHTML = html;
    };
    form.quantidade.addEventListener("input", calcResumo);
    form.precoUnitario.addEventListener("input", calcResumo);
    calcResumo();
    form.addEventListener("submit", (e) => submitOperacao(e, acao));
    setTimeout(() => (form.corretoraId || form.quantidade).focus(), 50);
}

async function submitOperacao(e, acao) {
    e.preventDefault();
    const form = e.target;
    const tipo = form.dataset.tipo;
    const btn = $("#btnOperacao");

    const acaoId = tipo === "COMPRA" ? acao.id : Number(form.dataset.acaoId);
    const corretoraId = tipo === "COMPRA" ? Number(form.corretoraId.value) : Number(form.dataset.corretoraId);
    const quantidade = Number(form.quantidade.value);
    const precoUnitario = Number(form.precoUnitario.value);

    if (tipo === "COMPRA" && !corretoraId) return toast("Validação", "Selecione uma corretora.", "err");
    if (!quantidade || quantidade <= 0) return toast("Validação", "Informe uma quantidade válida.", "err");
    if (!precoUnitario || precoUnitario <= 0) return toast("Validação", "Informe um preço unitário válido.", "err");

    const resumoHtml = $("#opResumo").innerHTML;
    const { isConfirmed } = await Swal.fire({
        title: tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda",
        html: `<div class="swal-resumo">${resumoHtml}</div>`,
        showCancelButton: true,
        confirmButtonText: tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda",
        cancelButtonText: "Cancelar",
    });
    if (!isConfirmed) return;

    setLoading(btn, true, tipo === "COMPRA" ? "Comprando…" : "Vendendo…");
    try {
        await api("/operacoes", {
            method: "POST",
            body: JSON.stringify({ acaoId, corretoraId, tipo, quantidade, precoUnitario }),
        });
        toast(tipo === "COMPRA" ? "Compra registrada" : "Venda registrada", `${quantidade} un. de ${acao.ticker}`, "ok");
        fecharModal();
        await carregarTudo();
    } catch (err) {
        toast(tipo === "COMPRA" ? "Falha na compra" : "Falha na venda", err.message, "err");
    } finally {
        setLoading(btn, false, tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda");
    }
}

/* ------------------------- Acoes: submit ---------------------------- */
async function submitAcao(e) {
    e.preventDefault();
    const btn = $("#btnSalvarAcao");
    const fd = new FormData(e.target);
    const body = { ticker: (fd.get("ticker") || "").trim().toUpperCase(), mercado: fd.get("mercado") };
    if (!body.ticker) return toast("Validação", "Informe o ticker.", "err");

    setLoading(btn, true, "Cadastrando…");
    try {
        const nova = await api("/acoes", { method: "POST", body: JSON.stringify(body) });
        toast("Ação cadastrada", `${nova.ticker} — ${fmtMoeda(nova.cotacaoAtual, nova.moeda)}`, "ok");
        e.target.reset();
        await carregarTudo();
    } catch (err) {
        toast("Não foi possível cadastrar", err.message, "err");
    } finally {
        setLoading(btn, false, "Cadastrar ação");
    }
}

async function buscarAcao(e) {
    e.preventDefault();
    const ticker = $("#buscaTicker").value.trim();
    const box = $("#buscaAcaoResultado");
    if (!ticker) return;
    box.innerHTML = `<div class="loading-row"><span class="spin"></span> Buscando…</div>`;
    try {
        const a = await api(`/acoes/ticker/${encodeURIComponent(ticker)}`);
        box.innerHTML = `<div class="panel" style="background-color:var(--background)">${detalheAcaoHTML(a)}</div>`;
    } catch (err) {
        box.innerHTML = `<div class="empty">${esc(err.message)}</div>`;
    }
}

async function atualizarCotacao(id) {
    toast("Atualizando cotação…", "", "info");
    try {
        const a = await api(`/acoes/${id}/atualizar-cotacao`, { method: "PUT" });
        toast("Cotação atualizada", `${a.ticker} — ${fmtMoeda(a.cotacaoAtual, a.moeda)}`, "ok");
        await carregarTudo();
    } catch (err) {
        toast("Falha ao atualizar", err.message, "err");
    }
}

async function excluirAcao(id) {
    const a = state.acoes.find((x) => x.id === id);
    if (!a) return;

    const { isConfirmed } = await Swal.fire({
        title: "Excluir ação?",
        html: `Isso remove <b>${esc(a.ticker)}</b> do catálogo. Só é possível excluir ações sem compras/vendas registradas.`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Excluir",
        cancelButtonText: "Cancelar",
    });
    if (!isConfirmed) return;

    try {
        await api(`/acoes/${id}`, { method: "DELETE" });
        toast("Ação excluída", a.ticker, "ok");
        await carregarTudo();
    } catch (err) {
        toast("Não foi possível excluir", err.message, "err");
    }
}

/* ----------------------- Corretoras: submit -------------------------- */
async function submitCorretora(e) {
    e.preventDefault();
    const btn = $("#btnSalvarCorretora");
    const fd = new FormData(e.target);
    const cnpj = onlyDigits(fd.get("cnpj"));
    if (cnpj.length !== 14) return toast("Validação", "CNPJ deve conter 14 dígitos.", "err");

    setLoading(btn, true, "Validando e cadastrando…");
    try {
        const nova = await api("/corretoras", { method: "POST", body: JSON.stringify({ cnpj }) });
        toast("Corretora cadastrada", nova.nomeFantasia || nova.razaoSocial, "ok");
        e.target.reset();
        await carregarTudo();
    } catch (err) {
        toast("Não foi possível cadastrar", err.message, "err");
    } finally {
        setLoading(btn, false, "Cadastrar corretora");
    }
}

async function buscarCorretora(e) {
    e.preventDefault();
    const cnpj = onlyDigits($("#buscaCnpj").value);
    const box = $("#buscaCorretoraResultado");
    if (!cnpj) return;
    box.innerHTML = `<div class="loading-row"><span class="spin"></span> Buscando…</div>`;
    try {
        const c = await api(`/corretoras/cnpj/${encodeURIComponent(cnpj)}`);
        box.innerHTML = `<div class="panel" style="background-color:var(--background)">${detalheCorretoraHTML(c)}</div>`;
    } catch (err) {
        box.innerHTML = `<div class="empty">${esc(err.message)}</div>`;
    }
}

/* ----------------------------- UI helpers ---------------------------- */
function setLoading(btn, loading, label) {
    if (!btn) return;
    btn.disabled = loading;
    btn.innerHTML = loading ? `<span class="spin"></span> ${label}` : label;
}

/* ---------------------------- Eventos --------------------------------- */
function bind() {
    $$(".nav-item").forEach((b) => b.addEventListener("click", () => setView(b.dataset.view)));
    $$("[data-goto]").forEach((b) => b.addEventListener("click", () => setView(b.dataset.goto)));
    $("#menuToggle").addEventListener("click", () => $("#sidebar").classList.toggle("open"));
    $("#refreshAll").addEventListener("click", carregarTudo);

    $("#formAcao").addEventListener("submit", submitAcao);
    $("#formBuscaAcao").addEventListener("submit", buscarAcao);
    $("#formCorretora").addEventListener("submit", submitCorretora);
    $("#formBuscaCorretora").addEventListener("submit", buscarCorretora);

    $("#filterAcoes").addEventListener("input", renderAcoes);
    $("#filterPosicoes").addEventListener("input", renderPosicoes);
    $("#filterPosicoesMercado").addEventListener("change", renderPosicoes);
    $("#filterPosicoesCorretora").addEventListener("change", renderPosicoes);
    $("#filterPosicoesResultado").addEventListener("change", renderPosicoes);
    $("#filterOperacoes").addEventListener("input", renderOperacoes);
    $("#filterCorretoras").addEventListener("input", renderCorretoras);

    $("#btnOperacoesAnterior").addEventListener("click", () => mudarPaginaOperacoes(-1));
    $("#btnOperacoesProxima").addEventListener("click", () => mudarPaginaOperacoes(1));

    $$(".chart-periodo").forEach((b) => b.addEventListener("click", () => mudarPeriodoChart(b.dataset.periodo)));
    const chartWrap = $("#rendimentoChartWrap");
    chartWrap.addEventListener("mousemove", (e) => moverTooltipChart(e.clientX, e.clientY));
    chartWrap.addEventListener("mouseleave", esconderTooltipChart);

    $("#modalClose").addEventListener("click", fecharModal);
    $("#modal").addEventListener("click", (e) => { if (e.target.id === "modal") fecharModal(); });
    document.addEventListener("keydown", (e) => { if (e.key === "Escape") fecharModal(); });

    document.addEventListener("click", (e) => {
        const kebab = e.target.closest("[data-kebab]");
        if (kebab) {
            const menu = $(`#rowMenu${kebab.dataset.kebab}`);
            const jaAberto = menu.classList.contains("open");
            $$(".row-menu.open").forEach((m) => m.classList.remove("open"));
            if (!jaAberto) menu.classList.add("open");
            return;
        }
        if (!e.target.closest(".row-menu")) $$(".row-menu.open").forEach((m) => m.classList.remove("open"));

        const buy = e.target.closest("[data-buy]");
        if (buy) { abrirCompraModal(Number(buy.dataset.buy)); return; }

        const sell = e.target.closest("[data-sell]");
        if (sell) { abrirVendaModal(Number(sell.dataset.sell)); return; }

        const r = e.target.closest("[data-refresh-acao]");
        if (r) { atualizarCotacao(Number(r.dataset.refreshAcao)); return; }

        const del = e.target.closest("[data-delete-acao]");
        if (del) { excluirAcao(Number(del.dataset.deleteAcao)); return; }

        const da = e.target.closest("[data-detail-acao]");
        if (da) {
            const a = state.acoes.find((x) => x.id === Number(da.dataset.detailAcao));
            if (a) abrirModal(detalheAcaoHTML(a));
            return;
        }
        const dc = e.target.closest("[data-detail-corretora]");
        if (dc) {
            const c = state.corretoras.find((x) => x.id === Number(dc.dataset.detailCorretora));
            if (c) abrirModal(detalheCorretoraHTML(c));
            return;
        }
    });
}

/* ----------------------------- Init ------------------------------------ */
bind();
carregarTudo();
