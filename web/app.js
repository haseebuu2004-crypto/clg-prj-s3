const STORAGE_KEY = 'skillswap.mobile.v1';
const LEVELS = ['Beginner', 'Intermediate', 'Advanced'];
const PAGE_TITLES = { dashboard: 'Dashboard', matches: 'Find partners', skills: 'My skills', sessions: 'Sessions', profile: 'Profile & join' };
let page = 'dashboard';
let matchMode = 'direct';
let sessionFilter = 'all';
let editingProfile = false;
let mobileMenuOpen = false;
let toastTimer;

const state = loadState();

function demoStudent(id, name, email, offered, wanted) {
  return { id, name, email, skills: [
    ...offered.map(([skill, level]) => ({ name: skill, category: 'General', level, direction: 'OFFERED' })),
    ...wanted.map(([skill, level]) => ({ name: skill, category: 'General', level, direction: 'WANTED' }))
  ] };
}

function seedState() {
  const students = [
    demoStudent('demo-asha', 'Asha Rao', 'asha@skillswap.demo', [['Java', 'Advanced']], [['Python', 'Intermediate']]),
    demoStudent('demo-dev', 'Dev Nair', 'dev@skillswap.demo', [['Python', 'Advanced']], [['Java', 'Advanced']]),
    demoStudent('demo-meera', 'Meera Nair', 'meera@skillswap.demo', [['Java', 'Intermediate']], [['Python', 'Intermediate']]),
    demoStudent('demo-arjun', 'Arjun Menon', 'arjun@skillswap.demo', [['Python', 'Advanced']], [['SQL', 'Intermediate']]),
    demoStudent('demo-kabir', 'Kabir Shah', 'kabir@skillswap.demo', [['SQL', 'Advanced']], [['Java', 'Intermediate']]),
    demoStudent('demo-tara', 'Tara Joseph', 'tara@skillswap.demo', [['French', 'Advanced']], [['Graphic Design', 'Beginner']])
  ];
  const now = Date.now();
  const past = days => new Date(now - days * 86400000).toISOString();
  const sessions = [
    { id: 'demo-history-dev-asha', teacherId: 'demo-dev', learnerId: 'demo-asha', skillName: 'Python', scheduledAt: past(7), notes: 'Seeded demo history', status: 'COMPLETED', completedAt: past(7), matchGroupId: 'demo-history', rating: { reviewerId: 'demo-asha', stars: 5, feedback: 'Clear examples and patient explanations.', learned: 'Python basics and functions.', createdAt: past(6) } },
    { id: 'demo-history-asha-dev', teacherId: 'demo-asha', learnerId: 'demo-dev', skillName: 'Java', scheduledAt: past(7), notes: 'Seeded demo history', status: 'COMPLETED', completedAt: past(7), matchGroupId: 'demo-history', rating: { reviewerId: 'demo-dev', stars: 4, feedback: 'The object-oriented examples helped a lot.', learned: 'Classes and constructors.', createdAt: past(6) } }
  ];
  return { schema: 1, students, sessions, currentId: 'demo-asha' };
}

function loadState() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const parsed = JSON.parse(saved);
      if (parsed && Array.isArray(parsed.students) && Array.isArray(parsed.sessions) && parsed.students.length) {
        if (!parsed.students.some(student => student.id === parsed.currentId)) parsed.currentId = parsed.students[0].id;
        return parsed;
      }
    }
  } catch { /* Start with a fresh demo if this browser blocks or has damaged local storage. */ }
  const initial = seedState();
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(initial)); } catch { /* The app remains usable for this tab. */ }
  return initial;
}

function saveState() {
  const status = document.getElementById('save-state');
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    if (status) { status.classList.remove('unsaved'); status.title = 'Saved in this browser on this device'; }
    return true;
  } catch {
    if (status) { status.classList.add('unsaved'); status.title = 'Browser storage is unavailable. Changes may be lost when this tab closes.'; }
    return false;
  }
}

