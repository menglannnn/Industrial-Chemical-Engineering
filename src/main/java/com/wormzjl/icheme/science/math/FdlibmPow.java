package com.wormzjl.icheme.science.math;

/**
 * fdlibm 5.3 {@code __ieee754_pow}, transliterated from {@code e_pow.c} (netlib.org/fdlibm, "@(#)e_pow.c 1.5 04/04/22
 * SMI") with the constant tables hoisted out of the call, so a call allocates nothing. The arithmetic is the C
 * source's operation for operation; Java evaluates every {@code double} operation with IEEE 754 round-to-nearest (JLS
 * 15.4, strict since Java 17), so the result is bitwise the fdlibm result, which is what {@code StrictMath.pow} is
 * specified to return. Used through {@link DeterministicMath#pow}.
 *
 * <pre>
 * ====================================================
 * Copyright (C) 2004 by Sun Microsystems, Inc. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 * </pre>
 */
final class FdlibmPow {
    private FdlibmPow() {
    }

    private static final double[] BP = {1.0, 1.5};
    private static final double[] DP_H = {0.0, 5.84962487220764160156e-01}; // 0x3FE2B803, 0x40000000
    private static final double[] DP_L = {0.0, 1.35003920212974897128e-08}; // 0x3E4CFDEB, 0x43CFD006
    private static final double ZERO = 0.0;
    private static final double ONE = 1.0;
    private static final double TWO = 2.0;
    private static final double TWO53 = 9007199254740992.0; // 0x43400000, 0x00000000
    private static final double HUGE = 1.0e300;
    private static final double TINY = 1.0e-300;
    // poly coefs for (3/2)*(log(x)-2s-2/3*s**3
    private static final double L1 = 5.99999999999994648725e-01; // 0x3FE33333, 0x33333303
    private static final double L2 = 4.28571428578550184252e-01; // 0x3FDB6DB6, 0xDB6FABFF
    private static final double L3 = 3.33333329818377432918e-01; // 0x3FD55555, 0x518F264D
    private static final double L4 = 2.72728123808534006489e-01; // 0x3FD17460, 0xA91D4101
    private static final double L5 = 2.30660745775561754067e-01; // 0x3FCD864A, 0x93C9DB65
    private static final double L6 = 2.06975017800338417784e-01; // 0x3FCA7E28, 0x4A454EEF
    private static final double P1 = 1.66666666666666019037e-01; // 0x3FC55555, 0x5555553E
    private static final double P2 = -2.77777777770155933842e-03; // 0xBF66C16C, 0x16BEBD93
    private static final double P3 = 6.61375632143793436117e-05; // 0x3F11566A, 0xAF25DE2C
    private static final double P4 = -1.65339022054652515390e-06; // 0xBEBBBD41, 0xC5D26BF1
    private static final double P5 = 4.13813679705723846039e-08; // 0x3E663769, 0x72BEA4D0
    private static final double LG2 = 6.93147180559945286227e-01; // 0x3FE62E42, 0xFEFA39EF
    private static final double LG2_H = 6.93147182464599609375e-01; // 0x3FE62E43, 0x00000000
    private static final double LG2_L = -1.90465429995776804525e-09; // 0xBE205C61, 0x0CA86C39
    private static final double OVT = 8.0085662595372944372e-0017; // -(1024-log2(ovfl+.5ulp))
    private static final double CP = 9.61796693925975554329e-01; // 0x3FEEC709, 0xDC3A03FD =2/(3ln2)
    private static final double CP_H = 9.61796700954437255859e-01; // 0x3FEEC709, 0xE0000000 =(float)cp
    private static final double CP_L = -7.02846165095275826516e-09; // 0xBE3E2FE0, 0x145B01F5 =tail of cp_h
    private static final double IVLN2 = 1.44269504088896338700e+00; // 0x3FF71547, 0x652B82FE =1/ln2
    private static final double IVLN2_H = 1.44269502162933349609e+00; // 0x3FF71547, 0x60000000 =24b 1/ln2
    private static final double IVLN2_L = 1.92596299112661746887e-08; // 0x3E54AE0B, 0xF85DDF44 =1/ln2 tail

    private static int hi(double x) {
        return (int) (Double.doubleToRawLongBits(x) >>> 32);
    }

    private static int lo(double x) {
        return (int) Double.doubleToRawLongBits(x);
    }

