// ─────────────────────────────────────────────
// ESTADO
// ─────────────────────────────────────────────
let allDocs = [];
let currentPage = 0;
let pageInfo = { totalPages: 1, first: true, last: true };

// ─────────────────────────────────────────────
// CONFIRM DIALOG
// ─────────────────────────────────────────────

let confirmCallback = null;

function showConfirm(docName, onConfirm) {
  document.getElementById('confirm-msg').textContent = `"${docName}" será removido permanentemente.`;
  confirmCallback = onConfirm;
  document.getElementById('confirm-overlay').classList.add('open');
  document.getElementById('confirm-ok').onclick = () => { closeConfirm(); onConfirm(); };
}

function closeConfirm() {
  document.getElementById('confirm-overlay').classList.remove('open');
  confirmCallback = null;
}

function closeConfirmOutside(e) {
  if (e.target.id === 'confirm-overlay') closeConfirm();
}

// ─────────────────────────────────────────────
// TOASTS
// ─────────────────────────────────────────────

function showError(message) {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.innerHTML = `
    <svg class="toast-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
      <circle cx="7.5" cy="7.5" r="6.5" stroke="currentColor" stroke-width="1.4"/>
      <path d="M7.5 4.5v3M7.5 10h.01" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
    </svg>
    <div class="toast-body">
      <div class="toast-title">Erro</div>
      <div class="toast-msg">${message}</div>
    </div>
    <button class="toast-close" onclick="removeToast(this.closest('.toast'))">
      <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
        <path d="M1 1l8 8M9 1L1 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
    </button>`;
  container.appendChild(toast);
  setTimeout(() => removeToast(toast), 5000);
}

function removeToast(toast) {
  if (!toast || !toast.isConnected) return;
  toast.classList.add('removing');
  setTimeout(() => toast.remove(), 200);
}

function showUndoToast(docName, onUndo) {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = 'toast toast-undo';
  toast.innerHTML = `
    <svg class="toast-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
      <path d="M2 7.5a5.5 5.5 0 1010.5 2M2 7.5V4m0 3.5H5.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    <div class="toast-body">
      <div class="toast-title">Removendo documento</div>
      <div class="toast-msg">"${docName}"</div>
    </div>
    <button class="toast-action">Cancelar</button>
    <div class="toast-progress"></div>`;

  let cancelled = false;
  toast.querySelector('.toast-action').onclick = () => {
    cancelled = true;
    removeToast(toast);
    onUndo();
  };

  container.appendChild(toast);
  return { getCancelled: () => cancelled, toast };
}

// ─────────────────────────────────────────────
// API
// ─────────────────────────────────────────────

async function apiGetDocuments(page = 0) {
  const res = await fetch(`/documents?page=${page}`);
  const data = await res.json();
  if (!res.ok) { showError(data.detail || 'Erro ao carregar documentos.'); return null; }
  return data;
}

async function apiDeleteDocument(id) {
  const res = await fetch(`/documents/${id}`, { method: 'DELETE' });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    showError(data.detail || 'Erro ao remover documento.');
    return false;
  }
  return true;
}

async function apiQuery(query) {
  const res = await fetch('/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Session-Id': sessionId },
    body: JSON.stringify({ query })
  });
  const data = await res.json();
  if (!res.ok) { showError(data.detail || 'Erro ao processar consulta.'); return null; }
  return data;
}

// ─────────────────────────────────────────────
// DOCUMENTOS
// ─────────────────────────────────────────────

async function loadDocuments(page = 0) {
  const data = await apiGetDocuments(page);
  if (!data) return;
  allDocs = data.content;
  currentPage = data.number;
  pageInfo = { totalPages: data.totalPages, first: data.first, last: data.last, totalElements: data.totalElements };
  renderDocs(allDocs);
  updateStat();
  updateSyncInfo();
  renderPagination();
}

