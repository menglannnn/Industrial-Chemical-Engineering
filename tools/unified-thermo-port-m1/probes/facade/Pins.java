public class Pins {
    public static void main(String[] a) {
        double[] xa = {0.5, 1.0, 2.0, -3.75, 1e-3};
        for (double x : xa) System.out.println("atan " + x + " " + Double.toHexString(StrictMath.atan(x)) + " math " + Double.toHexString(Math.atan(x)));
        double[] xt = {0.5, 1.0, 2.0, -0.125, 1e-3};
        for (double x : xt) System.out.println("tanh " + x + " " + Double.toHexString(StrictMath.tanh(x)) + " math " + Double.toHexString(Math.tanh(x)));
    }
}
