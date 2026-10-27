import java.util.*;
public class BendCheck {
    static final double PITCH = 3 / 16.0, CLEAR = 1 / 64.0;
    static double c(int k) { return 1 / 8.0 + 3 * k / 16.0; }
    static final int[][] FACE = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    // Points along a lane's centreline: two arms from P to faces f1,f2, with an elbow of radius R when they turn.
    static List<double[]> line(double p, int f1, int f2, double R) {
        var pts = new ArrayList<double[]>();
        int a1 = axis(f1), a2 = axis(f2); boolean turn = a1 != a2;
        double cut = turn ? R : 0;
        for (int f : new int[]{f1, f2}) { int ax = axis(f); double end = FACE[f][ax] > 0 ? 1 : 0, dir = Math.signum(end - p);
            for (double s = cut; s <= Math.abs(end - p) + 1e-12; s += .002) { double[] q = {p, p, p}; q[ax] = p + dir * s; pts.add(q); } }
        if (turn) { double d1 = FACE[f1][a1], d2 = FACE[f2][a2]; // arc centre at P + R*d1 + R*d2
            for (double t = 0; t <= Math.PI / 2 + 1e-9; t += .01) { double[] q = {p, p, p};
                q[a1] = p + d1 * R - d1 * R * Math.cos(t); q[a2] = p + d2 * R - d2 * R * Math.sin(t); pts.add(q); } }
        return pts;
    }
    static int axis(int f) { for (int i = 0; i < 3; i++) if (FACE[f][i] != 0) return i; return -1; }
    public static void main(String[] args) {
        for (String rule : new String[]{"long radius 1.5 OD", "short: (m+1)/2 pitch"}) {
            double worst = 1e9; String at = "";
            for (int s1 = 0; s1 < 5; s1++) for (int m1 = 1; s1 + m1 <= 5; m1++) {
                double p1 = (c(s1) + c(s1 + m1 - 1)) / 2, od1 = Math.min(.8, m1 * PITCH - CLEAR);
                double R1 = rule.startsWith("long") ? Math.min(1.5 * od1, .4) : Math.min((m1 + 1) / 2.0 * PITCH, .4);
                for (int s2 = 0; s2 < 5; s2++) for (int m2 = 1; s2 + m2 <= 5; m2++) {
                    if (s2 < s1 + m1 && s1 < s2 + m2) continue;
                    double p2 = (c(s2) + c(s2 + m2 - 1)) / 2, od2 = Math.min(.8, m2 * PITCH - CLEAR);
                    double R2 = rule.startsWith("long") ? Math.min(1.5 * od2, .4) : Math.min((m2 + 1) / 2.0 * PITCH, .4);
                    for (int f1 = 0; f1 < 6; f1++) for (int g1 = f1 + 1; g1 < 6; g1++) { if ((f1 ^ 1) == g1 && false) continue;
                        var A = line(p1, f1, g1, (f1 ^ 1) == g1 ? 0 : R1);
                        for (int f2 = 0; f2 < 6; f2++) for (int g2 = f2 + 1; g2 < 6; g2++) {
                            var B = line(p2, f2, g2, (f2 ^ 1) == g2 ? 0 : R2);
                            double best = 1e9; for (var a : A) for (var b : B) { double dx=a[0]-b[0],dy=a[1]-b[1],dz=a[2]-b[2]; best = Math.min(best, dx*dx+dy*dy+dz*dz); }
                            double gap = Math.sqrt(best) - od1 / 2 - od2 / 2;
                            if (gap < worst) { worst = gap; at = "["+s1+","+(s1+m1)+") faces "+f1+","+g1+" vs ["+s2+","+(s2+m2)+") faces "+f2+","+g2; }
                        } }
                }
            }
            System.out.printf("%s: smallest gap %.4f m at %s%n", rule, worst, at);
        }
    }
}
