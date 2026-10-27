// lang-merge.js <lang.json> <entries.json>...: inserts each entries file's keys into the lang file as text lines, each
// after the anchor key the entries file names (its "@after" map: anchor -> list of keys, in order), or replaces the value
// of a key that already exists. The lang file is edited line by line so its other entries (and their \u escapes) stay
// as they are. Entries files: {"@after": {"anchor.key": ["new.key", ...]}, "new.key": "text", ...}.
const fs = require('fs');
const [langPath, ...entryPaths] = process.argv.slice(2);
let lines = fs.readFileSync(langPath, 'utf8').split('\n');
const keyOf = line => { const m = line.match(/^  "([^"]+)":/); return m ? m[1] : null; };
const lineFor = (key, value) => '  ' + JSON.stringify(key) + ': ' + JSON.stringify(value) + ',';
for (const path of entryPaths) {
  const entries = JSON.parse(fs.readFileSync(path, 'utf8'));
  const after = entries['@after'] || {};
  const placed = new Set();
  for (const [anchor, keys] of Object.entries(after)) {
    let at = lines.findIndex(l => keyOf(l) === anchor);
    if (at < 0) throw new Error(path + ': no anchor ' + anchor);
    for (const key of keys) {
      if (!(key in entries)) throw new Error(path + ': no value for ' + key);
      const existing = lines.findIndex(l => keyOf(l) === key);
      if (existing >= 0) { lines[existing] = lineFor(key, entries[key]); at = existing; }
      else { lines.splice(at + 1, 0, lineFor(key, entries[key])); at++; }
      placed.add(key);
    }
  }
  // A key no anchor places (a lane's flat map) replaces its existing line, or goes after the last key of its group (the
  // first three dot-separated parts: gui.createcheme.vessel.).
  for (const key of Object.keys(entries)) {
    if (key === '@after' || placed.has(key)) continue;
    const existing = lines.findIndex(l => keyOf(l) === key);
    if (existing >= 0) { lines[existing] = lineFor(key, entries[key]); continue; }
    const group = key.split('.').slice(0, 3).join('.') + '.';
    let at = -1;
    lines.forEach((l, i) => { if (keyOf(l) && keyOf(l).startsWith(group)) at = i; });
    if (at < 0) throw new Error(path + ': no group ' + group + ' for ' + key);
    lines.splice(at + 1, 0, lineFor(key, entries[key]));
  }
}
// The last entry before the closing brace has no comma.
const close = lines.lastIndexOf('}');
for (let i = 0; i < close; i++) if (keyOf(lines[i])) lines[i] = lines[i].replace(/,$/, '') + (i < close - 1 && lines.slice(i + 1, close).some(keyOf) ? ',' : '');
const text = lines.join('\n');
JSON.parse(text);
fs.writeFileSync(langPath, text);
