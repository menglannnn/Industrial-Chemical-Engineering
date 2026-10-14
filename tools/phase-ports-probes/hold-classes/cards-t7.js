// Appends the t7 restatement (merged code, campaign k02) to every card of REMAINING_HOLDS.md, before the card's closing
// separator, from the class lines of card-lines.js (logs/k22-card-lines.txt) and the variant transitions (k11, k15).
// Usage: node cards-t7.js <REMAINING_HOLDS.md> <k22-card-lines.txt>
const fs = require('fs');
const [file, linesFile] = process.argv.slice(2);
const raw = fs.readFileSync(file, 'utf8'), crlf = raw.includes('\r\n');
let text = raw.split('\r\n').join('\n');
const lines = fs.readFileSync(linesFile, 'utf8').split(/\r?\n/);
const info = {};
for (let i = 0; i < lines.length; i++) if (/^[A-Z][0-9]*$/.test(lines[i])) {
  const cls = lines[i], parse = l => { const m = l.trim().match(/^(defaults|random) (\d+): ([^;]*); against j18 (\d+) -> \d+ \(([^)]*)\); against i26 (\d+) -> \d+ \(([^)]*)\)$/); return m && { n: m[2], seeds: m[3], j18: m[4], dj: m[5], i26: m[6], di: m[7] }; };
  info[cls] = { defaults: parse(lines[i + 1]), random: parse(lines[i + 2]) };
}
// Variant moves against the committed nearest form (k11 strongest, k15 mixture), per class: cleared / worse seeds.
const variants = {
  C: 'strongest clears random 15 45 47 286 453 575 615 621 895 994 and defaults 12 96, adds random 19 444 767 and defaults 839; mixture clears random 15 47 286 615 621, adds random 767 and defaults 839',
  E2: 'strongest clears random 168 541 839 1000 and defaults 758 889 908, adds random 372 and defaults 401; mixture clears random 561 839 1000 and defaults 758, adds random 372',
  E3: 'no seed of the class moves with either variant (strongest clears defaults 505)',
  E4: 'strongest clears defaults 718; mixture adds random 815',
  E5: 'strongest clears defaults 157; mixture adds random 992 and defaults 821 853',
  E6: 'strongest clears random 9 48 71 273 274 407 916 and defaults 26 78 424 459 779 780 794 992, adds random 72 214 354 401 475 622 773 794 853 880; mixture clears random 9 30 48 71 164 273 274 407 768 916 and defaults 26 175 529 779 794 915 992, adds random 152 175 214 257 401 475 622 673 794 and defaults 364 401 518 673',
  E8: 'strongest adds random 357 and defaults 763; mixture clears defaults 352 556, adds defaults 665',
  E9: 'strongest clears defaults 9; mixture clears defaults 9 629, adds random 322',
  E10: 'strongest clears random 333; mixture leaves it',
  F: 'strongest clears defaults 72 1000; mixture clears defaults 1000, adds random 123 298 (budget seeds alone: k13, k17)',
  E1: 'none in any form', D: 'none in any form', E7: 'no move', A: 'none in any form',
};
const alone = { F: ' Replayed alone with one worker (`k03`): defaults all 11 hold on the budget again; random 246, 678, 788 hold on the budget, 279 and 829 hold in E8, 431 in C, 757 in E6.' };
const cards = [...text.matchAll(/^## ([A-Z][0-9]*): .*$/gm)];
let out = '', last = 0;
for (const card of cards) {
  const cls = card[1], start = card.index;
  const sep = text.indexOf('\n---\n', start);
  if (sep < 0) continue;
  const x = info[cls];
  const fmt = (d, v) => `${d} ${v.n}${v.n !== '0' ? ' (' + v.seeds + ')' : ''}`;
  const since = (d, v) => `${d}: ${v.j18} on \`j18\` (${v.dj}), ${v.i26} on \`i26\` (${v.di})`;
  const para = x ? `\n**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; \`62e6b790\`, campaign \`k02\`): ${fmt('defaults', x.defaults)}, ${fmt('random', x.random)}.** Review "Integration of R1 and the junction start rule (2026-10-12)".${alone[cls] ?? ''}\n- Against \`j18\` (option (a) and G2) and \`i26\` (R1 and the start rule): ${since('defaults', x.defaults)}; ${since('random', x.random)}.\n- The start-rule variants against nearest (\`k11\` strongest first, \`k15\` mixture, not committed): ${variants[cls]}.\n`
    : `\n**On the merged code (t7, campaign \`k02\`): defaults 0, random 0.**\n`;
  out += text.slice(last, sep) + para;
  last = sep;
}
out += text.slice(last);
fs.writeFileSync(file, crlf ? out.split('\n').join('\r\n') : out);
console.log('cards', cards.length);
