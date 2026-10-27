// M3a: the inner UV slope variants over the worktree's FluidTpEquilibrium.java (theta already 1e-5):
// V1 compare classifications for the secant; V2 V1 + drop the stored slope when consecutive answers' pieces differ;
// V3 V1 + the stored slope tagged with its piece, used only on that piece (default 1.0 otherwise).
const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
const outDir = process.argv[3];
function rep(s, a, b) { const c = s.split(a).length - 1; if (c !== 1) throw new Error('count ' + c + ' for ' + a.slice(0, 80)); return s.replace(a, b); }
const declOld = '        int previousPhases = -1;\n        double lastY = Double.NaN;\n';
const declNew = '        int previousPhases = -2;\n        Classification previousClassification = null;\n        double lastY = Double.NaN;\n';
const blockOld = `            int phases = result == null ? -1 : result.phases().size();
            if (!(slope > 0.0) || !Double.isFinite(slope)) {
                // A secant only between answers with the same phases: across a bubble or dew point V(P) has a kink.
                slope = Double.isNaN(previousY) || previousY == y || phases != previousPhases ? Double.NaN
                        : (residual - previousResidual) / (y - previousY);
                if (slope > 0.0 && Double.isFinite(slope)) w.innerSlope = slope; else slope = w.innerSlope;
            }
            previousPhases = phases;
`;
const head = `            int phases = result == null ? -1 : result.phases().size();
            Classification classification = result == null ? null : result.classification();
            boolean samePiece = phases == previousPhases && classification == previousClassification;
`;
const tail = `            previousPhases = phases;
            previousClassification = classification;
`;
const v1 = head + `            if (!(slope > 0.0) || !Double.isFinite(slope)) {
                slope = Double.isNaN(previousY) || previousY == y || !samePiece ? Double.NaN
                        : (residual - previousResidual) / (y - previousY);
                if (slope > 0.0 && Double.isFinite(slope)) w.innerSlope = slope; else slope = w.innerSlope;
            }
` + tail;
const v2 = head + `            if (previousPhases != -2 && !samePiece) w.innerSlope = 1.0;
            if (!(slope > 0.0) || !Double.isFinite(slope)) {
                slope = Double.isNaN(previousY) || previousY == y || !samePiece ? Double.NaN
                        : (residual - previousResidual) / (y - previousY);
                if (slope > 0.0 && Double.isFinite(slope)) w.innerSlope = slope; else slope = w.innerSlope;
            }
` + tail;
const v3 = head + `            if (!(slope > 0.0) || !Double.isFinite(slope)) {
                slope = Double.isNaN(previousY) || previousY == y || !samePiece ? Double.NaN
                        : (residual - previousResidual) / (y - previousY);
                if (slope > 0.0 && Double.isFinite(slope)) {
                    w.innerSlope = slope;
                    w.innerSlopePhases = phases;
                    w.innerSlopeClassification = classification;
                } else {
                    slope = phases == w.innerSlopePhases && classification == w.innerSlopeClassification ? w.innerSlope : 1.0;
                }
            }
` + tail;
for (const [name, block] of [['V1', v1], ['V2', v2], ['V3', v3]]) {
  let s = rep(src, declOld, declNew);
  s = rep(s, blockOld, block);
  if (name === 'V3') {
    s = rep(s, '        private double innerLooseness, innerSlope;\n', '        private double innerLooseness, innerSlope;\n        private int innerSlopePhases;\n        private Classification innerSlopeClassification;\n');
    s = rep(s, '        w.innerSlope = 1.0;\n', '        w.innerSlope = 1.0;\n        w.innerSlopePhases = -2;\n        w.innerSlopeClassification = null;\n');
  }
  fs.mkdirSync(outDir + '/' + name, {recursive: true});
  fs.writeFileSync(outDir + '/' + name + '/FluidTpEquilibrium.java', s);
}
console.log('variants written');
