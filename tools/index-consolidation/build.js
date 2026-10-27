// Builds the consolidated documentation index from the one-file documentation/INDEX.md.
// Usage: node build.js <documentation dir to read> <output documentation dir>
// Writes: INDEX.md (one short row per batch), <folder>/README.md per batch (purpose, status, documents,
// code areas, history, notes), repository/FILE_LOOKUP.md and repository/INDEX_2026-10-26_ARCHIVE.md.
// Refuses to run when a row has no entry in short.js, so a row added later is written by hand first.
const fs = require('fs');
const path = require('path');
const { parse } = require('./parse.js');
const short = require('./short.js');

const [src, out] = process.argv.slice(2);
const DAY = '2026-10-26';
const { text, rows, notes, lookup } = parse(src);

const extra = {
  '2026-10-15-custom-equipment-rendering': ['2026-10-26: `FLANGE_JOINT_REVIEW.md` (2026-10-20, branch `claude/competent-franklin-43848a` from `2e28a20a`, commit `bf38dee9` and later, not on `main`) was found only in a session worktree and copied here.'],
  '2026-09-27-jdk-determinism': ['2026-10-26: status corrected from "In progress": the code reached `main` in 0.7.0 with the phase-ports squash `1dba69b3` (CHANGELOG 0.7.0). Open: the `winsweep` run after that merge.'],
  '2026-10-14-empty-start-progressive-volume': ['2026-10-26: status corrected from "In progress": merged into the phase-ports branch (D57) and reached `main` in 0.7.0 with the squash `1dba69b3` (CHANGELOG 0.7.0, D55).'],
  '2026-09-24-minecraft-mcp-skill': ['2026-10-26: `.claude/skills/minecraft-mcp/` is not on `main`; no branch holds the skill commits `20b1587` and `1287d6d` (branch `claude/minecraft-mcp-skill-c3f0f0` is gone), which are still in the object store.'],
};
const extraDocs = {
  repository: ['[FILE_LOOKUP.md](FILE_LOOKUP.md): old flat names `documentation/<FILE>` and the batch folder that holds each file', '[INDEX_2026-10-26_ARCHIVE.md](INDEX_2026-10-26_ARCHIVE.md): the one-file index before the 2026-10-26 consolidation, frozen'],
};

// Rewrites relative link targets written from documentation/ for a file in documentation/<folder>/.
function relink(s, folder) {
  return s.replace(/\]\(([^)\s]+)\)/g, (m, t) => {
    if (/^[a-z]+:|^#|^\//i.test(t)) return m;
    if (t === folder || t === folder + '/') return '](./)';
    if (t.startsWith(folder + '/')) return '](' + t.slice(folder.length + 1) + ')';
    return '](../' + t + ')';
  });
}

// Splits s at separators outside backticks: "; " at paren depth <= maxDepth, and ". " + capital at depth 0.
function split(s, maxDepth, commas) {
  const parts = []; let cur = ''; let depth = 0; let tick = false;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (c === '`') tick = !tick;
    if (!tick && c === '(') depth++;
    if (!tick && c === ')') depth--;
    const sep = !tick && s[i + 1] === ' ' && (
      (c === ';' && depth <= maxDepth) ||
      (commas && c === ',' && depth === 0) ||
      (c === '.' && depth === 0 && /[A-Z]/.test(s[i + 2] || '')));
    if (sep) { if (c === '.') cur += c; parts.push(cur.trim()); cur = ''; i++; continue; }
    cur += c;
  }
  if (cur.trim()) parts.push(cur.trim());
  return parts.filter(Boolean);
}

// History bullets: the shallowest split whose longest bullet is under 600 characters.
function history(status) {
  let best = [status];
  for (const d of [0, 1, 2, 3, 4, 5]) {
    best = split(status, d, false);
    if (Math.max(...best.map(b => b.length)) < 600) break;
  }
  return best;
}

// Document bullets from the row's cell (links already relative to the folder), else the folder's own files.
function documents(r, dir, docs) {
  return listed(r, dir, docs).concat(extraDocs[r.folder] || []);
}

