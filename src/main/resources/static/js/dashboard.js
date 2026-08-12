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
    corretoras: [],
};

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
    return m === "EUA" ? `<span class="tag">🇺🇸 EUA</span>` : `<span class="tag">🇧🇷 Brasil</span>`;
}

function fmtResultado(valor, moeda) {
    if (valor == null) return '<span class="pl pl-zero">—</span>';
    const n = Number(valor);
    if (n === 0) return `<span class="pl pl-zero">${esc(fmtMoeda(0, moeda))}</span>`;
    const cls = n > 0 ? "pl-pos" : "pl-neg";
    const sinal = n > 0 ? "+" : "-";
    return `<span class="pl ${cls}">${sinal}${esc(fmtMoeda(Math.abs(n), moeda))}</span>`;
}

/* --------------------------- Toasts ------------------------------ */
function toast(titulo, msg = "", tipo = "ok") {
    const el = document.createElement("div");
    el.className = `toast ${tipo}`;
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
    for (const p of state.posicoes) {
        if (p.valorAtual != null && p.valorInvestido != null) naoRealizado += Number(p.valorAtual) - Number(p.valorInvestido);
    }
    const realizado = state.operacoes
        .filter((o) => o.tipo === "VENDA" && o.status === "ATIVA")
        .reduce((s, o) => s + Number(o.lucroPrejuizoRealizado || 0), 0);

    $("#statNaoRealizado").innerHTML = fmtResultado(naoRealizado, "BRL");
    $("#statRealizado").innerHTML = fmtResultado(realizado, "BRL");

    const dash = $("#dashPosicoes");
    const recentes = state.posicoes.slice(0, 6);
    if (!recentes.length) {
        dash.innerHTML = `<div class="empty">Nenhuma posição ainda. Vá em <b>Ações</b> para comprar.</div>`;
        return;
    }
    dash.innerHTML = recentes.map((p) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        return `
        <div class="mini-row">
            <span class="m-ticker">${esc(p.acaoTicker)}</span>
            <span class="m-corretora">${esc(p.corretoraNome)}</span>
            <span class="m-qtd">${esc(fmtNumero(p.quantidade))} un.</span>
            <span class="m-preco-medio">PM ${esc(fmtMoeda(p.precoMedio, moeda))}</span>
            <span class="m-valor">${esc(fmtMoeda(p.valorAtual, moeda))}</span>
        </div>`;
    }).join("");
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
            <td>${esc(a.ticker)}</td>
            <td>${esc(a.nomeEmpresa || "—")}</td>
            <td>${mercadoTag(a.mercado)}</td>
            <td class="num">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}</td>
            <td class="acoes-col">
                <button class="btn btn-buy btn-icon" data-buy="${a.id}">Comprar</button>
                <button class="btn btn-icon" title="Atualizar cotação" data-refresh-acao="${a.id}">⟳</button>
                <button class="btn btn-icon" title="Detalhes" data-detail-acao="${a.id}">⤢</button>
            </td>
        </tr>`).join("");

    if (state.acoes.length && !lista.length) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty">Nenhum resultado para o filtro.</td></tr>`;
    }
}

