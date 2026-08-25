# Architecture

Etichette Custom is a small Swing application, but a few invariants are kept
strict because the program controls serialized physical output.

## 1. A label is data

`Etichetta` owns the physical size, a list of `Campo` data sources and a list of
`Elemento` visual elements. An element references a field by identifier instead
of owning a second copy of its value.

That distinction enables two important workflows:

- QR and human-readable text can share one source and therefore cannot diverge;
- another QR, text or barcode can be made independent and advance a different
  sequence in the same print run.

Storage format v2 persists the sequence state with the field that owns it.
Format v1 is still accepted and upgraded on read.

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
than separate UI behaviours. SVG applies the same local-origin compensation so
its physical geometry matches the raster path.

## 4. Operator mode is separate from editing

The default production flow deliberately avoids exposing layout manipulation:

```text
Gallery -> Operator mode -> system print dialog
              |
              +-> Export
```

Layout editing is an explicit secondary action. This prevents an ordinary print
run from accidentally moving or resizing an element.

`Operatore` groups controls by unique data source, not by visual element. If QR
and text use the same field, only one input is shown.

## 5. Persistence is recoverable

Layouts are written through temporary files before replacement. Settings use
the same atomic-write approach. The daily register is append-only and readable
without the application.

The configured label and log directories are user settings; production paths
are not compiled into the program.

## 6. UI layout is tested as behaviour

Swing layout regressions are treated as real defects. The verification suite
checks minimum control widths, long paths, long semantic names, readable manual
viewports and enlarged-font profiles in addition to model and renderer tests.

The UI design system lives in `app.stile.Stile`; screens should use its spacing,
fonts, semantic colours and scaling helpers instead of introducing local pixel
constants where avoidable.

## 7. Release verification

`./verify.sh` is the release gate. It:

1. compiles application and tests for Java 8 with warnings as errors;
2. runs the historical core suite;
3. runs multi-data, persistence, undo/redo and rotation regressions;
4. runs Swing audits under Xvfb when available;
5. exercises enlarged UI profiles;
6. generates independent QR/barcode samples;
7. builds and validates the standalone JAR manifest.

The GitHub workflow additionally supports `ETICHETTE_PRIVATE_DENYLIST`, which
rejects configured private identifiers if they appear in tracked text files.
