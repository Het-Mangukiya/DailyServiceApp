#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

JBR_CANDIDATES=(
  "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
)

ensure_supported_jdk() {
  local java_version
  java_version="$(java -version 2>&1 | head -n 1)"

  # AGP/Gradle tasks can fail on very new JDKs (e.g., JDK 25).
  if [[ "$java_version" == *"version \"25"* ]]; then
    for jbr in "${JBR_CANDIDATES[@]}"; do
      if [[ -d "$jbr" ]]; then
        export JAVA_HOME="$jbr"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "Using Android Studio JBR: $JAVA_HOME"
        return
      fi
    done
    echo "Warning: JDK 25 detected and no Android Studio JBR found."
    echo "Set JAVA_HOME to JDK 17/21 before running quality checks."
  fi
}

ensure_supported_jdk

echo "Running lint..."
./gradlew :app:lintDebug

echo "Running unit tests..."
./gradlew :app:testDebugUnitTest

echo "Building debug APK..."
./gradlew :app:assembleDebug

echo "Building release APK..."
./gradlew :app:assembleRelease

echo "Quality gate passed."
