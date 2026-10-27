// Builds the short tools/INDEX.md and appends each row's detail to its folder's README.md.
// Usage: node tools-build.js <tools dir to read> <output tools dir> [--list]
// --list prints folder, derived short status and purpose length without writing.
const fs = require('fs');
const path = require('path');
const { cells } = require('./parse.js');
const tshort = require('./tools-short.js');

const [src, out, flag] = process.argv.slice(2);
const DAY = '2026-10-26';
const text = fs.readFileSync(path.join(src, 'INDEX.md'), 'utf8');
const lines = text.split(/\r?\n/);

const rows = []; const examples = []; let section = '';
lines.forEach((l, i) => {
  if (l.startsWith('## ')) section = l;
  if (l.startsWith('| [')) {
    const c = cells(l); const m = l.match(/^\| \[([^\]]+)\]\(([^)]*)\)/);
    const href = m[2]; const folder = href.replace(/\/README\.md$/, '').replace(/\/$/, '');
    rows.push({ line: i + 1, folder, purpose: c[1] || '', batch: c[2] || '', status: c[3] || '', entry: c[4] || '' });
  } else if (section.startsWith('## examples/')) examples.push(l);
});

// Leading clause of a status: up to the first " (", "; " or ", ".
const lead = s => s.split(/ \(|; |, /)[0].trim();
const shortStatus = r => (tshort[r.folder] && tshort[r.folder].s) || (r.status.length <= 45 ? r.status : lead(r.status));
const shortPurpose = r => (tshort[r.folder] && tshort[r.folder].p) || r.purpose;
const batches = r => {
  const names = [...new Set([...r.batch.matchAll(/\d{4}-\d{2}-\d{2}-[a-z0-9-]+[a-z0-9]/g)].map(m => m[0]))];
  return names.length ? names.map(n => '`' + n + '`').join(', ') : r.batch;
};
const date = r => (r.batch.match(/\d{4}-\d{2}-\d{2}/) || ['0000-00-00'])[0];

if (flag === '--list') {
  for (const r of rows) console.log(r.folder + ' | ' + shortStatus(r) + ' | p' + shortPurpose(r).length + (tshort[r.folder] ? '' : (r.purpose.length > 110 ? ' NEEDS-P' : '')));
  process.exit(0);
}
const long = rows.filter(r => shortPurpose(r).length > 110 || shortStatus(r).length > 60).map(r => r.folder);
if (long.length) { console.error('Too long, add to tools-short.js: ' + long.join(', ')); process.exit(1); }

// "; " splits at the shallowest paren depth that keeps bullets under 600 characters (same as build.js).
function split(s, maxDepth) {
  const parts = []; let cur = ''; let depth = 0; let tick = false;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (c === '`') tick = !tick;
    if (!tick && c === '(') depth++;
    if (!tick && c === ')') depth--;
    if (!tick && s[i + 1] === ' ' && ((c === ';' && depth <= maxDepth) || (c === '.' && depth === 0 && /[A-Z]/.test(s[i + 2] || '')))) {
      if (c === '.') cur += c; parts.push(cur.trim()); cur = ''; i++; continue;
    }
    cur += c;
  }
  if (cur.trim()) parts.push(cur.trim());
  return parts.filter(Boolean);
}
function bullets(s) {
  let best = [s];
  for (const d of [0, 1, 2, 3, 4, 5]) { best = split(s, d); if (Math.max(...best.map(b => b.length)) < 600) break; }
  return best.map(b => '- ' + b);
}
// Link targets written from tools/ rewritten for a file in tools/<folder>/.
function relink(s, folder) {
  return s.replace(/\]\(([^)\s]+)\)/g, (m, t) => {
    if (/^[a-z]+:|^#|^\//i.test(t)) return m;
    if (t === folder || t === folder + '/') return '](./)';
    if (t.startsWith(folder + '/')) return '](' + t.slice(folder.length + 1) + ')';
    return '](../' + t + ')';
  });
}

function record(r, h) {
  const rel = s => relink(s, r.folder);
  const out = ['Purpose: ' + rel(r.purpose), '', 'Batch: ' + rel(r.batch), '', 'Status: ' + shortStatus(r) + '. Index: [../INDEX.md](../INDEX.md).', ''];
  if (r.entry) out.push(h + ' Entry points', '', rel(r.entry), '');
  if (r.status) out.push(h + ' History', '', ...bullets(rel(r.status)), '');
  const n = tshort[r.folder] && tshort[r.folder].n;
  if (n) out.push(h + ' Notes', '', ...n.map(x => '- ' + x), '');
  return out.join('\n');
}
function write(file, body) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, body.replace(/\n{3,}/g, '\n\n').replace(/\s*$/, '\n'));
}

let appended = 0, created = 0;
for (const r of rows) {
  const dir = path.join(src, r.folder);
  if (!fs.statSync(dir).isDirectory()) continue; // development.gradle keeps its detail in the row
  const old = path.join(dir, 'README.md');
  if (fs.existsSync(old)) {
    write(path.join(out, r.folder, 'README.md'), fs.readFileSync(old, 'utf8').replace(/\s*$/, '') + '\n\n## Index record (moved from tools/INDEX.md on ' + DAY + ')\n\n' + record(r, '###'));
    appended++;
  } else {
    write(path.join(out, r.folder, 'README.md'), '# ' + r.folder + '\n\n' + record(r, '##'));
    created++;
  }
}

const sorted = rows.map((r, i) => ({ r, i })).sort((a, b) => date(b.r).localeCompare(date(a.r)) || a.i - b.i).map(x => x.r);
const head = lines.slice(0, lines.findIndex(l => l.startsWith('Last updated:'))).join('\n')
  .replace('Add a new tool as its own folder, with a README or protocol, and add a row here.',
    'Add a new tool as its own folder with a `README.md` (purpose, batch, how to run, entry points, history), and add one short row here: the folder, the purpose in a few words, the batch and the status with its date. Nothing else goes in the row. The index as it stood before ' + DAY + ' is frozen in [index-consolidation/INDEX_2026-10-26_ARCHIVE.md](index-consolidation/INDEX_2026-10-26_ARCHIVE.md).');
const index = [head.replace(/\s*$/, ''), '', '## Folders', '', '| Folder | Purpose | Batch | Status |', '|---|---|---|---|',
  ...sorted.map(r => {
    const dir = path.join(src, r.folder);
    const target = fs.statSync(dir).isDirectory() ? r.folder + '/README.md' : r.folder;
    return '| [' + r.folder + (fs.statSync(dir).isDirectory() ? '/' : '') + '](' + target + ') | ' + shortPurpose(r) + ' | ' + batches(r) + ' | ' + shortStatus(r) + ' |';
  }),
  '', ...examples].join('\n');
write(path.join(out, 'INDEX.md'), index);
write(path.join(out, 'index-consolidation', 'INDEX_2026-10-26_ARCHIVE.md'),
  '<!-- Frozen copy of tools/INDEX.md before the ' + DAY + ' consolidation. Link targets are prefixed with ../; nothing else changed. -->\n\n' + relink(text, 'index-consolidation'));
console.log('rows ' + rows.length + ', READMEs appended ' + appended + ', created ' + created);
