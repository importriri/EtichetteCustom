# Etichette Custom — operator manual

An application for composing, printing and exporting labels with QR codes and
text. Every print run is appended to a daily log.

- **Start**: double-click `EtichetteCustom.jar`. Java 8 or newer is the only
  requirement — no installer, no network.
- **One screen.** The run bar on top is what you touch every day. The label
  sits in the middle with the tool rail floating over it. The properties panel
  appears on the right only when something is selected. The printer and the
  settings live behind the two icons at the end of the run bar: you open them
  about once a month.


---

## The run bar

**Codice** — the code of the first label, e.g. `DEMO-4410.07_A2-01_000001`.
The last digits step by one on every label; everything to their left is left
alone.

**Quantità** — how many labels. **Esce** shows the whole run in purple:
`…300001 → …300050`. That line is what will actually print — if it is not what
you expect, stop there.

**Supporto** — width and height in millimetres, plus **⇄** to swap them. There
is no "orientation" setting: a portrait label is simply 30 × 50 instead of
50 × 30, and that button does it.

---

## Fields: how any label gets designed

A **field** is a named value. Elements call it by writing its name in braces,
and the program substitutes it on every label.


A composed part number, for example, is three fields —

| Name | Type | Value |
|---|---|---|
| `{articolo}` | Fixed | `DEMO-4410` |
| `{versione}` | Fixed | `DEMO_REV_A` |
| `{seriale}` | Sequential, 6 digits | `000001` |

— and a QR whose content is `{disegno}.{versione}_{seriale}`. It prints
`DEMO-4410.07_A2-01_000001`, then `…000002`. When the job changes you
change *the field*, not the layout.

**The three types:**

- **Fisso** — always the same value: drawing number, version, product wording.
- **Progressivo** — the last N digits step by one on every label, counted from
  the right. The preview shows the split on the real code with the stepping
  part highlighted, and how many labels are left before it runs out.
- **Chiesto a ogni stampa** — asked when you press Print. For a batch number
  that changes every time but stays constant within the run.

A label can carry as many fields as it needs, and **two different counters step
together**, each on its own. Deleting a field still used by an element raises a
warning first: without the field, the placeholder would print literally, braces
and all.

---

## Elements

An element is a line of text or a QR code. The tool rail floating over the
label drives all of them:

| Icon | What it does |
|---|---|
| ✥ | Select and move |
| T | Add a line of text |
| ▦ | Add a QR code |
| ⟳ | Rotate the selection by 90° (or press **R**) |
| ⧉ | Duplicate (**Ctrl+D**) |
| ✕ | Delete (**Del**) |
| ⊞ | Toggle the 5 mm grid and snapping (**Ctrl+G**) |

If your Windows font cannot draw one of those symbols, a short word appears
instead — deliberately, because an empty box is worse than an ugly label.

### The properties panel

It appears when you select something and disappears when you click empty space.

**Contenuto** — the text, with `{name}` placeholders. Below it the program
lists which fields you referenced; one that does not exist shows in red.

**Ruota 90°** — the big button. The current angle sits next to it, with
shortcuts for 0° and 180°.

### Text that wraps

For text elements only: **Va a capo a … mm**.

Narrow that measurement and the text lays out **on two lines, then three, then
four**. The type does not shrink — it stays the same height, which is what you
want when a long code does not fit across. Below the field the program says how
many lines it currently takes.

Breaks fall on spaces, hyphens and underscores, so
`DEMO-4410.07_A2-01_000001` splits after an underscore, where the eye expects
it, and never in the middle of a digit group. **Zero** means a single line.

The same thing works with the mouse: drag the orange handle at the bottom right
of a text element to narrow it and watch the lines form.

---

## Your own layouts

The app ships with **no customer layouts inside it** — only an empty label with
one text line and one QR, to be moved into place. Nobody else's part numbers
travel with the program, and once you have composed yours, they do not travel
either: they stay on this PC.

When the label looks right, use **Salva layout…** in the panel on the right and
give it a name. It comes back from **Apri layout…** at any time, and appears in
the list the next time the program starts.

