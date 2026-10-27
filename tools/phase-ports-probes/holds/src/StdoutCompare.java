import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** HOLDS helper: compares two concatenated JUnit system-out captures line by line after masking timings and sorting the
 * entries of every {...} map (immutable-map iteration order differs between JVM runs). Usage: java StdoutCompare.java a b */
public class StdoutCompare {
    static final Pattern MAP=Pattern.compile("\\{([^{}]*)\\}");
    static String norm(String line) {
        line=line.replaceAll("(ms|wallMs|nanos|Nanos|allocatedMB|bytes|MB)=[0-9.eE+-]+","$1=*").replaceAll("\\[\\d\\d:\\d\\d:\\d\\d\\]","[t]");
        line=line.replaceAll("(capture|encode|decode|write|load|register) [0-9.]+","$1 *").replaceAll("median ms by world size: \\{[^}]*\\}","median *");
        var m=MAP.matcher(line);var out=new StringBuilder();
        while(m.find()){var parts=new ArrayList<>(Arrays.asList(m.group(1).split(", ")));Collections.sort(parts);m.appendReplacement(out,Matcher.quoteReplacement("{"+String.join(", ",parts)+"}"));}
        m.appendTail(out);return out.toString();
    }
    public static void main(String[] a) throws Exception {
        var x=new ArrayList<String>();for(var l:Files.readAllLines(Path.of(a[0])))if(!l.contains("F1_")&&!l.contains("F2_"))x.add(norm(l));
        var y=new ArrayList<String>();for(var l:Files.readAllLines(Path.of(a[1])))if(!l.contains("F1_")&&!l.contains("F2_"))y.add(norm(l));
        var sx=new HashMap<String,Integer>();for(var l:x)sx.merge(l,1,Integer::sum);
        var sy=new HashMap<String,Integer>();for(var l:y)sy.merge(l,1,Integer::sum);
        int only=0;
        for(var e:sx.entrySet()){int d=e.getValue()-sy.getOrDefault(e.getKey(),0);if(d>0){only++;System.out.println("- "+e.getKey());}}
        for(var e:sy.entrySet()){int d=e.getValue()-sx.getOrDefault(e.getKey(),0);if(d>0){only++;System.out.println("+ "+e.getKey());}}
        System.out.println("lines a="+x.size()+" b="+y.size()+" differing="+only);
    }
}