function listed(r, dir, docs) {
  if (docs) {
    return split(docs, 0, true).filter(item => item !== '`README.md`' && item !== 'README.md').map(item => {
      const m = item.match(/^`?([^`\s(]+?)`?(\s.*)?$/);
      if (m && !m[1].includes('*') && fs.existsSync(path.join(dir, m[1]))) return '[' + m[1] + '](' + m[1] + ')' + (m[2] || '');
      return item;
    });
  }
  return fs.existsSync(dir)
    ? fs.readdirSync(dir).filter(f => f !== 'README.md').map(f => fs.statSync(path.join(dir, f)).isDirectory() ? '[' + f + '/](' + f + '/)' : '[' + f + '](' + f + ')')
    : [];
}

function card(r, h) {
  const dir = path.join(src, r.folder);
  const rel = s => relink(s, r.folder);
  const lines = [];
  lines.push('Purpose: ' + rel(r.purpose), '', 'Status: ' + short[r.folder].s + '. Index: [../INDEX.md](../INDEX.md).', '');
  const docs = documents(r, dir, rel(r.docs));
  if (docs.length) lines.push(h + ' Documents', '', ...docs.map(d => '- ' + d), '');
  if (r.code) lines.push(h + ' Code areas', '', rel(r.code), '');
  lines.push(h + ' History', '', ...history(rel(r.status)).map(b => '- ' + b), '');
  const n = (notes[r.folder] || []).map(rel).concat(extra[r.folder] || []);
  if (n.length) lines.push(h + ' Notes', '', ...n.map(x => '- ' + x), '');
  return lines.join('\n');
}

function write(file, body) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, body.replace(/\n{3,}/g, '\n\n').replace(/\s*$/, '\n'));
}

const missing = rows.filter(r => !short[r.folder]).map(r => r.folder);
if (missing.length) { console.error('No short.js entry for: ' + missing.join(', ')); process.exit(1); }

for (const r of rows) {
  const old = path.join(src, r.folder, 'README.md');
  const body = fs.existsSync(old)
    ? fs.readFileSync(old, 'utf8').replace(/\s*$/, '') + '\n\n## Index record (moved from INDEX.md on ' + DAY + ')\n\n' + card(r, '###')
    : '# ' + r.folder + '\n\n' + card(r, '##');
  write(path.join(out, r.folder, 'README.md'), body);
}

const batches = rows.filter(r => /^\d{4}-\d{2}-\d{2}-/.test(r.folder));
const others = rows.filter(r => !/^\d{4}-\d{2}-\d{2}-/.test(r.folder));
if (!batches.some(r => r.folder === '2026-10-26-index-consolidation')) batches.push({ folder: '2026-10-26-index-consolidation' });
const key = f => f.slice(0, 10) + (f.slice(11) > '' ? '' : '');
batches.sort((a, b) => (b.folder.slice(0, 10)).localeCompare(a.folder.slice(0, 10)) || a.folder.localeCompare(b.folder));

const index = [
  '# Documentation index',
  '',
  'One row per batch of work, newest first. A row holds only the purpose and the current status; the batch folder\'s `README.md` holds its history, documents, code areas and open items. Layout and update rule: [README.md](README.md). Research: [../research/INDEX.md](../research/INDEX.md). Tools: [../tools/INDEX.md](../tools/INDEX.md). Release history: `CHANGELOG.md` in the repository root. A document cited by its old flat path `documentation/<FILE>`: [repository/FILE_LOOKUP.md](repository/FILE_LOOKUP.md).',
  '',
  'Status words:',
  '',
  '- **Implemented YYYY-MM-DD (version)**: the code is on `main`; the date is the day it reached `main`.',
  '- **In progress since YYYY-MM-DD**: work has started on a branch and has not merged.',
  '- **Planned**: a plan exists and no implementation has started.',
  '- **Concluded YYYY-MM-DD**: research or measurement that was finished and was never meant to change code.',
  '- **Abandoned YYYY-MM-DD** / **Superseded YYYY-MM-DD by ...**: stopped, with the reason in the batch `README.md`.',
  '',
  '## Batches',
  '',
  '| Batch | Purpose | Status |',
  '|---|---|---|',
  ...batches.map(r => '| [' + r.folder + '](' + r.folder + '/README.md) | ' + short[r.folder].p + ' | ' + short[r.folder].s + ' |'),
  '',
  '## Other folders',
  '',
  '| Folder | Holds |',
  '|---|---|',
  ...others.map(r => '| [' + r.folder + '](' + r.folder + '/README.md) | ' + short[r.folder].p + ' |'),
  '',
];
write(path.join(out, 'INDEX.md'), index.join('\n'));

const lookupBody = lookup.filter(l => !l.startsWith('Pipe thermal-loss review:')).join('\n');
write(path.join(out, 'repository', 'FILE_LOOKUP.md'), [
  '# Old flat document names',
  '',
  'The flat `documentation/` folder was split into batch folders on 2026-09-23 ([DOCUMENTATION_ORGANIZATION_2026-09-23.md](DOCUMENTATION_ORGANIZATION_2026-09-23.md)). File names were kept, so a file cited by its old path `documentation/<FILE>` is found in the table below. Moved out of `INDEX.md` on ' + DAY + '; the table is not extended for later batches.',
  '',
  relink(lookupBody.replace(/^## /gm, '## '), 'repository'),
].join('\n'));

write(path.join(out, 'repository', 'INDEX_2026-10-26_ARCHIVE.md'),
  '<!-- Frozen copy of documentation/INDEX.md before the ' + DAY + ' consolidation. Link targets are prefixed with ../; nothing else changed. -->\n\n' + relink(text, 'repository'));

console.log('rows ' + rows.length + ', batches ' + batches.length + ', READMEs ' + rows.length + ', lookup lines ' + lookup.length);
