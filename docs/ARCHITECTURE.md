# Architecture

Etichette Custom is a small Swing application, but a few invariants are kept
strict because the program controls serialized physical output.

## 1. A label is data

`Etichetta` owns the physical size, a list of `Campo` content sources and a list
of `Elemento` visual elements. An element references a source by identifier
instead of owning a second copy of its value.

That distinction enables two important workflows:

- QR, barcode and human-readable text can share one source and therefore cannot
  drift into different values;
- another visual element can be made independent and use a fixed value, a
  print-time value or a separate sequence in the same label.

Storage format v4 persists text presentation state including preferred line
count. Older v3 files remain readable and default the new presentation fields
without changing their stored source data.

## 2. Sequences fail closed

`Serie` splits a value into an immutable prefix and a configurable numeric
window. A run is validated in full before the first page is printed. If the
requested copies would overflow any window, the run is rejected instead of
wrapping or carrying into the prefix.

Previewing a range never changes state. `Etichetta.consumaProgressivi()` is
called only after the print job reports success.

## 3. Screen and output share geometry

`app.render.Disegno` is the common renderer for previews, the editor canvas,
raster export and Java printing. `Ingombri` defines the visible bounds used by
selection, hit-testing, drag limits and rotation.

The 0°, 90°, 180° and 270° transforms are therefore shared invariants rather
than separate UI implementations. The editor exposes those states through one
90-degree rotation action. SVG applies the same local-origin compensation so
its physical geometry matches the raster path.

Readable-text presentation is also kept separate from source data. Separator
hiding and line wrapping operate on the displayed string; QR and Code 128
continue to consume the exact source value.

## 4. Print preparation is separate from editing

The normal production flow deliberately keeps layout manipulation away from the
print path:

```text
Gallery -> Print preparation -> system print dialog
                    |
                    +-> Export
```

Layout editing is an explicit secondary action. `Operatore` exposes only values
that belong to the current run: fixed values are omitted, print-time values are
requested, sequential sources show their starting value and outgoing range, and
copy count remains a plain text field.

Sequence-window configuration belongs to the editor and is not repeated before
every print.

## 5. The editor uses progressive disclosure

`Banco` is a direct-manipulation workspace. `Foglio` handles selection, drag,
resize, snapping and zoom; `Proprieta` renders the contextual inspector.

The default surface is intentionally small:

- the editor opens with no element selected;
- the inspector stays quiet until selection;
- exact measurements are hidden behind an explicit control;
- QR correction settings are hidden behind QR options;
- content behavior and sequence-window details are hidden until requested;
- shared-content detach/link controls are shown only after opening the compact
  shared-content control;
- label orientation and advanced data management live in one overflow menu.

The model remains complete even when the UI does not expose every property at
once.

## 6. Persistence is recoverable

Layouts are written through temporary files before replacement. Settings use
the same atomic-write approach. The daily register is append-only and readable
without the application.

The configured label and log directories are user settings; production paths
are not compiled into the program. A fresh empty archive receives one editable
example and is never repopulated after the user has saved their own state.

## 7. UI layout is tested as behaviour

Swing layout regressions are treated as real defects. The verification suite
checks control visibility, first-run surface complexity, direct resize,
progressive disclosure, readable text behavior and enlarged-font profiles in
addition to model and renderer tests.

The UI design system lives in `app.stile.Stile`; screens use its spacing, fonts,
semantic colors and scaling helpers. Major side panels cap their physical width
at large UI scales so Windows HiDPI does not consume the entire canvas.

Native Windows graphical runs retain PNG evidence for visual review. A green
layout assertion is necessary but is not treated as a substitute for looking at
the rendered screens.

## 8. Release verification

`./verify.sh` is the local release gate. It:

1. compiles application and tests for Java 8 with warnings as errors;
2. runs the historical core suite;
3. runs multi-data, persistence, undo/redo, text and rotation regressions;
4. runs Swing audits under Xvfb when available;
5. exercises enlarged UI profiles;
6. generates controlled QR/barcode samples;
7. builds and validates the standalone JAR manifest.

GitHub Actions adds a native Windows job that compiles the same sources and runs
core plus graphical audits at several UI scales. Release publishing depends on
both Linux and Windows verification.

The workflow also supports `ETICHETTE_PRIVATE_DENYLIST`, which rejects configured
private identifiers if they appear in tracked text files.
