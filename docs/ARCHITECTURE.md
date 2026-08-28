# Architecture

Etichette Custom is intentionally small. The design is built around a few
invariants that keep printed output predictable.

## Model

`Etichetta` owns the physical label size, content sources (`Campo`) and visual
elements (`Elemento`). Elements reference a source instead of storing another
copy of its value.

This allows QR, barcode and readable text to share one exact source while still
having independent layout and presentation.

Readable text may also reference one logical part of that source. These derived
parts are presentation only: splitting a code into movable text blocks never
changes the value consumed by QR, barcode, sequences or print logging.

Storage format v5 persists this presentation state. Older v3 and v4 files remain
readable and default to the complete source.

## Sequences

`Serie` separates an immutable prefix from a configurable numeric window. A full
print run is validated before printing starts. Overflow fails closed instead of
wrapping into the prefix.

Previewing does not mutate state. Sequence counters are consumed only after a
successful print job.

## Rendering

`app.render.Disegno` is shared by the editor, preview, raster export and Java
printing. `Ingombri` uses the same text and rotation geometry for selection,
hit-testing and drag bounds.

Readable-text transformations happen before layout. QR and Code 128 always use
the exact source value.

## Editor

`Banco` owns the editing workspace, `Foglio` handles direct manipulation and
`Proprieta` provides the contextual inspector.

The default surface exposes common actions only. Measurements, QR details,
content behavior and shared-source management are opened when needed. The model
remains complete even when those controls are not permanently visible.

## Print flow

The production path is separate from layout editing:

```text
Gallery -> Print preparation -> system print dialog
                    |
                    +-> Export
```

Print preparation contains only run-time input: copy count, requested values and
sequence ranges. Fixed data and layout settings remain in the model/editor.

## Persistence

Layouts and settings use replace-after-write persistence. The daily print log is
append-only and readable without the application. User paths are settings, not
compiled-in production values.

A fresh empty archive receives one editable example. Existing user state is not
reseeded.

## Verification

`./verify.sh` is the release gate. It compiles for Java 8 with warnings treated
as errors, runs model/rendering regressions, exercises Swing layouts, validates
QR/barcode samples and builds the standalone JAR.

GitHub Actions repeats the gate on Linux and native Windows. Graphical audits run
at several UI scales and retain PNG evidence for visual inspection. Release
publishing depends on both platforms passing.
