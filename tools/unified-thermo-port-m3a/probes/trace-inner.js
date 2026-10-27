// M3a probe: copies FluidTpEquilibrium.java with trace prints in innerUv (the nested UV search's pressure solve), on with
// -Dm3a.trace=1: one IUV line per TP answer (classification, residual, slope used, stored slope), IUV-STALL when a
// pressure search ends after its step budget. Usage: node trace-inner.js <in.java> <out.java>
const fs = require('fs');
let s = fs.readFileSync(process.argv[2], 'utf8');
function rep(a, b) { const c = s.split(a).length - 1; if (c !== 1) throw new Error('count ' + c + ' for ' + a.slice(0, 80)); s = s.replace(a, b); }
const on = 'System.getProperty("m3a.trace") != null';
rep('    private int innerUv(double t, double v, PhaseCompetition competition, Workspace w) {\n',
    '    private int innerUv(double t, double v, PhaseCompetition competition, Workspace w) {\n        if (' + on + ') System.out.println("IUV-START t=" + t + " stored=" + w.innerSlope);\n');
rep('            boolean exact = Math.abs(residual) <= SPECIFICATION_TOLERANCE && Math.abs(step) <= tolerance;\n',
    '            boolean exact = Math.abs(residual) <= SPECIFICATION_TOLERANCE && Math.abs(step) <= tolerance;\n' +
    '            if (' + on + ') System.out.println("IUV k=" + steps + " y=" + y + " cls=" + (result == null ? "COEXISTENCE" : String.valueOf(result.classification())) + " res=" + residual + " slope=" + slope + " stored=" + w.innerSlope + " step=" + step + " bracketed=" + s.bracketed());\n');
rep('                w.innerFailure = "no convergence of the UV pressure search at " + t + " K in " + steps + " TP equilibria";\n',
    '                w.innerFailure = "no convergence of the UV pressure search at " + t + " K in " + steps + " TP equilibria";\n                if (' + on + ') System.out.println("IUV-STALL t=" + t);\n');
fs.writeFileSync(process.argv[3], s);
console.log('instrumented ' + process.argv[3]);
