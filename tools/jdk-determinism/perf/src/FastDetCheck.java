import com.wormzjl.createcheme.science.math.DeterministicMath;
import com.wormzjl.createcheme.science.math.FastDetMath;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Accuracy, output hash and cost of the candidate kernels (batch 2026-09-27-jdk-determinism, pow performance study).
 *   java FastDetCheck accuracy [n]   per function and input class: inputs, count != StrictMath, max and mean ulp vs
 *                                    StrictMath, and an output hash (SplitMix64 inputs, identical on every JVM)
 *   java FastDetCheck specials       pow/log/exp on a grid of special values: must equal StrictMath bit for bit
 *   java FastDetCheck bench          indicative ns/call (warmed loop, volatile sink, best of 25 rounds; not JMH)
 */
public final class FastDetCheck {
    static long state;
    static long next() { long z = (state += 0x9E3779B97F4A7C15L); z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; return z ^ (z >>> 31); }
    static double unit() { return (next() >>> 11) * 0x1p-53; }
    static double uniform(double a, double b) { return a + (b - a) * unit(); }
    static double logUniform(double a, double b) { return StrictMath.exp(uniform(StrictMath.log(a), StrictMath.log(b))); }

    interface F2 { double apply(double x, double y); }
    interface Gen { void fill(double[] xy); }

