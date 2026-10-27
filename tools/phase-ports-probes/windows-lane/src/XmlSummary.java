import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * Java port of tools/phase-ports-probes/wp1/src/xmlsummary.py (python is not installed on the owner's Windows machine).
 * Usage: java XmlSummary.java <build/test-results/test> <junction-lines out> <test-names out> [raw-junction-lines out]
 * Prints classes/tests/failures/errors/skipped and the failing test names; writes the MIXED_GAS/LIQUID_JUNCTION lines
 * with ms, bytes and allocatedMB masked (the Python script's masking), the sorted test names and, optionally, the
 * unmasked lines (for the MIXED_GAS_COST ms/MB readout). Written without backslash escapes on purpose.
 */
public class XmlSummary {
    public static void main(String[] a) throws Exception {
        String nl = String.valueOf((char) 10);
        int tests = 0, fail = 0, err = 0, skip = 0, classes = 0;
        List<String> lines = new ArrayList<>(), names = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Path.of(a[0]), "*.xml")) {
            ds.forEach(files::add);
        }
        files.sort(Comparator.comparing(Path::toString));
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        for (Path f : files) {
            Element r = db.parse(f.toFile()).getDocumentElement();
            classes++;
            tests += Integer.parseInt(r.getAttribute("tests"));
            fail += Integer.parseInt(r.getAttribute("failures"));
            err += Integer.parseInt(r.getAttribute("errors"));
            skip += Integer.parseInt(r.getAttribute("skipped"));
            NodeList tcs = r.getElementsByTagName("testcase");
            for (int i = 0; i < tcs.getLength(); i++) {
                Element tc = (Element) tcs.item(i);
                names.add(tc.getAttribute("classname") + "." + tc.getAttribute("name"));
                if (tc.getElementsByTagName("failure").getLength() > 0 || tc.getElementsByTagName("error").getLength() > 0) {
                    System.out.println("FAILED " + tc.getAttribute("classname") + " " + tc.getAttribute("name"));
                }
            }
            NodeList so = r.getElementsByTagName("system-out");
            for (int i = 0; i < so.getLength(); i++) {
                if (so.item(i).getParentNode() != r) {
                    continue;
                }
                so.item(i).getTextContent().lines()
                        .filter(l -> l.startsWith("MIXED_GAS") || l.startsWith("LIQUID_JUNCTION"))
                        .forEach(lines::add);
            }
        }
        System.out.printf("classes=%d tests=%d failures=%d errors=%d skipped=%d%n", classes, tests, fail, err, skip);
        List<String> norm = new ArrayList<>();
        for (String l : lines) {
            norm.add(l.replaceAll("(?<![A-Za-z])ms=[0-9]+", "ms=*")
                    .replaceAll("allocatedMB=[0-9.]+", "allocatedMB=*")
                    .replaceAll("bytes=[0-9]+", "bytes=*"));
        }
        Files.writeString(Path.of(a[1]), String.join(nl, norm) + nl);
        Collections.sort(names);
        Files.writeString(Path.of(a[2]), String.join(nl, names) + nl);
        if (a.length > 3) {
            Files.writeString(Path.of(a[3]), String.join(nl, lines) + nl);
        }
        System.out.println("junction lines " + lines.size());
    }
}
