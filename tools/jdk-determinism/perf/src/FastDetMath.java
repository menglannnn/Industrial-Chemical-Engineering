package com.wormzjl.createcheme.science.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Candidates C and D of the pow performance study (batch 2026-09-27-jdk-determinism): table-driven log, exp and pow in
 * plain IEEE double arithmetic (+ - * /, comparisons, Math.rint, raw bit conversions, table lookups; no fma, no
 * StrictMath/Math transcendental), so every conforming JVM computes the same bits (JLS 15.4, strict since JDK 17).
 * Not bitwise equal to StrictMath: about 0.5 ulp from the true value (measured against StrictMath by FastDetCheck).
 *
 * <p>Tang-style reductions with 128-entry tables. log: x = 2^k m, m in [1,2), F = 1 + j/128 nearest to m, u = (m-F)/F,
 * ln x = k ln2 + ln F + ln(1+u) with a degree-8 series, carried as a double-double for pow. exp: n = rint(x 128/ln2),
 * r = x - n ln2/128, exp x = 2^(n>>7) 2^((n&127)/128) (1 + expm1(r)) with a degree-6 series. pow(x,y) = exp(y ln x)
 * with ln x as a double-double and an exact Dekker product for y ln x. Every input outside the fast domain (x <= 0,
 * subnormal, NaN, infinite, y = 0, |y ln x| > 708, |y| >= 2^30) goes to the fdlibm routines, so special cases have
 * StrictMath's semantics exactly.
 *
 * <p>Prototype: the tables are computed at class initialisation from exact BigDecimal series (deterministic; the
 * production form would embed them as hex literals printed by {@code FastDetCheck tables}).
 */
public final class FastDetMath {
    private FastDetMath() {}

    static final double[] EXP_HI = new double[128], EXP_LO = new double[128];
    static final double[] LOG_INV = new double[128], LOG_HI = new double[128], LOG_LO = new double[128];
    static final double LN2_HI, LN2_LO, L_HI, L_LO, INV_L;

    static {
        MathContext mc = new MathContext(60, RoundingMode.HALF_EVEN);
        BigDecimal ln2 = atanh2(BigDecimal.ONE.divide(BigDecimal.valueOf(3), mc), mc);
        LN2_HI = Double.longBitsToDouble(Double.doubleToRawLongBits(ln2.doubleValue()) & ~0xfffL);
        LN2_LO = ln2.subtract(new BigDecimal(LN2_HI), mc).doubleValue();
        BigDecimal l = ln2.divide(BigDecimal.valueOf(128), mc);
        L_HI = Double.longBitsToDouble(Double.doubleToRawLongBits(l.doubleValue()) & ~0xfffffL);
        L_LO = l.subtract(new BigDecimal(L_HI), mc).doubleValue();
        INV_L = BigDecimal.valueOf(128).divide(ln2, mc).doubleValue();
        for (int j = 0; j < 128; j++) {
            BigDecimal t = exp(ln2.multiply(BigDecimal.valueOf(j)).divide(BigDecimal.valueOf(128), mc), mc);
            EXP_HI[j] = t.doubleValue();
            EXP_LO[j] = t.subtract(new BigDecimal(EXP_HI[j]), mc).doubleValue();
            BigDecimal f = BigDecimal.ONE.add(BigDecimal.valueOf(j).divide(BigDecimal.valueOf(128), mc));
            LOG_INV[j] = BigDecimal.ONE.divide(f, mc).doubleValue();
            // ln F = 2 atanh((F-1)/(F+1))
            BigDecimal lf = j == 0 ? BigDecimal.ZERO : atanh2(f.subtract(BigDecimal.ONE).divide(f.add(BigDecimal.ONE), mc), mc);
            LOG_HI[j] = lf.doubleValue();
            LOG_LO[j] = lf.subtract(new BigDecimal(LOG_HI[j]), mc).doubleValue();
        }
    }

    /** 2 atanh(z) = ln((1+z)/(1-z)) by its series, |z| <= 1/3. */
    private static BigDecimal atanh2(BigDecimal z, MathContext mc) {
        BigDecimal z2 = z.multiply(z, mc), term = z, sum = BigDecimal.ZERO;
        BigDecimal eps = BigDecimal.ONE.movePointLeft(58);
        for (int k = 0; k < 400; k++) {
            BigDecimal add = term.divide(BigDecimal.valueOf(2L * k + 1), mc);
            sum = sum.add(add, mc);
            if (add.abs().compareTo(eps) < 0) break;
            term = term.multiply(z2, mc);
        }
        return sum.multiply(BigDecimal.valueOf(2), mc);
    }

    private static BigDecimal exp(BigDecimal x, MathContext mc) {
        BigDecimal sum = BigDecimal.ONE, term = BigDecimal.ONE, eps = BigDecimal.ONE.movePointLeft(58);
        for (int k = 1; k < 400; k++) {
            term = term.multiply(x, mc).divide(BigDecimal.valueOf(k), mc);
            sum = sum.add(term, mc);
            if (term.abs().compareTo(eps) < 0) break;
        }
        return sum;
    }

    // ---- log --------------------------------------------------------------------------------------------------------

    private static final double C3 = 1.0 / 3, C4 = -0.25, C5 = 0.2, C6 = -1.0 / 6, C7 = 1.0 / 7, C8 = -0.125;

