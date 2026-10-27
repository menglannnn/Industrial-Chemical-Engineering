public class RackCheck {
    static final double PITCH = 3 / 16.0, CLEAR = 1 / 64.0, FRAME = 1 / 32.0;
    static double c(int k) { return 1 / 8.0 + 3 * k / 16.0; }
    static final double[][] FACE = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    // Arm from P to the face point of face f: axis-parallel segment.
    static double[][] arm(double p, int f) {
        double[] a = {p, p, p}, b = {p, p, p};
        for (int i = 0; i < 3; i++) if (FACE[f][i] != 0) b[i] = FACE[f][i] > 0 ? 1 : 0;
        return new double[][]{a, b};
    }
    static double segDist(double[] p1, double[] q1, double[] p2, double[] q2) {
        double[] d1 = sub(q1, p1), d2 = sub(q2, p2), r = sub(p1, p2);
        double a = dot(d1, d1), e = dot(d2, d2), f = dot(d2, r), s, t;
        double cc = dot(d1, r), b = dot(d1, d2), den = a * e - b * b;
        s = den > 1e-12 ? clamp((b * f - cc * e) / den) : 0;
        t = (b * s + f) / e;
        if (t < 0) { t = 0; s = clamp(-cc / a); } else if (t > 1) { t = 1; s = clamp((b - cc) / a); }
        double[] x = add(p1, mul(d1, s)), y = add(p2, mul(d2, t));
        return Math.sqrt(dot(sub(x, y), sub(x, y)));
    }
    static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
    static double[] sub(double[] a, double[] b) { return new double[]{a[0]-b[0], a[1]-b[1], a[2]-b[2]}; }
    static double[] add(double[] a, double[] b) { return new double[]{a[0]+b[0], a[1]+b[1], a[2]+b[2]}; }
    static double[] mul(double[] a, double s) { return new double[]{a[0]*s, a[1]*s, a[2]*s}; }
    static double dot(double[] a, double[] b) { return a[0]*b[0] + a[1]*b[1] + a[2]*b[2]; }
    public static void main(String[] args) {
        double worst = 1e9; String at = ""; int pairs = 0; double worstFrame = 1e9;
        for (int s1 = 0; s1 < 5; s1++) for (int m1 = 1; s1 + m1 <= 5; m1++) {
            double p1 = (c(s1) + c(s1 + m1 - 1)) / 2, r1 = Math.min(.8, m1 * PITCH - CLEAR) / 2;
            worstFrame = Math.min(worstFrame, Math.min(p1 - r1, 1 - p1 - r1));
            for (int s2 = 0; s2 < 5; s2++) for (int m2 = 1; s2 + m2 <= 5; m2++) {
                if (s2 < s1 + m1 && s1 < s2 + m2) continue; // ranges overlap: same cell cannot hold both
                double p2 = (c(s2) + c(s2 + m2 - 1)) / 2, r2 = Math.min(.8, m2 * PITCH - CLEAR) / 2;
                for (int f1 = 0; f1 < 6; f1++) for (int f2 = 0; f2 < 6; f2++) {
                    var a = arm(p1, f1); var b = arm(p2, f2);
                    double gap = segDist(a[0], a[1], b[0], b[1]) - r1 - r2; pairs++;
                    if (gap < worst) { worst = gap; at = "slots ["+s1+","+(s1+m1)+") face "+f1+" vs ["+s2+","+(s2+m2)+") face "+f2; }
                }
            }
        }
        System.out.printf("arm pairs checked: %d%nsmallest surface gap: %.6f m (1/64 = %.6f) at %s%nsmallest margin to block edge: %.6f m (frame 1/32 = %.6f)%n", pairs, worst, CLEAR, at, worstFrame, FRAME);
    }
}
