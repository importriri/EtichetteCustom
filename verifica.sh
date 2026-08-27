#!/usr/bin/env bash
set -euo pipefail
RADICE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)";cd "$RADICE"
LINT=(-Xlint:all,-serial,-this-escape,-options -Werror);JAVA8=(--release 8 -encoding UTF-8)
passo(){ printf '\n== %s ==\n' "$1"; }
passo pulizia;rm -rf build prove-build dist;mkdir -p build prove-build dist
mapfile -t SORGENTI < <(find src -name '*.java' -print|sort);mapfile -t PROVE < <(find prove -name '*.java' -print|sort)
passo "compilo l'applicazione (Java 8, avvisi = errori)";javac -d build "${JAVA8[@]}" "${LINT[@]}" "${SORGENTI[@]}"
passo "compilo le prove";javac -d prove-build -cp build "${JAVA8[@]}" "${LINT[@]}" "${PROVE[@]}"
passo "regressioni del core";java -Djava.awt.headless=true -cp build:prove-build prove.Tutte;java -Djava.awt.headless=true -cp build:prove-build prove.ProvaRelease
passo "audit grafico off-screen";for scala in 12 15 18 24;do java -Djava.awt.headless=true -cp build:prove-build prove.ProvaEditorGrafico "$scala";done
if command -v xvfb-run >/dev/null 2>&1;then passo "audit dell'interfaccia";xvfb-run -a java -cp build:prove-build prove.ProvaInterfaccia;for scala in 12 15 18 24;do xvfb-run -a java -cp build:prove-build prove.ProvaScala "$scala";done;xvfb-run -a java -cp build:prove-build prove.ProvaDialoghi;else passo "audit dell'interfaccia";printf 'xvfb-run non disponibile: audit con finestre saltati.\n';fi
passo "provino e campioni";java -Djava.awt.headless=true -cp build:prove-build prove.Provino provino.png;java -Djava.awt.headless=true -cp build:prove-build prove.Campioni campioni
passo "JAR eseguibile";jar --create --file dist/EtichetteCustom.jar --main-class app.Avvio -C build .;jar --list --file dist/EtichetteCustom.jar|grep -qx 'app/Avvio.class';unzip -p dist/EtichetteCustom.jar META-INF/MANIFEST.MF|grep -q '^Main-Class: app.Avvio';sha256sum dist/EtichetteCustom.jar;printf '\nEtichette Custom verification: PASS\n'
