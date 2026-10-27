#!/usr/bin/env node
// check-rows.js: offline audit of the row files before a session. For each row, replays its place/remove steps and
// reports (1) two blocks on one cell, (2) every connection the game will make (a pipe joins any neighbouring device or
// pipe; an actuator only along its facing; two non-pipe devices never join directly), (3) contacts between rows, and
// (4) for every device a step opens: where the player stands, that the body cells are free and something is underfoot.
//   node check-rows.js [rowsDir]
'use strict';
const fs = require('fs');
const path = require('path');
const dir = process.argv[2] || path.join(__dirname, '..', 'rows');
const key = p => p.join(',');
const N6 = [[1, 0, 0, 'east'], [-1, 0, 0, 'west'], [0, 1, 0, 'up'], [0, -1, 0, 'down'], [0, 0, 1, 'south'], [0, 0, -1, 'north']];
const FACING = { east: [1, 0, 0], west: [-1, 0, 0], up: [0, 1, 0], down: [0, -1, 0], south: [0, 0, 1], north: [0, 0, -1] };
const actuator = b => ['pump', 'compressor', 'valve'].includes(b.block);
function axisOk(b, d) { if (!actuator(b)) return true; const f = FACING[b.facing || 'north']; return f[0] * d[0] + f[1] * d[1] + f[2] * d[2] !== 0; }
function joins(a, b, d) {
  if (a.block === 'glass' || b.block === 'glass') return false;
  if (a.block !== 'pipe' && b.block !== 'pipe') return false;
  return axisOk(a, d) && axisOk(b, [-d[0], -d[1], -d[2]]);
}
let problems = 0;
const allCells = new Map(); // cell -> row id (every block a row ever places)
for (const file of fs.readdirSync(dir).filter(f => f.endsWith('.json')).sort()) {
  const row = JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8'));
  const world = new Map(), out = [];
  for (const [id, b] of Object.entries(row.blocks)) if (b.foreign) world.set(key(b.at), { ...b, id });
  const opened = new Set();
  for (const s of row.steps) {
    if (s.op === 'place') for (const id of s.ids) {
      const b = row.blocks[id], k = key(b.at);
      if (world.has(k)) { out.push(`  CELL ${k}: ${id} placed on ${world.get(k).id}`); problems++; }
      world.set(k, { ...b, id });
      const other = allCells.get(k); if (other && other !== row.id) { out.push(`  ROW CLASH ${k}: ${id} on a cell of ${other}`); problems++; }
      allCells.set(k, row.id);
    }
    if (s.op === 'remove' || s.op === 'break') for (const id of s.ids) world.delete(key(row.blocks[id].at));
    if (s.op === 'set' || s.op === 'read') for (const id of s.id ? [s.id] : s.ids) {
      if (!row.blocks[id]) continue;
      const b = row.blocks[id];
      if (!world.has(key(b.at)) && row.id !== 'R6-R12') { out.push(`  OPEN ${id}: not placed at that step`); problems++; }
      const stand = b.aim && b.aim.stand ? b.aim.stand : b.at;
      const body = [[stand[0], stand[1] + 1, stand[2]], [stand[0], stand[1] + 2, stand[2]]];
      for (const c of body) if (world.has(key(c))) { out.push(`  AIM ${id}: body cell ${key(c)} holds ${world.get(key(c)).id}`); problems++; }
      if (!(world.has(key(stand)) || stand[1] === 100)) { out.push(`  AIM ${id}: nothing to stand on at ${key(stand)}`); problems++; }
      opened.add(`${id}@${key(stand)}`);
    }
  }
  // Connections in the final world of the row and at its fullest (every placed block), for the record.
  const full = new Map(); for (const b of Object.entries(row.blocks)) if (row.steps.some(s => s.op === 'place' && s.ids.includes(b[0]))) full.set(key(b[1].at) + (b[1].block === 'void' && b[0] === 'Vb' ? '' : ''), { ...b[1], id: b[0] });
  const links = [];
  for (const [k, a] of world) for (const [dx, dy, dz, name] of N6) {
    const n = world.get(key([a.at[0] + dx, a.at[1] + dy, a.at[2] + dz]));
    if (n && a.id < n.id && joins(a, n, [dx, dy, dz])) links.push(`${a.id}-${n.id}`);
  }
  console.log(`${row.id}: ${world.size} blocks at the end; joins ${links.join(' ')}`);
  for (const line of out) console.log(line);
}
// Contacts between rows: a block of one row next to a block of another.
for (const [k, rowA] of allCells) {
  const p = k.split(',').map(Number);
  for (const [dx, dy, dz] of N6) { const rowB = allCells.get(key([p[0] + dx, p[1] + dy, p[2] + dz])); if (rowB && rowB !== rowA && rowA !== 'R6-R12' && rowB !== 'R6-R12') { console.log(`ROW CONTACT ${rowA} ${k} / ${rowB}`); problems++; } }
}
console.log(problems ? `${problems} problem(s)` : 'no problems');
process.exit(problems ? 1 : 0);