Layouts are one file each, in plain text, under `layout/` inside the settings
folder. Copy them onto a stick to move a design to the other PC in the
department — no export, no import, just files.

### The preview is a workbench

The preview is drawn by the same engine that prints: what you see is what comes
out.

| Gesture | Effect |
|---|---|
| Drag the element body | moves it (0.1 mm snap; **Shift** for 1 mm) |
| Drag the **orange square**, bottom right | resizes it |
| Drag the **green dot**, top right | rotates it (**Shift** snaps to 15°) |
| Mouse wheel | grows and shrinks |
| Arrow keys | nudge by 0.1 mm (**Shift** 1 mm) |
| **R** key | quarter turn |
| **+** / **−** keys | resize |

The faint grid is 5 mm and is never printed.

### Warnings

Below the preview, in orange. They appear when:

- an element **runs off the label** — with the exact millimetres of overhang
- the **QR module is too small** to be read reliably
- at that DPI a module would be **under two printer dots**: the printer rounds
  it and the QR comes out dirty
- the QR is **below the minimum side** you configured in the settings
- the **counter cannot cover** the requested quantity

A label with no warnings is a label that will print.

### Printing and exporting

**Stampa** (green) sends the run to the printer configured in the *Printer*
window.

**PDF** puts one label per page in a single file, **already turned to the
configured print orientation**: printing it from a browser no longer needs the
orientation picked by hand. **PNG** and **SVG** write one file per label with
the code in the name; the PNG declares its DPI, so dropping it into Word gives
the correct physical size.

#### Printing through the PDF

The normal route is the **Stampa** button: it goes straight to the queue with
this program's calibration, and never passes through a dialog that could resize
anything. The PDF is for archiving, for sending a label to someone else, or for
printing from a PC where the program is not installed.

The file itself asks for **actual-size printing**, so Acrobat and Chrome do not
"fit to page" on their own. If your PDF viewer ignores that, set its print
dialog to:

| Setting | Value |
|---|---|
| Size / Scale | **Actual size** or **100%** — never "Fit to page" |
| Orientation / Layout | **Portrait**. The program already baked the rotation into the file: choosing "landscape" here turns it a second time |
| Paper | the label format, the same one configured in the driver |
| Margins | none |

The symptom of getting it wrong is always the same: a small label in the corner
of a big sheet, or a QR straddling two labels.

---

## Printer (🖨 icon) — making the print come out straight

This window exists for one reason: **a badly calibrated thermal printer does not
report errors — it prints, and prints wrong**. If labels come out blank, or the
QR straddles two of them, the cure is here.

### Coda di stampa (print queue)

Pick the printer (e.g. `Datamax E-4203`). Underneath, in grey, is **the most
important line of all**: green when the driver declares a label-sized page, orange when it does not: what the driver declares.

> `Datamax E-4203 — pagina dichiarata 50,0 x 30,0 mm, area stampabile 50,0 x 30,0 mm, 203 dpi`

If it says **210 x 297 mm**, that queue is still set to A4 in Windows: the
printer will feed a whole sheet's worth of stock per label, and you will get
dozens of blank labels with the artwork scattered among them. No amount of
offset correction can fix that — fix the form in the Windows printer
properties, or force it here with *Misura personalizzata*.

**Chiedi la stampante a ogni stampa** — clear it and printing goes straight to
the chosen queue, no dialogs. That is how production works.

### Pagina mandata al driver (page sent to the driver)

| Mode | When |
|---|---|
| Come l'etichetta | almost always: the page is exactly the size of the stock |
| Quella della stampante | when the Windows form is already correct and should be left alone |
| Misura personalizzata | when the stock pitch differs from the printed area (wide gaps, or two labels per pitch) |

### Verso di stampa (print orientation)

The direction the roll feeds has nothing to do with the direction the label was
composed in. If it comes out lying on its side, try **90°** or **270°** — the
same thing as picking *"Landscape"* by hand in the browser's print dialog, but
saved once and for all. It applies to the exported PDF too.

### Taratura del tiro (registration offsets)

Two numbers in millimetres: positive moves right and down.

