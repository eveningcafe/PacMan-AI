#!/usr/bin/env bash
# Build + run PacMan-AI without NetBeans/ant.
#   ./build.sh                 -> build out/PacmanAI.jar
#   ./build.sh run [args...]   -> build, then run (e.g. ./build.sh run -albe 3 -repeat 1)
# Uses $JAVA_HOME if set, else ~/.jdks/openjdk-20, else javac on PATH.
set -euo pipefail
cd "$(dirname "$0")"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then JBIN="$JAVA_HOME/bin/"
elif [[ -x "$HOME/.jdks/openjdk-20/bin/javac" ]]; then JBIN="$HOME/.jdks/openjdk-20/bin/"
else JBIN=""; fi

rm -rf out && mkdir -p out/classes
"${JBIN}javac" --release 11 -encoding UTF-8 -nowarn \
    -cp commons-cli-1.3.1.jar -d out/classes src/projectai/*.java
cp -r src/images out/classes/
cp commons-cli-1.3.1.jar out/
printf 'Main-Class: projectai.Main\nClass-Path: commons-cli-1.3.1.jar\n' > out/manifest.mf
"${JBIN}jar" cfm out/PacmanAI.jar out/manifest.mf -C out/classes .
echo "Built out/PacmanAI.jar"

if [[ "${1:-}" == "run" ]]; then
    shift
    exec "${JBIN}java" -jar out/PacmanAI.jar "$@"
fi
