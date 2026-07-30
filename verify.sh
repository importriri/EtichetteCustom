#!/usr/bin/env bash
set -euo pipefail

for command_name in java javac ant xvfb-run timeout unzip jar python3 rsync; do
    command -v "${command_name}" >/dev/null || {
        printf 'missing command: %s\n' "${command_name}" >&2
        exit 2
    }
done

if [[ -n "${ETICHETTE_PRIVATE_DENYLIST:-}" ]]; then
    python3 tools/private_identifier_guard.py
elif [[ "${CI:-}" == "true" ]]; then
    printf 'ETICHETTE_PRIVATE_DENYLIST is required in CI\n' >&2
    exit 2
else
    printf 'private identifier guard skipped: no local denylist supplied\n'
fi

rm -rf out build dist
mkdir -p out

javac --release 8 -encoding UTF-8 -Xlint:-options \
    -d out $(find src test -name '*.java')
rsync -a --exclude='*.java' src/ out/

xvfb-run -a bash -eu -o pipefail <<'TESTS'
java -cp out app.core.QrCodeTest
java -cp out app.core.SerialWindowTest
java -cp out app.core.LabelModelTest
java -cp out app.core.ExportTest
java -cp out app.core.LayoutTest
java -cp out app.core.PrintTest
java -cp out app.ui.ManualRenderTest
java -cp out app.config.LogTargetTest
java -cp out app.ui.StartupSmokeTest
java -cp out app.ui.StartupSmokeTest --saved
java -cp out app.ui.WindowsLookTest
TESTS

xvfb-run -a -s '-screen 0 1366x768x24' \
    java -cp out app.ui.LayoutAuditTest
xvfb-run -a -s '-screen 0 1600x900x24' \
    java -Detichette.uiscale=1.5 -cp out app.ui.LayoutAuditTest
xvfb-run -a -s '-screen 0 1600x900x24 -dpi 144' \
    java -cp out app.ui.LayoutAuditTest

ant clean jar

test -s dist/EtichetteCustom.jar
unzip -p dist/EtichetteCustom.jar META-INF/MANIFEST.MF \
    | tr -d '\r' \
    | grep -Fx 'Main-Class: app.Main'
jar tf dist/EtichetteCustom.jar | grep -Fx 'app/Main.class'
jar tf dist/EtichetteCustom.jar | grep -Fx 'app/docs/MANUAL.it.md'
jar tf dist/EtichetteCustom.jar | grep -Fx 'app/docs/MANUAL.en.md'

xvfb-run -a bash -eu -c '
java -jar dist/EtichetteCustom.jar >/tmp/package-startup.log 2>&1 &
pid=$!
sleep 5
kill -0 "$pid"
kill -TERM "$pid"
for attempt in 1 2 3 4 5; do
    kill -0 "$pid" 2>/dev/null || exit 0
    sleep 1
done
kill -KILL "$pid"
wait "$pid" || true
'

printf 'Etichette Custom verification: PASS\n'
sha256sum dist/EtichetteCustom.jar
