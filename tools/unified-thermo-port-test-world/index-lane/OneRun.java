import java.lang.reflect.Method;
import java.nio.file.*;
import javax.tools.ToolProvider;

/**
 * One JVM per commit for the index lane (F1/F2 bisect, 2026-10-16): compiles the export's main sources (unless
 * main/.compiled exists) and the probes in-process with javac, then runs the JUnit selectors through M3Run. Launched in
 * source-file mode by onejvm.sh with the run classpath (export test/ and main/ directories first), so the classes it
 * writes are loaded by the application class loader exactly as run.sh loads them.
 * Arguments: the export directory, then the selectors.
 */
public class OneRun {
    public static void main(String[] args) throws Exception {
        Path x=Path.of(args[0]);
        var javac=ToolProvider.getSystemJavaCompiler();
        long started=System.nanoTime();
        if(!Files.exists(x.resolve("main/.compiled"))) {
            int m=javac.run(null,System.out,System.err,"-nowarn","-encoding","UTF-8","-proc:none","-d",x.resolve("main").toString(),
                    "@"+x.resolve("main-cp.txt"),"@"+x.resolve("main.txt"));
            System.out.printf("ONERUN main javac=%d (%.1f s)%n",m,(System.nanoTime()-started)/1e9);
            if(m!=0)System.exit(2);
            Files.writeString(x.resolve("main/.compiled"),"ok");
        } else System.out.println("ONERUN main reused");
        long probes=System.nanoTime();
        int t=javac.run(null,System.out,System.err,"-nowarn","-encoding","UTF-8","-proc:none","-implicit:class","-d",x.resolve("test").toString(),
                "@"+x.resolve("test-cp.txt"),"@"+x.resolve("test.txt"));
        System.out.printf("ONERUN probes javac=%d (%.1f s)%n",t,(System.nanoTime()-probes)/1e9);
        if(t!=0)System.exit(3);
        String[] selectors=java.util.Arrays.copyOfRange(args,1,args.length);
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        Method main=Class.forName("M3Run",true,ClassLoader.getSystemClassLoader()).getMethod("main",String[].class);
        main.invoke(null,(Object)selectors);
    }
}
