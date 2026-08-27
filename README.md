<div align="center">

# Etichette Custom

A standalone Java/Swing application for designing, preparing, exporting and
printing serialized labels with text, QR codes and Code 128 barcodes.

[![tests](https://github.com/importriri/etichette-custom/actions/workflows/tests.yml/badge.svg)](https://github.com/importriri/etichette-custom/actions/workflows/tests.yml)
[![lint](https://github.com/importriri/etichette-custom/actions/workflows/lint.yml/badge.svg)](https://github.com/importriri/etichette-custom/actions/workflows/lint.yml)

![Etichette Custom demo](docs/demo.gif)

</div>

## Workflow

Etichette Custom keeps routine printing separate from layout editing.

**Gallery → Operator mode → Print** is the normal production path. Choose a
label from its rendered preview, enter run-time values and copy count, verify the
outgoing sequence range, then open the operating-system print dialog.

**Gallery → Edit layout** opens a small direct-manipulation workspace. Select an
element, drag it to move it and drag its visible corner handles to resize it.
Numeric geometry stays hidden until exact millimetre values are actually needed.

A fresh installation starts with one 50 × 30 mm **Example** label. The
application does not repopulate a catalogue of templates on later starts.

| Gallery | Operator mode | Layout editor |
|---|---|---|
| ![Gallery](docs/screenshot-vetrina.png) | ![Operator mode](docs/screenshot-operatore.png) | ![Layout editor](docs/screenshot-editor.png) |

## Editor philosophy

The everyday editor deliberately avoids a wall of numeric controls.

- drag an element to move it;
- drag a blue corner handle to resize it;
- QR elements stay square while resizing;
- choose text alignment directly;
- use automatic wrapping or force 1, 2 or 3 lines;
- rotate through 0°, 90°, 180° and 270°;
- hide punctuation in readable text without changing QR/barcode data;
- open **Precise measurements** only when X/Y/width/font values are required.

Long readable codes prefer natural separators when wrapping and are never meant
to lose characters silently.

## Shared data without duplicated input

Visual elements and data are separate concepts. A QR and its readable text can
use the same source value, so the operator enters that code once. The editor
presents this as **Use the same code as** rather than exposing storage IDs.

An element can also be separated with **Use a different code** and receive its
own fixed value, print-time value or sequence.

Each data source can be:

- **fixed** — stored with the layout;
- **sequential** — its own independent numeric window and counter;
- **requested at print time** — entered for each run.

A single label can therefore contain several independent sequences. Every
sequence is validated before printing and refuses a run that would overflow its
configured numeric window.

## Source data and readable text

QR and Code 128 always use the exact source string. Readable text is only a
presentation of that value.

For example, the source may be:

```text
210150.022_02-01.262350009
```

The QR continues to encode the complete string even when the readable text is
configured to hide dots and separators or to split itself over multiple lines.

## Printing guarantees

The application follows a conservative transaction order:

1. validate the complete run;
2. open the operating-system print dialog;
3. print every page;
4. only after a successful print, consume sequence numbers;
5. persist the updated layout and append the daily log.

Cancelling the print dialog does **not** consume a number.

The page passed to Java printing has the physical label size and zero margins.
The print queue remains the one selected by the operating system.

## One renderer

Preview, editor canvas, PNG, PDF and Java printing all use the same drawing
code. SVG follows the same millimetre geometry and rotation rules.

That keeps element positions, wrapping, QR modules, barcode bars and rotations
consistent instead of maintaining separate screen and print layouts.

## Windows and Linux UI verification

The primary editor avoids `JSpinner` and platform-dependent native controls for
routine geometry. Its grid is drawn by the application and physical label
geometry remains millimetre-based regardless of desktop scaling.

CI builds the same Java 8-compatible sources on Linux and on a native Windows
runner. The graphical audit exercises several UI scales corresponding to common
100%, 125%, 150% and 200% profiles and retains PNG evidence for visual review.
The audit also checks that primary controls remain horizontally visible, text
presentation does not mutate source data and corner dragging really resizes a QR.

See [Windows UI audit](docs/windows-ui-audit.md) for the compatibility rules.

## Operator UI

The interface remains operator-first:

- real rendered previews in the gallery;
- protected print-preparation mode;
- semantic data names instead of storage identifiers;
- exact sequence ranges before printing;
- QR/barcode readability warnings in plain language;
- direct move/resize in the editor;
- undo and redo;
- unified settings for paths, printer metadata, manuals and project info;
- built-in Italian and English operator manuals.

The bundled examples and screenshots use synthetic data. Production identifiers
are not required in the source tree.

### Settings

Paths, printer metadata, manuals and project information live in one settings
dialog. Long paths remain readable and the built-in manual follows the same UI
scaling as the rest of the application.

![Settings](docs/screenshot-impostazioni.png)

## Export

| Output | Behaviour |
|---|---|
| Print | one physical-size page per label |
| PDF | one file containing the complete run |
| PNG | one raster image per label at the selected resolution |
| SVG | vector output with physical dimensions expressed in millimetres |

## Build and verify

Requirements: JDK 8 or newer. The full Linux UI audit also uses Xvfb.

```bash
./verify.sh
java -jar dist/EtichetteCustom.jar
```

`verify.sh` compiles for Java 8 with warnings as errors, executes core and
behaviour regressions, runs Swing layout audits when a display is available,
generates QR/barcode samples and builds the standalone JAR. GitHub Actions adds
a native Windows graphical job to the same verification surface.

When `ETICHETTE_PRIVATE_DENYLIST` is configured in CI, the repository also runs
a fail-closed scan for private identifiers before verification.

## Source layout

```text
src/app/codice/       QR and Code 128 encoders
src/app/modello/      labels, data fields, sequences and settings
src/app/render/       shared drawing and geometry
src/app/archivio/     layout storage and daily log
src/app/stampa/       print job
src/app/esporta/      PNG, PDF and SVG export
src/app/stile/        UI design system
src/app/ui/           gallery, operator mode, editor and dialogs
prove/                plain-JDK regression and UI audit programs
```

See [Architecture](docs/ARCHITECTURE.md) for the invariants behind the design.

## Manuals

- [Manuale operatore — Italiano](docs/MANUAL.it.md)
- [Operator manual — English](docs/MANUAL.en.md)

The same workflow is also documented inside the JAR under **Impostazioni →
Manuale**.

## License

MIT