    /** __LO(x) = 0. */
    private static double clearLo(double x) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(x) & 0xFFFFFFFF00000000L);
    }

    /** __HI(x) = high, keeping the low word. */
    private static double withHi(double x, int high) {
        return Double.longBitsToDouble(((long) high << 32) | (Double.doubleToRawLongBits(x) & 0xFFFFFFFFL));
    }

    static double pow(double x, double y) {
        double z, ax, zH, zL, pH, pL;
        double y1, t1, t2, r, s, t, u, v, w;
        int i, j, k, yisint, n;
        int hx, hy, ix, iy;
        int lx, ly;

        hx = hi(x);
        lx = lo(x);
        hy = hi(y);
        ly = lo(y);
        ix = hx & 0x7fffffff;
        iy = hy & 0x7fffffff;

        // y==zero: x**0 = 1
        if ((iy | ly) == 0) {
            return ONE;
        }

        // +-NaN return x+y
        if (ix > 0x7ff00000 || ((ix == 0x7ff00000) && (lx != 0))
                || iy > 0x7ff00000 || ((iy == 0x7ff00000) && (ly != 0))) {
            return x + y;
        }

        // determine if y is an odd int when x < 0: 0 not an integer, 1 odd, 2 even
        yisint = 0;
        if (hx < 0) {
            if (iy >= 0x43400000) {
                yisint = 2; // even integer y
            } else if (iy >= 0x3ff00000) {
                k = (iy >> 20) - 0x3ff; // exponent
                if (k > 20) {
                    j = ly >>> (52 - k);
                    if ((j << (52 - k)) == ly) {
                        yisint = 2 - (j & 1);
                    }
                } else if (ly == 0) {
                    j = iy >> (20 - k);
                    if ((j << (20 - k)) == iy) {
                        yisint = 2 - (j & 1);
                    }
                }
            }
        }

        // special value of y
        if (ly == 0) {
            if (iy == 0x7ff00000) { // y is +-inf
                if (((ix - 0x3ff00000) | lx) == 0) {
                    return y - y; // inf**+-1 is NaN
                } else if (ix >= 0x3ff00000) { // (|x|>1)**+-inf = inf,0
                    return (hy >= 0) ? y : ZERO;
                } else { // (|x|<1)**-,+inf = inf,0
                    return (hy < 0) ? -y : ZERO;
                }
            }
            if (iy == 0x3ff00000) { // y is +-1
                if (hy < 0) {
                    return ONE / x;
                } else {
                    return x;
                }
            }
            if (hy == 0x40000000) {
                return x * x; // y is 2
            }
            if (hy == 0x3fe00000) { // y is 0.5
                if (hx >= 0) { // x >= +0
                    return Math.sqrt(x);
                }
            }
        }

        ax = Math.abs(x);
        // special value of x
        if (lx == 0) {
            if (ix == 0x7ff00000 || ix == 0 || ix == 0x3ff00000) {
                z = ax; // x is +-0,+-inf,+-1
                if (hy < 0) {
                    z = ONE / z; // z = (1/|x|)
                }
                if (hx < 0) {
                    if (((ix - 0x3ff00000) | yisint) == 0) {
                        z = (z - z) / (z - z); // (-1)**non-int is NaN
                    } else if (yisint == 1) {
                        z = -z; // (x<0)**odd = -(|x|**odd)
                    }
                }
                return z;
            }
        }

        n = (hx >> 31) + 1;

        // (x<0)**(non-int) is NaN
        if ((n | yisint) == 0) {
            return (x - x) / (x - x);
        }

        s = ONE; // s (sign of result -ve**odd) = -1 else = 1
        if ((n | (yisint - 1)) == 0) {
            s = -ONE; // (-ve)**(odd int)
        }

        // |y| is huge
        if (iy > 0x41e00000) { // if |y| > 2**31
            if (iy > 0x43f00000) { // if |y| > 2**64, must o/uflow
                if (ix <= 0x3fefffff) {
                    return (hy < 0) ? HUGE * HUGE : TINY * TINY;
                }
                if (ix >= 0x3ff00000) {
                    return (hy > 0) ? HUGE * HUGE : TINY * TINY;
                }
            }
            // over/underflow if x is not close to one
            if (ix < 0x3fefffff) {
                return (hy < 0) ? s * HUGE * HUGE : s * TINY * TINY;
            }
            if (ix > 0x3ff00000) {
                return (hy > 0) ? s * HUGE * HUGE : s * TINY * TINY;
            }
            // now |1-x| is tiny <= 2**-20, suffice to compute log(x) by x-x^2/2+x^3/3-x^4/4
            t = ax - ONE; // t has 20 trailing zeros
            w = (t * t) * (0.5 - t * (0.3333333333333333333333 - t * 0.25));
            u = IVLN2_H * t; // ivln2_h has 21 sig. bits
            v = t * IVLN2_L - w * IVLN2;
            t1 = u + v;
            t1 = clearLo(t1);
            t2 = v - (t1 - u);
        } else {
            double ss, s2, sH, sL, tH, tL;
            n = 0;
            // take care subnormal number
            if (ix < 0x00100000) {
                ax *= TWO53;
                n -= 53;
                ix = hi(ax);
            }
            n += ((ix) >> 20) - 0x3ff;
            j = ix & 0x000fffff;
            // determine interval
            ix = j | 0x3ff00000; // normalize ix
            if (j <= 0x3988E) {
                k = 0; // |x|<sqrt(3/2)
            } else if (j < 0xBB67A) {
                k = 1; // |x|<sqrt(3)
            } else {
                k = 0;
                n += 1;
                ix -= 0x00100000;
            }
            ax = withHi(ax, ix);

            // compute ss = s_h+s_l = (x-1)/(x+1) or (x-1.5)/(x+1.5)
            u = ax - BP[k]; // bp[0]=1.0, bp[1]=1.5
            v = ONE / (ax + BP[k]);
            ss = u * v;
            sH = ss;
            sH = clearLo(sH);
            // t_h=ax+bp[k] High
            tH = ZERO;
            tH = withHi(tH, ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18));
            tL = ax - (tH - BP[k]);
            sL = v * ((u - sH * tH) - sH * tL);
            // compute log(ax)
            s2 = ss * ss;
            r = s2 * s2 * (L1 + s2 * (L2 + s2 * (L3 + s2 * (L4 + s2 * (L5 + s2 * L6)))));
            r += sL * (sH + ss);
            s2 = sH * sH;
            tH = 3.0 + s2 + r;
            tH = clearLo(tH);
            tL = r - ((tH - 3.0) - s2);
            // u+v = ss*(1+...)
            u = sH * tH;
            v = sL * tH + tL * ss;
            // 2/(3log2)*(ss+...)
            pH = u + v;
            pH = clearLo(pH);
            pL = v - (pH - u);
            zH = CP_H * pH; // cp_h+cp_l = 2/(3*log2)
            zL = CP_L * pH + pL * CP + DP_L[k];
            // log2(ax) = (ss+..)*2/(3*log2) = n + dp_h + z_h + z_l
            t = (double) n;
            t1 = (((zH + zL) + DP_H[k]) + t);
            t1 = clearLo(t1);
            t2 = zL - (((t1 - t) - DP_H[k]) - zH);
        }

        // split up y into y1+y2 and compute (y1+y2)*(t1+t2)
        y1 = y;
        y1 = clearLo(y1);
        pL = (y - y1) * t1 + y * t2;
        pH = y1 * t1;
        z = pL + pH;
        j = hi(z);
        i = lo(z);
        if (j >= 0x40900000) { // z >= 1024
            if (((j - 0x40900000) | i) != 0) { // if z > 1024
                return s * HUGE * HUGE; // overflow
            } else {
                if (pL + OVT > z - pH) {
                    return s * HUGE * HUGE; // overflow
                }
            }
        } else if ((j & 0x7fffffff) >= 0x4090cc00) { // z <= -1075
            if (((j - 0xc090cc00) | i) != 0) { // z < -1075
                return s * TINY * TINY; // underflow
            } else {
                if (pL <= z - pH) {
                    return s * TINY * TINY; // underflow
                }
            }
        }
        // compute 2**(p_h+p_l)
        i = j & 0x7fffffff;
        k = (i >> 20) - 0x3ff;
        n = 0;
        if (i > 0x3fe00000) { // if |z| > 0.5, set n = [z+0.5]
            n = j + (0x00100000 >> (k + 1));
            k = ((n & 0x7fffffff) >> 20) - 0x3ff; // new k for n
            t = ZERO;
            t = withHi(t, (n & ~(0x000fffff >> k)));
            n = ((n & 0x000fffff) | 0x00100000) >> (20 - k);
            if (j < 0) {
                n = -n;
            }
            pH -= t;
        }
        t = pL + pH;
        t = clearLo(t);
        u = t * LG2_H;
        v = (pL - (t - pH)) * LG2 + t * LG2_L;
        z = u + v;
        w = v - (z - u);
        t = z * z;
        t1 = z - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
        r = (z * t1) / (t1 - TWO) - (w + z * w);
        z = ONE - (r - z);
        j = hi(z);
        j += (n << 20);
        if ((j >> 20) <= 0) {
            z = Math.scalb(z, n); // subnormal output
        } else {
            z = withHi(z, hi(z) + (n << 20));
        }
        return s * z;
    }
}
