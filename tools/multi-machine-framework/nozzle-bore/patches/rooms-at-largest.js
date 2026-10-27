// Patches the copy of ColumnStructure: every side nozzle's room at a pipe's largest flange (owner rule 2026-10-21).
const fs = require('fs');
const p = 'tmp/f3-reflux/patch/com/wormzjl/icheme/science/equipment/ColumnStructure.java';
let s = fs.readFileSync(p, 'utf8');
const edits = [
  ['neck(PipeSizes.flange(nozzle.defaultOd()) / 2)', 'neck(PipeSizes.flange(PipeSizes.MAX_OUTSIDE) / 2)'],
  ['double half = PipeSizes.flange(od) / 2, neck = Rooms.neck(half),', 'double half = PipeSizes.flange(od) / 2, neck = Rooms.neck(PipeSizes.flange(PipeSizes.MAX_OUTSIDE) / 2),'],
];
for (const [a, b] of edits) { if (s.split(a).length !== 2) { console.log('mismatch: ' + a); process.exit(1); } s = s.replace(a, b); }
fs.writeFileSync(p, s); console.log('patched');
