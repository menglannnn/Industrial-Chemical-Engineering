// M1 probe: copies FluidTpEquilibrium.java with trace prints in the nested UV (outer failures, inner steps).
const fs = require('fs');
let s = fs.readFileSync(process.argv[2], 'utf8');
function rep(a, b) { const c = s.split(a).length - 1; if (c !== 1) throw new Error('count ' + c + ' for ' + a.slice(0, 80)); s = s.replace(a, b); }
const on = 'System.getProperty("m1.trace") != null';
rep('            if (outcome == INNER_FAILED) {',
    '            if (outcome == INNER_FAILED) { if (' + on + ') System.err.println("INNER_FAILED t=" + t + " lastT=" + lastT + " retreats=" + retreats + " held=" + (w.innerResult != null) + " " + String.valueOf(w.innerFailure).substring(0, Math.min(90, String.valueOf(w.innerFailure).length())));');
rep('            EquilibriumResult result = w.innerResult;',
    '            EquilibriumResult result = w.innerResult; if (' + on + ') System.err.println("INNER " + outcome + " t=" + t + " cls=" + (outcome == INNER_FOUND ? String.valueOf(w.innerResult.classification()) : "-"));');
rep(`            EquilibriumResult result = outerTp(t, p, competition, w);
            if (!result.converged()) {
                if (Double.isNaN(lastY) || ++retreats > MAXIMUM_RETREATS) {`,
    `            EquilibriumResult result = outerTp(t, p, competition, w);
            if (` + on + `) { double cr = 0; for (PhaseAmounts ph : result.phases()) if (ph.kind() == PhaseKind.SOLID) for (double a : ph.amounts()) cr += a; System.err.println("  step y=" + y + " P=" + p + " " + result.status() + " " + result.classification() + " vol=" + (result.converged() && result.classification() != Classification.PURE_COEXISTENCE_UNDERDETERMINED ? String.valueOf(result.volume()) : "-") + " crystal=" + cr + " lower=" + s.lower + " upper=" + s.upper + " kernel=" + w.outerKernel + (result.converged() ? "" : " | " + result.detail().substring(0, Math.min(120, result.detail().length())))); }
            if (!result.converged()) {
                if (Double.isNaN(lastY) || ++retreats > MAXIMUM_RETREATS) {`);
fs.writeFileSync(process.argv[3], s);
console.log('ok');
