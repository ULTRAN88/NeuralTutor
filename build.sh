#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
rm -rf "$ROOT/build/classes" "$ROOT/dist"
mkdir -p "$ROOT/build/classes" "$ROOT/dist"

find "$ROOT/src" -name "*.java" > "$ROOT/build/sources.txt"
javac --release 21 -cp "$ROOT/lib/sqlite-jdbc.jar" -d "$ROOT/build/classes" @"$ROOT/build/sources.txt"

cat > "$ROOT/build/manifest.mf" <<EOF
Manifest-Version: 1.0
Main-Class: com.neuraltutor.Main
EOF

# Copy compiled classes, then overlay the SQLite JDBC classes/resources.
mkdir -p "$ROOT/build/fat"
cp -R "$ROOT/build/classes/." "$ROOT/build/fat/"
TMP="$ROOT/build/sqlite_extract"
rm -rf "$TMP"
mkdir -p "$TMP"
(cd "$TMP" && jar xf "$ROOT/lib/sqlite-jdbc.jar")
cp -R "$TMP/org" "$ROOT/build/fat/"
mkdir -p "$ROOT/build/fat/META-INF/services"
cp "$TMP/META-INF/services/java.sql.Driver" "$ROOT/build/fat/META-INF/services/java.sql.Driver"

jar --create --file "$ROOT/dist/NeuralTutor-Pro.jar" \
    --manifest "$ROOT/build/manifest.mf" \
    -C "$ROOT/build/fat" .

echo "Built $ROOT/dist/NeuralTutor-Pro.jar"