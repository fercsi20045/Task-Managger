// ===== KONFIGURÁCIÓ =====
const API = '/api';

// ===== ÁLLAPOT =====
let token = localStorage.getItem('token') || null;
let currentUsername = localStorage.getItem('username') || null;

// ===== OLDAL BETÖLTÉS =====
document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        showTaskSection();
        loadTasks();
    }
});

// ===== AUTH: FÜLVÁLTÁS =====
function switchTab(tab) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    event.target.classList.add('active');
    document.getElementById('login-form').style.display    = tab === 'login'    ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
}

// ===== REGISZTRÁCIÓ =====
async function register() {
    const username = document.getElementById('reg-username').value.trim();
    const email    = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    const errEl    = document.getElementById('reg-error');
    const okEl     = document.getElementById('reg-success');

    errEl.textContent = '';
    okEl.textContent  = '';

    if (!username || !email || !password) {
        errEl.textContent = 'Minden mező kitöltése kötelező.';
        return;
    }

    try {
        const res = await fetch(`${API}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });
        const data = await res.json();

        if (!res.ok) {
            errEl.textContent = data.error || 'Regisztráció sikertelen.';
        } else {
            okEl.textContent = 'Sikeres regisztráció! Most bejelentkezhetsz.';
        }
    } catch {
        errEl.textContent = 'Hálózati hiba.';
    }
}

// ===== BEJELENTKEZÉS =====
async function login() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const errEl    = document.getElementById('login-error');
    errEl.textContent = '';

    if (!username || !password) {
        errEl.textContent = 'Add meg a felhasználónevet és jelszót.';
        return;
    }

    try {
        const res = await fetch(`${API}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();

        if (!res.ok) {
            errEl.textContent = 'Hibás felhasználónév vagy jelszó.';
        } else {
            token = data.token;
            currentUsername = data.username;
            localStorage.setItem('token', token);
            localStorage.setItem('username', currentUsername);
            showTaskSection();
            loadTasks();
        }
    } catch {
        errEl.textContent = 'Hálózati hiba.';
    }
}

// ===== KIJELENTKEZÉS =====
function logout() {
    token = null;
    currentUsername = null;
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    document.getElementById('auth-section').style.display = 'block';
    document.getElementById('task-section').style.display = 'none';
    document.getElementById('logout-btn').style.display   = 'none';
    document.getElementById('nav-username').textContent   = '';
}

// ===== SZEKCIÓ VÁLTÁS =====
function showTaskSection() {
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('task-section').style.display = 'block';
    document.getElementById('logout-btn').style.display   = 'inline-block';
    document.getElementById('nav-username').textContent   = currentUsername;
}

// ===== FELADATOK BETÖLTÉSE =====
async function loadTasks() {
    const status = document.getElementById('status-filter').value;
    const url    = status ? `${API}/tasks?status=${status}` : `${API}/tasks`;

    try {
        const res = await fetch(url, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (res.status === 401) { logout(); return; }

        const tasks = await res.json();
        renderTasks(tasks);
    } catch {
        console.error('Feladatok betöltése sikertelen.');
    }
}

// ===== FELADATOK MEGJELENÍTÉSE =====
function renderTasks(tasks) {
    const list    = document.getElementById('task-list');
    const emptyEl = document.getElementById('empty-msg');
    list.innerHTML = '';

    if (tasks.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }
    emptyEl.style.display = 'none';

    const statusLabels = { TODO: 'Elvégzendő', IN_PROGRESS: 'Folyamatban', DONE: 'Kész' };

    tasks.forEach(task => {
        const card = document.createElement('div');
        card.className = `task-card ${task.status}`;

        let deadlineHtml = '';
        if (task.deadline) {
            const dl       = new Date(task.deadline);
            const overdue  = dl < new Date() && task.status !== 'DONE';
            const label    = dl.toLocaleDateString('hu-HU', { year:'numeric', month:'short', day:'numeric' });
            deadlineHtml   = `<span class="task-deadline ${overdue ? 'overdue' : ''}">⏰ ${label}${overdue ? ' (lejárt!)' : ''}</span>`;
        }

        card.innerHTML = `
            <div class="task-card-title">${escHtml(task.title)}</div>
            ${task.description ? `<div class="task-card-desc">${escHtml(task.description)}</div>` : ''}
            <span class="task-badge badge-${task.status}">${statusLabels[task.status]}</span>
            ${deadlineHtml}
            <div class="task-card-actions">
                <button class="btn btn-outline" onclick="openModal(${task.id})">Szerkesztés</button>
                ${task.status !== 'DONE'
                    ? `<button class="btn btn-primary" onclick="markDone(${task.id})">✓ Kész</button>`
                    : ''}
                <button class="btn btn-danger" onclick="deleteTask(${task.id})">Törlés</button>
            </div>`;

        list.appendChild(card);
    });
}

// ===== MODAL MEGNYITÁS =====
async function openModal(taskId = null) {
    document.getElementById('task-id').value    = '';
    document.getElementById('task-title').value = '';
    document.getElementById('task-desc').value  = '';
    document.getElementById('task-status').value = 'TODO';
    document.getElementById('task-deadline').value = '';
    document.getElementById('task-error').textContent = '';
    document.getElementById('modal-title').textContent = taskId ? 'Feladat szerkesztése' : 'Új feladat';

    if (taskId) {
        const res  = await fetch(`${API}/tasks/${taskId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const task = await res.json();
        document.getElementById('task-id').value     = task.id;
        document.getElementById('task-title').value  = task.title;
        document.getElementById('task-desc').value   = task.description || '';
        document.getElementById('task-status').value = task.status;
        if (task.deadline) {
            document.getElementById('task-deadline').value = task.deadline.slice(0, 16);
        }
    }

    document.getElementById('task-modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('task-modal').style.display = 'none';
}
function closeModalOutside(e) {
    if (e.target === document.getElementById('task-modal')) closeModal();
}

// ===== FELADAT MENTÉS =====
async function saveTask() {
    const id       = document.getElementById('task-id').value;
    const title    = document.getElementById('task-title').value.trim();
    const desc     = document.getElementById('task-desc').value.trim();
    const status   = document.getElementById('task-status').value;
    const deadline = document.getElementById('task-deadline').value;
    const errEl    = document.getElementById('task-error');
    errEl.textContent = '';

    if (!title) { errEl.textContent = 'A cím megadása kötelező.'; return; }

    const body = { title, description: desc, status, deadline: deadline ? deadline + ':00' : null };
    const url    = id ? `${API}/tasks/${id}` : `${API}/tasks`;
    const method = id ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify(body)
        });

        if (!res.ok) {
            const d = await res.json();
            errEl.textContent = d.error || 'Mentés sikertelen.';
        } else {
            closeModal();
            loadTasks();
        }
    } catch {
        errEl.textContent = 'Hálózati hiba.';
    }
}

// ===== GYORS KÉSZ JELÖLÉS =====
async function markDone(taskId) {
    await fetch(`${API}/tasks/${taskId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ status: 'DONE' })
    });
    loadTasks();
}

// ===== FELADAT TÖRLÉSE =====
async function deleteTask(taskId) {
    if (!confirm('Biztosan törölni szeretnéd ezt a feladatot?')) return;
    await fetch(`${API}/tasks/${taskId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    });
    loadTasks();
}

// ===== SEGÉDFÜGGVÉNY: HTML escape =====
function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
