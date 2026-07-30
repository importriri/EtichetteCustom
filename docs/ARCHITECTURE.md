# Architecture

Four decisions explain the rest of the code.

## 1. One layout, five backends

`LabelLayout` draws onto a `LabelCanvas`, an interface with a single method:

```java
public interface LabelCanvas {
    void fill(Shape shapeMm);
}
```

The preview, the PNG rasteriser, the SVG writer, the PDF writer and the print
job are five implementations of that one method. Everything that reaches a
label — QR modules, glyph outlines, the calibration grid — arrives as a
geometric shape in millimetres.

The consequence is the point: **the preview and the print cannot disagree**,
because there is no second layout to drift. When the preview shows one thing
and the PDF prints another, the bug is in a backend, and there are only five
short ones to look at.

`TransformedCanvas` is the same idea used once more: print orientation is a
transform wrapped around a canvas, so the rotation is applied identically to
the screen, the PDF and the printer.

## 2. Elements are a list, not fields

A label is a `List<LabelElement>`. Each element carries a kind (`TESTO` or
`QR`), a content template, a position, a size, a rotation and an alignment.

The first version had three fixed slots — code, QR, tag — and every new
customer layout meant new fields, new spinners, new persistence keys and new
branches in the renderer. With a list, a customer layout is *data*, composed in
the app and saved as a file rather than written into the program:

```java
m.add(LabelElement.qr("QR", "{articolo}.{revisione}_{seriale}", 15, 7, 20));
```

`Templates` holds those presets. Adding a customer's label is a ten-line method
in one file — no change to the engine, the persistence or the UI.

Content is a template string where `{codice}` is replaced by the running code.
That single placeholder covers literal text, the serial alone, and the joined
`drawing.version_serial` string the specification asks for, without inventing a
field system.

## 3. Fail-closed numbering

`SerialWindow` slices the code into an immutable prefix and an *N*-digit
counter window. When a requested run does not fit in what remains of the
window, the app refuses **before** printing label one — never at half a roll —
because both wrapping to zero and carrying into the prefix would produce two
labels with the same QR.

The check is in `checkRun`, called before anything is materialised: `run(n)`
either returns every code or none.

## 4. The printing subsystem earns its complexity

This is the part that came from a real production failure, so it is worth
recording precisely.

**Symptom.** Printing from the exported PDF was perfect. Pressing *Stampa* in
the app produced runs of blank labels with the QR straddling two of them.

**Cause.** The old code did this:

```java
job.setPrintable(this, pageFormat());   // our 50 × 30 mm page
if (!job.printDialog()) return;         // ← the native Windows dialog
job.print();
```

`printDialog()` returns the **driver's** page format, not the one handed to
`setPrintable`. The job started on a Letter/A4 page with the label drawn in one
corner; the printer advanced a whole sheet's worth of stock per page. In the
sandbox the default page reports as 612 × 792 pt = 215.9 × 279.4 mm — exactly
the failure mode.

**The three defences, all applied together:**

1. **A `Book` (`Pageable`)** — the page format is asked for *per page*, so ours
   survives whatever the dialog did. It is reinstalled after the dialog returns
   as well.
2. **Attributes** — `MediaPrintableArea`, `Media` and `OrientationRequested`
   are passed to `job.print(attrs)`, so the size reaches the driver by a second,
   independent route.
3. **Raster mode, by default** — the label is rasterised at the model's DPI and
   drawn as a 1:1 image, scaled by `72.0 / dpi`. A bitmap is the one thing a
   thermal driver cannot reinterpret.

On top of that, `PrintSetup` exposes what a mis-set queue needs: page mode
(label / driver / custom), print orientation, and X/Y registration offsets. And
because a badly calibrated printer reports no error at all, `calibrationPage`
prints a 5 mm grid with corner marks: the operator measures it with a ruler and
types the difference back.

`LabelPrinter.describe()` prints what the driver claims, in millimetres. If a
label printer answers "210 × 297 mm", no offset will ever fix it, and the
operator should see that sentence rather than guess.

## Tests

Nine suites, plain JDK, no JUnit — each is a `main` that prints what it checked
and exits 1 on the first failure.

| Suite | What it locks |
|---|---|
| `QrCodeTest` | two frozen matrices verified against two independent decoders, structure invariants, exact version boundaries, UTF-8 byte counting, all 8 masks |
| `SerialWindowTest` | prefix/window slicing, zero-padding, the fail-closed exhaustion policy |
| `LabelModelTest` | elements, storage v2 round-trip, legacy v1 import, quarter turns, orientation, warnings, every preset fitting its own label |
| `LayoutTest` | rotation compared path-by-path against `AffineTransform.getRotateInstance` about the anchor; alignment; QR modules at their nominal centres |
| `ExportTest` | PNG size and declared DPI, SVG well-formedness, byte-exact PDF xref offsets, raster sampled module by module against the QR matrix |
| `PrintTest` | page format per mode, zero margins, driver attributes, one page per label and not one more, both render modes, offsets moving the artwork, the page turning with the orientation, the calibration grid reaching the corners |
| `LogTargetTest` | dated file naming, writability probes, the degraded fallback path |
| `StartupSmokeTest` | launches through `Main.main`, types a code, verifies the preview paints, opens every tab — twice: first run and restart with saved settings |
| `LayoutAuditTest` | every component fits its container and gets the width it asks for, at 96 dpi and at 144 dpi |

