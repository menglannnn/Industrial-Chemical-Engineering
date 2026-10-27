import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Dead-code and same-argument finder over src/main, src/test and src/fluidGameTest with javac attribution.
 *  Usage: java DeadCode (worktree) (classpath) > report.tsv
 *  Liveness: roots are annotated entry points (the @Test family, @SubscribeEvent, @Mod, @GameTest...), main methods,
 *  overrides of methods outside the project, @MethodSource names. An element is live when a live element references
 *  it (self-references do not count); a class is live when a member is live; an overrider is live when the overridden
 *  method is live; enum constants are live when values()/valueOf() of their enum is referenced. Two passes: roots from
 *  src/main only, then all roots; "TESTONLY" marks main declarations only reached from tests. LIVE methods are printed
 *  only when every call site passes the same text for some argument (SAMEARG). */
public class DeadCode {
  record Decl(Element e, String set, String file, long line, long endLine, String kind) {}
  static Trees trees; static Elements els; static Types types;
  static Map<Element, Decl> decls = new LinkedHashMap<>();
  static Map<Element, Set<Element>> edges = new HashMap<>();
  static Set<Element> rootsMain = new HashSet<>(), rootsTest = new HashSet<>();
  static Map<Element, Map<Integer, Set<String>>> argText = new HashMap<>();
  static Map<Element, Integer> callSites = new HashMap<>();
  static Set<Element> hasOverrider = new HashSet<>();
  static Set<TypeElement> enumsEnumerated = new HashSet<>();
  static Set<String> methodSourceNames = new HashSet<>();
  static final Set<String> ROOT_ANN = Set.of("Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "BeforeEach", "BeforeAll", "AfterEach", "AfterAll", "SubscribeEvent", "GameTest", "GameTestGenerator", "GameTestHolder", "EventBusSubscriber", "Mod", "Nested", "TestTemplate");

  public static void main(String[] args) throws Exception {
    Path root = Paths.get(args[0]); String cp = args[1];
    List<Path> files = new ArrayList<>(); Map<Path, String> setOf = new HashMap<>();
    for (String s : new String[] {"src/main/java", "src/test/java", "src/fluidGameTest/java"}) {
      Path d = root.resolve(s); if (!Files.isDirectory(d)) continue;
      String set = s.startsWith("src/main") ? "main" : s.startsWith("src/test") ? "test" : "gt";
      try (var st = Files.walk(d)) { st.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> { files.add(p); setOf.put(p, set); }); }
    }
    JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
    var fm = jc.getStandardFileManager(null, null, StandardCharsets.UTF_8);
    var units = fm.getJavaFileObjectsFromPaths(files);
    List<String> opts = List.of("-cp", cp, "-proc:none", "-encoding", "UTF-8", "-nowarn", "-Xmaxerrs", "20", "-implicit:none");
    JavacTask task = (JavacTask) jc.getTask(null, fm, d -> { if (d.getKind() == Diagnostic.Kind.ERROR) System.err.println(d); }, opts, null, units);
    Iterable<? extends CompilationUnitTree> cus = task.parse();
    task.analyze();
    trees = Trees.instance(task); els = task.getElements(); types = task.getTypes();
    for (CompilationUnitTree cu : cus) {
      Path p = Paths.get(cu.getSourceFile().toUri());
      new DeclScanner(cu, setOf.get(p), root.relativize(p).toString().replace('\\', '/')).scan(cu, null);
    }
    for (CompilationUnitTree cu : cus) new UseScanner(cu, setOf.get(Paths.get(cu.getSourceFile().toUri()))).scan(cu, null);
    for (Decl d : decls.values()) {
      if (!(d.e instanceof ExecutableElement m) || m.getKind() != ElementKind.METHOD) continue;
      TypeElement owner = (TypeElement) m.getEnclosingElement();
      for (TypeMirror sup : allSupertypes(owner.asType())) {
        Element se = types.asElement(sup); if (!(se instanceof TypeElement st)) continue;
        for (Element cand : st.getEnclosedElements()) {
          if (cand instanceof ExecutableElement sm && sm.getKind() == ElementKind.METHOD && els.overrides(m, sm, owner)) {
            if (decls.containsKey(sm)) { edge(sm, m); hasOverrider.add(sm); } else { rootsMain.add(m); }
          }
        }
      }
    }
    for (Decl d : decls.values()) if (d.e instanceof ExecutableElement m && methodSourceNames.contains(m.getSimpleName().toString())) rootsTest.add(m);
    for (Decl d : decls.values()) if (d.e.getKind() == ElementKind.ENUM_CONSTANT && enumsEnumerated.contains((TypeElement) d.e.getEnclosingElement())) edge(d.e.getEnclosingElement(), d.e);
    Set<Element> liveMain = propagate(rootsMain), liveAll = propagate(union(rootsMain, rootsTest));
    System.out.println("status\tkind\tset\tfile\tline\tend\tlines\telement\tnote");
    for (Decl d : decls.values()) {
      String status = liveAll.contains(d.e) ? (liveMain.contains(d.e) ? "LIVE" : (d.set.equals("main") ? "TESTONLY" : "LIVE")) : "DEAD";
      String note = sameArgs(d);
      if (status.equals("LIVE") && note.isEmpty()) continue;
      System.out.println(status + "\t" + d.kind + "\t" + d.set + "\t" + d.file + "\t" + d.line + "\t" + d.endLine + "\t" + (d.endLine - d.line + 1) + "\t" + sig(d.e) + "\t" + note);
    }
  }
  static String sameArgs(Decl d) {
    if (!(d.e instanceof ExecutableElement m)) return "";
    Map<Integer, Set<String>> a = argText.get(m); if (a == null) return "";
    if (hasOverrider.contains(m)) return "";
    int n = callSites.getOrDefault(m, 0); if (n < 2) return "";
    StringBuilder sb = new StringBuilder();
    for (var en : a.entrySet()) if (en.getValue().size() == 1) sb.append("arg").append(en.getKey()).append("=").append(en.getValue().iterator().next().replaceAll("\\s+", " ")).append(" ");
    return sb.length() == 0 ? "" : "SAMEARG(" + n + " calls) " + sb.toString().trim();
  }
  static List<TypeMirror> allSupertypes(TypeMirror t) {
    List<TypeMirror> out = new ArrayList<>(); Deque<TypeMirror> q = new ArrayDeque<>(types.directSupertypes(t)); Set<String> seen = new HashSet<>();
    while (!q.isEmpty()) { TypeMirror s = q.pop(); if (!seen.add(s.toString())) continue; out.add(s); q.addAll(types.directSupertypes(s)); }
    return out;
  }
  static Set<Element> union(Set<Element> a, Set<Element> b) { Set<Element> s = new HashSet<>(a); s.addAll(b); return s; }
  static Set<Element> propagate(Set<Element> roots) {
    Set<Element> live = new HashSet<>(); Deque<Element> q = new ArrayDeque<>(roots);
    while (!q.isEmpty()) {
      Element e = q.pop(); if (!live.add(e)) continue;
      for (Element t : edges.getOrDefault(e, Set.of())) q.push(t);
      Element owner = e.getEnclosingElement();
      while (owner != null && !(owner instanceof PackageElement)) { if (decls.containsKey(owner)) { q.push(owner); break; } owner = owner.getEnclosingElement(); }
    }
    return live;
  }
  static void edge(Element from, Element to) { if (from == null || to == null || from == to) return; edges.computeIfAbsent(from, k -> new HashSet<>()).add(to); }
  static String sig(Element e) {
    if (e instanceof TypeElement t) return t.getQualifiedName().toString();
    Element o = e.getEnclosingElement(); String on = o instanceof TypeElement t ? t.getQualifiedName().toString() : String.valueOf(o);
    return on + "#" + e;
  }
  static Element declaredEnclosing(TreePath p) {
    for (TreePath q = p.getParentPath(); q != null; q = q.getParentPath()) {
      Tree t = q.getLeaf();
      boolean classMember = t instanceof VariableTree && q.getParentPath() != null && q.getParentPath().getLeaf() instanceof ClassTree;
      if (t instanceof MethodTree || t instanceof ClassTree || classMember) {
        Element e = trees.getElement(q); if (e != null && decls.containsKey(e)) return e;
      }
    }
    return null;
  }
  static class DeclScanner extends TreePathScanner<Void, Void> {
    final CompilationUnitTree cu; final String set, file; final LineMap lm; final SourcePositions sp;
    DeclScanner(CompilationUnitTree cu, String set, String file) { this.cu = cu; this.set = set; this.file = file; lm = cu.getLineMap(); sp = trees.getSourcePositions(); }
    void add(Tree t, String kind) {
      Element e = trees.getElement(getCurrentPath()); if (e == null) return;
      long s = sp.getStartPosition(cu, t), en = sp.getEndPosition(cu, t); if (s < 0 || en < 0) return;
      decls.put(e, new Decl(e, set, file, lm.getLineNumber(s), lm.getLineNumber(en), kind));
      boolean root = false;
      for (AnnotationMirror a : e.getAnnotationMirrors()) {
        String n = a.getAnnotationType().asElement().getSimpleName().toString();
        if (ROOT_ANN.contains(n)) root = true;
        if (n.equals("MethodSource")) for (var v : a.getElementValues().values()) for (String part : v.toString().replaceAll("[{}\"]", "").split(",")) methodSourceNames.add(part.trim().replaceAll(".*#", ""));
      }
      if (e instanceof ExecutableElement m && m.getSimpleName().contentEquals("main") && m.getModifiers().contains(Modifier.STATIC)) root = true;
      if (root) (set.equals("main") ? rootsMain : rootsTest).add(e);
      if (root && e instanceof TypeElement te) for (Element c : te.getEnclosedElements()) if (c.getKind() == ElementKind.CONSTRUCTOR) (set.equals("main") ? rootsMain : rootsTest).add(c);
    }
    @Override public Void visitClass(ClassTree t, Void v) { if (t.getSimpleName().length() > 0) add(t, t.getKind().name()); return super.visitClass(t, v); }
    @Override public Void visitMethod(MethodTree t, Void v) { add(t, t.getReturnType() == null ? "CTOR" : "METHOD"); return super.visitMethod(t, v); }
    @Override public Void visitVariable(VariableTree t, Void v) {
      if (getCurrentPath().getParentPath().getLeaf() instanceof ClassTree) {
        Element e = trees.getElement(getCurrentPath());
        add(t, e != null && e.getKind() == ElementKind.ENUM_CONSTANT ? "ENUMCONST" : e != null && e.getKind() == ElementKind.RECORD_COMPONENT ? "RECORDCOMP" : "FIELD");
        if (e != null && t.getInitializer() != null && e.getModifiers().contains(Modifier.STATIC) && (t.getInitializer().getKind() == Tree.Kind.METHOD_INVOCATION || t.getInitializer().getKind() == Tree.Kind.NEW_CLASS)) edge(trees.getElement(getCurrentPath().getParentPath()), e);
      }
      return super.visitVariable(t, v);
    }
  }
  static class UseScanner extends TreePathScanner<Void, Void> {
    final CompilationUnitTree cu; final String set; final SourcePositions sp; final CharSequence src;
    UseScanner(CompilationUnitTree cu, String set) throws Exception { this.cu = cu; this.set = set; sp = trees.getSourcePositions(); src = cu.getSourceFile().getCharContent(true); }
    void use(Tree t) {
      Element e = trees.getElement(getCurrentPath()); if (e == null) return;
      Element target = e;
      if (!decls.containsKey(target)) {
        if (e instanceof ExecutableElement m && m.getEnclosingElement() instanceof TypeElement te) {
          if (te.getKind() == ElementKind.RECORD) for (Element c : te.getEnclosedElements()) if ((c.getKind() == ElementKind.FIELD || c.getKind() == ElementKind.RECORD_COMPONENT) && c.getSimpleName().equals(m.getSimpleName()) && decls.containsKey(c)) target = c;
          if (te.getKind() == ElementKind.ENUM && (m.getSimpleName().contentEquals("values") || m.getSimpleName().contentEquals("valueOf"))) enumsEnumerated.add(te);
        }
        if (!decls.containsKey(target)) return;
      }
      Element from = declaredEnclosing(getCurrentPath());
      if (from == null) { (set.equals("main") ? rootsMain : rootsTest).add(target); return; }
      edge(from, target);
    }
    @Override public Void visitIdentifier(IdentifierTree t, Void v) { use(t); return super.visitIdentifier(t, v); }
    @Override public Void visitMemberSelect(MemberSelectTree t, Void v) { use(t); return super.visitMemberSelect(t, v); }
    @Override public Void visitMemberReference(MemberReferenceTree t, Void v) { use(t); return super.visitMemberReference(t, v); }
    @Override public Void visitNewClass(NewClassTree t, Void v) { use(t); args(t.getArguments()); return super.visitNewClass(t, v); }
    @Override public Void visitMethodInvocation(MethodInvocationTree t, Void v) { args(t.getArguments()); return super.visitMethodInvocation(t, v); }
    void args(List<? extends ExpressionTree> as) {
      Element e = trees.getElement(getCurrentPath()); if (!(e instanceof ExecutableElement m) || !decls.containsKey(m)) return;
      callSites.merge(m, 1, Integer::sum);
      Map<Integer, Set<String>> per = argText.computeIfAbsent(m, k -> new TreeMap<>());
      for (int i = 0; i < as.size(); i++) {
        long s = sp.getStartPosition(cu, as.get(i)), en = sp.getEndPosition(cu, as.get(i));
        per.computeIfAbsent(i, k -> new HashSet<>()).add(s >= 0 && en >= 0 ? src.subSequence((int) s, (int) en).toString() : "?");
      }
    }
  }
}
