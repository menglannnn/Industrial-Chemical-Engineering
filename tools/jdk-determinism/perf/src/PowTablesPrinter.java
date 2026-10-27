package com.wormzjl.createcheme.science.math;

/**
 * Prints FastDetMath's BigDecimal-generated tables and constants as Java hexadecimal double literals (exact), the form
 * embedded in {@code science/math/DeterministicPow.java} (batch 2026-09-27-jdk-determinism, candidate C).
 * {@code java -cp <perf classes;main> com.wormzjl.createcheme.science.math.PowTablesPrinter}
 */
public final class PowTablesPrinter {
    public static void main(String[] args) {
        constant("LN2_HI", FastDetMath.LN2_HI);
        constant("LN2_LO", FastDetMath.LN2_LO);
        constant("L_HI", FastDetMath.L_HI);
        constant("L_LO", FastDetMath.L_LO);
        constant("INV_L", FastDetMath.INV_L);
        table("EXP_HI", FastDetMath.EXP_HI);
        table("EXP_LO", FastDetMath.EXP_LO);
        table("LOG_INV", FastDetMath.LOG_INV);
        table("LOG_HI", FastDetMath.LOG_HI);
        table("LOG_LO", FastDetMath.LOG_LO);
    }

    static void constant(String name, double v) {
        System.out.println("    private static final double " + name + " = " + Double.toHexString(v) + ";");
    }

    static void table(String name, double[] t) {
        StringBuilder b = new StringBuilder("    private static final double[] " + name + " = {\n");
        for (int i = 0; i < t.length; i++) {
            if (i % 4 == 0) b.append("        ");
            b.append(Double.toHexString(t[i])).append(',');
            b.append(i % 4 == 3 || i == t.length - 1 ? "\n" : " ");
        }
        System.out.print(b.append("    };\n"));
    }
}