function renderDocs(docs) {
  const grid = document.getElementById('doc-grid');
  if (!docs.length) {
    grid.innerHTML = `
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
          <rect x="8" y="4" width="32" height="40" rx="3" stroke="#3d4d6b" stroke-width="2"/>
          <path d="M16 16h16M16 24h16M16 32h8" stroke="#3d4d6b" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <p>Nenhum documento indexado ainda.</p>
      </div>`;
    return;
  }
  grid.innerHTML = docs.map((doc, i) => `
    <div class="doc-card" style="animation-delay:${i * 0.05}s">
      <div class="doc-card-top">
        <div class="doc-icon">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <rect x="2" y="1" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/>
            <path d="M5 5h6M5 8h6M5 11h4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="doc-info">
          <div class="doc-name" title="${doc.name}">${doc.name}</div>
          <div class="doc-path"><span class="notion-badge">↗ Notion</span></div>
        </div>
      </div>
      <div class="doc-meta">
        <span class="doc-date">${formatDate(doc.addedAt)}</span>
        <div class="doc-actions">
          <button class="btn-icon" onclick="deleteDoc('${doc.id}')" title="Remover">
            <svg width="11" height="11" viewBox="0 0 11 11" fill="none">
              <path d="M1 1l9 9M10 1L1 10" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </div>
    </div>`).join('');
}

function filterDocs() {
  const q = document.getElementById('search-input').value.toLowerCase();
  renderDocs(allDocs.filter(d => d.name.toLowerCase().includes(q)));
}

function updateSyncInfo() {
  const el = document.getElementById('sync-info');
  if (!el) return;
  if (!allDocs.length) { el.textContent = ''; return; }
  const latest = allDocs.reduce((a, b) => new Date(a.addedAt) > new Date(b.addedAt) ? a : b);
  el.innerHTML = `último sync: <strong>${formatDate(latest.addedAt)}</strong>`;
}

function updateStat() {
  const n = pageInfo.totalElements ?? allDocs.length;
  document.getElementById('stat-count').innerHTML =
    `<strong>${n}</strong> documento${n !== 1 ? 's' : ''} indexado${n !== 1 ? 's' : ''}`;
}

function renderPagination() {
  let el = document.getElementById('pagination');
  if (!el) {
    el = document.createElement('div');
    el.id = 'pagination';
    el.className = 'pagination';
    document.getElementById('doc-grid').after(el);
  }
  if (pageInfo.totalPages <= 1) { el.innerHTML = ''; return; }
  el.innerHTML = `
    <button class="page-btn" onclick="loadDocuments(${currentPage - 1})" ${pageInfo.first ? 'disabled' : ''}>
      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
        <path d="M7.5 2L3.5 6l4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>
    <span class="page-info">${currentPage + 1} / ${pageInfo.totalPages}</span>
    <button class="page-btn" onclick="loadDocuments(${currentPage + 1})" ${pageInfo.last ? 'disabled' : ''}>
      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
        <path d="M4.5 2L8.5 6l-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>`;
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
}

async function deleteDoc(id) {
  const doc = allDocs.find(d => d.id === id);
  if (!doc) return;

  showConfirm(doc.name, () => {
    const { getCancelled, toast } = showUndoToast(doc.name, () => {});

    setTimeout(async () => {
      if (getCancelled()) return;
      removeToast(toast);
      const ok = await apiDeleteDocument(id);
      if (!ok) return;
      allDocs = allDocs.filter(d => d.id !== id);
      filterDocs();
      updateStat();
    }, 5000);
  });
}

// ─────────────────────────────────────────────
// CHAT
// ─────────────────────────────────────────────

let chatOpen = false;

function toggleChat() {
  chatOpen = !chatOpen;
  document.getElementById('chat-panel').classList.toggle('open', chatOpen);
}

function handleKey(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
}

function appendMsg(text, type) {
  const box = document.getElementById('chat-messages');
  const div = document.createElement('div');
  div.className = `msg ${type}`;
  if (type === 'ai') {
    div.innerHTML = marked.parse(text);
  } else {
    div.textContent = text;
  }
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
  return div;
}

function appendLoading() {
  const box = document.getElementById('chat-messages');
  const div = document.createElement('div');
  div.className = 'msg ai msg-loading';
  div.innerHTML = '<span></span><span></span><span></span>';
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
  return div;
}

// ─────────────────────────────────────────────
// CHAT RESIZE
// ─────────────────────────────────────────────

