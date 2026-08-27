# Windows UI audit

This note records the UI compatibility and visual-acceptance rules for Etichette Custom 2.0.2.

## Design rules

- Keep Swing look and feel deterministic across Windows and Linux. Do not depend on the native Windows Look & Feel for primary controls.
- Primary editing is direct manipulation on the label: select, drag and resize first; use the inspector for semantic choices.
- Use progressive disclosure. A control that is not needed for the current selection or workflow should not occupy permanent space.
- Numeric geometry is secondary and hidden by default.
- Do not use `JSpinner` in the primary workflow. Windows delegates can render arrows/editors differently across scale factors.
- One concept should normally have one compact control: one rotation action, one alignment chooser and one line-count chooser.
- Shared-content detach/link controls, sequence-window settings and QR correction details remain collapsed until requested.
- Fixed data stays out of print preparation. Sequence-window configuration stays in the editor rather than being repeated before every print.
- The grid is application-rendered and must remain visible independently of OS theme.
- QR resizing remains square and visibly handle-driven.
- QR/barcode data always uses the exact source value; text presentation never mutates source data.
- Major editor side panels must not scale until they consume the working canvas on HiDPI profiles; physical widths are capped where necessary.
- A new archive contains one example label only. Reopening an archive must not repopulate removed templates.

## Scale matrix

The graphical CI audit runs at four base-font profiles approximating common desktop scales:

| Profile | Purpose |
|---|---|
| 12 | 100% baseline |
| 15 | 125% / common Windows office setup |
| 18 | 150% enlarged UI |
| 24 | 200% HiDPI / accessibility stress |

Passing a scale means more than constructing the Swing tree. The rendered controls must stay horizontally inside their container, the grid must remain visible, the canvas must retain useful working space and direct manipulation must still update the model.

## Required flow checks

Every release candidate must prove at least the following on a native Windows runner and on Linux:

1. no `JSpinner` exists in the primary gallery/editor/print flow;
2. a one-label gallery exposes one new-label action and does not waste space on search or empty print-history chrome;
3. the layout editor opens with the inspector quiet until an element is selected;
4. left/centre/right alignment is reachable through one chooser;
5. automatic and explicit three-line text layout is reachable through one chooser;
6. repeated use of the single rotate action reaches 0, 90, 180 and 270 degrees;
7. shared-content actions remain hidden until the shared-content control is opened;
8. content behavior choices and sequence-window details remain hidden until explicitly requested;
9. the separator toggle changes presentation only;
10. the source code remains byte-for-byte unchanged for QR/barcode use;
11. precise measurement fields stay hidden until requested;
12. primary controls do not clip horizontally at any tested scale;
13. the editor grid has visible structure;
14. dragging a QR corner resizes it directly and keeps square geometry;
15. long codes wrap without silently dropping characters;
16. print preparation contains only run-time values and has no sequence-configuration chooser;
17. first-run storage creates one example label and later starts load only saved labels.

## Visual review

The CI job retains PNGs of the first-run gallery, workspace, inspector and print preparation at every scale. A green automated run is necessary but not sufficient for release: retained images are reviewed for spacing, hierarchy, text clipping, contrast, redundant affordances, accidental native-widget styling and disproportion between side panels and working canvas.

A screenshot that looks wrong keeps the candidate unreleased even if structural assertions pass. When practical, the visual defect should become a regression assertion. Horizontal-clipping, duplicate new-label and simplified-print checks were introduced this way.

## Rendering boundary

Physical label geometry is always millimetre-based. Windows display scaling may enlarge the application UI, but it must not alter page size, element coordinates, QR module geometry or exported/printed dimensions.

Preview, editor, PNG, PDF and printing share the same renderer so platform-specific UI scaling does not create a second label-layout implementation.

## Research basis

Swing delegates are look-and-feel specific, and OpenJDK has documented Windows HiDPI rendering defects across native Swing Look & Feel generations. For this small standalone JAR the application keeps a built-in cross-platform styling layer and application-controlled primary controls instead of making production editing depend on native spinner/button delegates or an additional runtime dependency.
