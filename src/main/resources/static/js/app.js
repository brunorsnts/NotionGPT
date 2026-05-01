// ─────────────────────────────────────────────
// ESTADO
// ─────────────────────────────────────────────
let allDocs = [];

// ─────────────────────────────────────────────
// API
// ─────────────────────────────────────────────

async function apiGetDocuments() {
  const res = await fetch('/documents');
  const data = await res.json();
  return data.content;
}

async function apiAddDocument(file) {
  const formData = new FormData();
  formData.append('file', file);
  const res = await fetch('/documents', {
    method: 'POST',
    body: formData
  });
  return res.json();
}

async function apiDeleteDocument(id) {
  await fetch(`/documents/${id}`, { method: 'DELETE' });
  return true;
}

async function apiQuery(query) {
  const res = await fetch('/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query })
  });
  return res.json();
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
  await apiDeleteDocument(id);
  allDocs = allDocs.filter(d => d.id !== id);
  filterDocs();
  updateStat();
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

  const { reply } = await apiQuery(query);

  loading.remove();
  appendMsg(reply, 'ai');
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
// INIT
// ─────────────────────────────────────────────
loadDocuments();