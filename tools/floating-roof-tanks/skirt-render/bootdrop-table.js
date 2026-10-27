// Boot depth, minimum saddle height and drain neck per lying-drum diameter: the STAND_OUT-only face rule (7d53cef5,
// "Old") against bootDrop with the boot giving way (bae8620a, "New"). "short": the old drain neck under NECK.
const STAND_OUT = .15, MAX = .8, NECK = Math.min(.15, .04 + .25 * MAX) + (MAX > .6 ? 1 / 16 : 1 / 32) + .03;
const lyingAxis = d => { const c = Math.ceil(d); return (c % 2 === 0 ? c + 1 : c) / 2; };
const dropOld = s => s / 4 + STAND_OUT;
const dropNew = s => { const depth = s / 4, e = MAX / s; return Math.max(depth + STAND_OUT, depth * Math.sqrt(Math.max(0, 1 - e * e)) + NECK); };
console.log("D     boot  depthOld depthNew  supOld supNew  neckOld neckNew");
for (let d = .5; d <= 10; d += .5) {
    const low = d < .9 ? .3 : d < 1.4 ? .4 : .5, s = Math.min(d / 2, Math.max(low, d / 3)), shell = lyingAxis(d) - d / 2;
    const row = [];
    for (const drop of [dropOld, dropNew]) {
        const reach = shell - s / 4 - STAND_OUT, len = Math.max(.4, reach - Math.floor(reach - Math.max(.9, .6 * d)) - 1 - 1e-6 - (drop(s) - s / 4 - STAND_OUT));
        const bottom = shell - len, face = Math.floor(bottom - drop(s));
        const depth = s / 4, e = Math.min(1, MAX / s), leaves = bottom - depth * Math.sqrt(1 - e * e);
        row.push({len, sup: Math.max(1, 1 - face), neck: leaves - face});
    }
    console.log([d.toFixed(1).padEnd(5), s.toFixed(2).padEnd(5), row[0].len.toFixed(3).padStart(8), row[1].len.toFixed(3).padStart(8),
        String(row[0].sup).padStart(7), String(row[1].sup).padStart(6), row[0].neck.toFixed(3).padStart(8), row[1].neck.toFixed(3).padStart(7),
        row[0].sup !== row[1].sup ? " SUPPORT" : "", row[0].neck < NECK - 1e-9 ? " short" : ""].join(" "));
}
console.log("NECK", NECK.toFixed(4));
