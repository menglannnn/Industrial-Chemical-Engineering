#!/usr/bin/env bash
# Runs one Gradle invocation under the build/gradle.lock protocol (M2 agent). Usage: gradle-locked.sh <log-file> <gradle args...>
set -u
cd "D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/stoic-bell-ae6019" || exit 2
log="$1"; shift
waited=0
while [ -e build/gradle.lock ]; do
  if [ $waited -eq 0 ]; then echo "waiting for build/gradle.lock: $(cat build/gradle.lock)"; fi
  sleep 15; waited=$((waited+15))
  if [ $waited -ge 3600 ]; then echo "lock still held after 1 h; giving up"; exit 3; fi
done
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
if "$JAVA_HOME/bin/jps" -lvm 2>/dev/null | grep -E "fml.modFolders|runMcpClient|runClient" >/dev/null; then
  echo "a dev client is running; refusing to start Gradle"; "$JAVA_HOME/bin/jps" -lvm | grep -E "fml.modFolders|runMcpClient|runClient" | cut -c1-200; exit 4
fi
mkdir -p build
echo "fl-48fb5678 $(date '+%Y-%m-%d %H:%M:%S')" > build/gradle.lock
trap 'rm -f build/gradle.lock' EXIT INT TERM
start=$(date +%s)
./gradlew "$@" --offline --console=plain > "$log" 2>&1
status=$?
echo "exit=$status seconds=$(( $(date +%s) - start ))" >> "$log"
echo "exit=$status seconds=$(( $(date +%s) - start ))"
exit $status