The last one deserves a note: it was written from photographs of the app
running on a shop-floor Windows PC, where spinner values were clipped under
their arrows. Rather than eyeballing screenshots forever, the audit walks the
live component tree and fails on any component narrower than its own preferred
size.

## Layout of the source

```
src/app/
  Main.java                 entry point, look and feel, shared UI colours
  config/
    AppTheme.java           the design system: palette, components, borders
    UiScale.java            DPI scaling — 1.0 when the toolkit already scales
    SettingsManager.java    atomic properties file in APPDATA / ~/.config
    LogTarget.java          log folder + dated file name, writability probe
    Docs.java               finds the manuals next to the JAR, else online
  core/
    QrCode.java             the encoder, versions 1–40, three modes
    SerialWindow.java       the fail-closed counter
    LabelElement.java       one element: kind, content, position, size, rotation
    LabelModel.java         media, DPI, ECC, the element list, warnings, storage
    Templates.java          the presets, as data
    LabelLayout.java        the only layout algorithm
    PrintSetup.java         queue, page mode, orientation, offsets, render mode
    LabelPrinter.java       Pageable printing, raster/vector, calibration page
    DayLog.java             append-only daily log with fallback
    export/                 LabelCanvas + PNG, SVG, PDF, Graphics2D, Transformed
  ui/
    MainWindow.java         three tabs and a status bar
    TabLabel.java           content, elements, geometry, actions
    TabPrinter.java         queue, page, orientation, offsets, calibration
    TabSettings.java        log, numbering, QR, appearance, manuals
    PreviewPanel.java       drag to move, handles to resize and rotate
```


---

## Fields, and why "the code" stopped existing

The first version had one code. Everything referred to it, and anything that
was not it — a batch number, a drawing number, a second counter — had nowhere
to live.

`LabelField` replaced it with a named value. An element's content is a template
(`{disegno}.{versione}_{seriale}`) and `LabelModel.valuesAt(i)` produces the map
for the *i*-th label; every backend renders from that map. Three types cover the
shop floor:

| Type | Filled by | Example |
|---|---|---|
| `FISSO` | the office, once per job | drawing number |
| `SEQUENZIALE` | the counter, one step per label | serial |
| `CHIESTO` | the operator, when the run starts | batch |

Only `SEQUENZIALE` can run out, and only it is checked before printing. Adding
a fourth type — a date, a shift letter — is a case in `valueAt` and an entry in
the enum; nothing else in the program knows the difference.

A placeholder with no matching field is deliberately *not* silently removed: it
prints literally, and the model reports it in `warnings()`. An incomplete label
that nobody notices is worse than an ugly one that everybody does.

## Text wrapping

A text element carries a wrap width in millimetres. `LabelLayout.wrapLines`
breaks on spaces, hyphens and underscores — the separators a part number
already contains — and never force-splits a token that is wider than the limit:
it overflows, and the "outside the label" warning says so.

The type size does not change. Narrowing a text element makes it *taller*, not
smaller, which is what an operator means when a long code will not fit across a
25 mm label.

## The screen

One window, no tabs, and one rule: **every action has exactly one home**. The
tool rail owns add/rotate/duplicate/delete/grid; the properties panel owns the
numbers; the run bar owns the run. The panel is not rendered at all when
nothing is selected.

The rail floats in a `JLayeredPane` over the preview so it keeps a constant
distance from the label at any window width. The run bar uses `WrapLayout`
rather than `FlowLayout` because the stock one lays components out on several
rows but then reports the height of one — which is how the window came to ask
for 1533 px on a 1280 px screen, caught by `LayoutAuditTest` rather than by
anyone looking at it.


## The manual is a resource, not a link

`src/app/docs/MANUAL.it.md` and `MANUAL.en.md` are copied next to the classes at
build time and read from the classpath by `Manuals`, which never throws and
never returns null — a build that lost the resource still opens the manual pane
and says the file is missing.

The alternative, which this project used first, was to look for the file on
disk beside the JAR and fall back to a GitHub URL. It is wrong twice over on a
shop floor: copying just the JAR leaves the operator with no manual at all, and
that machine often has neither a spare browser tab nor internet.

`Markdown` is 200 lines and converts exactly what the manuals use — headings,
paragraphs, bold, inline code, both list kinds, tables, rules. Escaping happens
first and markup second, so a stray angle bracket in the text cannot close a tag
it never opened. It takes text and returns an HTML fragment, with no Swing
anywhere, which is why `ManualRenderTest` can check both manuals — including
that neither carries a real customer's part numbers — without a display.
