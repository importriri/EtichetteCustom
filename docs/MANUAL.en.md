# Operator manual

## 1. Gallery

Choose a label from its real preview. A normal click opens **Operator mode**.
Use **Edit layout** only when the physical design must change.

The search box filters saved labels by name. All application settings are
collected under **Settings…**.

## 2. Prepare a run

The screen shows one card for each data source actually used by the label. If a
QR code and its readable text share one source, the value is entered once.

Set the copy count. Each sequential source shows the exact first and last code
that will be produced.

One label may contain several independent sequences. Each keeps its own prefix,
numeric window and counter.

## 3. Print

Check the preflight panel and press **Print**. The real printer queue is selected
in the operating-system print dialog.

Cancelling that dialog consumes **no** sequence number. Counters advance only
after a successful print job.

## 4. Edit layout

The editor can add text, readable code, QR, barcode and line elements.

Main shortcuts:

- `R` — rotate 90°;
- `Ctrl+D` — duplicate;
- `Delete` — remove;
- `Ctrl+Z` — undo;
- `Ctrl+Y` or `Ctrl+Shift+Z` — redo.

Coordinates use millimetres. After rotation the editor keeps an element inside
the label whenever possible.

## 5. Shared and independent data

Every visual element reads one data source. Linking QR and text to the same
source guarantees that both represent the same value.

Use **Make independent** when an element needs its own value or sequence.
Human-readable names are shown in the UI while stable technical identifiers
remain inside the layout file.

## 6. QR and barcode

Quality indicators flag codes that are too small or have a reduced quiet zone.
Orange means the physical result deserves attention; red indicates invalid
content.

Final acceptance should always include a scan test on the real stock and
printer.

## 7. Settings

**General** contains label and log folders. **Printer** stores the name used in
the log and the DPI used by readability checks.

**Manual** contains this guide in Italian and English. **Info** links to the
GitHub repository.
