import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Runs the JUnit {@code @Test} methods of the given test classes without Gradle, so parallel lanes in one worktree can
 * gate their own compiled classes (their output directory first on the class path). Only plain {@code @Test} methods
 * are supported (the equipment tests use nothing else). Args: class names; {@code -m name} keeps one method.
 * Exit code 1 when any test fails.
 */
public class TestRunner {
    public static void main(String[] args) throws Exception {
        String only = null;
        int passed = 0, failed = 0;
        var classes = new java.util.ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-m")) only = args[++i];
            else classes.add(args[i]);
        }
        var test = Class.forName("org.junit.jupiter.api.Test").asSubclass(java.lang.annotation.Annotation.class);
        for (String name : classes) {
            var type = Class.forName(name);
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Method[] methods = type.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (var method : methods) {
                if (!method.isAnnotationPresent(test) || method.getParameterCount() != 0) continue;
                if (only != null && !method.getName().equals(only)) continue;
                method.setAccessible(true);
                long start = System.nanoTime();
                try {
                    method.invoke(constructor.newInstance());
                    passed++;
                    System.out.printf("PASS %s.%s (%d ms)%n", type.getSimpleName(), method.getName(), (System.nanoTime() - start) / 1_000_000);
                } catch (InvocationTargetException e) {
                    failed++;
                    var cause = e.getCause();
                    System.out.printf("FAIL %s.%s: %s%n", type.getSimpleName(), method.getName(), cause);
                    for (var frame : cause.getStackTrace())
                        if (frame.getClassName().startsWith("com.wormzjl")) System.out.println("    at " + frame);
                }
            }
        }
        System.out.printf("%d passed, %d failed%n", passed, failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