function initChatResize() {
  const panel  = document.getElementById('chat-panel');
  const handle = document.getElementById('chat-resize-handle');

  const MIN_W = 280, MAX_W = 640;
  const MIN_H = 300, MAX_H = Math.floor(window.innerHeight * 0.85);

  let startX, startY, startW, startH;

  handle.addEventListener('mousedown', e => {
    e.preventDefault();
    startX = e.clientX;
    startY = e.clientY;
    startW = panel.offsetWidth;
    startH = panel.offsetHeight;
    panel.style.transition = 'none';
    document.body.style.userSelect = 'none';
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  });

  function onMove(e) {
    const w = Math.min(MAX_W, Math.max(MIN_W, startW + (startX - e.clientX)));
    const h = Math.min(MAX_H, Math.max(MIN_H, startH + (startY - e.clientY)));
    panel.style.width  = w + 'px';
    panel.style.height = h + 'px';
  }

  function onUp() {
    document.removeEventListener('mousemove', onMove);
    document.removeEventListener('mouseup', onUp);
    panel.style.transition = '';
    document.body.style.userSelect = '';
  }
}

// ─────────────────────────────────────────────
// STATS
// ─────────────────────────────────────────────

let statsOpen = false;

async function apiGetStats() {
  const res = await fetch('/stats');
  const data = await res.json();
  if (!res.ok) { showError(data.detail || 'Erro ao carregar métricas.'); return null; }
  return data;
}

async function loadStats() {
  document.getElementById('stats-body').innerHTML =
    '<div class="stats-loading">Carregando métricas...</div>';
  const data = await apiGetStats();
  if (data) renderStats(data);
}