    static long ulps(double a, double b) {
        if (Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b)) return 0;
        if (Double.isNaN(a) || Double.isNaN(b) || Math.signum(a) != Math.signum(b)) return Long.MAX_VALUE;
        return Math.abs(Double.doubleToRawLongBits(a) - Double.doubleToRawLongBits(b));
    }

    static void accuracy(String name, int n, Gen gen, F2 fast, F2 strict) {
        double[] xy = new double[2];
        long differ = 0, max = 0, hash = 1469598103934665603L; double sum = 0; double wx = 0, wy = 0;
        state = name.hashCode() * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < n; i++) {
            gen.fill(xy);
            double a = fast.apply(xy[0], xy[1]), b = strict.apply(xy[0], xy[1]);
            hash = (hash ^ Double.doubleToRawLongBits(a)) * 1099511628211L;
            long u = ulps(a, b);
            if (u != 0) { differ++; sum += u; if (u > max) { max = u; wx = xy[0]; wy = xy[1]; } }
        }
        System.out.printf("%-34s n=%d differ=%d (%.2f%%) maxUlp=%d meanUlp=%.4f hash=%016x worst=(%s, %s)%n", name, n, differ,
                100.0 * differ / n, max, sum / n, hash, Double.toHexString(wx), Double.toHexString(wy));
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "accuracy";
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 10_000_000;
        System.out.println("# " + System.getProperty("java.runtime.version") + " " + System.getProperty("java.vm.name")
                + " " + String.join(" ", java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()));
        switch (mode) {
            case "accuracy" -> {
                accuracy("log x in [1e-300,1e300]", n, xy -> xy[0] = logUniform(1e-300, 1e300), (x, y) -> FastDetMath.log(x), (x, y) -> StrictMath.log(x));
                accuracy("log x in [0.5,2]", n, xy -> xy[0] = uniform(0.5, 2), (x, y) -> FastDetMath.log(x), (x, y) -> StrictMath.log(x));
                accuracy("log x = 1 +- 2^-k", n, xy -> xy[0] = 1 + (next() < 0 ? -1 : 1) * StrictMath.scalb(unit(), -(int) (unit() * 50)), (x, y) -> FastDetMath.log(x), (x, y) -> StrictMath.log(x));
                accuracy("log x in [1e-70,1e3] (census)", n, xy -> xy[0] = logUniform(1e-70, 1e3), (x, y) -> FastDetMath.log(x), (x, y) -> StrictMath.log(x));
                accuracy("exp x in [-708,708]", n, xy -> xy[0] = uniform(-708, 708), (x, y) -> FastDetMath.exp(x), (x, y) -> StrictMath.exp(x));
                accuracy("exp x in [-1,1]", n, xy -> xy[0] = uniform(-1, 1), (x, y) -> FastDetMath.exp(x), (x, y) -> StrictMath.exp(x));
                accuracy("exp x in [-340,70] (census)", n, xy -> xy[0] = uniform(-340, 70), (x, y) -> FastDetMath.exp(x), (x, y) -> StrictMath.exp(x));
                accuracy("exp |x| < 1e-8", n, xy -> xy[0] = uniform(-1e-8, 1e-8), (x, y) -> FastDetMath.exp(x), (x, y) -> StrictMath.exp(x));
                accuracy("pow x in [1e-10,1e10], y in [-4,4]", n, xy -> { xy[0] = logUniform(1e-10, 1e10); xy[1] = uniform(-4, 4); }, DeterministicMath::pow, StrictMath::pow);
                accuracy("pow Re^0.9, Re in [2e3,1e7]", n, xy -> { xy[0] = logUniform(2e3, 1e7); xy[1] = 0.9; }, DeterministicMath::pow, StrictMath::pow);
                accuracy("pow x in [1e-300,1e300], y in [-2,2]", n, xy -> { xy[0] = logUniform(1e-300, 1e300); xy[1] = uniform(-2, 2); }, DeterministicMath::pow, StrictMath::pow);
                accuracy("pow x in [0.9,1.1], y in [-3000,3000]", n, xy -> { xy[0] = uniform(0.9, 1.1); xy[1] = uniform(-3000, 3000); }, DeterministicMath::pow, StrictMath::pow);
                accuracy("pow x in [0,10], y small int/half", n, xy -> { xy[0] = uniform(0, 10); xy[1] = 0.5 * (int) uniform(-16, 17); }, DeterministicMath::pow, StrictMath::pow);
                accuracy("pow random bit patterns", n, xy -> { xy[0] = Double.longBitsToDouble(next()); xy[1] = Double.longBitsToDouble(next()); }, DeterministicMath::pow, StrictMath::pow);
                accuracy("strictPow (FdlibmPow) vs StrictMath", n / 10, xy -> { xy[0] = logUniform(1e-10, 1e10); xy[1] = uniform(-4, 4); }, DeterministicMath::strictPow, StrictMath::pow);
                accuracy("production pow vs FastDetMath.pow", n, xy -> { xy[0] = logUniform(1e-300, 1e300); xy[1] = uniform(-4, 4); }, DeterministicMath::pow, FastDetMath::pow);
                trueError(Math.min(n / 50, 200_000));
            }
            case "specials" -> specials();
            case "bench" -> bench();
            default -> throw new IllegalArgumentException(mode);
        }
    }

    /** Error against the exact value (BigDecimal, 40 digits) for a sample, for both StrictMath and the candidates. */
    static void trueError(int n) {
        MathContext mc = new MathContext(40);
        state = 12345;
        double maxFast = 0, maxStrict = 0;
        for (int i = 0; i < n; i++) {
            double x = logUniform(1e-30, 1e30);
            // ln x exact: x = 2^e m, via the table-free series of FastDetMath's generator is not public; use StrictMath.log
            // as the centre and one Newton correction with exp in BigDecimal: L1 = L0 + (x - e^L0) / e^L0
            double l0 = StrictMath.log(x);
            BigDecimal el = expBig(new BigDecimal(l0), mc);
            BigDecimal exact = new BigDecimal(l0).add(new BigDecimal(x).subtract(el, mc).divide(el, mc), mc);
            maxFast = Math.max(maxFast, ulpErr(FastDetMath.log(x), exact));
            maxStrict = Math.max(maxStrict, ulpErr(l0, exact));
        }
        System.out.printf("%-34s n=%d maxErrUlp FastDetMath=%.3f StrictMath=%.3f%n", "log true error x in [1e-30,1e30]", n, maxFast, maxStrict);
        maxFast = 0; maxStrict = 0;
        for (int i = 0; i < n; i++) {
            double x = uniform(-700, 700);
            BigDecimal exact = expBig(new BigDecimal(x), mc);
            maxFast = Math.max(maxFast, ulpErr(FastDetMath.exp(x), exact));
            maxStrict = Math.max(maxStrict, ulpErr(StrictMath.exp(x), exact));
        }
        System.out.printf("%-34s n=%d maxErrUlp FastDetMath=%.3f StrictMath=%.3f%n", "exp true error x in [-700,700]", n, maxFast, maxStrict);
        maxFast = 0; maxStrict = 0;
        for (int i = 0; i < n; i++) {
            double x = logUniform(2e3, 1e7), y = 0.9;
            BigDecimal lx = new BigDecimal(StrictMath.log(x));
            BigDecimal el = expBig(lx, mc);
            lx = lx.add(new BigDecimal(x).subtract(el, mc).divide(el, mc), mc);
            BigDecimal exact = expBig(lx.multiply(new BigDecimal(y), mc), mc);
            maxFast = Math.max(maxFast, ulpErr(FastDetMath.pow(x, y), exact));
            maxStrict = Math.max(maxStrict, ulpErr(StrictMath.pow(x, y), exact));
        }
        System.out.printf("%-34s n=%d maxErrUlp FastDetMath=%.3f StrictMath=%.3f%n", "pow Re^0.9 true error", n, maxFast, maxStrict);
    }

    static double ulpErr(double v, BigDecimal exact) {
        return new BigDecimal(v).subtract(exact).abs().divide(new BigDecimal(Math.ulp(exact.doubleValue())), MathContext.DECIMAL64).doubleValue();
    }

    static BigDecimal expBig(BigDecimal x, MathContext mc) {
        // e^x = (e^(x/2^k))^(2^k), series on the reduced argument
        int k = 12;
        BigDecimal r = x.divide(BigDecimal.valueOf(1L << k), mc), sum = BigDecimal.ONE, term = BigDecimal.ONE;
        for (int i = 1; i < 60; i++) { term = term.multiply(r, mc).divide(BigDecimal.valueOf(i), mc); sum = sum.add(term, mc); }
        for (int i = 0; i < k; i++) sum = sum.multiply(sum, mc);
        return sum;
    }

    static void specials() {
        double[] v = {0.0, -0.0, 1.0, -1.0, 2.0, -2.0, 0.5, -0.5, 3.0, -3.0, 1.5, -1.5, Double.MIN_VALUE, -Double.MIN_VALUE,
                Double.MIN_NORMAL, 0x1p-1060, Double.MAX_VALUE, -Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NaN, 1.0 + 0x1p-52, 1.0 - 0x1p-53, 0x1p1023, 0x1p-1022, 1e300, 1e-300,
                709.78, -745.13, 708.0, -708.0, 1024.0, -1075.0, 0.9, 2000.0, 1e7, 0x1p30, -0x1p31};
        long total = 0, bad = 0;
        for (double x : v) {
            for (double y : v) {
                total++;
                if (Double.doubleToRawLongBits(FastDetMath.pow(x, y)) != Double.doubleToRawLongBits(StrictMath.pow(x, y))
                        && !(Double.isNaN(FastDetMath.pow(x, y)) && Double.isNaN(StrictMath.pow(x, y)))) {
                    long u = ulps(FastDetMath.pow(x, y), StrictMath.pow(x, y));
                    if (u > 1) { bad++; System.out.println("pow(" + x + ", " + y + ") fast=" + FastDetMath.pow(x, y) + " strict=" + StrictMath.pow(x, y)); }
                }
            }
            for (String f : new String[]{"log", "exp"}) {
                double a = f.equals("log") ? FastDetMath.log(x) : FastDetMath.exp(x), b = f.equals("log") ? StrictMath.log(x) : StrictMath.exp(x);
                total++;
                if (ulps(a, b) > 1) { bad++; System.out.println(f + "(" + x + ") fast=" + a + " strict=" + b); }
            }
        }
        System.out.println("specials: " + total + " cases, " + bad + " differ from StrictMath by more than 1 ulp or in class (NaN/inf/sign/zero)");
    }

    static volatile double sink;

    static void bench() {
        int n = 1 << 14;
        double[] x = new double[n], y = new double[n], re = new double[n], z = new double[n], c = new double[n], d = new double[n];
        state = 7;
        for (int i = 0; i < n; i++) { x[i] = logUniform(1e-6, 1e6); y[i] = uniform(-30, 30); re[i] = logUniform(2e3, 1e7); z[i] = uniform(-3, 3); c[i] = uniform(-4.2, 1.1); d[i] = uniform(-0.99, 0.99); }
        String[] names = {"log Math", "log StrictMath", "log FastDet", "exp Math", "exp StrictMath", "exp FastDet",
                "pow Math", "pow StrictMath", "pow strictPow(FdlibmPow)", "pow FastDet",
                "Re^0.9 Math", "Re^0.9 strictPow", "Re^0.9 FastDet", "x^5 strictPow", "x^5 multiply", "x^1.5 strictPow", "x^1.5 x*sqrt(x)",
                "cos Math", "cos StrictMath", "acos Math", "acos StrictMath", "cbrt Math", "cbrt StrictMath", "log10 Math", "log10 StrictMath", "expm1 StrictMath"};
        double[] best = new double[names.length];
        java.util.Arrays.fill(best, Double.MAX_VALUE);
        for (int round = 0; round < 25; round++) {
            for (int f = 0; f < names.length; f++) {
                long t0 = System.nanoTime();
                double acc = 0;
                for (int rep = 0; rep < 8; rep++) {
                    switch (f) {
                        case 0 -> { for (int i = 0; i < n; i++) acc += Math.log(x[i]); }
                        case 1 -> { for (int i = 0; i < n; i++) acc += StrictMath.log(x[i]); }
                        case 2 -> { for (int i = 0; i < n; i++) acc += FastDetMath.log(x[i]); }
                        case 3 -> { for (int i = 0; i < n; i++) acc += Math.exp(y[i]); }
                        case 4 -> { for (int i = 0; i < n; i++) acc += StrictMath.exp(y[i]); }
                        case 5 -> { for (int i = 0; i < n; i++) acc += FastDetMath.exp(y[i]); }
                        case 6 -> { for (int i = 0; i < n; i++) acc += Math.pow(x[i], z[i]); }
                        case 7 -> { for (int i = 0; i < n; i++) acc += StrictMath.pow(x[i], z[i]); }
                        case 8 -> { for (int i = 0; i < n; i++) acc += DeterministicMath.strictPow(x[i], z[i]); }
                        case 9 -> { for (int i = 0; i < n; i++) acc += FastDetMath.pow(x[i], z[i]); }
                        case 10 -> { for (int i = 0; i < n; i++) acc += Math.pow(re[i], 0.9); }
                        case 11 -> { for (int i = 0; i < n; i++) acc += DeterministicMath.strictPow(re[i], 0.9); }
                        case 12 -> { for (int i = 0; i < n; i++) acc += FastDetMath.pow(re[i], 0.9); }
                        case 13 -> { for (int i = 0; i < n; i++) acc += DeterministicMath.strictPow(z[i], 5); }
                        case 14 -> { for (int i = 0; i < n; i++) { double s = z[i] * z[i]; acc += s * s * z[i]; } }
                        case 15 -> { for (int i = 0; i < n; i++) acc += DeterministicMath.strictPow(x[i], 1.5); }
                        case 16 -> { for (int i = 0; i < n; i++) acc += x[i] * Math.sqrt(x[i]); }
                        case 17 -> { for (int i = 0; i < n; i++) acc += Math.cos(c[i]); }
                        case 18 -> { for (int i = 0; i < n; i++) acc += StrictMath.cos(c[i]); }
                        case 19 -> { for (int i = 0; i < n; i++) acc += Math.acos(d[i]); }
                        case 20 -> { for (int i = 0; i < n; i++) acc += StrictMath.acos(d[i]); }
                        case 21 -> { for (int i = 0; i < n; i++) acc += Math.cbrt(y[i]); }
                        case 22 -> { for (int i = 0; i < n; i++) acc += StrictMath.cbrt(y[i]); }
                        case 23 -> { for (int i = 0; i < n; i++) acc += Math.log10(x[i]); }
                        case 24 -> { for (int i = 0; i < n; i++) acc += StrictMath.log10(x[i]); }
                        default -> { for (int i = 0; i < n; i++) acc += StrictMath.expm1(z[i]); }
                    }
                }
                sink = acc;
                best[f] = Math.min(best[f], (System.nanoTime() - t0) / (8.0 * n));
            }
        }
        for (int f = 0; f < names.length; f++) System.out.printf("%-24s %6.2f ns/call%n", names[f], best[f]);
    }
}
