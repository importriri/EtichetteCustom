# Operator manual

## 1. Gallery

Choose a label from its real preview. A normal click opens **Operator mode**.
Use **Edit layout** only when the physical design must change.

A fresh installation starts with one **Example** label. From then on the gallery
shows only labels that were created or saved by the user.

The search box filters saved labels by name. All application settings are
collected under **Settings…**.

## 2. Prepare a run

The screen shows one card for each data source actually used by the label. If a
QR code and its readable text use the same code, the value is entered once.

Set the copy count. Each sequential source shows the exact first and last code
that will be produced.

One label may contain several independent sequences. Each keeps its own prefix,
numeric suffix and counter.

## 3. Print

Check the summary and press **Print**. The real printer queue is selected in the
operating-system print dialog.

Cancelling that dialog consumes **no** sequence number. Counters advance only
after a successful print job.

## 4. Edit layout

The editor is designed around direct manipulation:

1. click an element to select it;
2. drag it to move it;
3. drag a blue corner handle to resize it;
4. use the right-hand inspector only for choices that cannot be made directly
   on the label.

QR elements remain square while resizing. The grid is a visual reference and
the editor keeps geometry inside the label whenever possible.

X/Y/width and text-size measurements are hidden during normal work. Open
**Precise measurements** only when an exact millimetre value is required.

Main shortcuts:

- `R` — rotate 90°;
- `Ctrl+D` — duplicate;
- `Delete` — remove;
- `Ctrl+Z` — undo;
- `Ctrl+Y` or `Ctrl+Shift+Z` — redo.

## 5. Readable text

Selecting a text element exposes the everyday choices directly:

- left, centre or right alignment;
- automatic layout or 1, 2 or 3 lines;
- 0°, 90°, 180° or 270° rotation;
- **Show dots and symbols**.

Automatic layout tries to preserve a large readable text size and prefers
natural separators when wrapping a long code. It must never silently drop
characters.

**Show dots and symbols** affects presentation only. QR and barcode content
always retains the exact source value. For example a QR may contain
`210150.022_02-01.262350009` while its human-readable text is presented without
selected separators.

## 6. Reuse one code across elements

QR, readable text and barcode elements can use one shared code. Use
**Use the same code as** to select data already used by another element.

If only the selected element needs a different value, press **Use a different
code**. The user does not need to manage internal field identifiers; the UI
shows readable element/data names instead.

## 7. QR and barcode

Quality indicators flag codes that are too small or have insufficient quiet
space. If a QR is too small, enlarge it directly with a corner handle. A red
error indicates invalid content.

Final acceptance should always include a scan test on the real stock and
printer.

## 8. Windows and UI scaling

The editor uses its own consistent controls and grid instead of relying on
platform-specific spinner rendering. Project CI exercises the editor natively on
Windows at several UI scales, including profiles corresponding to common 125%,
150% and 200% display settings.

Changing Windows display scaling does not change the physical label geometry:
printing and export continue to use millimetres.

## 9. Settings

**General** contains label and log folders. **Printer** stores the name used in
the log and the DPI used by readability checks.

**Manual** contains this guide in Italian and English. **Info** links to the
GitHub repository.
