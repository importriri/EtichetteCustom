<div align="center">

# Etichette Custom

A standalone Java/Swing desktop tool for designing and printing serialized
labels with readable text, QR codes and Code 128 barcodes.

[![tests](https://github.com/importriri/etichette-custom/actions/workflows/tests.yml/badge.svg)](https://github.com/importriri/etichette-custom/actions/workflows/tests.yml)
[![lint](https://github.com/importriri/etichette-custom/actions/workflows/lint.yml/badge.svg)](https://github.com/importriri/etichette-custom/actions/workflows/lint.yml)

![Etichette Custom demo](docs/demo.gif)

</div>

## Why it exists

Small production labels are easy to print badly: a sequence can be consumed too
early, a readable code can drift away from its QR payload, or a layout can look
different between preview and print.

Etichette Custom keeps those concerns in one small application. The same label
model drives the editor, preview, export and Java printing path, while sequence
numbers advance only after a successful print.

The application is used as a standalone Java 8-compatible JAR with no runtime
dependencies.

## Workflow

### Gallery

Open an existing label or create a new one. A fresh installation starts with one
small editable example instead of a catalogue of templates. Search appears only
when the gallery is large enough to need it, and recent print history appears
only after a run has been recorded.

### Layout editor

Select an element on the label, drag it to move it and drag a blue corner handle
to resize it. The inspector is contextual: text controls appear for text, QR
health appears for QR elements, and exact millimetre fields stay hidden until
**Precise measurements** is opened.

Routine actions stay compact:

- one alignment chooser instead of three permanent buttons;
- one line-count chooser for automatic, 1, 2 or 3 lines;
- one **Rotate 90°** action instead of four angle buttons;
- one source value can be shared by QR, barcode and readable text;
- fixed, sequential and print-time values expose their extra controls only when
  they are relevant.

QR and Code 128 always receive the exact source string. Hiding punctuation or
wrapping readable text changes presentation only.

### Print preparation

The print view exposes only run-time choices. Fixed values stay out of the way;
a sequential field shows its starting value and outgoing range, while a
print-time field asks for the value needed by that run. Sequence configuration
remains in the editor instead of being repeated before every print.

The final action opens the operating-system print dialog. Cancelling that dialog
does not consume sequence numbers.

## Safety and consistency

- Preview, editor canvas, PNG, PDF and Java printing share the same renderer.
- QR/barcode payloads stay independent from readable-text presentation.
- Every sequence is validated before a run and refuses numeric-window overflow.
- Sequence counters advance only after successful printing.
- Updated state is persisted after the print and the run is appended to the log.
- Undo and redo cover layout edits.
- Routine geometry uses direct manipulation instead of spinner-heavy forms.

## Screenshots

| Gallery | Layout editor | Print preparation |
|---|---|---|
| ![Gallery](docs/screenshot-vetrina.png) | ![Layout editor](docs/screenshot-editor.png) | ![Print preparation](docs/screenshot-operatore.png) |

The screenshots, demo and bundled example use synthetic data.

## Windows UI verification

CI compiles the Java 8-compatible sources on Linux and a native Windows runner.
The graphical audit renders the first-run gallery, editor and print flow at
several UI scales corresponding to common 100%, 125%, 150% and 200% profiles.
The PNG evidence is retained for visual review; a green layout test alone is not
considered sufficient evidence for a UI change.

The audit also checks direct resize behavior, text alignment and wrapping,
rotation, source-data preservation and the absence of `JSpinner` from the
primary workflow. Details are in
[`docs/windows-ui-audit.md`](docs/windows-ui-audit.md).

## Project layout

```text
src/app/modello/      labels, content sources, sequences and settings
src/app/render/       shared drawing, text layout and hit geometry
src/app/codice/       built-in QR and Code 128 encoders
src/app/archivio/     layout persistence and print history
src/app/stampa/       physical print job
src/app/esporta/      PNG, PDF and SVG export
src/app/ui/           gallery, editor, print preparation and dialogs
src/app/stile/        Swing design system
prove/                plain-JDK regression and graphical audit programs
```

The model and renderer do not depend on the UI layer. More detail is in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Build and verify

Requirements: JDK 8 or newer. The full Linux display audit also uses Xvfb.

```bash
./verify.sh
java -jar dist/EtichetteCustom.jar
```

`verify.sh` compiles with warnings as errors, runs the core and behavior suites,
executes graphical audits when a display is available, builds the standalone
JAR, checks its manifest and generates controlled QR/barcode samples. GitHub
Actions adds the native Windows graphical matrix.

When `ETICHETTE_PRIVATE_DENYLIST` is configured in CI, the repository also runs
a fail-closed scan for private identifiers before verification.

## Manuals

- [Italiano](docs/MANUAL.it.md)
- [English](docs/MANUAL.en.md)

The manuals are also available inside the JAR from the settings view.

## License

MIT
