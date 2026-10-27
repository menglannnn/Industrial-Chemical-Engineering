package com.wormzjl.createcheme.science.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CENSUS VARIANT (tools/jdk-determinism/perf/census, never committed to src/): the facade with one extra overload per
 * function taking a call-site id ("File.java:line", inserted by census.sh into a scratch copy of the tree). Returns the
 * same values as the real facade (StrictMath / FdlibmPow). Per site: calls, argument range, how often the argument
 * repeats the site's previous call bit for bit, and for pow the exponent values (up to 16 distinct, else "variable").
 * Written to -Dcensus.out at JVM exit.
 */
public final class DeterministicMath {
    private DeterministicMath() {}

    static final class Site {
        final String key; long calls, repeats; double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY; long lastX = 1L << 63 | 0x7ff8dead, lastY;
        final TreeMap<Double, long[]> exponents = new TreeMap<>(); boolean variable;
        Site(String key) { this.key = key; }
        synchronized void hit(double x) {
            calls++; long b = Double.doubleToRawLongBits(x); if (b == lastX) repeats++; lastX = b;
            if (x < min) min = x; if (x > max) max = x;
        }
        synchronized void hit(double x, double y) {
            calls++; long b = Double.doubleToRawLongBits(x), c = Double.doubleToRawLongBits(y);
            if (b == lastX && c == lastY) repeats++; lastX = b; lastY = c;
            if (x < min) min = x; if (x > max) max = x; if (y < ymin) ymin = y; if (y > ymax) ymax = y;
            if (!variable) { long[] n = exponents.get(y); if (n == null) { if (exponents.size() >= 16) variable = true; else exponents.put(y, new long[]{1}); } else n[0]++; }
        }
    }
    private static final ConcurrentHashMap<String, Site> SITES = new ConcurrentHashMap<>();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            String out = System.getProperty("census.out"); if (out == null) return;
            List<Site> all = new ArrayList<>(SITES.values()); all.sort((a, b) -> Long.compare(b.calls, a.calls));
            StringBuilder s = new StringBuilder("# calls repeats site fn min max [exponents]\n");
            Map<String, long[]> perFn = new TreeMap<>();
            for (Site t : all) {
                String fn = t.key.substring(t.key.lastIndexOf(' ') + 1);
                perFn.computeIfAbsent(fn, k -> new long[1])[0] += t.calls;
                s.append(t.calls).append(' ').append(t.repeats).append(' ').append(t.key).append(' ').append(t.min).append(' ').append(t.max);
                if (fn.equals("pow")) {
                    if (t.variable) s.append(" y=variable[").append(t.ymin).append(',').append(t.ymax).append(']');
                    else { s.append(" y="); for (var e : t.exponents.entrySet()) s.append(e.getKey()).append('x').append(e.getValue()[0]).append(';'); }
                }
                s.append('\n');
            }
            s.append("# per function\n"); perFn.forEach((k, v) -> s.append("# ").append(k).append(' ').append(v[0]).append('\n'));
            try { java.nio.file.Files.writeString(java.nio.file.Path.of(out), s.toString()); } catch (Exception e) { throw new RuntimeException(e); }
        }));
    }
    private static Site site(String id, String fn) { return SITES.computeIfAbsent(id + " " + fn, Site::new); }

    public static double log(String id, double x) { site(id, "log").hit(x); return StrictMath.log(x); }
    public static double log10(String id, double x) { site(id, "log10").hit(x); return StrictMath.log10(x); }
    public static double log1p(String id, double x) { site(id, "log1p").hit(x); return StrictMath.log1p(x); }
    public static double exp(String id, double x) { site(id, "exp").hit(x); return StrictMath.exp(x); }
    public static double expm1(String id, double x) { site(id, "expm1").hit(x); return StrictMath.expm1(x); }
    public static double cbrt(String id, double x) { site(id, "cbrt").hit(x); return StrictMath.cbrt(x); }
    public static double cos(String id, double x) { site(id, "cos").hit(x); return StrictMath.cos(x); }
    public static double sin(String id, double x) { site(id, "sin").hit(x); return StrictMath.sin(x); }
    public static double tan(String id, double x) { site(id, "tan").hit(x); return StrictMath.tan(x); }
    public static double acos(String id, double x) { site(id, "acos").hit(x); return StrictMath.acos(x); }
    public static double asin(String id, double x) { site(id, "asin").hit(x); return StrictMath.asin(x); }
    public static double atan(String id, double x) { site(id, "atan").hit(x); return StrictMath.atan(x); }
    public static double atan2(String id, double y, double x) { site(id, "atan2").hit(y, x); return StrictMath.atan2(y, x); }
    public static double sinh(String id, double x) { site(id, "sinh").hit(x); return StrictMath.sinh(x); }
    public static double cosh(String id, double x) { site(id, "cosh").hit(x); return StrictMath.cosh(x); }
    public static double tanh(String id, double x) { site(id, "tanh").hit(x); return StrictMath.tanh(x); }
    public static double hypot(String id, double x, double y) { site(id, "hypot").hit(x, y); return StrictMath.hypot(x, y); }
    public static double pow(String id, double x, double y) { site(id, "pow").hit(x, y); return FdlibmPow.pow(x, y); }

    public static double log(double x) { return log("unsited", x); }
    public static double log10(double x) { return log10("unsited", x); }
    public static double log1p(double x) { return log1p("unsited", x); }
    public static double exp(double x) { return exp("unsited", x); }
    public static double expm1(double x) { return expm1("unsited", x); }
    public static double cbrt(double x) { return cbrt("unsited", x); }
    public static double cos(double x) { return cos("unsited", x); }
    public static double sin(double x) { return sin("unsited", x); }
    public static double tan(double x) { return tan("unsited", x); }
    public static double acos(double x) { return acos("unsited", x); }
    public static double asin(double x) { return asin("unsited", x); }
    public static double atan(double x) { return atan("unsited", x); }
    public static double atan2(double y, double x) { return atan2("unsited", y, x); }
    public static double sinh(double x) { return sinh("unsited", x); }
    public static double cosh(double x) { return cosh("unsited", x); }
    public static double tanh(double x) { return tanh("unsited", x); }
    public static double hypot(double x, double y) { return hypot("unsited", x, y); }
    public static double pow(double x, double y) { return pow("unsited", x, y); }
    public static double strictPow(double x, double y) { return pow("unsited-strict", x, y); }
}
