// M3a: summarises the innerUv trace (trace-inner.js) per probe case: inner searches, their TP answers, the longest one,
// stalls (a pressure search ending on its step budget), and the steps that took the stored slope right after the
// classification changed. Usage: node inner-stats.js <probe output with -Dm3a.trace=1>
const fs = require('fs');
const lines = fs.readFileSync(process.argv[2], 'utf8').replace(/\r/g, '').split('\n');
let calls = 0, steps = 0, longest = 0, cur = 0, stalls = 0, storedAfterChange = 0, prevCls = null, curT = null, longestT = null;
const close = () => { if (cur > longest) { longest = cur; longestT = curT; } cur = 0; };
for (const l of lines) {
  if (l.startsWith('IUV-START')) { close(); calls++; prevCls = null; curT = l.split(' ')[1]; continue; }
  if (l.startsWith('IUV-STALL')) { stalls++; continue; }
  if (l.startsWith('IUV k=')) {
    steps++; cur++;
    const cls = (l.match(/ cls=(\S+)/) || [])[1];
    const slope = (l.match(/ slope=(\S+)/) || [])[1], stored = (l.match(/ stored=(\S+)/) || [])[1];
    if (prevCls !== null && cls !== prevCls && slope === stored) storedAfterChange++;
    prevCls = cls;
    continue;
  }
  if (l.trim() === '' ) continue;
  close();
  console.log(`${l.slice(0, 120)}\n    inner searches ${calls}, inner TP answers ${steps}, longest ${longest} (${longestT}), stalls ${stalls}, stored slope taken at a class change ${storedAfterChange}`);
  calls = steps = longest = stalls = storedAfterChange = 0; longestT = null;
}
