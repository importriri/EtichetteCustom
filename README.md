<div align="center">

# Etichette Custom

A Java/Swing application for designing, previewing, exporting and printing
serialized QR labels.

[![tests](https://github.com/importriri/etichette-custom/actions/workflows/tests.yml/badge.svg)](https://github.com/importriri/etichette-custom/actions/workflows/tests.yml)

![Demo](docs/demo.gif)

</div>

## Main idea

A label is stored as a list of text and QR elements over a list of named fields.
The same `LabelLayout` draws every output:

```text
preview  |  PNG  |  SVG  |  PDF  |  Java print queue
```

Position, size, rotation, alignment and text wrapping are therefore shared
instead of being reimplemented for each exporter.

The application was developed against a Datamax E-Class thermal printer, but it
uses the standard Java print service API and can target any printer queue
visible to the operating system.

## Features

- free placement of text and QR elements;
- fixed, sequential and run-time fields referenced as `{name}`;
- reusable layouts stored in the user's settings directory;
- numeric windows that stop before they can wrap into duplicate labels;
- PNG, SVG and PDF export;
- vector text outlines with no embedded font requirement;
- page size, orientation, registration offsets and scale controls;
- calibration sheet with a 5 mm grid;
- visible fallback when the preferred log directory is unavailable;
- light and dark interface themes;
- Italian and English manuals embedded in the JAR.

No customer layout or production identifier is compiled into the application.
A new installation starts with a simple text element and one QR element.

## Printing notes

The first version produced correct PDFs but could feed blank labels when the
printer driver replaced the requested page format with an A4 format. The print
path now supplies the page format for every page, sends explicit print
attributes and offers raster mode when a driver should receive a 1:1 image.
The printer dialog also shows the page geometry reported by the driver.

Exported PDFs include `/PrintScaling /None` so PDF viewers do not silently use
"fit to page".

## QR encoder

The QR encoder is implemented in the project and supports versions 1-40,
numeric, alphanumeric and UTF-8 byte modes, all masks and all four error
correction levels.

The normal test suite keeps frozen matrices and structural checks. The optional
scripts in `verifica/` compare the Java matrices with Segno and decode rendered
QRs with zxing-cpp. Those Python packages are development tools and are not
runtime dependencies.

## Build and verify

Requirements: JDK 8 or newer, Ant and Xvfb for the display tests on Linux.

```bash
./verify.sh
ant jar
java -jar dist/EtichetteCustom.jar
```

`verify.sh` runs the private-value guard when
`ETICHETTE_PRIVATE_DENYLIST` is set, compiles for Java 8, runs unit, printing
and display-profile suites, builds the standalone JAR and checks its packaged
manuals and entry point.

## Repository layout

```text
src/app/core/      label model, QR encoder, layout, exporters and printing
src/app/ui/        editor, properties, dialogs and theme
src/app/config/    settings and log target
src/app/docs/      manuals embedded in the JAR
test/              plain-JDK test programs
verifica/          optional independent QR development checks
```

## Manuals

- [Manuale operatore](src/app/docs/MANUAL.it.md)
- [Operator manual](src/app/docs/MANUAL.en.md)
- [Architecture notes](docs/ARCHITECTURE.md)

## License

MIT
