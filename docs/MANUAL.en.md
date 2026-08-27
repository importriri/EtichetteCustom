# Operator manual

## 1. Gallery

Choose a label from its rendered preview. A normal click opens print
preparation. Use **Edit layout** only when the physical design must change.

A fresh installation starts with one editable **Example** label. From then on
the gallery shows only labels that were created or saved by the user.

The gallery stays intentionally quiet: search appears only when there are enough
labels to need it, and recent print history appears only after a run has been
recorded. Create a label with **New label**. Rename, duplicate and delete actions
are available from the label card menu.

## 2. Prepare a run

This screen contains only choices that belong to the current run.

Fixed values are not repeated here. A sequential source shows its starting code
and the exact outgoing range. A value configured as **Ask when printing** gets an
input field for this run.

Set the copy count, check the preview and range, then press **Print**.
Sequence-window configuration stays in the editor instead of being shown before
every print.

## 3. Print

The real printer queue is selected in the operating-system print dialog.

Cancelling that dialog consumes **no** sequence number. Counters advance only
after a successful print job. The updated label state is then saved and the run
is appended to the print log.

## 4. Edit layout

The editor opens with no element selected. The right-hand inspector stays quiet
until you click something on the label or in the element list.

The normal workflow is:

1. click an element;
2. drag it to move it;
3. drag a blue corner handle to resize it;
4. use the contextual inspector only for choices that cannot be made directly
   on the label.

QR elements stay square while resizing. Exact X/Y/size values remain behind
**Precise measurements**. Label orientation and advanced data tools are kept in
the top **More** menu instead of occupying permanent space.

Main shortcuts:

- `R` — rotate 90°;
- `Ctrl+D` — duplicate;
- `Delete` — remove;
- `Ctrl+Z` — undo;
- `Ctrl+Y` or `Ctrl+Shift+Z` — redo.

## 5. Readable text

Selecting text exposes only the common choices:

- one alignment chooser: left, centre or right;
- one line chooser: automatic, 1, 2 or 3 lines;
- **Show dots and symbols**;
- one **Rotate 90°** action.

Click **Rotate 90°** repeatedly to cycle through 0°, 90°, 180° and 270°.
Automatic layout prefers natural separators when wrapping a long code and must
never silently drop characters.

**Show dots and symbols** changes presentation only. QR and barcode payloads
always retain the exact source value.

## 6. Content and shared values

Every QR, barcode or text element reads from a content source. The editor avoids
exposing internal source identifiers.

The current behavior is summarized in plain language:

- **Does not change** — stored with the label;
- **Increases automatically** — advances a numeric window;
- **Asked when printing** — entered for each run.

Open **How it changes…** only when that behavior or the sequence window needs to
change.

When several elements use the same value, the inspector shows one compact link
such as **QR + Text**. Open it only when the selected element must become
independent. An independent element can also choose **Use existing content…**
to reuse a value already present on the label.

## 7. QR and barcode

QR and barcode cards show a plain-language health result first. QR correction
level and technical measurements stay behind **QR options** or **Precise
measurements**.

If a QR is too small, enlarge it with a corner handle. If it is too close to an
edge, move it inward. A content error is shown directly in the inspector.

Final acceptance should always include a scan test on the real stock and
printer.

## 8. Windows and UI scaling

The primary workflow avoids `JSpinner` and uses application-rendered geometry.
CI exercises the first-run gallery, editor and print flow natively on Windows at
several UI scales corresponding to common 100%, 125%, 150% and 200% profiles.

Changing Windows display scaling does not change physical label geometry:
printing and export continue to use millimetres.

## 9. Settings

**General** contains label and log folders. **Printer** stores printer metadata
and the DPI used by readability checks.

**Manual** contains this guide in Italian and English. **Info** links to the
GitHub repository.
