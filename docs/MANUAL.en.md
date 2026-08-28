# Operator manual

## Gallery

Choose a label to prepare a print run. Use **Edit layout** only when the physical
design needs to change. A fresh installation starts with one editable example.

## Print preparation

Set the number of copies, fill any value marked **Ask when printing**, check the
preview and print.

Sequential values show the outgoing range before the job starts. Cancelling the
system print dialog does not advance a sequence. Counters are saved only after a
successful print.

## Layout editor

Click an element to edit it. Drag it to move it and drag a blue corner handle to
resize it. The right-hand inspector shows controls for the selected element;
precise measurements and advanced settings appear only when opened.

Useful shortcuts:

- `R` — rotate 90°
- `Ctrl+D` — duplicate
- `Delete` — remove
- `Ctrl+Z` — undo
- `Ctrl+Y` or `Ctrl+Shift+Z` — redo

## Readable text

Text can be aligned left, centre or right and laid out automatically or on up to
three lines. Automatic wrapping prefers natural separators and keeps logical
code groups intact whenever they can fit.

**Show dots and symbols** affects readable text only. The source used by QR and
barcode elements is not changed.

Open **Arrange text…** when the readable code needs a custom physical layout.
The complete source can be split into movable text blocks at natural separators.
Each block still references the same source, so the blocks may be positioned in
any visual order while the QR or barcode continues to encode the complete exact
value.

## Content

A content source can be fixed, sequential, or requested at print time. Several
elements may share the same source. Use the shared-content control only when an
element needs to become independent or reuse another existing source.

## QR and barcode

The inspector reports whether a code is large enough and, for QR, whether there
is enough white space around it. Resize or move the element directly on the
label. Technical QR settings remain available under **QR options**.

Always finish a new physical layout with a scan test on the real printer and
stock.

## Settings

**General** contains label and log folders. **Printer** stores printer metadata
and DPI used by readability checks. **Manual** contains the operator guides and
**Info** links to the project repository.
