// ─────────────────────────────────────────────
// ESTADO
// ─────────────────────────────────────────────
let allDocs = [];

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

async function apiGetDocuments() {
  const res = await fetch('/documents');
  const data = await res.json();
  if (!res.ok) { showError(data.detail || 'Erro ao carregar documentos.'); return []; }
  return data.content;
}

async function apiAddDocument(file) {
  const formData = new FormData();
  formData.append('file', file);
  const res = await fetch('/documents', { method: 'POST', body: formData });
  const data = await res.json();
  if (!res.ok) { showError(data.detail || 'Erro ao adicionar documento.'); return null; }
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
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query })
  });
  const data = await res.json();
  if (!res.ok) { showError(data.detail || 'Erro ao processar consulta.'); return null; }
  return data;
}

// ─────────────────────────────────────────────
// DOCUMENTOS
// ─────────────────────────────────────────────

async function loadDocuments() {
  allDocs = await apiGetDocuments();
  renderDocs(allDocs);
  updateStat();
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
          <div class="doc-path">${doc.path}</div>
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
  renderDocs(allDocs.filter(d =>
    d.name.toLowerCase().includes(q) || d.path.toLowerCase().includes(q)
  ));
}

function updateStat() {
  const n = allDocs.length;
  document.getElementById('stat-count').innerHTML =
    `<strong>${n}</strong> documento${n !== 1 ? 's' : ''} indexado${n !== 1 ? 's' : ''}`;
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
// MODAL
// ─────────────────────────────────────────────

function openModal()  { document.getElementById('modal').classList.add('open'); }
function closeModal() { document.getElementById('modal').classList.remove('open'); }

function closeModalOutside(e) {
  if (e.target.id === 'modal') closeModal();
}

async function addDocument() {
  const fileInput = document.getElementById('doc-file-input');
  const file = fileInput.files[0];
  if (!file) return;

  const doc = await apiAddDocument(file);
  if (!doc) return;

  allDocs.unshift(doc);
  filterDocs();
  updateStat();

  fileInput.value = '';
  closeModal();
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

async function sendMessage() {
  const input = document.getElementById('chat-input');
  const query = input.value.trim();
  if (!query) return;

  input.value = '';
  appendMsg(query, 'user');

  const loading = appendLoading();
  document.getElementById('btn-send').disabled = true;

  const data = await apiQuery(query);

  loading.remove();
  if (data) appendMsg(data.reply, 'ai');
  document.getElementById('btn-send').disabled = false;
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
// INIT
// ─────────────────────────────────────────────
loadDocuments();
initChatResize();