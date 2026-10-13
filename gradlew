#!/usr/bin/env sh
# Offline fallback launcher for environments without an installed Gradle distribution.
# Supports clean, build, test, check, logicTest, and runGame using the JDK only.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT"
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
TASKS="$*"
case "$TASKS" in *runGame*) TASK=runGame;; *logicTest*|*test*|*check*) TASK=logicTest;; *) TASK=build;; esac
[ "$TASKS" = "clean" ] && { rm -rf build; exit 0; }
rm -rf build/classes && mkdir -p build/classes/java/main build/classes/java/test
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -encoding UTF-8 -d build/classes/java/main
if [ "$TASK" = logicTest ]; then
  find src/test/java -name '*.java' -print0 | xargs -0 -r javac --release 17 -encoding UTF-8 -cp build/classes/java/main -d build/classes/java/test
  java -Djava.awt.headless=true -cp build/classes/java/main:build/classes/java/test com.lostvault.GameLogicTest
elif [ "$TASK" = runGame ]; then java -cp build/classes/java/main com.lostvault.Main
else echo 'BUILD SUCCESSFUL (offline JDK fallback; Gradle itself is not installed)'; fi
