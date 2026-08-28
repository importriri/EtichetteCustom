# Changelog

## 2.0.3 — 2026-08-28

- Keep logical code groups intact when readable text wraps on narrow layouts.
- Add movable text blocks derived from one shared source, so readable code parts can be arranged freely while QR and barcode payloads remain exact.
- Improve inspector control sizing and rendering across Linux and Windows.
- Strengthen graphical regression coverage for text layout, control clipping and shared-source preservation.
- Extend the label format without breaking older v3/v4 files.

## 2.0.2 — 2026-08-27

- Simplify the editor around contextual controls and direct manipulation.
- Keep secondary measurements, QR settings and content behavior hidden until needed.
- Streamline gallery and print preparation for the normal production path.
- Improve large-scale and Windows HiDPI layout behavior.
- Refresh repository media and expand native Windows graphical verification.

## 2.0.1 — 2026-08-27

- Add direct move and resize editing for label elements.
- Add readable-text alignment, line layout, separator visibility and rotation controls.
- Keep QR/barcode payloads independent from readable-text presentation.
- Improve long-code wrapping and grid visibility.
- Add native Windows CI and retained graphical audit evidence.
- Start new installations with one editable example label.

## 2.0.0 — 2026-08-27

- Introduce the v2 data directory and a clean first-run experience.
- Add shared content sources for readable text and machine-readable codes.
- Persist text presentation settings while retaining older-file compatibility.
- Use one renderer for preview, export and print.

## 1.0.0 — 2026-08-25

- Introduce the gallery, protected print flow and layout editor.
- Support multiple independent sequences and shared data sources.
- Validate complete sequence ranges before printing.
- Preserve counters when the system print dialog is cancelled.
- Add undo/redo, rotated geometry handling, settings and print history.
- Add Italian and English in-app manuals and release verification.
