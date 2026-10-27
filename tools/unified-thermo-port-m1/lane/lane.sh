#!/usr/bin/env bash
# Gradle-free lane of the M1 port (unified thermo port, batch 2026-09-24-coolprop-low-temperature): compiles a source
# tree's science/** main sources plus listed test sources with javac, and runs JUnit 5 classes through Launch.java,
# printing each class's own output. Used to record the printed numbers of the engine families on branch 9aaafd4 (from a
# git archive extract) and to compare them with the port's (run on the Gradle-compiled classes). No timing.
# Usage, from Git Bash:
#   lane.sh build <src-root> <out-dir> <test path relative to <src-root>/test/java/com/wormzjl/createcheme>...
#   lane.sh run <class path entries separated by ';' (Windows form)> <TestClass>...
set -euo pipefail
LANE="$(cd "$(dirname "$0")" && pwd)"
JDK="/c/Program Files/Java/jdk-21.0.11/bin"
G=/c/Users/wormz/.gradle/caches/modules-2/files-2.1
m() { cygpath -m "$1"; }
jar() { m "$(find "$G/$1" -name "$2" | head -1)"; }
JARS="$(jar com.google.code.gson gson-2.10.1.jar);$(jar org.ejml ejml-core-0.44.0.jar);$(jar org.ejml ejml-ddense-0.44.0.jar);$(jar org.ejml ejml-dsparse-0.44.0.jar)"
JARS="$JARS;$(jar org.junit.jupiter junit-jupiter-api-5.11.4.jar);$(jar org.junit.jupiter junit-jupiter-params-5.11.4.jar);$(jar org.junit.jupiter junit-jupiter-engine-5.11.4.jar)"
JARS="$JARS;$(jar org.junit.platform junit-platform-commons-1.11.4.jar);$(jar org.junit.platform junit-platform-engine-1.11.4.jar);$(jar org.junit.platform junit-platform-launcher-1.11.4.jar)"
JARS="$JARS;$(jar org.opentest4j opentest4j-1.3.0.jar);$(jar org.apiguardian apiguardian-api-1.1.2.jar)"
MODE="$1"; shift
if [ "$MODE" = build ]; then
  ROOT="$1"; OUT="$2"; shift 2
  rm -rf "$OUT"; mkdir -p "$OUT"
  mapfile -t SOURCES < <(find "$ROOT/main/java/com/wormzjl/createcheme/science" -name '*.java')
  for t in "$@"; do
    p="$ROOT/test/java/com/wormzjl/createcheme/$t"
    if [ -d "$p" ]; then mapfile -t -O "${#SOURCES[@]}" SOURCES < <(find "$p" -maxdepth 1 -name '*.java'); else SOURCES+=("$p"); fi
  done
  SOURCES+=("$LANE/Launch.java")
  ARGS="$OUT.javac.txt"
  { printf -- '-nowarn\n-encoding\nUTF-8\n-proc:none\n-d\n"%s"\n-cp\n"%s"\n' "$(m "$OUT")" "$JARS"; for f in "${SOURCES[@]}"; do printf '"%s"\n' "$(m "$f")"; done; } > "$ARGS"
  "$JDK/javac" "@$(m "$ARGS")"
  echo "compiled ${#SOURCES[@]} sources into $OUT"
elif [ "$MODE" = run ]; then
  CP="$1"; shift
  ARGS="$(mktemp)"
  printf -- '-cp\n"%s;%s"\n' "$CP" "$JARS" > "$ARGS"
  "$JDK/java" -Xshare:off "@$(m "$ARGS")" Launch "$@"
  rm -f "$ARGS"
else
  echo "unknown mode $MODE" >&2; exit 2
fi
