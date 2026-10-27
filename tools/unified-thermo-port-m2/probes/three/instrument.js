// Makes instrumented copies of the engine's TangentPlaneStability and FluidTpEquilibrium for the three-phase probe
// (M2): prints each product stability verdict with its trials when the static PROBE flag is set. The copies shadow the
// Gradle-compiled classes on the probe's class path; the worktree's sources are not touched.
// Usage: node instrument.js <worktree> <out-src-dir>
const fs = require("fs"), path = require("path");
const [wt, out] = process.argv.slice(2);
function patch(rel, edits) {
  let s = fs.readFileSync(path.join(wt, rel), "utf8").replace(/\r\n/g, "\n");
  for (const [from, to] of edits) {
    if (s.split(from).length !== 2) throw new Error(rel + ": expected one occurrence of " + from.slice(0, 80));
    s = s.replace(from, () => to);
  }
  const dest = path.join(out, rel.replace(/^src\/main\/java\//, ""));
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, s);
}
const tps = "src/main/java/com/wormzjl/createcheme/science/thermo/TangentPlaneStability.java";
patch(tps, [
  ["public final class TangentPlaneStability {\n",
   "public final class TangentPlaneStability {\n    public static volatile boolean PROBE = false;\n    public static String lastTrace = \"\";\n"],
  ["        for (int trial = 0; trial < trialCount; trial++) {\n            Outcome outcome = runTrial(w, trial);\n            trials++;\n            Point end = w.current;\n",
   "        StringBuilder probeTrace = new StringBuilder();\n        for (int trial = 0; trial < trialCount; trial++) {\n            Outcome outcome = runTrial(w, trial);\n            trials++;\n            Point end = w.current;\n            if (PROBE) probeTrace.append(\" trial\").append(trial).append('=').append(outcome).append(\" tm=\").append(end.distance).append(\" root=\").append(end.root).append(\" trivial=\").append(end.trivial).append(';');\n"],
  ["        if (proven || w.localStability == LOCALLY_UNSTABLE) verdict = Verdict.UNSTABLE;\n",
   "        if (PROBE) lastTrace = \"proven=\" + proven + \" local=\" + w.localStability + \" feedRoot=\" + w.feedRoot + \" present=\" + w.presentCount + probeTrace;\n        if (proven || w.localStability == LOCALLY_UNSTABLE) verdict = Verdict.UNSTABLE;\n"],
]);
const fte = "src/main/java/com/wormzjl/createcheme/science/thermo/phase/FluidTpEquilibrium.java";
patch(fte, [
  ["        TangentPlaneStability.Result liquidCheck = w.branched\n                ? stability.test(t, w.liquidPressure, w.vaporPressure, translations, w.liquidAmounts, w.stability)\n                : stability.test(t, w.liquidPressure, w.liquidAmounts, w.stability);\n",
   "        TangentPlaneStability.Result liquidCheck = w.branched\n                ? stability.test(t, w.liquidPressure, w.vaporPressure, translations, w.liquidAmounts, w.stability)\n                : stability.test(t, w.liquidPressure, w.liquidAmounts, w.stability);\n        String probeLiquid = TangentPlaneStability.lastTrace;\n"],
  ["        w.productDistance = Math.min(liquidCheck.minimumTangentPlaneDistance(), vapourCheck.minimumTangentPlaneDistance());\n",
   "        w.productDistance = Math.min(liquidCheck.minimumTangentPlaneDistance(), vapourCheck.minimumTangentPlaneDistance());\n        if (TangentPlaneStability.PROBE) {\n            System.out.println(\"  SPLIT t=\" + t + \" pL=\" + w.liquidPressure + \" pV=\" + w.vaporPressure + \" tie=\" + w.tieLine + \" branched=\" + w.branched\n                    + \" vL=\" + liquid.molarVolume() + \" vV=\" + vapour.molarVolume());\n            System.out.println(\"   liquid=\" + Arrays.toString(w.liquidAmounts));\n            System.out.println(\"   vapour=\" + Arrays.toString(w.vaporAmounts));\n            System.out.println(\"   liquidCheck \" + liquidCheck.verdict() + \" tm=\" + liquidCheck.minimumTangentPlaneDistance() + \" trialRoot=\" + liquidCheck.trialRoot() + \" \" + probeLiquid);\n            System.out.println(\"    trialL=\" + Arrays.toString(liquidCheck.trialComposition()));\n            System.out.println(\"   vapourCheck \" + vapourCheck.verdict() + \" tm=\" + vapourCheck.minimumTangentPlaneDistance() + \" trialRoot=\" + vapourCheck.trialRoot() + \" \" + TangentPlaneStability.lastTrace);\n            System.out.println(\"    trialV=\" + Arrays.toString(vapourCheck.trialComposition()));\n        }\n"],
]);
console.log("instrumented copies in " + out);

// RRFIX=1: option A of the M2 report, Rachford-Rice in the minor phase's fraction. When the root lies above one half
// the vapour dominates and the liquid fraction delta = 1 - beta is the variable (1 + beta (K - 1) = K - delta (K - 1)),
// so a trace liquid's fraction and shares keep full relative precision (a split is admissible while delta > 0, even where
// 1 - delta rounds to 1); below one half the code is unchanged.
if (process.env.RRFIX === "1") {
  const rel = fte;
  const dest = path.join(out, rel.replace(/^src\/main\/java\//, ""));
  let s = fs.readFileSync(dest, "utf8");
  const edits = [
    ["    public static final class Workspace implements EquilibriumService.Workspace {\n",
     "    public static final class Workspace implements EquilibriumService.Workspace {\n        double probeDelta; boolean probeVapourMajor;\n"],
    ["        if (!(smallest < 1.0 && largest > 1.0)) return Double.NaN;\n        double lower = 1.0 / (1.0 - largest);\n        double upper = 1.0 / (1.0 - smallest);\n        double beta = 0.5;\n",
     "        if (!(smallest < 1.0 && largest > 1.0)) return Double.NaN;\n        double lower = 1.0 / (1.0 - largest);\n        double upper = 1.0 / (1.0 - smallest);\n" +
     "        double half = 0.0;\n        for (int a = 0; a < w.presentCount; a++) { int i = w.present[a]; half += w.z[i] * (w.k[i] - 1.0) / (1.0 + 0.5 * (w.k[i] - 1.0)); }\n" +
     "        w.probeVapourMajor = half > 0.0;\n" +
     "        if (w.probeVapourMajor) {\n" +
     "            double lo = 1.0 - upper, hi = 0.5, delta = 0.25;\n" +
     "            for (int step = 0; step < MAXIMUM_RACHFORD_RICE_STEPS; step++) {\n" +
     "                double f = 0.0, slope = 0.0;\n" +
     "                for (int a = 0; a < w.presentCount; a++) { int i = w.present[a]; double d = w.k[i] - 1.0; double den = w.k[i] - delta * d; f += w.z[i] * d / den; slope += w.z[i] * d * d / (den * den); }\n" +
     "                w.rachfordRiceSteps++;\n" +
     "                if (f == 0.0) break;\n" +
     "                if (f < 0.0) lo = delta; else hi = delta;\n" +
     "                double next = delta - f / slope;\n" +
     "                if (!(next > lo && next < hi)) next = lo > 0.0 ? Math.sqrt(lo * hi) : 0.5 * (lo + hi);\n" +
     "                if (next == delta || Math.abs(next - delta) <= 1.0e-15 * Math.abs(delta)) { delta = next; break; }\n" +
     "                delta = next;\n" +
     "            }\n" +
     "            double sx = 0.0, sy = 0.0; Arrays.fill(w.x, 0.0); Arrays.fill(w.y, 0.0);\n" +
     "            for (int a = 0; a < w.presentCount; a++) { int i = w.present[a]; double xi = w.z[i] / (w.k[i] - delta * (w.k[i] - 1.0)); w.x[i] = xi; w.y[i] = w.k[i] * xi; sx += w.x[i]; sy += w.y[i]; }\n" +
     "            for (int a = 0; a < w.presentCount; a++) { int i = w.present[a]; w.x[i] /= sx; w.y[i] /= sy; }\n" +
     "            w.probeDelta = delta;\n" +
     "            return 1.0 - delta;\n" +
     "        }\n" +
     "        double beta = 0.5;\n"],
    ["                    double denominator = 1.0 + beta * (w.k[i] - 1.0);\n                    double vapourShare = beta * w.k[i] / denominator;\n                    double liquidShare = (1.0 - beta) / denominator;\n                    if (vapourShare <= liquidShare) {\n                        w.vaporAmounts[i] = Math.min(w.feed[i], w.feed[i] * vapourShare);",
     "                    double denominator = w.probeVapourMajor ? w.k[i] - w.probeDelta * (w.k[i] - 1.0) : 1.0 + beta * (w.k[i] - 1.0);\n                    double vapourShare = (w.probeVapourMajor ? 1.0 - w.probeDelta : beta) * w.k[i] / denominator;\n                    double liquidShare = (w.probeVapourMajor ? w.probeDelta : 1.0 - beta) / denominator;\n                    if (vapourShare <= liquidShare) {\n                        w.vaporAmounts[i] = Math.min(w.feed[i], w.feed[i] * vapourShare);"],
    ["                    double denominator = 1.0 + beta * (w.k[i] - 1.0);\n                    w.vaporMoles[i] = w.z[i] * (beta * w.k[i] / denominator);\n                    w.liquidMoles[i] = w.z[i] * ((1.0 - beta) / denominator);\n",
     "                    double denominator = w.probeVapourMajor ? w.k[i] - w.probeDelta * (w.k[i] - 1.0) : 1.0 + beta * (w.k[i] - 1.0);\n                    w.vaporMoles[i] = w.z[i] * ((w.probeVapourMajor ? 1.0 - w.probeDelta : beta) * w.k[i] / denominator);\n                    w.liquidMoles[i] = w.z[i] * ((w.probeVapourMajor ? w.probeDelta : 1.0 - beta) / denominator);\n"],
    ["            double beta = split(w);\n            if (!(beta > 0.0 && beta < 1.0)) {\n",
     "            double beta = split(w);\n            if (!(beta > 0.0 && (w.probeVapourMajor ? w.probeDelta > 0.0 : beta < 1.0))) {\n"],
    ["            double beta = split(w);\n            if (beta > 0.0 && beta < 1.0) {\n",
     "            double beta = split(w);\n            if (beta > 0.0 && (w.probeVapourMajor ? w.probeDelta > 0.0 : beta < 1.0)) {\n"],
  ];
  for (const [from, to] of edits) {
    if (s.split(from).length !== 2) throw new Error("RRFIX: expected one occurrence of " + from.slice(0, 90));
    s = s.replace(from, () => to);
  }
  fs.writeFileSync(dest, s);
  console.log("RRFIX applied");
}
