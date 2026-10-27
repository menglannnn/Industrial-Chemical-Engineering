/**
 * IAPWS R10-06(2009), the Gibbs function of ice Ih (Feistel and Wagner 2006), and IAPWS R14-08(2011)'s sublimation
 * pressure of ice Ih, for the ice record of WP4 (batch 2026-09-24-coolprop-low-temperature). Offline only: it is not
 * part of the mod. Coefficients as the WP4 brief lists them (from memory); {@link #main} first checks them against the
 * release's check values and stops when they do not reproduce.
 *
 * <p>g(T, p) = g0(p) - s0 Tt tau + Tt Re sum_k r_k [(t_k - tau) ln(t_k - tau) + (t_k + tau) ln(t_k + tau)
 * - 2 t_k ln t_k - tau^2/t_k], tau = T/Tt, g0(p) = sum g0k (pi - pi0)^k, r2(p) = sum r2k (pi - pi0)^k, pi = p/pt.</p>
 */
public final class R1006 {
    public static final double TT = 273.16, PT = 611.657, P0 = 101325.0;
    /** kg/mol, the IAPWS molar mass the brief names. */
    public static final double M = 0.018015268;
    static final double[] G0 = {-0.632020233335886e6, 0.655022213658955, -0.189369929326131e-7, 0.339746123271053e-14,
            -0.556464869058991e-21};
    static final double S0 = -0.332733756492168e4;
    static final C T1 = new C(0.368017112855051e-1, 0.510878114959572e-1);
    static final C R1 = new C(0.447050716285388e2, 0.656876847463481e2);
    static final C T2 = new C(0.337315741065416, 0.335449415919309);
    static final C[] R2 = {new C(-0.725974574329220e2, -0.781008427112870e2), new C(-0.557107698030123e-4, 0.464578634580806e-4),
            new C(0.234801409215913e-10, -0.285651142904972e-10)};

    record C(double re, double im) {
        C plus(C o) { return new C(re + o.re, im + o.im); }
        C minus(C o) { return new C(re - o.re, im - o.im); }
        C times(C o) { return new C(re * o.re - im * o.im, re * o.im + im * o.re); }
        C times(double s) { return new C(re * s, im * s); }
        C div(C o) { double d = o.re * o.re + o.im * o.im; return new C((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d); }
        C log() { return new C(StrictMath.log(StrictMath.hypot(re, im)), StrictMath.atan2(im, re)); }
        static C real(double x) { return new C(x, 0); }
    }

    /** The release's properties at (T, p): g, g_T, g_p, g_TT, g_Tp, g_pp (SI per kg). */
    public record Derivatives(double g, double gT, double gp, double gTT, double gTp, double gpp) {
        public double density() { return 1 / gp; }
        public double enthalpy(double t) { return g - t * gT; }
        public double entropy() { return -gT; }
        public double heatCapacity(double t) { return -t * gTT; }
        public double expansion() { return gTp / gp; }
        public double compressibility() { return -gpp / gp; }
    }

    public static Derivatives at(double t, double p) {
        double tau = t / TT, dpi = (p - P0) / PT;
        double g0 = 0, g0p = 0, g0pp = 0;
        for (int k = 4; k >= 0; k--) g0 = g0 * dpi + G0[k];
        for (int k = 4; k >= 1; k--) g0p = g0p * dpi + k * G0[k];
        for (int k = 4; k >= 2; k--) g0pp = g0pp * dpi + k * (k - 1) * G0[k];
        g0p /= PT;
        g0pp /= PT * PT;
        C r2 = R2[0].plus(R2[1].times(dpi)).plus(R2[2].times(dpi * dpi));
        C r2p = R2[1].plus(R2[2].times(2 * dpi)).times(1 / PT);
        C r2pp = R2[2].times(2 / (PT * PT));
        C ta = C.real(tau);
        C bracket1 = bracket(T1, ta), bracket2 = bracket(T2, ta);
        double g = g0 - S0 * TT * tau + TT * (R1.times(bracket1).plus(r2.times(bracket2))).re();
        // d/dtau of the bracket: -ln(t - tau) + ln(t + tau) - 2 tau/t; second: 1/(t - tau) + 1/(t + tau) - 2/t.
        C d1 = slope(T1, ta), d2 = slope(T2, ta);
        C c1 = curvature(T1, ta), c2 = curvature(T2, ta);
        double gT = -S0 + (R1.times(d1).plus(r2.times(d2))).re();
        double gTT = (R1.times(c1).plus(r2.times(c2))).re() / TT;
        double gp = g0p + TT * r2p.times(bracket2).re();
        double gTp = r2p.times(d2).re();
        double gpp = g0pp + TT * r2pp.times(bracket2).re();
        return new Derivatives(g, gT, gp, gTT, gTp, gpp);
    }

