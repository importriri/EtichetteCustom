#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

LINT=(-Xlint:all,-serial,-this-escape,-options -Werror)
JAVA8=(--release 8 -encoding UTF-8)

step() {
    printf '\n== %s ==\n' "$1"
}

step "Clean"
rm -rf build prove-build dist
mkdir -p build prove-build dist

mapfile -t SOURCES < <(find src -name '*.java' -print | sort)
mapfile -t TESTS < <(find prove -name '*.java' -print | sort)

step "Compile application (Java 8, warnings as errors)"
javac -d build "${JAVA8[@]}" "${LINT[@]}" "${SOURCES[@]}"

step "Compile tests"
javac -d prove-build -cp build "${JAVA8[@]}" "${LINT[@]}" "${TESTS[@]}"

step "Core and release regressions"
java -Djava.awt.headless=true -cp build:prove-build prove.Tutte
java -Djava.awt.headless=true -cp build:prove-build prove.ProvaRelease

step "Off-screen graphical audits"
for scale in 12 15 18 24; do
    java -Djava.awt.headless=true -cp build:prove-build prove.ProvaEditorGrafico "$scale"
    java -Djava.awt.headless=true -cp build:prove-build prove.ProvaFlussoGrafico "$scale"
done

step "Display-backed Swing audits"
if command -v xvfb-run >/dev/null 2>&1; then
    xvfb-run -a java -cp build:prove-build prove.ProvaInterfaccia
    for scale in 12 15 18 24; do
        xvfb-run -a java -cp build:prove-build prove.ProvaScala "$scale"
    done
    xvfb-run -a java -cp build:prove-build prove.ProvaDialoghi
else
    printf 'xvfb-run is unavailable; display-backed audits were skipped.\n'
fi

step "Reference output and samples"
java -Djava.awt.headless=true -cp build:prove-build prove.Provino provino.png
java -Djava.awt.headless=true -cp build:prove-build prove.Campioni campioni

step "Standalone executable JAR"
jar --create --file dist/EtichetteCustom.jar --main-class app.Avvio -C build .
jar --list --file dist/EtichetteCustom.jar | grep -qx 'app/Avvio.class'
unzip -p dist/EtichetteCustom.jar META-INF/MANIFEST.MF | grep -q '^Main-Class: app.Avvio'
sha256sum dist/EtichetteCustom.jar
printf '\nEtichette Custom verification: PASS\n'
