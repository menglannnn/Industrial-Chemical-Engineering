import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fits the heat-capacity segments of the ice record {@code crystals/ice_ih.json} (WP4) to IAPWS R10-06's c_p of ice Ih
 * at the triple-point pressure (611.657 Pa), from the numerical box's floor (10 K) to the triple point (273.16 K), and
 * reports the record's fit quality against R10-06: c_p, the enthalpy and entropy from the anchor, and the molar volume
 * of the record's linear form (v0 = 19.65 cm3/mol, expansion 1.6e-4 /K, compressibility 1.2e-10 /Pa, as approved).
 *
 * <p>Form: polynomials in T (the record's {@code polynomial} segments), each fitted by least squares on c_p relative
 * to R10-06's (samples every 0.25 K), the upper segment first and each lower one constrained to meet it at the joint
 * (c_p continuous). The coefficients are printed rounded to 10 significant digits, the digits the record carries, and
 * the quality is evaluated from those rounded digits, exactly as {@code CrystalReference} integrates them.</p>
 *
 * <p>Run: {@code javac -d out R1006.java Fit.java && java -cp out Fit} (JDK 21).</p>
 */
public final class Fit {
    static final double[] JOINTS = {10, 60, 273.16};
    static final int[] DEGREES = {5, 4};

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        if (!R1006.verify()) System.exit(1);
        int segments = JOINTS.length - 1;
        double[][] coefficients = new double[segments][];
        // The upper segment first, free; each lower one meets the segment above it at their joint.
        for (int s = segments - 1; s >= 0; s--) {
            double joint = s == segments - 1 ? Double.NaN : JOINTS[s + 1];
            double anchor = s == segments - 1 ? Double.NaN : value(coefficients[s + 1], joint);
            coefficients[s] = round(fit(JOINTS[s], JOINTS[s + 1], DEGREES[s], joint, anchor));
        }
        System.out.println();
        System.out.println("Segments (Cp = sum c_k T^k, J/(mol K)):");
        for (int s = 0; s < segments; s++) {
            StringBuilder line = new StringBuilder();
            for (int k = 0; k < coefficients[s].length; k++) line.append(k == 0 ? "" : ", ").append(format(coefficients[s][k]));
            System.out.printf("  %.2f .. %.2f K: [%s]%n", JOINTS[s], JOINTS[s + 1], line);
        }
        for (int s = 0; s + 1 < segments; s++) {
            System.out.printf("  joint %.2f K: c_p %.6f below, %.6f above%n", JOINTS[s + 1], value(coefficients[s], JOINTS[s + 1]),
                    value(coefficients[s + 1], JOINTS[s + 1]));
        }
        // Quality: c_p, h - h(Tt) and s - s(Tt) of the record against R10-06 at the triple-point pressure.
        double hT = R1006.at(R1006.TT, R1006.PT).enthalpy(R1006.TT) * R1006.M, sT = R1006.at(R1006.TT, R1006.PT).entropy() * R1006.M;
        double worstCp = 0, worstCpAt = 0, worstH = 0, worstHAt = 0, worstS = 0, worstSAt = 0, worstHAbs = 0;
        System.out.println();
        System.out.println("   T K   cp record   cp R10-06    rel     dh record    dh R10-06    rel     ds record   ds R10-06   v record    v R10-06   rel");
        List<Double> report = new ArrayList<>(List.of(10.0, 20.0, 40.0, 60.0, 80.0, 100.0, 111.6, 120.0, 150.0, 200.0, 250.0, 263.15, 270.0));
        for (double t = 10; t <= R1006.TT + 1e-9; t += 0.05) {
            double cp = cp(coefficients, t), ref = R1006.at(t, R1006.PT).heatCapacity(t) * R1006.M;
            double dh = enthalpy(coefficients, t) - enthalpy(coefficients, R1006.TT), dhRef = R1006.at(t, R1006.PT).enthalpy(t) * R1006.M - hT;
            double ds = entropy(coefficients, t) - entropy(coefficients, R1006.TT), dsRef = R1006.at(t, R1006.PT).entropy() * R1006.M - sT;
            double rc = Math.abs(cp - ref) / ref;
            if (rc > worstCp) { worstCp = rc; worstCpAt = t; }
            if (t < R1006.TT - 1) {
                double rh = Math.abs(dh - dhRef) / Math.abs(dhRef);
                if (rh > worstH) { worstH = rh; worstHAt = t; }
                double rs = Math.abs(ds - dsRef) / Math.abs(dsRef);
                if (rs > worstS) { worstS = rs; worstSAt = t; }
            }
            worstHAbs = Math.max(worstHAbs, Math.abs(dh - dhRef));
        }
        for (double t : report) {
            double cp = cp(coefficients, t), ref = R1006.at(t, R1006.PT).heatCapacity(t) * R1006.M;
            double dh = enthalpy(coefficients, t) - enthalpy(coefficients, R1006.TT), dhRef = R1006.at(t, R1006.PT).enthalpy(t) * R1006.M - hT;
            double ds = entropy(coefficients, t) - entropy(coefficients, R1006.TT), dsRef = R1006.at(t, R1006.PT).entropy() * R1006.M - sT;
            double v = 1.965e-5 * (1 + 1.6e-4 * (t - R1006.TT)), vRef = R1006.M / R1006.at(t, R1006.PT).density();
            System.out.printf("%7.2f %10.5f %10.5f %8.1e %12.3f %12.3f %8.1e %11.4f %11.4f %11.5e %11.5e %+6.2f%%%n", t, cp, ref, Math.abs(cp - ref) / ref,
                    dh, dhRef, Math.abs(dh - dhRef) / Math.abs(dhRef), ds, dsRef, v, vRef, 100 * (v - vRef) / vRef);
        }
        System.out.printf("%nWorst over 10 .. 273.16 K (0.05 K steps): c_p %.2e at %.2f K; h - h(Tt) %.2e at %.2f K (absolute worst %.3f J/mol);"
                + " s - s(Tt) %.2e at %.2f K%n", worstCp, worstCpAt, worstH, worstHAt, worstHAbs, worstS, worstSAt);
        var anchor = R1006.at(R1006.TT, R1006.PT);
        System.out.printf("R10-06 at the anchor: v %.6e m3/mol (rho %.4f), expansion %.4e /K, compressibility %.4e /Pa, c_p %.5f J/(mol K)%n",
                R1006.M / anchor.density(), anchor.density(), anchor.expansion(), anchor.compressibility(), anchor.heatCapacity(R1006.TT) * R1006.M);
        double dvFusion = 18.0191e-6 - 1.965e-5;
        System.out.printf("Clapeyron with v_ice 19.65 cm3/mol: computed in the mod from IF97's liquid at the anchor (about %.4e K/Pa)%n",
                R1006.TT * dvFusion / 6007);
    }

    static double[] fit(double low, double high, int degree, double joint, double anchor) {
        boolean constrained = !Double.isNaN(joint);
        int unknowns = constrained ? degree : degree + 1;
        double[][] normal = new double[unknowns][unknowns + 1];
        for (double t = low; t <= high + 1e-9; t += 0.25) {
            double ref = R1006.at(t, R1006.PT).heatCapacity(t) * R1006.M, weight = 1 / (ref * ref);
            double[] row = new double[unknowns];
            double rhs = ref;
            for (int j = 0; j < unknowns; j++) {
                int k = constrained ? j + 1 : j;
                row[j] = constrained ? Math.pow(t, k) - Math.pow(joint, k) : Math.pow(t, k);
            }
            if (constrained) rhs -= anchor;
            for (int i = 0; i < unknowns; i++) {
                for (int j = 0; j < unknowns; j++) normal[i][j] += weight * row[i] * row[j];
                normal[i][unknowns] += weight * row[i] * rhs;
            }
        }
        double[] x = solve(normal);
        double[] c = new double[degree + 1];
        if (!constrained) return x;
        double c0 = anchor;
        for (int j = 0; j < unknowns; j++) { c[j + 1] = x[j]; c0 -= x[j] * Math.pow(joint, j + 1); }
        c[0] = c0;
        return c;
    }

    static double[] solve(double[][] a) {
        int n = a.length;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(a[r][col]) > Math.abs(a[pivot][col])) pivot = r;
            double[] swap = a[col]; a[col] = a[pivot]; a[pivot] = swap;
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double f = a[r][col] / a[col][col];
                for (int c = col; c <= n; c++) a[r][c] -= f * a[col][c];
            }
        }
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = a[i][n] / a[i][i];
        return x;
    }

    static double[] round(double[] c) {
        double[] out = new double[c.length];
        for (int k = 0; k < c.length; k++) out[k] = Double.parseDouble(format(c[k]));
        return out;
    }

    static String format(double v) { return String.format(Locale.ROOT, "%.9e", v); }

    static double value(double[] c, double t) {
        double sum = 0;
        for (int k = c.length - 1; k >= 0; k--) sum = sum * t + c[k];
        return sum;
    }

    static int segment(double t) {
        for (int s = 0; s < JOINTS.length - 2; s++) if (t <= JOINTS[s + 1]) return s;
        return JOINTS.length - 2;
    }

    static double cp(double[][] c, double t) { return value(c[segment(t)], t); }

    /** {@code integral_10^T c_p dT}, as CrystalReference.Polynomial integrates it (powers from each segment's minimum). */
    static double enthalpy(double[][] c, double t) {
        double h = 0;
        int s = segment(t);
        for (int k = 0; k < s; k++) h += integral(c[k], JOINTS[k], JOINTS[k + 1]);
        return h + integral(c[s], JOINTS[s], t);
    }

    static double integral(double[] c, double from, double to) {
        double sum = 0;
        for (int k = 0; k < c.length; k++) sum += c[k] * (StrictMath.pow(to, k + 1) - StrictMath.pow(from, k + 1)) / (k + 1);
        return sum;
    }

    static double entropy(double[][] c, double t) {
        double s = 0;
        int seg = segment(t);
        for (int k = 0; k < seg; k++) s += entropyIntegral(c[k], JOINTS[k], JOINTS[k + 1]);
        return s + entropyIntegral(c[seg], JOINTS[seg], t);
    }

    static double entropyIntegral(double[] c, double from, double to) {
        double sum = c[0] * StrictMath.log(to / from);
        for (int k = 1; k < c.length; k++) sum += c[k] * (StrictMath.pow(to, k) - StrictMath.pow(from, k)) / k;
        return sum;
    }
}
