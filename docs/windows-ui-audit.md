# UI acceptance

Etichette Custom is verified on native Windows and Linux before release. The
purpose of this audit is simple: the editor must remain readable, direct and
predictable at normal and enlarged desktop scales.

## Rules

- Keep the primary workflow focused on direct manipulation.
- Reveal secondary controls only when they are relevant.
- Keep physical geometry in millimetres and independent from desktop scaling.
- Keep primary controls inside their containers without text clipping.
- Keep the canvas useful even when UI fonts are enlarged.
- Keep QR resize square and preserve a visible quiet zone warning.
- Never change QR/barcode source data as a side effect of readable-text layout.
- Preserve logical text groups when wrapping whenever the available space allows
  it.
- Movable readable-text blocks may reorder presentation, never source data.
- A fresh archive contains one editable example and does not reseed later.

## Scale matrix

Graphical CI runs with base-font profiles of 12, 15, 18 and 24 pixels. These
cover the normal desktop range and an enlarged accessibility/HiDPI stress case.

At every profile the audit checks layout bounds, text visibility, editor working
space, direct resize, contextual controls and model updates.

## Release checks

The retained graphical run covers the first-run gallery, editor, contextual
inspector and print preparation. It also exercises:

- content and shared-source disclosure;
- readable-text alignment and line layout;
- separator hiding without source mutation;
- logical-group wrapping and movable text blocks;
- rotation and direct resize;
- precise measurement disclosure;
- QR readability feedback;
- print preparation without editor-only configuration;
- media generation used by the repository.

Automated checks are followed by visual inspection of the retained PNGs. A
candidate is not accepted when spacing, clipping, hierarchy or control rendering
looks wrong even if the component tree is technically valid.

## Rendering boundary

Preview, editor, PNG, PDF and Java printing share the same renderer. Desktop UI
scaling may change application controls, but it must not change label size,
element coordinates, QR module geometry or exported/printed dimensions.
