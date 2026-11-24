// Small helpers shared by the pages. The student is only a random code kept in this browser.
const _dgktMem = {};
function _dgktGet(k) { try { return localStorage.getItem(k); } catch (e) { return _dgktMem[k] || null; } }
function _dgktSet(k, v) { try { localStorage.setItem(k, v); } catch (e) { _dgktMem[k] = v; } }
function dgktStudentCode() {
  let c = _dgktGet('dgktStudent');
  if (!c) { c = 's' + Math.random().toString(36).slice(2, 10) + Date.now().toString(36); _dgktSet('dgktStudent', c); }
  return c;
}
function dgktNickname() { return _dgktGet('dgktNick') || ''; }
function dgktSetNickname(n) { _dgktSet('dgktNick', n || ''); }

/** Forget this browser's student code and nickname (used after the owner clears all data). */
function dgktNewStudent() { _dgktSet('dgktStudent', ''); _dgktSet('dgktNick', ''); }
