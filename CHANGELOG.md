# Changelog

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
