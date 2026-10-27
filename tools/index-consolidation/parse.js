// Reads documentation/INDEX.md (old one-file layout) into rows, batch notes and the lookup sections.
// Usage as a script: node parse.js <documentation dir>  -> prints one JSON line per row.
const fs = require('fs');
const path = require('path');

// Splits a markdown table row on pipes outside backticks.
function cells(line) {
  const out = []; let cur = ''; let tick = false;
  for (let i = 1; i < line.length; i++) {
    const c = line[i];
    if (c === '`') tick = !tick;
    if (c === '|' && !tick) { out.push(cur.trim()); cur = ''; } else cur += c;
  }
  if (cur.trim()) out.push(cur.trim());
  return out;
}

function parse(docDir) {
  const text = fs.readFileSync(path.join(docDir, 'INDEX.md'), 'utf8');
  const lines = text.split(/\r?\n/);
  const rows = [];
  const notes = {};
  let section = '';
  const lookup = [];
  for (const line of lines) {
    if (line.startsWith('## ')) { section = line.slice(3).trim(); }
    const m = line.match(/^\| \[([^\]]+)\]\(([^)]*)\)/);
    if (m && !line.startsWith('| `')) {
      const c = cells(line);
      rows.push({ folder: m[1], purpose: c[1], status: c[2], docs: c[3] || '', code: c[4] || '' });
      continue;
    }
    const n = line.match(/^- \*\*([0-9a-z-]+)\*\*: (.*)$/);
    if (n && section === 'Batch notes') {
      (notes[n[1]] = notes[n[1]] || []).push(n[2]);
      continue;
    }
    if (section === 'Tracked files that still cite the old flat paths' || section === 'File lookup') lookup.push(line);
  }
  return { text, rows, notes, lookup };
}

module.exports = { cells, parse };
if (require.main === module) {
  const { rows, notes } = parse(process.argv[2]);
  for (const r of rows) {
    console.log(JSON.stringify({ f: r.folder, pl: r.purpose.length, p: r.purpose.slice(0, 160), s: r.status.slice(0, 240), sl: r.status.length }));
  }
  console.error(rows.length + ' rows; notes for ' + Object.keys(notes).join(', '));
}
