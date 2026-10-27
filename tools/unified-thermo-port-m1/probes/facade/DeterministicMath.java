package com.wormzjl.createcheme.science.math;

/** Probe only (M1 lane): the facade delegating to java.lang.Math, as the branch's code called it. */
public final class DeterministicMath {
    private DeterministicMath() {}
    public static double log(double x) { return Math.log(x); }
    public static double log10(double x) { return Math.log10(x); }
    public static double log1p(double x) { return Math.log1p(x); }
    public static double exp(double x) { return Math.exp(x); }
    public static double expm1(double x) { return Math.expm1(x); }
    public static double cbrt(double x) { return Math.cbrt(x); }
    public static double cos(double x) { return Math.cos(x); }
    public static double acos(double x) { return Math.acos(x); }
    public static double atan(double x) { return Math.atan(x); }
    public static double tanh(double x) { return Math.tanh(x); }
    public static double pow(double x, double y) { return Math.pow(x, y); }
    public static double strictPow(double x, double y) { return Math.pow(x, y); }
}