**Two-minute procedure:**

1. press **Stampa pagina di taratura**: out comes a 5 mm grid with the label
   border and corner marks;
2. look at the corner marks. If one is cut off, the artwork is shifted that way;
3. measure with a ruler how far the printed border sits from the real edge of
   the stock;
4. type that difference **with the sign reversed** into the X and Y offsets;
5. print the calibration again. When the frame matches the stock, press **Salva
   taratura**.

**Stampa un'etichetta di prova** uses the sample code from the settings window —
the final check before launching a real run.

### Scala (%)

The last knob, and the last one to touch. It only helps when the driver
rescales on its own: the print comes out straight and centred but **larger or
smaller than life**.

1. print the calibration page;
2. measure **one square of the grid** with a ruler: it must be 5.0 mm;
3. if you measure 4.5, the driver is printing at 90%: type `111` (5.0 divided
   by 4.5). If you measure 5.5, type `91`;
4. print again and check.

`100` means "leave it alone" and is right almost always. If you need a scale far
from 100, the real problem is the paper size configured in the driver — read the
diagnosis line at the top of the window.

### Che cosa riceve la stampante (what the printer receives)

- **Immagine al DPI della stampante** (default) — the label is rasterised and
  sent as a 1:1 image. No driver can reinterpret it.
- **Vettoriale** — outlines, as in the PDF. Sharper where the driver handles it
  well.

If the print looks grainy, or QR modules come out uneven, switch modes: they
are two independent routes to the same result.

---

## Settings (⚙ icon)

**Registro giornaliero** (daily log) — the folder and the file-name pattern:
`%s` is replaced with the date, so `etichette-%s.log` produces
`etichette-2026-07-25.log` — a new file every day, no line ever overwritten.
The folder is tested **the moment you choose it**.

**Numerazione progressiva** (serial numbering) — how many trailing digits step,
from 1 to 9, counted from the right. The box below shows the split on your own
code, with the stepping window highlighted in purple, and how many labels are
left before it runs out.

**Stampa e QR** — 203 dpi is the native resolution of the Datamax E-Class.
Raise the error correction (QUARTILE or HIGH) if labels get dirty or scratched;
a customer specification may ask for QUARTILE. The module threshold is the
size below which the "unreliable read" warning appears.

**Aspetto** (appearance) — light (`latte`) or dark (`mocha`), applied at the
next start.

**Manuali** — the two buttons open this manual and the Italian one, from the
`docs` folder next to the JAR; if it isn't there, the online copy is opened.

---

## The daily log

Every print and export appends a line: time, kind (STAMPA / PDF / PNG / SVG),
quantity, first and last code of the run. Since codes are sequential, first +
last + quantity reconstruct every single label in the run.

If the chosen folder is unreachable the program **does not stop**: it writes to
a local fallback folder and says so in the status bar. When that happens, check
the network path or pick another folder.

---

## Troubleshooting

**Blank labels, or the QR straddling two of them.**
It is the driver's page format. Go to *Stampante* and read the diagnosis line:
if it declares a size that isn't the stock, that's your problem. Then: page
mode *Come l'etichetta*, render *Immagine*, and print the calibration page.

**It comes out sideways.** Print orientation 90° or 270° in *Stampante*.

**It is a few millimetres off.** Registration offsets, procedure above.

**"Contatore esaurito" / "restano N etichette, ne hai chieste M".**
The digit window cannot cover the run. The program refuses to start: wrapping
to zero would produce two labels with the same QR. Reduce the quantity, change
the starting code, or widen the stepping window.

**"Modulo del QR a X mm: la lettura diventa incerta".**
The QR is too dense for its size. Enlarge the QR, shorten the content, or lower
the error correction.

**An element "esce dall'etichetta" (runs off the label).** The warning says by
how much and on which side. Shrink it (A−), move it, or enlarge the media.

**The PNG looks tiny or huge elsewhere.** Some programs ignore the declared
DPI. For sharing, the PDF is the faithful route.

**The status bar says "Registro non scrivibile".** The labels still printed:
the log went to the local fallback folder.