    private static C bracket(C t, C tau) {
        C a = t.minus(tau), b = t.plus(tau);
        return a.times(a.log()).plus(b.times(b.log())).minus(t.times(t.log()).times(2)).minus(tau.times(tau).div(t));
    }

    private static C slope(C t, C tau) {
        C a = t.minus(tau), b = t.plus(tau);
        return b.log().minus(a.log()).minus(tau.times(2).div(t));
    }

    private static C curvature(C t, C tau) {
        C one = C.real(1);
        return one.div(t.minus(tau)).plus(one.div(t.plus(tau))).minus(one.times(2).div(t));
    }

    /** R14-08 equation 6: the sublimation pressure of ice Ih, Pa. */
    public static double sublimationPressure(double t) {
        double theta = t / TT;
        double[] a = {-0.212144006e2, 0.273203819e2, -0.610598130e1};
        double[] b = {0.333333333e-2, 0.120666667e1, 0.170333333e1};
        double sum = 0;
        for (int i = 0; i < 3; i++) sum += a[i] * StrictMath.pow(theta, b[i]);
        return PT * StrictMath.exp(sum / theta);
    }

    static boolean check(String what, double value, double expected) {
        double relative = Math.abs(value - expected) / Math.abs(expected);
        boolean ok = relative < 1e-9;
        System.out.printf("%-40s %.12e  release %.12e  rel %.2e %s%n", what, value, expected, relative, ok ? "ok" : "MISMATCH");
        return ok;
    }

    /** Checks the coefficients against the release's check values; false (and printed) when they do not reproduce. */
    public static boolean verify() {
        boolean ok = true;
        var a = at(273.16, 611.657);
        ok &= check("cp 273.16 K, 611.657 Pa (J/kg K)", a.heatCapacity(273.16), 2096.78431622);
        ok &= check("rho 273.16 K, 611.657 Pa (kg/m3)", a.density(), 916.709492200);
        ok &= check("h 273.16 K, 611.657 Pa (J/kg)", a.enthalpy(273.16), -333444.253966);
        var b = at(273.152519, 101325);
        ok &= check("cp 273.152519 K, 101325 Pa", b.heatCapacity(273.152519), 2096.71391024);
        ok &= check("rho 273.152519 K, 101325 Pa", b.density(), 916.721463419);
        var c = at(100, 1e8);
        ok &= check("cp 100 K, 100 MPa", c.heatCapacity(100), 866.333195517);
        ok &= check("rho 100 K, 100 MPa", c.density(), 941.678203297);
        System.out.println(ok ? "R10-06 CHECK VALUES REPRODUCED" : "R10-06 CHECK VALUES NOT REPRODUCED: STOP");
        return ok;
    }

    public static void main(String[] args) {
        boolean ok = verify();
        System.out.printf("R14-08 p_sub: 263.15 K %.6g Pa, 230 K %.9g Pa, 190 K %.6e Pa, 150 K %.6e Pa, 111.6 K %.4e Pa%n",
                sublimationPressure(263.15), sublimationPressure(230), sublimationPressure(190), sublimationPressure(150),
                sublimationPressure(111.6));
        if (!ok) System.exit(1);
    }
}
