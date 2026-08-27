# Changelog

## 2.0.1 — unreleased

- Rework the layout editor around direct manipulation: drag elements to move them and drag visible corner handles to resize them.
- Keep QR resize square automatically so a quick mouse gesture cannot distort its geometry.
- Hide X/Y/width/font measurements by default; precise millimetre controls remain available only when explicitly requested.
- Remove `JSpinner` from the primary editor to avoid inconsistent Windows delegates and make routine editing easier to scan.
- Expose text alignment, automatic/1/2/3-line layout, separator visibility and 0/90/180/270 degree rotation directly in the inspector.
- Keep punctuation hiding presentation-only: QR and barcode data always retain the exact source string.
- Improve automatic wrapping so long codes prefer natural separators, balance lines and never silently lose characters.
- Increase editor-grid contrast and keep the grid application-rendered rather than dependent on the operating-system theme.
- Make primary editor controls use responsive equal-width groups so common Windows UI scales do not clip the rightmost choices.
- Add native Windows CI alongside Linux, compiling for Java 8 and running the application/tests on Temurin 21.
- Run graphical editor audits at four UI scales approximating 100%, 125%, 150% and 200%, retaining PNG evidence as CI artifacts.
- Verify that alignment, three-line layout, 270 degree rotation, hidden separators, exact source data and direct QR resizing work through the UI.
- Keep the v2 storage location and first-run behaviour: a new archive contains one example label and subsequent starts load only saved user labels.

## 2.0.0 — 2026-08-27

- Start from a fresh v2 data directory so production PCs do not inherit labels from older versions.
- Show one simple 50 x 30 mm example label on first run instead of a gallery of predefined templates.
- Keep the exact source value for QR/barcode data while allowing the human-readable text to hide punctuation/separators.
- Persist text presentation options in the label format while remaining able to read older files.
- Support left, center and right text alignment in the renderer.
- Keep text wrapping capped at three lines and preserve existing 0/90/180/270 degree rotation support.
- Preserve the legacy sample fixtures for verification while production uses the single fresh v2 example.
- Use the same renderer for preview, export and print so the displayed label and printed label stay consistent.

## 1.0.0 — 2026-08-25

- Introduced the gallery, protected operator mode and dedicated layout editor.
- Added multiple independent sequential data sources per label.
- Added shared data sources so QR, text and barcodes can represent one value.
- Added exact sequence-range preflight before printing.
- Ensured cancelled print dialogs never consume sequence numbers.
- Added undo/redo and corrected rotated element bounds, hit-testing and SVG geometry.
- Added semantic data names and direct QR/content editing.
- Unified paths, printer metadata, manuals and project information in Settings.
- Improved Swing spacing, scaling, readability and long-path handling.
- Added Italian and English in-app manuals.
- Expanded release verification with behaviour, layout and scaling regressions.