function animateCount(el, target, decimals, suffix) {
  const duration = 600;
  const start = performance.now();
  function step(now) {
    const t = Math.min((now - start) / duration, 1);
    const ease = 1 - Math.pow(1 - t, 3);
    const val = target * ease;
    el.textContent = decimals === 0 ? Math.round(val) + suffix : val.toFixed(decimals) + suffix;
    if (t < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

function renderStats(s) {
  const total = s.totalQueries;
  const dash = '--';

  const successRateNum = total === 0 ? null : s.successCount / total * 100;
  const avgLatencyNum  = total === 0 ? null : Math.round(s.avgLatencyMs);
  const p95LatencyNum  = total === 0 ? null : Math.round(s.p95LatencyMs);
  const avgScoreNum    = total === 0 ? null : s.avgScore * 100;
  const avgMatchesNum  = total === 0 ? null : s.avgMatchesCount;

  const bufferPct = s.historyCapacity > 0
    ? Math.round(s.historySize / s.historyCapacity * 100)
    : 0;

  const topPages = (s.topPages || []).slice(0, 5);
  const maxCount = topPages.length > 0 ? topPages[0].count : 1;

  document.getElementById('stats-body').innerHTML = `
    <div>
      <div class="stats-section-label">Consultas</div>
      <div class="stats-grid">
        <div class="stats-metric-card">
          <div class="stats-metric-label">Total de consultas</div>
          <div class="stats-metric-value accent" data-count="${total}" data-dec="0" data-suffix="">${total === 0 ? dash : '0'}</div>
        </div>
        <div class="stats-metric-card">
          <div class="stats-metric-label">Taxa de sucesso</div>
          <div class="stats-metric-value${successRateNum !== null ? ' accent' : ''}"
               data-count="${successRateNum ?? ''}" data-dec="1" data-suffix="%">${successRateNum !== null ? '0%' : dash}</div>
        </div>
        <div class="stats-metric-card">
          <div class="stats-metric-label">Latência média</div>
          <div class="stats-metric-value" data-count="${avgLatencyNum ?? ''}" data-dec="0" data-suffix=" ms">${avgLatencyNum !== null ? '0 ms' : dash}</div>
        </div>
        <div class="stats-metric-card">
          <div class="stats-metric-label">Latência p95</div>
          <div class="stats-metric-value${p95LatencyNum !== null ? ' danger' : ''}"
               data-count="${p95LatencyNum ?? ''}" data-dec="0" data-suffix=" ms">${p95LatencyNum !== null ? '0 ms' : dash}</div>
        </div>
      </div>
    </div>

    <div>
      <div class="stats-section-label">Qualidade da recuperação</div>
      <div class="stats-wide-card">
        <div class="stats-wide-item">
          <div class="stats-metric-label">Score médio</div>
          <div class="stats-metric-value amber" data-count="${avgScoreNum ?? ''}" data-dec="1" data-suffix="%">${avgScoreNum !== null ? '0%' : dash}</div>
        </div>
        <div class="stats-wide-sep"></div>
        <div class="stats-wide-item">
          <div class="stats-metric-label">Matches médios</div>
          <div class="stats-metric-value" data-count="${avgMatchesNum ?? ''}" data-dec="1" data-suffix="">${avgMatchesNum !== null ? '0' : dash}</div>
        </div>
      </div>
    </div>

    <div>
      <div class="stats-section-label">Páginas mais consultadas</div>
      <div class="stats-top-pages">
        ${topPages.length === 0
          ? '<div class="stats-loading">Sem dados ainda.</div>'
          : topPages.map((p, i) => `
            <div class="stats-page-row">
              <div class="stats-page-heat" style="width:${Math.round(p.count / maxCount * 100)}%"></div>
              <span class="stats-page-rank">${i + 1}</span>
              <span class="stats-page-name" title="${p.titulo}">${p.titulo}</span>
              <span class="stats-page-count">${p.count}×</span>
            </div>`).join('')}
      </div>
    </div>

    <div>
      <div class="stats-section-label">Buffer de histórico</div>
      <div class="stats-buffer-card">
        <div class="stats-buffer-info">
          <span class="stats-buffer-label">Capacidade utilizada</span>
          <span class="stats-buffer-val">${s.historySize} / ${s.historyCapacity}</span>
        </div>
        <div class="stats-progress-track">
          <div class="stats-progress-fill" style="width:0%" data-target="${bufferPct}"></div>
        </div>
      </div>
    </div>`;

  document.querySelectorAll('#stats-body [data-count]').forEach(el => {
    const target = parseFloat(el.dataset.count);
    if (isNaN(target)) return;
    const dec = parseInt(el.dataset.dec ?? '0');
    const suffix = el.dataset.suffix ?? '';
    animateCount(el, target, dec, suffix);
  });

  const fill = document.querySelector('.stats-progress-fill[data-target]');
  if (fill) requestAnimationFrame(() => { fill.style.width = fill.dataset.target + '%'; });
}

function toggleStats() {
  statsOpen = !statsOpen;
  const overlay = document.getElementById('stats-overlay');
  const btn = document.querySelector('.stats-btn');
  overlay.classList.toggle('open', statsOpen);
  btn.classList.toggle('active', statsOpen);
  if (statsOpen) loadStats();
}

function closeStatsOutside(e) {
  if (e.target.id === 'stats-overlay') toggleStats();
}

// ─────────────────────────────────────────────
// SESSÃO MULTI-TURN + HISTÓRICO
// ─────────────────────────────────────────────

const SESSIONS_KEY   = 'notion-sessions';
const SESSION_ID_KEY = 'notion-session-id';
const MAX_SESSIONS   = 10;
const WELCOME_MSG    = 'Olá! Pergunte-me qualquer coisa sobre seus resumos de estudo.';

// ── Persistência de sessões ──────────────────

function loadSessions() {
  try {
    return JSON.parse(localStorage.getItem(SESSIONS_KEY) || '[]');
  } catch {
    return [];
  }
}

function saveSessions(sessions) {
  localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions));
}

function getSession(id) {
  return loadSessions().find(s => s.id === id) || null;
}

function upsertSession(session) {
  let sessions = loadSessions();
  const idx = sessions.findIndex(s => s.id === session.id);
  if (idx >= 0) {
    sessions[idx] = session;
  } else {
    sessions.unshift(session);
    if (sessions.length > MAX_SESSIONS) {
      sessions = sessions.slice(0, MAX_SESSIONS);
    }
  }
  saveSessions(sessions);
}

function removeSessionIfEmpty(id) {
  // Only keep sessions that have at least one user message
  let sessions = loadSessions();
  const session = sessions.find(s => s.id === id);
  if (!session) return;
  const hasUserMsg = session.messages.some(m => m.type === 'user');
  if (!hasUserMsg) {
    sessions = sessions.filter(s => s.id !== id);
    saveSessions(sessions);
  }
}

// ── Estado da sessão ─────────────────────────

let sessionId = localStorage.getItem(SESSION_ID_KEY)
    ?? (() => {
        const id = crypto.randomUUID();
        localStorage.setItem(SESSION_ID_KEY, id);
        return id;
    })();

// Ensure the current session exists in history (handles page reload)
(function initCurrentSession() {
  if (!getSession(sessionId)) {
    upsertSession({ id: sessionId, title: null, createdAt: new Date().toISOString(), messages: [] });
  }
})();

// ── Salvar mensagens na sessão ───────────────

function addMessageToSession(type, text) {
  const sessions = loadSessions();
  const session = sessions.find(s => s.id === sessionId);
  if (!session) return;

  session.messages.push({ type, text });

  // Derive title from first user message
  if (!session.title && type === 'user') {
    session.title = text.length > 40 ? text.slice(0, 40).trimEnd() + '…' : text;
  }

  saveSessions(sessions);
}

// ── Chat: envio de mensagem com persistência ─

async function sendMessage() {
  const input = document.getElementById('chat-input');
  const query = input.value.trim();
  if (!query) return;

  input.value = '';
  appendMsg(query, 'user');
  addMessageToSession('user', query);

  const loading = appendLoading();
  document.getElementById('btn-send').disabled = true;

  const data = await apiQuery(query);

  loading.remove();
  if (data) {
    appendMsg(data.reply, 'ai');
    addMessageToSession('ai', data.reply);
  }
  document.getElementById('btn-send').disabled = false;
}

// ── Nova conversa ────────────────────────────

function newConversation() {
  // Persist current session to history (remove if it has no messages)
  removeSessionIfEmpty(sessionId);

  // Create a fresh session
  const id = crypto.randomUUID();
  localStorage.setItem(SESSION_ID_KEY, id);
  sessionId = id;
  upsertSession({ id, title: null, createdAt: new Date().toISOString(), messages: [] });

  const box = document.getElementById('chat-messages');
  box.innerHTML = `<div class="msg ai">${WELCOME_MSG}</div>`;

  // Refresh history list if open
  if (historyOpen) renderHistoryList();
}

// ── Painel de histórico ──────────────────────

let historyOpen = false;

function toggleHistory() {
  historyOpen = !historyOpen;
  const panel = document.getElementById('history-panel');
  const btn   = document.getElementById('btn-history');
  panel.classList.toggle('open', historyOpen);
  btn.classList.toggle('active', historyOpen);
  if (historyOpen) renderHistoryList();
}

function renderHistoryList() {
  const list = document.getElementById('history-list');
  const sessions = loadSessions().filter(s => s.messages.some(m => m.type === 'user'));

  if (!sessions.length) {
    list.innerHTML = `
      <div class="history-empty">
        <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
          <circle cx="16" cy="16" r="12" stroke="#3d4d6b" stroke-width="1.5"/>
          <path d="M16 9v7l4 4" stroke="#3d4d6b" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span class="history-empty-text">sem sessões salvas</span>
      </div>`;
    return;
  }

  list.innerHTML = sessions.map((s, i) => {
    const isActive = s.id === sessionId;
    const msgCount = s.messages.filter(m => m.type === 'user').length;
    const date     = formatHistoryDate(s.createdAt);
    const title    = s.title || 'Sessão sem título';
    return `
      <div class="history-item${isActive ? ' active' : ''}"
           style="animation-delay:${i * 0.04}s"
           onclick="loadHistorySession('${s.id}')">
        <div class="history-item-title">${escapeHtml(title)}</div>
        <div class="history-item-meta">
          <span class="history-item-date">${date}</span>
          <span class="history-item-count">· ${msgCount} pergunta${msgCount !== 1 ? 's' : ''}</span>
          ${isActive ? '<span class="history-item-active-badge">ativa</span>' : ''}
        </div>
      </div>`;
  }).join('');
}

function loadHistorySession(id) {
  if (id === sessionId) {
    toggleHistory();
    return;
  }

  // Persist current session before switching
  removeSessionIfEmpty(sessionId);

  sessionId = id;
  localStorage.setItem(SESSION_ID_KEY, id);

  const session = getSession(id);
  const box = document.getElementById('chat-messages');
  box.innerHTML = `<div class="msg ai">${WELCOME_MSG}</div>`;

  if (session && session.messages.length) {
    session.messages.forEach(m => appendMsg(m.text, m.type));
  }

  toggleHistory();
}

function formatHistoryDate(iso) {
  const d = new Date(iso);
  const now = new Date();
  const diffMs = now - d;
  const diffMin = Math.floor(diffMs / 60000);
  const diffH   = Math.floor(diffMs / 3600000);
  const diffD   = Math.floor(diffMs / 86400000);

  if (diffMin < 1)  return 'agora';
  if (diffMin < 60) return `${diffMin}min atrás`;
  if (diffH < 24)   return `${diffH}h atrás`;
  if (diffD < 7)    return `${diffD}d atrás`;
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });
}

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ─────────────────────────────────────────────
// INIT
// ─────────────────────────────────────────────
loadDocuments();
initChatResize();