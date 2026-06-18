#!/usr/bin/env bash
# Verify Java 25 before Maven builds. Sourced by java-test.sh and CI helpers.
set -euo pipefail

need_major=25
ver="${JAVA_HOME:+$(PATH="$JAVA_HOME/bin:$PATH" java -version 2>&1 | head -1)}"
ver="${ver:-$(java -version 2>&1 | head -1)}"

if ! java -version 2>&1 | grep -qE '"25\.'; then
  echo "ERROR: Java 25 LTS required for java-service (Spring Boot 4.1)." >&2
  echo "  Detected: $(java -version 2>&1 | head -1)" >&2
  echo "  Fix (Homebrew):" >&2
  echo "    brew install openjdk@25" >&2
  echo "    export JAVA_HOME=\"\$(brew --prefix openjdk@25)/libexec/openjdk.jdk/Contents/Home\"" >&2
  echo "    export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >&2
  echo "  Then from repo root: make java-test" >&2
  echo "  Or: cd java-service && mvn test" >&2
  exit 1
fi