function renderPosicoes() {
    const filtro = $("#filterPosicoes").value.trim().toLowerCase();
    const tbody = $("#tablePosicoes tbody");
    const lista = state.posicoes.filter((p) =>
        !filtro || (p.acaoTicker || "").toLowerCase().includes(filtro) || (p.corretoraNome || "").toLowerCase().includes(filtro));

    $("#countPosicoes").textContent = state.posicoes.length;
    $("#emptyPosicoes").classList.toggle("hidden", state.posicoes.length > 0);

    tbody.innerHTML = lista.map((p, i) => {
        const moeda = moedaPorTicker(p.acaoTicker);
        const resultado = p.valorAtual != null ? Number(p.valorAtual) - Number(p.valorInvestido) : null;
        return `
        <tr>
            <td>${esc(p.acaoTicker)}</td>
            <td>${esc(p.corretoraNome)}</td>
            <td class="num">${esc(fmtNumero(p.quantidade))}</td>
            <td class="num">${esc(fmtMoeda(p.precoMedio, moeda))}</td>
            <td class="num">${esc(fmtMoeda(p.valorInvestido, moeda))}</td>
            <td class="num">${esc(fmtMoeda(p.valorAtual, moeda))}</td>
            <td class="num">${fmtResultado(resultado, moeda)}</td>
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
        return `
        <tr>
            <td>${esc(fmtData(o.dataHora))}</td>
            <td>${esc(o.acaoTicker)}</td>
            <td>${esc(o.corretoraNome)}</td>
            <td>${tipoTag(o.tipo)}</td>
            <td class="num">${esc(fmtNumero(o.quantidade))}</td>
            <td class="num">${esc(fmtMoeda(o.precoUnitario, moeda))}</td>
            <td>${o.status === "CANCELADA" ? '<span class="tag">Cancelada</span>' : '<span class="tag tag-cvm">Ativa</span>'}</td>
            <td class="num">${o.tipo === "VENDA" ? fmtResultado(o.lucroPrejuizoRealizado, moeda) : '<span class="pl pl-zero">—</span>'}</td>
        </tr>`;
    }).join("");

    if (state.operacoes.length && !lista.length) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty">Nenhum resultado para o filtro.</td></tr>`;
    }
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
                ${c.validadaNaCvm ? '<span class="tag tag-cvm">✔ CVM</span>' : ""}
            </div>
            <div class="row">${esc(fmtCnpj(c.cnpj))}</div>
            <div class="row">📍 <b>${esc(c.cidade || "—")}${c.uf ? "/" + esc(c.uf) : ""}</b></div>
        </div>`).join("");

    if (state.corretoras.length && !lista.length) {
        wrap.innerHTML = `<div class="empty">Nenhum resultado para o filtro.</div>`;
    }
}

function renderTudo() {
    renderVisaoGeral();
    renderAcoes();
    renderPosicoes();
    renderOperacoes();
    renderCorretoras();
}

/* --------------------------- Carregar ------------------------------ */
async function carregarTudo() {
    try {
        const [saldo, posicoes, acoes, operacoes, corretoras] = await Promise.all([
            api("/carteiras/me/saldo"),
            api("/carteiras/me"),
            api("/acoes"),
            api("/carteiras/me/operacoes"),
            api("/corretoras"),
        ]);
        state.saldo = saldo;
        state.posicoes = posicoes || [];
        state.acoes = acoes || [];
        state.operacoes = operacoes || [];
        state.corretoras = corretoras || [];
        renderTudo();
    } catch (e) {
        toast("Falha ao carregar dados", e.message, "err");
    }
}

/* --------------------------- Detalhes ------------------------------ */
function abrirModal(html) {
    $("#modalContent").innerHTML = html;
    $("#modal").classList.remove("hidden");
}
function fecharModal() { $("#modal").classList.add("hidden"); }

function detalheAcaoHTML(a) {
    return `
        <h2>${esc(a.ticker)} ${mercadoTag(a.mercado)}</h2>
        <p class="sub">${esc(a.nomeEmpresa || "Empresa não informada")}</p>
        <div class="detail-grid">
            <div class="detail-item"><span class="k">Cotação atual</span><span class="v">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}</span></div>
            <div class="detail-item"><span class="k">Atualizada em</span><span class="v">${esc(fmtData(a.dataHoraCotacao))}</span></div>
        </div>
        <div style="margin-top:20px; display:flex; gap:10px; flex-wrap:wrap">
            <button class="btn btn-buy" data-buy="${a.id}">Comprar</button>
            <button class="btn" data-refresh-acao="${a.id}">⟳ Atualizar cotação</button>
        </div>`;
}

function detalheCorretoraHTML(c) {
    const endereco = [c.logradouro, c.numero, c.complemento].filter(Boolean).join(", ");
    return `
        <h2>${esc(c.nomeFantasia || c.razaoSocial)} ${c.validadaNaCvm ? '<span class="tag tag-cvm">✔ CVM</span>' : ""}</h2>
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
        let html = `<div class="op-resumo-row"><span>Valor total</span><b>${esc(fmtMoeda(total, moeda))}</b></div>`;
        if (posicao) {
            const resultado = (preco - Number(posicao.precoMedio)) * qtd;
            html += `<div class="op-resumo-row"><span>Resultado estimado</span>${fmtResultado(resultado, moeda)}</div>`;
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
        box.innerHTML = `<div class="panel" style="background-color:var(--rendo-color-bg)">${detalheAcaoHTML(a)}</div>`;
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
        box.innerHTML = `<div class="panel" style="background-color:var(--rendo-color-bg)">${detalheCorretoraHTML(c)}</div>`;
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
    $("#filterOperacoes").addEventListener("input", renderOperacoes);
    $("#filterCorretoras").addEventListener("input", renderCorretoras);

    $("#modalClose").addEventListener("click", fecharModal);
    $("#modal").addEventListener("click", (e) => { if (e.target.id === "modal") fecharModal(); });
    document.addEventListener("keydown", (e) => { if (e.key === "Escape") fecharModal(); });

    document.addEventListener("click", (e) => {
        const buy = e.target.closest("[data-buy]");
        if (buy) { abrirCompraModal(Number(buy.dataset.buy)); return; }

        const sell = e.target.closest("[data-sell]");
        if (sell) { abrirVendaModal(Number(sell.dataset.sell)); return; }

        const r = e.target.closest("[data-refresh-acao]");
        if (r) { atualizarCotacao(Number(r.dataset.refreshAcao)); return; }

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