function uid(prefix) {
  const value = globalThis.crypto?.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${value}`;
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
}

function currentStudent() { return state.students.find(student => student.id === state.currentId) || state.students[0]; }
function studentById(id) { return state.students.find(student => student.id === id); }
function offered(student) { return student.skills.filter(skill => skill.direction === 'OFFERED'); }
function wanted(student) { return student.skills.filter(skill => skill.direction === 'WANTED'); }
function skillIndex(student, direction, name) { return student.skills.findIndex(skill => skill.direction === direction && skill.name.trim().toLowerCase() === name.trim().toLowerCase()); }
function emailTaken(email, exceptId = '') { return state.students.some(student => student.id !== exceptId && student.email.toLowerCase() === email.toLowerCase()); }
function norm(text) { return String(text || '').trim().toLowerCase(); }
function levelRank(level) { return LEVELS.indexOf(level); }
function compatible(teacher, request) { return norm(teacher.name) === norm(request.name) && levelRank(teacher.level) >= levelRank(request.level); }
function initials(name) { return String(name || '?').trim().split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase(); }
function averageRating(studentId) {
  const ratings = state.sessions.filter(session => session.teacherId === studentId && session.rating).map(session => session.rating.stars);
  return { average: ratings.length ? ratings.reduce((sum, score) => sum + score, 0) / ratings.length : 0, count: ratings.length };
}
function displayDate(value, options = { dateStyle: 'medium', timeStyle: 'short' }) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Date to be arranged' : new Intl.DateTimeFormat(undefined, options).format(date);
}
function localDateTimeInput(value) {
  const date = value ? new Date(value) : new Date(Date.now() + 86400000);
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}
function setToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3200);
}
function setMobileMenuOpen(open, restoreFocus = true) {
  mobileMenuOpen = open;
  const layer = document.getElementById('mobile-menu-layer');
  const toggle = document.querySelector('.mobile-menu-toggle');
  if (!layer || !toggle) return;
  layer.hidden = !open;
  document.body.classList.toggle('menu-open', open);
  toggle.setAttribute('aria-expanded', String(open));
  toggle.setAttribute('aria-label', open ? 'Close navigation menu' : 'Open navigation menu');
  if (open) {
    requestAnimationFrame(() => layer.querySelector('.mobile-menu-items [data-page].active')?.focus());
  } else if (restoreFocus) {
    toggle.focus({ preventScroll: true });
  }
}
function saveAndRender(message) {
  const saved = saveState();
  render();
  if (message) setToast(saved ? message : `${message} This browser could not save the change.`);
}
function skillChips(skills) {
  if (!skills.length) return '<span class="field-hint">No skills added yet.</span>';
  return skills.map(skill => `<span class="skill-chip">${escapeHtml(skill.name)} · ${escapeHtml(skill.level)}</span>`).join(' ');
}
function render() {
  const active = currentStudent();
  document.title = `Skill Swap | ${PAGE_TITLES[page] || 'Learn together'}`;
  document.getElementById('page-title').textContent = PAGE_TITLES[page] || 'Skill Swap';
  document.getElementById('profile-picker').innerHTML = state.students.map(student => `<option value="${escapeHtml(student.id)}" ${student.id === active.id ? 'selected' : ''}>${escapeHtml(student.name)}</option>`).join('');
  document.querySelectorAll('[data-page]').forEach(button => {
    const isActive = button.dataset.page === page;
    button.classList.toggle('active', isActive);
    if (isActive) button.setAttribute('aria-current', 'page'); else button.removeAttribute('aria-current');
  });
  const renderers = { dashboard: renderDashboard, matches: renderMatches, skills: renderSkills, sessions: renderSessions, profile: renderProfile };
  document.getElementById('main-content').innerHTML = (renderers[page] || renderDashboard)();
}

function pageHead(title, description) {
  return `<header class="page-head"><h1>${title}</h1><p>${description}</p></header>`;
}
function emptyState(icon, title, detail, action = '') {
  return `<div class="empty-state"><div class="empty-icon">${icon}</div><h3>${title}</h3><p>${detail}</p>${action}</div>`;
}
function statCard(icon, value, label, tone = '') {
  return `<article class="stat-card"><span class="stat-icon ${tone}">${icon}</span><div><div class="stat-value">${value}</div><div class="stat-label">${label}</div></div></article>`;
}

function renderDashboard() {
  const active = currentStudent();
  const upcoming = state.sessions.filter(session => [session.teacherId, session.learnerId].includes(active.id) && ['PENDING', 'ACCEPTED'].includes(session.status)).length;
  const completed = state.sessions.filter(session => [session.teacherId, session.learnerId].includes(active.id) && session.status === 'COMPLETED').length;
  const rating = averageRating(active.id);
  const feedback = state.sessions.filter(session => session.teacherId === active.id && session.rating).sort((a, b) => new Date(b.rating.createdAt) - new Date(a.rating.createdAt)).slice(0, 2);
  const upcomingSession = state.sessions.filter(session => [session.teacherId, session.learnerId].includes(active.id) && ['PENDING', 'ACCEPTED'].includes(session.status)).sort((a,b) => new Date(a.scheduledAt)-new Date(b.scheduledAt))[0];
  return `<section class="hero"><div class="hero-copy"><span class="hero-kicker">✧ A little knowledge goes a long way</span><h1>Hi, ${escapeHtml(active.name.split(' ')[0])}. What would you love to learn?</h1><p>Share what you know, meet a fellow learner, and grow your skills together — one friendly exchange at a time.</p><div class="hero-actions"><button class="button" data-page="matches">Find my partner <span aria-hidden="true">→</span></button><button class="button secondary" data-page="skills">Update my skills</button></div></div><div class="hero-art" aria-hidden="true"><div class="art-orbit"><div class="art-core">✦</div><span class="art-dot one">Aa</span><span class="art-dot two">⌘</span><span class="art-dot three">♪</span></div></div></section>
  <section class="stats-grid" aria-label="Your activity">${statCard('✦', offered(active).length, 'Skills to teach')}${statCard('◌', wanted(active).length, 'Learning goals', 'plum')}${statCard('◷', upcoming, 'Upcoming sessions', 'gold')}${statCard('★', rating.count ? rating.average.toFixed(1) : '—', `${rating.count} teaching review${rating.count === 1 ? '' : 's'}`, 'green')}</section>
  <div class="two-col"><section class="panel"><div class="section-row" style="margin:0"><div><h2>Make a little progress</h2><p>Choose where you’d like to go next.</p></div></div><button class="quick-link" data-page="matches"><span class="link-icon">✦</span><span><strong>Find a learning partner</strong><small>See direct swaps and learning circles</small></span><span class="arrow">›</span></button><button class="quick-link" data-page="sessions"><span class="link-icon">◷</span><span><strong>Check your sessions</strong><small>${upcoming ? `${upcoming} session${upcoming === 1 ? '' : 's'} need your attention` : 'See your learning history'}</small></span><span class="arrow">›</span></button><button class="quick-link" data-page="profile"><span class="link-icon">◎</span><span><strong>Meet the demo classmates</strong><small>Switch profiles to explore different matches</small></span><span class="arrow">›</span></button></section>
  <section class="panel"><div class="section-row" style="margin:0"><div><h2>${upcomingSession ? 'Next session' : 'Learning feels better together'}</h2><p>${upcomingSession ? 'A little time set aside for learning.' : `${completed} session${completed === 1 ? '' : 's'} completed with your community.`}</p></div></div>${upcomingSession ? upcomingSessionSummary(upcomingSession) : feedback.length ? feedback.map(feedbackItem).join('') : `<div class="empty-state" style="margin-top:13px;padding:20px 12px"><div class="empty-icon">♡</div><h3>Your next chapter starts here</h3><p>Complete a skill exchange and your classmates’ kind feedback will show up here.</p></div>`}</section></div>
  <div class="section-row"><div><h2>A warm campus community</h2><p>Find someone who can teach what you want to learn.</p></div><button class="button secondary small" data-page="profile">Browse students</button></div>
  <div class="two-col">${[...state.students].filter(student => student.id !== active.id).slice(0,2).map(student => `<article class="panel" style="display:flex;align-items:center;gap:13px"><span class="avatar ${avatarTone(student.id)}">${initials(student.name)}</span><div style="min-width:0"><strong>${escapeHtml(student.name)}</strong><div class="field-hint" style="margin-top:4px">Can teach ${escapeHtml(offered(student).map(skill => skill.name).join(', ') || 'new skills')}</div></div></article>`).join('') || emptyState('◎','Add more classmates','Register students from Profile & join.')}</div>`;
}
function upcomingSessionSummary(session) {
  const active = currentStudent();
  const partner = studentById(session.teacherId === active.id ? session.learnerId : session.teacherId);
  return `<div class="session-detail" style="margin-top:14px"><div><span>Learning</span><strong>${escapeHtml(session.skillName)} with ${escapeHtml(partner?.name || 'a classmate')}</strong></div><div><span>When</span><strong>${escapeHtml(displayDate(session.scheduledAt))}</strong></div></div><button class="button secondary small" data-page="sessions">Open session details</button>`;
}
function feedbackItem(session) {
  const teacher = studentById(session.teacherId);
  return `<div class="feedback-item"><div class="feedback-top"><span>${escapeHtml(teacher?.name || 'Student')} · ${escapeHtml(session.skillName)}</span><span class="feedback-stars">${'★'.repeat(session.rating.stars)}${'☆'.repeat(5-session.rating.stars)}</span></div><p>${escapeHtml(session.rating.feedback || session.rating.learned || 'A thoughtful skill exchange.')}</p></div>`;
}
function avatarTone(id) {
  const tones = ['','plum','green','gold'];
  let hash = 0; for (const char of String(id)) hash = (hash * 31 + char.charCodeAt(0)) | 0;
  return tones[Math.abs(hash) % tones.length];
}

function findDirectMatches(focusId = state.currentId) {
  const focus = studentById(focusId);
  if (!focus) return [];
  const results = [];
  for (const partner of state.students) {
    if (partner.id === focus.id) continue;
    const exchanges = [];
    for (const request of wanted(focus)) {
      const teaching = offered(partner).find(skill => compatible(skill, request));
      if (teaching) exchanges.push({ learnerId: focus.id, teacherId: partner.id, skillName: request.name });
    }
    for (const request of wanted(partner)) {
      const teaching = offered(focus).find(skill => compatible(skill, request));
      if (teaching) exchanges.push({ learnerId: partner.id, teacherId: focus.id, skillName: request.name });
    }
    if (exchanges.length) results.push({ type: 'DIRECT', participantIds: [focus.id, partner.id], exchanges });
  }
  return results.sort((a,b) => b.exchanges.length - a.exchanges.length || partnerRating(b)-partnerRating(a));
}
function edgeBetween(learnerId, teacherId) {
  const learner = studentById(learnerId);
  const teacher = studentById(teacherId);
  if (!learner || !teacher) return null;
  const request = wanted(learner).find(skill => offered(teacher).some(offeredSkill => compatible(offeredSkill, skill)));
  return request ? { learnerId, teacherId, skillName: request.name } : null;
}
function findChainMatches(focusId = state.currentId) {
  const results = [];
  const seen = new Set();
  const walk = (path, exchanges) => {
    if (path.length >= 3) {
      const closingEdge = edgeBetween(path[path.length - 1], focusId);
      if (closingEdge) {
        const key = path.join('>');
        if (!seen.has(key)) {
          seen.add(key);
          results.push({ type: 'CHAIN', participantIds: [...path], exchanges: [...exchanges, closingEdge] });
        }
      }
    }
    if (path.length >= 4 || results.length >= 30) return;
    const currentId = path[path.length - 1];
    for (const candidate of state.students) {
      if (candidate.id === focusId || path.includes(candidate.id)) continue;
      const edge = edgeBetween(currentId, candidate.id);
      if (edge) walk([...path, candidate.id], [...exchanges, edge]);
    }
  };
  walk([focusId], []);
  return results.sort((a,b) => a.participantIds.length - b.participantIds.length || partnerRating(b)-partnerRating(a));
}
function partnerRating(match) {
  const ratings = match.participantIds.filter(id => id !== state.currentId).map(id => averageRating(id).average || 3);
  return ratings.length ? ratings.reduce((sum,value)=>sum+value,0) / ratings.length : 3;
}

function renderMatches() {
  const active = currentStudent();
  const matches = matchMode === 'direct' ? findDirectMatches() : findChainMatches();
  const directCount = findDirectMatches().length;
  const chainCount = findChainMatches().length;
  const intro = matchMode === 'direct'
    ? 'We check real skill exchanges and make sure each teacher meets the learner’s requested level.'
    : 'Learning circles connect three or four classmates into a complete exchange.';
  const buttons = `<div class="match-controls"><button class="button ${matchMode === 'direct' ? '' : 'secondary'}" data-action="match-mode" data-mode="direct">Find direct swaps</button><button class="button ${matchMode === 'chain' ? '' : 'secondary'}" data-action="match-mode" data-mode="chain">Explore learning circles</button><span class="match-help">Direct swaps are reciprocal. Circles create complete cycles of 3–4 students.</span></div>`;
  const list = matches.length ? `<div class="match-list">${matches.map((match,index) => renderMatchCard(match,index)).join('')}</div>` : matchMode === 'direct'
    ? emptyState('✦', 'No direct swap just yet', `${escapeHtml(active.name)} may still find a place in a learning circle. Try exploring a chain exchange.`, `<button class="button" data-action="match-mode" data-mode="chain">Explore learning circles (${chainCount})</button>`)
    : emptyState('◌', 'No learning circle found yet', 'Try adding another skill you can teach or want to learn. More classmates can open up a new circle.', `<button class="button secondary" data-page="skills">Update my skills</button>`);
  return `${pageHead('Find your learning partner', intro)}${buttons}<div class="match-help" style="margin:-4px 0 14px">${matchMode === 'direct' ? `${directCount} direct partner${directCount===1?'':'s'} found for ${escapeHtml(active.name)}.` : `${chainCount} learning circle${chainCount===1?'':'s'} found. Each student receives one of their requested skills.`}</div>${list}`;
}
function renderMatchCard(match, index) {
  const active = currentStudent();
  const partnerId = match.participantIds.find(id => id !== active.id);
  const partner = studentById(partnerId);
  const rating = averageRating(partnerId || '');
  const descriptions = match.exchanges.map(exchange => {
    const learner = studentById(exchange.learnerId);
    const teacher = studentById(exchange.teacherId);
    const phrase = exchange.learnerId === active.id ? 'You learn' : exchange.teacherId === active.id ? 'You teach' : `${escapeHtml(learner?.name || 'Student')} learns`;
    const provider = exchange.teacherId === active.id ? 'you' : escapeHtml(teacher?.name || 'a classmate');
    return `<p>${phrase} <strong>${escapeHtml(exchange.skillName)}</strong> from ${provider}</p>`;
  }).join('');
  const others = match.participantIds.map(id => studentById(id)?.name).filter(name => name && name !== active.name);
  const path = match.type === 'CHAIN' ? `<div class="chain-path">Learning circle: ${[active.name, ...others, active.name].map(escapeHtml).join(' → ')}</div>` : '';
  const partnerText = match.type === 'DIRECT' ? (others[0] || 'Classmate') : `${match.participantIds.length}-student learning circle`;
  return `<article class="match-card"><div class="match-card-head"><span class="avatar ${avatarTone(partnerId)}">${match.type === 'DIRECT' ? initials(partner?.name) : '✧'}</span><div><h3>${escapeHtml(partnerText)}</h3><p>${match.type === 'DIRECT' ? `${offered(partner || {skills:[]}).length} skill${offered(partner || {skills:[]}).length===1?'':'s'} shared · ${escapeHtml(partner?.email || '')}` : `${match.participantIds.length} classmates · each skill level checked`}</p></div></div><span class="match-tag">${match.type === 'DIRECT' ? 'Reciprocal' : 'Learning circle'}</span><div class="match-exchanges">${descriptions}${path}</div><div class="match-card-foot"><button class="button small" data-action="request-match" data-index="${index}" data-mode="${matchMode}">Request full exchange</button><span class="match-rating">${match.type === 'DIRECT' ? `${partner?.name ? escapeHtml(partner.name) + ' · ' : ''}${rating.count ? rating.average.toFixed(1) : 'New'} average · ${rating.count} review${rating.count===1?'':'s'}` : `Community rating average · ${partnerRating(match).toFixed(1)}`}</span></div></article>`;
}

function renderSkills() {
  const active = currentStudent();
  const list = (direction, title) => {
    const skills = active.skills.filter(skill => skill.direction === direction);
    return `<section class="skill-list-card"><h2>${title} <span class="field-hint">(${skills.length})</span></h2>${skills.length ? skills.map(skill => `<div class="skill-row"><div class="skill-main"><strong>${escapeHtml(skill.name)}</strong><small>${escapeHtml(skill.level)} · ${escapeHtml(skill.category || 'General')}</small></div><div class="skill-actions"><button class="icon-button" title="Edit ${escapeHtml(skill.name)}" aria-label="Edit ${escapeHtml(skill.name)}" data-action="edit-skill" data-name="${escapeHtml(skill.name)}" data-direction="${direction}">✎</button><button class="icon-button" title="Remove ${escapeHtml(skill.name)}" aria-label="Remove ${escapeHtml(skill.name)}" data-action="remove-skill" data-name="${escapeHtml(skill.name)}" data-direction="${direction}">×</button></div></div>`).join('') : '<p class="field-hint">Add a skill to get started.</p>'}</section>`;
  };
  return `${pageHead('My skills', 'A clear profile helps classmates find the right exchange. Add skills you can share and goals you’re excited to learn.')}
    <div class="skill-columns">${list('OFFERED', 'Can teach')}${list('WANTED', 'Wants to learn')}</div>
    <section class="panel"><h2>Add a skill</h2><p class="form-note">Your skill list belongs to ${escapeHtml(active.name)}. Names are matched without case differences.</p><form data-form="add-skill"><div class="form-grid"><div class="field"><label for="skill-name">Skill name</label><input id="skill-name" name="name" placeholder="e.g. Public speaking" required maxlength="60"></div><div class="field"><label for="skill-direction">I want to</label><select id="skill-direction" name="direction"><option value="OFFERED">Teach this skill</option><option value="WANTED">Learn this skill</option></select></div><div class="field"><label for="skill-level">Proficiency</label><select id="skill-level" name="level">${LEVELS.map(level => `<option>${level}</option>`).join('')}</select></div><div class="field"><label for="skill-category">Category</label><input id="skill-category" name="category" placeholder="General" maxlength="40"></div></div><div class="form-actions"><button class="button" type="submit">Add skill</button></div></form></section>`;
}

function renderProfile() {
  const active = currentStudent();
  const rating = averageRating(active.id);
  const editForm = editingProfile ? `<form data-form="edit-profile" class="inline-form"><div class="form-grid"><div class="field"><label for="edit-name">Name</label><input id="edit-name" name="name" value="${escapeHtml(active.name)}" required maxlength="80"></div><div class="field"><label for="edit-email">Email</label><input id="edit-email" name="email" type="email" value="${escapeHtml(active.email)}" required></div></div><div class="form-actions"><button class="button secondary small" type="button" data-action="toggle-profile-edit">Cancel</button><button class="button small">Save profile</button></div></form>` : '';
  return `${pageHead('Profile & join', 'Build a welcoming class community. Switch between demo profiles or add a classmate on this device.')}
    <section class="profile-card"><div><div class="profile-main"><span class="avatar ${avatarTone(active.id)}">${initials(active.name)}</span><div style="min-width:0"><h2>${escapeHtml(active.name)}</h2><p class="email">${escapeHtml(active.email)}</p><div class="profile-stats"><span>${offered(active).length} skill${offered(active).length===1?'':'s'} to teach</span><span>${wanted(active).length} learning goal${wanted(active).length===1?'':'s'}</span></div></div></div><button class="button secondary small" style="margin-top:13px" data-action="toggle-profile-edit">${editingProfile ? 'Close editor' : 'Edit profile'}</button></div><div class="rating-box"><strong>${rating.count ? rating.average.toFixed(1) : '—'} ★</strong><small>TEACHING RATING<br>${rating.count} saved review${rating.count===1?'':'s'}</small></div>${editForm}</section>
    <div class="profile-grid"><section class="profile-summary"><h3>Can teach</h3><p>${escapeHtml(offered(active).map(skill => `${skill.name} · ${skill.level} · ${skill.category}`).join('  |  ') || 'Add a skill you can share.')}</p></section><section class="profile-summary"><h3>Wants to learn</h3><p>${escapeHtml(wanted(active).map(skill => `${skill.name} · ${skill.level} · ${skill.category}`).join('  |  ') || 'Add a skill you would like to learn.')}</p></section></div>
    <section class="panel" style="margin-bottom:15px"><h2>Register a student</h2><p class="form-note">Email addresses must be unique. Profiles are stored in this browser on this device.</p><form data-form="register"><div class="form-grid"><div class="field"><label for="register-name">Name</label><input id="register-name" name="name" autocomplete="name" required maxlength="80"></div><div class="field"><label for="register-email">Email</label><input id="register-email" name="email" type="email" autocomplete="email" required></div><div class="field"><label for="register-offered">Can teach</label><input id="register-offered" name="offered" placeholder="Java, Drawing" required><span class="field-hint">Separate multiple skill names with commas.</span></div><div class="field"><label for="register-teach-level">Teaching level</label><select id="register-teach-level" name="teachLevel">${LEVELS.map(level=>`<option>${level}</option>`).join('')}</select></div><div class="field"><label for="register-wanted">Wants to learn</label><input id="register-wanted" name="wanted" placeholder="Python, Photography" required><span class="field-hint">Add at least one learning goal.</span></div><div class="field"><label for="register-learn-level">Requested level</label><select id="register-learn-level" name="learnLevel">${LEVELS.map(level=>`<option>${level}</option>`).join('')}</select></div></div><div class="form-actions"><button class="button" type="submit">Create student profile</button></div></form></section>
    <section class="panel"><div class="section-row" style="margin:0"><div><h2>Class profiles</h2><p>Choose a profile to explore its dashboard, matches, and sessions.</p></div><button class="button secondary small" data-action="add-demo-data">Add demo profiles</button></div><div class="profile-list">${state.students.map(student => `<button class="student-choice ${student.id===active.id?'current':''}" data-action="switch-profile" data-id="${escapeHtml(student.id)}"><strong>${escapeHtml(student.name)}${student.id===active.id?' · You':''}</strong><small>${escapeHtml(student.email)} · ${offered(student).length} teach · ${wanted(student).length} learn</small></button>`).join('')}</div></section>`;
}

function sessionStatusClass(status) { return String(status).toLowerCase(); }
function renderSessions() {
  const active = currentStudent();
  const own = state.sessions.filter(session => session.teacherId === active.id || session.learnerId === active.id).sort((a,b) => new Date(b.scheduledAt)-new Date(a.scheduledAt));
  const visible = sessionFilter === 'all' ? own : own.filter(session => session.status === sessionFilter.toUpperCase());
  const list = visible.length ? `<div class="session-list">${visible.map(renderSessionCard).join('')}</div>` : emptyState('◷', sessionFilter === 'all' ? 'No sessions yet' : `No ${escapeHtml(sessionFilter)} sessions`, 'Find a partner and request an exchange. You can manage every session here.', `<button class="button" data-page="matches">Find a partner</button>`);
  return `${pageHead('Sessions', 'Keep your exchanges organized. Accept requests, find a time that works, and celebrate completed learning.')}
    <div class="match-controls"><span class="match-help">${own.length} session${own.length===1?'':'s'} for ${escapeHtml(active.name)}</span><label><span class="sr-only">Filter sessions</span><select class="filter-select" id="session-filter"><option value="all" ${sessionFilter==='all'?'selected':''}>All sessions</option>${['pending','accepted','completed','cancelled'].map(status=>`<option value="${status}" ${sessionFilter===status?'selected':''}>${status[0].toUpperCase()+status.slice(1)}</option>`).join('')}</select></label></div>${list}`;
}
function renderSessionCard(session) {
  const active = currentStudent();
  const isTeacher = session.teacherId === active.id;
  const partner = studentById(isTeacher ? session.learnerId : session.teacherId);
  const role = isTeacher ? 'You are teaching' : 'You are learning';
  const canRate = !isTeacher && session.status === 'COMPLETED' && !session.rating;
  const actions = [];
  if (isTeacher && session.status === 'PENDING') actions.push(`<button class="button small" data-action="accept-session" data-id="${escapeHtml(session.id)}">Accept request</button>`);
  if (session.status === 'ACCEPTED') actions.push(`<button class="button small" data-action="complete-session" data-id="${escapeHtml(session.id)}">Mark complete</button>`);
  if (['PENDING','ACCEPTED'].includes(session.status)) actions.push(`<button class="button danger small" data-action="cancel-session" data-id="${escapeHtml(session.id)}">Cancel</button>`);
  const reschedule = ['PENDING','ACCEPTED'].includes(session.status) ? `<details class="session-extra"><summary>Suggest another time</summary><form data-form="reschedule" data-id="${escapeHtml(session.id)}"><div class="form-grid"><div class="field full-width"><label>New date and time</label><input name="scheduledAt" type="datetime-local" value="${escapeHtml(localDateTimeInput(session.scheduledAt))}" required></div></div><div class="form-actions"><button class="button secondary small">Save new time</button></div></form></details>` : '';
  const rateForm = canRate ? `<details class="session-extra" open><summary>Leave a kind review</summary><form data-form="rate-session" data-id="${escapeHtml(session.id)}"><div class="form-grid"><div class="field"><label>Stars</label><select name="stars">${[5,4,3,2,1].map(score=>`<option value="${score}" ${score===5?'selected':''}>${'★'.repeat(score)} (${score})</option>`).join('')}</select></div><div class="field"><label>What did you learn?</label><input name="learned" maxlength="120" placeholder="Loops and functions"></div><div class="field full-width"><label>Feedback</label><textarea name="feedback" maxlength="500" placeholder="What made this session helpful?"></textarea></div></div><div class="form-actions"><button class="button small">Save review</button></div></form></details>` : '';
  const review = session.rating ? `<div class="session-feedback"><strong>Review · ${'★'.repeat(session.rating.stars)}${'☆'.repeat(5-session.rating.stars)}</strong>${session.rating.learned ? `<p>Learned: ${escapeHtml(session.rating.learned)}</p>` : ''}${session.rating.feedback ? `<p>${escapeHtml(session.rating.feedback)}</p>` : ''}</div>` : '';
  return `<article class="session-card"><div class="session-top"><div class="session-person"><span class="avatar ${avatarTone(partner?.id || '')}">${initials(partner?.name || '?')}</span><div><h3>${escapeHtml(partner?.name || 'Classmate')}</h3><p>${role} · ${escapeHtml(session.skillName)}</p></div></div><span class="status-pill ${sessionStatusClass(session.status)}">${escapeHtml(session.status[0]+session.status.slice(1).toLowerCase())}</span></div><div class="session-detail"><div><span>Skill</span><strong>${escapeHtml(session.skillName)}</strong></div><div><span>Scheduled</span><strong>${escapeHtml(displayDate(session.scheduledAt))}</strong></div><div><span>Your role</span><strong>${role}</strong></div><div><span>Exchange</span><strong>${session.matchGroupId === session.id ? 'Direct exchange' : 'Linked learning circle'}</strong></div></div><div class="session-actions">${actions.join('') || `<span class="match-rating">${session.status==='COMPLETED'?'A lovely exchange, completed.':session.status==='CANCELLED'?'This session was cancelled.':'No action needed right now.'}</span>`}</div>${reschedule}${rateForm}${review}</article>`;
}

function splitSkillNames(value) {
  return [...new Set(String(value || '').split(',').map(name => name.trim()).filter(Boolean).map(name => name.slice(0,60)))];
}
function registerProfile(form) {
  const data = new FormData(form);
  const name = String(data.get('name') || '').trim();
  const email = String(data.get('email') || '').trim().toLowerCase();
  const teachNames = splitSkillNames(data.get('offered'));
  const learnNames = splitSkillNames(data.get('wanted'));
  if (!name) throw new Error('Enter the student’s name.');
  if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(email)) throw new Error('Enter a valid email address.');
  if (emailTaken(email)) throw new Error('A profile with that email already exists.');
  if (!teachNames.length || !learnNames.length) throw new Error('Add at least one skill to teach and one to learn.');
  const skills = [
    ...teachNames.map(skill => ({ name: skill, category: 'General', level: String(data.get('teachLevel')), direction: 'OFFERED' })),
    ...learnNames.map(skill => ({ name: skill, category: 'General', level: String(data.get('learnLevel')), direction: 'WANTED' }))
  ];
  const student = { id: uid('student'), name, email, skills };
  state.students.push(student);
  state.currentId = student.id;
  editingProfile = false;
  saveAndRender(`${student.name}’s profile is ready.`);
}
function ensureDemoData() {
  const demo = seedState();
  const added = [];
  const idMap = new Map();
  for (const profile of demo.students) {
    const existing = state.students.find(student => student.email.toLowerCase() === profile.email.toLowerCase());
    if (existing) idMap.set(profile.id, existing.id);
    else { state.students.push(profile); idMap.set(profile.id, profile.id); added.push(profile.name); }
  }
  for (const sample of demo.sessions) {
    if (state.sessions.some(session => session.id === sample.id)) continue;
    const mapped = { ...sample, teacherId: idMap.get(sample.teacherId), learnerId: idMap.get(sample.learnerId), rating: { ...sample.rating, reviewerId: idMap.get(sample.rating.reviewerId) } };
    state.sessions.push(mapped);
  }
  saveAndRender(added.length ? `Added ${added.length} demo profile${added.length===1?'':'s'}.` : 'Demo profiles and sample reviews are ready.');
}
function requestMatch(index, mode) {
  const matches = mode === 'chain' ? findChainMatches() : findDirectMatches();
  const match = matches[index];
  if (!match) throw new Error('That match has changed. Refresh the match list and try again.');
  if (!match.exchanges.every(exchange => studentById(exchange.learnerId) && studentById(exchange.teacherId))) throw new Error('A student in this exchange is no longer available.');
  const groupId = uid('exchange');
  const created = match.exchanges.map((exchange, offset) => ({
    id: uid('session'), teacherId: exchange.teacherId, learnerId: exchange.learnerId, skillName: exchange.skillName,
    scheduledAt: new Date(Date.now() + 86400000 + offset * 3600000).toISOString(),
    notes: match.type === 'CHAIN' ? 'Learning circle · each participant receives one requested skill' : 'Direct skill exchange',
    status: 'PENDING', completedAt: null, matchGroupId: groupId, rating: null
  }));
  state.sessions.push(...created);
  page = 'sessions';
  sessionFilter = 'all';
  saveAndRender(`Exchange request sent. ${created.length} linked session${created.length===1?'':'s'} added.`);
}

function addSkill(form) {
  const data = new FormData(form);
  const name = String(data.get('name') || '').trim();
  const direction = String(data.get('direction'));
  const level = String(data.get('level'));
  const category = String(data.get('category') || 'General').trim() || 'General';
  const active = currentStudent();
  if (!name) throw new Error('Enter a skill name.');
  if (!['OFFERED','WANTED'].includes(direction) || !LEVELS.includes(level)) throw new Error('Choose a valid skill direction and level.');
  if (skillIndex(active, direction, name) >= 0) throw new Error('That skill is already in this list.');
  active.skills.push({ name, direction, level, category });
  saveAndRender(`${name} added to your skills.`);
}
function openSkillEditor(name, direction) {
  const skill = currentStudent().skills.find(item => item.direction === direction && norm(item.name) === norm(name));
  if (!skill) throw new Error('That skill is no longer in this list.');
  document.getElementById('dialog-root').innerHTML = `<div class="dialog-backdrop" data-action="close-dialog"><section class="dialog" role="dialog" aria-modal="true" aria-labelledby="edit-skill-title"><div class="dialog-head"><h2 id="edit-skill-title">Edit skill</h2><button class="icon-button" type="button" aria-label="Close" data-action="close-dialog">×</button></div><form data-form="edit-skill"><input type="hidden" name="oldName" value="${escapeHtml(skill.name)}"><input type="hidden" name="direction" value="${escapeHtml(direction)}"><div class="form-grid"><div class="field full-width"><label>Skill name</label><input name="name" value="${escapeHtml(skill.name)}" required maxlength="60"></div><div class="field"><label>Proficiency</label><select name="level">${LEVELS.map(level=>`<option ${skill.level===level?'selected':''}>${level}</option>`).join('')}</select></div><div class="field"><label>Category</label><input name="category" value="${escapeHtml(skill.category)}" maxlength="40"></div></div><div class="form-actions"><button class="button secondary small" type="button" data-action="close-dialog">Cancel</button><button class="button small">Save skill</button></div></form></section></div>`;
}
function updateSkill(form) {
  const data = new FormData(form);
  const active = currentStudent();
  const direction = String(data.get('direction'));
  const oldName = String(data.get('oldName'));
  const newName = String(data.get('name') || '').trim();
  const index = skillIndex(active, direction, oldName);
  if (index < 0) throw new Error('That skill is no longer in this list.');
  const duplicate = skillIndex(active, direction, newName);
  if (!newName) throw new Error('Enter a skill name.');
  if (duplicate >= 0 && duplicate !== index) throw new Error('That skill is already in this list.');
  active.skills[index] = { ...active.skills[index], name: newName, category: String(data.get('category') || 'General').trim() || 'General', level: String(data.get('level')) };
  document.getElementById('dialog-root').innerHTML = '';
  saveAndRender(`${newName} updated.`);
}
function editProfile(form) {
  const data = new FormData(form);
  const active = currentStudent();
  const name = String(data.get('name') || '').trim();
  const email = String(data.get('email') || '').trim().toLowerCase();
  if (!name) throw new Error('Enter the student’s name.');
  if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(email)) throw new Error('Enter a valid email address.');
  if (emailTaken(email, active.id)) throw new Error('A profile with that email already exists.');
  active.name = name;
  active.email = email;
  editingProfile = false;
  saveAndRender('Your profile was updated.');
}

function sessionById(id) {
  const session = state.sessions.find(item => item.id === id);
  if (!session) throw new Error('That session was not found.');
  return session;
}
function requireParticipant(session) {
  if (![session.teacherId, session.learnerId].includes(state.currentId)) throw new Error('Only a session participant can update this session.');
}
function handleSessionAction(action, id) {
  const session = sessionById(id);
  requireParticipant(session);
  if (action === 'accept-session') {
    if (session.teacherId !== state.currentId) throw new Error('Switch to the teaching student to accept this request.');
    if (session.status !== 'PENDING') throw new Error('Only a pending session can be accepted.');
    session.status = 'ACCEPTED';
  } else if (action === 'complete-session') {
    if (session.status !== 'ACCEPTED') throw new Error('Accept the session before marking it complete.');
    session.status = 'COMPLETED';
    session.completedAt = new Date().toISOString();
  } else if (action === 'cancel-session') {
    if (!['PENDING','ACCEPTED'].includes(session.status)) throw new Error('This session can no longer be cancelled.');
    session.status = 'CANCELLED';
  }
  saveAndRender(action === 'accept-session' ? 'Session accepted.' : action === 'complete-session' ? 'Session marked complete.' : 'Session cancelled.');
}
function rescheduleSession(form) {
  const session = sessionById(form.dataset.id);
  requireParticipant(session);
  if (!['PENDING','ACCEPTED'].includes(session.status)) throw new Error('Only pending or accepted sessions can be rescheduled.');
  const date = new Date(new FormData(form).get('scheduledAt'));
  if (Number.isNaN(date.getTime()) || date.getTime() <= Date.now()) throw new Error('Choose a future date and time.');
  session.scheduledAt = date.toISOString();
  saveAndRender('New session time saved.');
}
function rateSession(form) {
  const session = sessionById(form.dataset.id);
  if (session.learnerId !== state.currentId) throw new Error('Only the learner can leave feedback.');
  if (session.status !== 'COMPLETED') throw new Error('Only a completed session can be rated.');
  if (session.rating) throw new Error('This session already has feedback.');
  const data = new FormData(form);
  const stars = Number(data.get('stars'));
  if (!Number.isInteger(stars) || stars < 1 || stars > 5) throw new Error('Choose a rating from one to five stars.');
  session.rating = { reviewerId: state.currentId, stars, feedback: String(data.get('feedback') || '').trim(), learned: String(data.get('learned') || '').trim(), createdAt: new Date().toISOString() };
  saveAndRender('Thank you for sharing your feedback.');
}

document.addEventListener('click', event => {
  const pageButton = event.target.closest('[data-page]');
  if (pageButton) {
    setMobileMenuOpen(false, false);
    page = pageButton.dataset.page;
    if (page === 'profile') editingProfile = false;
    render();
    document.getElementById('main-content').focus({ preventScroll: true });
    window.scrollTo({ top: 0, behavior: 'smooth' });
    return;
  }
  const button = event.target.closest('[data-action]');
  if (!button) return;
  const action = button.dataset.action;
  try {
    if (action === 'toggle-mobile-menu') setMobileMenuOpen(!mobileMenuOpen);
    else if (action === 'close-mobile-menu') setMobileMenuOpen(false);
    else if (action === 'match-mode') { matchMode = button.dataset.mode; render(); }
    else if (action === 'request-match') requestMatch(Number(button.dataset.index), button.dataset.mode);
    else if (action === 'switch-profile') { state.currentId = button.dataset.id; editingProfile = false; saveAndRender(`Now viewing ${currentStudent().name}’s profile.`); }
    else if (action === 'add-demo-data') ensureDemoData();
    else if (action === 'toggle-profile-edit') { editingProfile = !editingProfile; render(); }
    else if (action === 'edit-skill') openSkillEditor(button.dataset.name, button.dataset.direction);
    else if (action === 'remove-skill') {
      const active = currentStudent();
      const index = skillIndex(active, button.dataset.direction, button.dataset.name);
      if (index < 0) throw new Error('That skill is no longer in this list.');
      active.skills.splice(index, 1);
      saveAndRender(`${button.dataset.name} removed from your skills.`);
    }
    else if (['accept-session','complete-session','cancel-session'].includes(action)) handleSessionAction(action, button.dataset.id);
    else if (action === 'close-dialog') {
      if (event.target === button || button.classList.contains('icon-button') || event.target.classList.contains('dialog-backdrop')) document.getElementById('dialog-root').innerHTML = '';
    }
  } catch (error) { setToast(error.message || 'Something went wrong. Please try again.'); }
});

document.addEventListener('change', event => {
  if (event.target.id === 'profile-picker') {
    state.currentId = event.target.value;
    editingProfile = false;
    saveAndRender(`Now viewing ${currentStudent().name}’s profile.`);
  } else if (event.target.id === 'session-filter') {
    sessionFilter = event.target.value;
    render();
  }
});

document.addEventListener('submit', event => {
  const form = event.target.closest('form[data-form]');
  if (!form) return;
  event.preventDefault();
  try {
    switch (form.dataset.form) {
      case 'register': registerProfile(form); break;
      case 'edit-profile': editProfile(form); break;
      case 'add-skill': addSkill(form); break;
      case 'edit-skill': updateSkill(form); break;
      case 'reschedule': rescheduleSession(form); break;
      case 'rate-session': rateSession(form); break;
      default: break;
    }
  } catch (error) { setToast(error.message || 'Please check the details and try again.'); }
});

document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && mobileMenuOpen) setMobileMenuOpen(false);
});

render();
if ('serviceWorker' in navigator && /^https?:$/.test(location.protocol)) {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}));
}
