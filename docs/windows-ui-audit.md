# Windows UI audit

This note records the UI compatibility and visual-acceptance rules for Etichette Custom 2.0.1.

## Design rules

- Keep Swing look and feel deterministic across Windows and Linux. Do not depend on the native Windows Look & Feel for primary controls.
- Primary editing is direct manipulation on the label: select, drag and resize first; use the inspector for semantic choices.
- Numeric geometry is secondary and hidden by default.
- Do not use `JSpinner` in the primary editor. Windows delegates can render arrows/editors differently across scale factors.
- The grid is application-rendered and must remain visible independently of OS theme.
- QR resizing remains square and visibly handle-driven.
- Text controls expose alignment, separator visibility, rotation and line layout directly.
- QR/barcode data always uses the exact source value; text presentation never mutates source data.
- A new archive contains one example label only. Reopening an archive must not repopulate removed templates.

## Scale matrix

The graphical CI audit runs at four base-font profiles approximating common desktop scales:

| Profile | Purpose |
|---|---|
| 12 | 100% baseline |
| 15 | 125% / common Windows office setup |
| 18 | 150% enlarged UI |
| 24 | 200% HiDPI / accessibility stress |

Passing a scale means more than constructing the Swing tree. The rendered controls must stay horizontally inside the inspector, the grid must remain visible and the direct-resize gesture must update the model.

## Required editor checks

Every release candidate must prove at least the following on a native Windows runner and on Linux:

1. no `JSpinner` exists in the primary inspector;
2. left/centre/right alignment is reachable;
3. automatic and explicit three-line text layout is reachable;
4. 270 degree rotation is reachable;
5. the separator toggle changes presentation only;
6. the source code remains byte-for-byte unchanged for QR/barcode use;
7. precise measurement fields stay hidden until requested;
8. primary controls do not clip horizontally at any tested scale;
9. the editor grid has visible structure;
10. dragging a QR corner resizes it directly and keeps square geometry;
11. long codes wrap without silently dropping characters;
12. first-run storage creates one example label and later starts load only saved labels.

## Visual review

The CI job retains PNGs of the editor and inspector for every scale. A green automated run is necessary but not sufficient for release: the retained images are reviewed for spacing, text clipping, hierarchy, contrast, accidental native-widget styling and obvious disproportion between labels and controls.

If a screenshot looks wrong even though the structural assertion passes, the candidate remains unreleased and a new regression assertion should be added when practical. This is how the current horizontal-clipping checks were introduced.

## Rendering boundary

Physical label geometry is always millimetre-based. Windows display scaling may enlarge the application UI, but it must not alter page size, element coordinates, QR module geometry or exported/printed dimensions.

Preview, editor, PNG, PDF and printing share the same renderer so platform-specific UI scaling does not create a second label-layout implementation.

## Research basis

Swing delegates are look-and-feel specific, and OpenJDK has documented Windows HiDPI rendering defects across native Swing Look & Feel generations. For this small standalone JAR the application keeps a built-in cross-platform styling layer and custom primary controls instead of making production editing depend on native spinner/button delegates or an additional runtime dependency.
