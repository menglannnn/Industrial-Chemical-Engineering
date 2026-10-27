package com.wormzjl.createcheme.science.thermo.phase;

import com.wormzjl.createcheme.science.thermo.reference.HelmholtzFluid;

public final class MeltProbe {
    public static void main(String[] args) throws Exception {
        var m = CrystalPhaseEvaluatorTest.class.getDeclaredMethod("jaegerSpanRecord");
        m.setAccessible(true);
        var record = (com.wormzjl.createcheme.science.material.CrystalReference) m.invoke(null);
        var co2 = HelmholtzFluid.load("CarbonDioxide");
        double[] c = SolidCarbonDioxideReferences.oracleConstants(record, co2);
        var js = record.gibbsFunction();
        System.out.println("constants " + c[0] + " " + c[1]);
        for (double t : new double[] {216.6, 216.7, 217, 217.5, 218, 219, 220, 222, 225, 230, 235, 240, 245, 250, 255, 260, 265, 270}) {
            double start = SolidCarbonDioxideReferences.spanWagnerMeltingPressure(t);
            double lnP = Math.log(start);
            StringBuilder sb = new StringBuilder("T " + t + " start " + start + ":");
            boolean done = false;
            for (int it = 0; it < 100 && !done; it++) {
                double p = Math.exp(lnP);
                double gs = js.evaluate(t, p, c[0], c[1]).gibbs();
                double gf = co2.stateAtPressure(t, p, HelmholtzFluid.Root.LIQUID).gibbsEnergy();
                double f = gs - gf;
                if (Math.abs(f) < 1e-9) { sb.append(" converged it " + it + " f " + f); done = true; break; }
                double d = p * (js.evaluate(t, p, c[0], c[1]).volume() - 1 / co2.density(t, p, HelmholtzFluid.Root.LIQUID));
                double step = Math.max(-1.0, Math.min(1.0, -f / d));
                lnP += step;
                if (it < 6 || it > 95) sb.append(String.format(" [%d f %.3e step %.3e]", it, f, step));
                if (Math.abs(step) < 1e-15) { sb.append(" step-stop it " + it); done = true; }
            }
            if (!done) sb.append(" NOT CONVERGED");
            System.out.println(sb);
        }
    }
}