    /** Natural logarithm, about 0.5 ulp; StrictMath.log semantics outside the positive normal range. */
    public static double log(double x) {
        long bits = Double.doubleToRawLongBits(x);
        if (bits < 0x0010000000000000L || bits >= 0x7ff0000000000000L) return StrictMath.log(x); // <= +0, subnormal, negative, NaN, inf
        int k = (int) (bits >>> 52) - 1023;
        long mbits = (bits & 0x000fffffffffffffL) | 0x3ff0000000000000L;
        double m = Double.longBitsToDouble(mbits);
        int j = (int) (((mbits >>> 44) & 0xff) + 1) >> 1;   // F = 1 + j/128 nearest to m
        double f, F;
        if (j == 128) { k++; j = 0; f = (m - 2.0) * 0.5; F = 1.0; } else { F = 1.0 + j * 0x1p-7; f = m - F; } // exact
        double inv = LOG_INV[j], u = f * inv;
        // u + c = f/F to ~2^-100: the residual f - u F is exact because u is split so that both products are exact
        double uh = Double.longBitsToDouble(Double.doubleToRawLongBits(u) & 0xfffffffff8000000L), ul = u - uh;
        double c = ((f - uh * F) - ul * F) * inv;
        double tail = u * u * (-0.5 + u * (C3 + u * (C4 + u * (C5 + u * (C6 + u * (C7 + u * C8)))))) - u * c;
        double a = k * LN2_HI, hi = a + LOG_HI[j];
        double e1 = k == 0 ? 0.0 : (a - hi) + LOG_HI[j];              // Fast2Sum: |k ln2| >= ln F when k != 0
        double s = hi + u, bb = s - hi, e2 = (hi - (s - bb)) + (u - bb); // TwoSum
        return s + (e2 + (e1 + (k * LN2_LO + LOG_LO[j]) + c + tail));
    }

    // ---- exp --------------------------------------------------------------------------------------------------------

    private static final double E2 = 0.5, E3 = 1.0 / 6, E4 = 1.0 / 24, E5 = 1.0 / 120, E6 = 1.0 / 720;

    /** e^x, about 0.5 ulp; StrictMath.exp outside |x| <= 708 (overflow, underflow, NaN). */
    public static double exp(double x) {
        if (!(Math.abs(x) <= 708.0)) return StrictMath.exp(x);
        return expKernel(x, 0.0);
    }

    /** e^(x+xl) for |x| <= 708, |xl| below x's ulp. */
    private static double expKernel(double x, double xl) {
        double nd = Math.rint(x * INV_L);
        int n = (int) nd;
        double r = ((x - nd * L_HI) - nd * L_LO) + xl;   // x - nd L_HI is exact (nd L_HI exact, Sterbenz)
        double p = r + r * r * (E2 + r * (E3 + r * (E4 + r * (E5 + r * E6))));
        int j = n & 127, e = n >> 7;
        double t = EXP_HI[j];
        double v = t + (EXP_LO[j] + t * p);
        return v * Double.longBitsToDouble((long) (e + 1023) << 52);  // e in [-1022, 1021]
    }

    // ---- pow --------------------------------------------------------------------------------------------------------

    private static final double SPLIT = 134217729.0; // 2^27 + 1, Veltkamp split

    /** x^y, about 0.5 ulp plus |y ln x| 2^-68; FdlibmPow (= StrictMath.pow) outside the fast domain. */
    public static double pow(double x, double y) {
        long bits = Double.doubleToRawLongBits(x);
        if (bits < 0x0010000000000000L || bits >= 0x7ff0000000000000L || !(Math.abs(y) < 0x1p30) || y == 0.0) return FdlibmPow.pow(x, y);
        // ln x as hi + lo (the log kernel above, kept as a double-double)
        int k = (int) (bits >>> 52) - 1023;
        long mbits = (bits & 0x000fffffffffffffL) | 0x3ff0000000000000L;
        double m = Double.longBitsToDouble(mbits);
        int j = (int) (((mbits >>> 44) & 0xff) + 1) >> 1;
        double f, F;
        if (j == 128) { k++; j = 0; f = (m - 2.0) * 0.5; F = 1.0; } else { F = 1.0 + j * 0x1p-7; f = m - F; }
        double inv = LOG_INV[j], u = f * inv;
        double uh = Double.longBitsToDouble(Double.doubleToRawLongBits(u) & 0xfffffffff8000000L), ul = u - uh;
        double c = ((f - uh * F) - ul * F) * inv;
        double tail = u * u * (-0.5 + u * (C3 + u * (C4 + u * (C5 + u * (C6 + u * (C7 + u * C8)))))) - u * c;
        double a = k * LN2_HI, h0 = a + LOG_HI[j];
        double e1 = k == 0 ? 0.0 : (a - h0) + LOG_HI[j];
        double s = h0 + u, bb = s - h0, e2 = (h0 - (s - bb)) + (u - bb);
        double lo0 = e2 + (e1 + (k * LN2_LO + LOG_LO[j]) + c + tail);
        double hi = s + lo0, lo = lo0 - (hi - s);
        // y ln x = p + pl, p = y hi with its exact rounding error (Dekker product, no fma)
        double p = y * hi;
        if (!(Math.abs(p) <= 708.0)) return FdlibmPow.pow(x, y);
        double cy = SPLIT * y, yh = cy - (cy - y), yl = y - yh;
        double ch = SPLIT * hi, hh = ch - (ch - hi), hl = hi - hh;
        double err = ((yh * hh - p) + yh * hl + yl * hh) + yl * hl;
        return expKernel(p, err + y * lo);
    }
}
