# Windows UI audit

This note records the UI compatibility rules for Etichette Custom 2.0.1.

- Keep Swing look and feel deterministic across Windows and Linux. Do not depend on the native Windows Look & Feel for primary controls.
- Primary editing is direct manipulation on the label: select, drag, resize, rotate.
- Numeric geometry is secondary and hidden by default.
- Do not use `JSpinner` in the primary editor. Windows delegates can render arrows/editors differently across scale factors.
- Test 100%, 125%, 150% and 200% equivalent UI scales.
- The grid is application-rendered and must remain visible independently of OS theme.
- Text controls expose alignment, separator visibility, rotation and line layout directly.
- QR/barcode data always uses the exact source value; text presentation never mutates source data.

Research basis: Swing delegates are look-and-feel specific; OpenJDK has documented Windows HiDPI rendering defects in native Swing L&F generations. For this small standalone JAR we keep a built-in cross-platform L&F and custom primary controls rather than adding an external runtime dependency.
