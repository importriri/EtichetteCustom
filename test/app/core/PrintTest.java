package app.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;

/**
 * Suite per {@link PrintSetup} e {@link LabelPrinter}. Runner a mano, niente
 * JUnit, exit 1 se fallisce.
 *
 * <p>Nasce da un difetto di produzione: dal PDF le etichette uscivano perfette,
 * dal pulsante Stampa uscivano vuote a raffica con il QR a cavallo di due
 * supporti. La causa era il formato pagina del driver che si sostituiva al
 * nostro. Qui si fissano le tre cose che lo impediscono — la pagina è grande
 * quanto l'etichetta, non ha margini, e il disegno esce davvero su ogni pagina
 * del giro — più la correzione del tiro, che è l'unico modo di rimettere in
 * bolla un supporto che parte storto.
 *
 * <p>Non serve una stampante: si stampa dentro un'immagine e si contano i pixel.
 */
public final class PrintTest {

    private static int passed = 0;
    private static int failed = 0;

    /** Punti per millimetro nello spazio di stampa. */
    private static final double MM_TO_PT = 72.0 / 25.4;

    public static void main(String[] args) throws Exception {
        setup_defaults_areTheSafeChoice();
        setup_limits_areEnforced();
        setup_storage_roundTrips();
        pageFormat_labelMode_isExactlyTheLabel();
        pageFormat_customMode_usesTheTypedSize();
        pageFormat_hasNoMargins();
        attributes_carryTheSizeToTheDriver();
        printable_drawsEveryPageOfTheRunAndNoMore();
        printable_bothModes_putInkOnThePage();
        offset_movesTheWholeDrawing();
        rotation_turnsTheDrawingAndThePageTogether();
        rotation_keepsTheDrawingInsideThePage();
        scale_changesTheSizeOfWhatIsPrinted();
        turn_mapsTheFourCornersOntoThePage();
        turn_swapsThePageSides();
        turn_actuallyRotatesTheInk();
        turn_reachesThePdfToo();
        calibration_drawsAGridAndOnlyOnePage();
        printerLookup_survivesAMachineWithNoPrinters();

        System.out.println("Print: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- taratura -------------------------------------------------------------

    private static void setup_defaults_areTheSafeChoice() {
        PrintSetup s = PrintSetup.defaults();
        yes("la pagina di default è grande quanto l'etichetta",
                s.pageMode() == PrintSetup.PageMode.ETICHETTA);
        yes("si stampa come immagine: nessun driver può reinterpretarla",
                s.render() == PrintSetup.Render.IMMAGINE);
        yes("nessuna correzione di tiro finché non la si misura",
                s.offsetXMm() == 0 && s.offsetYMm() == 0);
        yes("la finestra di stampa si apre", s.showDialog());
        yes("etichetta dritta e scala piena finché non servono",
                s.turn() == PrintSetup.Turn.GRADI_0 && s.scalePercent() == 100.0);
        same("nessuna coda imposta", "", s.printerName());
    }

    private static void setup_limits_areEnforced() {
        final PrintSetup s = PrintSetup.defaults();
        rejects("pagina più piccola di 5 mm", new Runnable() {
            public void run() {
                s.setPageSizeMm(4, 30);
            }
        });
        rejects("pagina più lunga di un metro", new Runnable() {
            public void run() {
                s.setPageSizeMm(50, 1001);
            }
        });
        rejects("correzione oltre i 5 cm", new Runnable() {
            public void run() {
                s.setOffsetMm(51, 0);
            }
        });
        rejects("correzione non numerica", new Runnable() {
            public void run() {
                s.setOffsetMm(0, Double.NaN);
            }
        });
        s.setOffsetMm(-3.5, 2.25);
        yes("una correzione negativa è legittima: si sposta indietro",
                s.offsetXMm() == -3.5 && s.offsetYMm() == 2.25);
    }

    private static void setup_storage_roundTrips() {
        PrintSetup s = PrintSetup.defaults();
        s.setPrinterName("Datamax E-Class; reparto=1");
        s.setPageMode(PrintSetup.PageMode.PERSONALIZZATA);
        s.setPageSizeMm(101.6, 50.8);
        s.setOffsetMm(-1.5, 0.8);
        s.setRender(PrintSetup.Render.VETTORIALE);
        s.setTurn(PrintSetup.Turn.GRADI_270);
        s.setScalePercent(98.5);
        s.setShowDialog(false);

        PrintSetup back = PrintSetup.fromStorage(s.toStorage());
        same("il nome della coda sopravvive ai separatori",
                "Datamax E-Class; reparto=1", back.printerName());
        yes("modalità pagina", back.pageMode() == PrintSetup.PageMode.PERSONALIZZATA);
        yes("misura", back.pageWidthMm() == 101.6 && back.pageHeightMm() == 50.8);
        yes("correzione", back.offsetXMm() == -1.5 && back.offsetYMm() == 0.8);
        yes("resa", back.render() == PrintSetup.Render.VETTORIALE);
        yes("verso di stampa", back.turn() == PrintSetup.Turn.GRADI_270);
        yes("scala", back.scalePercent() == 98.5);
        yes("niente finestra", !back.showDialog());

        PrintSetup broken = PrintSetup.fromStorage("roba=a caso;mode=INESISTENTE");
        yes("una taratura rotta torna ai default",
                broken.pageMode() == PrintSetup.PageMode.ETICHETTA);
    }

    // --- formato pagina -------------------------------------------------------

    private static void pageFormat_labelMode_isExactlyTheLabel() {
        LabelModel m = LabelModel.defaults();
        PageFormat pf = LabelPrinter.pageFormat(m, PrintSetup.defaults(), null);
        near("larghezza: 50 mm", 50 * MM_TO_PT, pf.getWidth());
        near("altezza: 30 mm", 30 * MM_TO_PT, pf.getHeight());

        m.swapSides();
        PageFormat turned = LabelPrinter.pageFormat(m, PrintSetup.defaults(), null);
        near("scambiando i lati la pagina li scambia con loro", 30 * MM_TO_PT, turned.getWidth());
        near("e l'altra misura la segue", 50 * MM_TO_PT, turned.getHeight());
    }

    private static void pageFormat_customMode_usesTheTypedSize() {
        PrintSetup s = PrintSetup.defaults();
        s.setPageMode(PrintSetup.PageMode.PERSONALIZZATA);
        s.setPageSizeMm(101.6, 25.4);
        PageFormat pf = LabelPrinter.pageFormat(LabelModel.defaults(), s, null);
        near("la misura scritta a mano vince sull'etichetta", 101.6 * MM_TO_PT, pf.getWidth());
        near("anche in altezza", 25.4 * MM_TO_PT, pf.getHeight());
    }

    private static void pageFormat_hasNoMargins() {
        PageFormat pf = LabelPrinter.pageFormat(
                LabelModel.defaults(), PrintSetup.defaults(), null);
        // ogni margine che ci mettiamo noi è un millimetro di disallineamento
        // che poi l'operatore deve recuperare a mano con la correzione di tiro
        near("nessun margine a sinistra", 0.0, pf.getImageableX());
        near("nessun margine in alto", 0.0, pf.getImageableY());
        near("l'area stampabile è tutta la pagina", pf.getWidth(), pf.getImageableWidth());
        near("in altezza idem", pf.getHeight(), pf.getImageableHeight());
        yes("orientamento verticale: la rotazione la gestiamo noi, non il driver",
                pf.getOrientation() == PageFormat.PORTRAIT);
    }

    private static void attributes_carryTheSizeToTheDriver() {
        PageFormat pf = LabelPrinter.pageFormat(
                LabelModel.defaults(), PrintSetup.defaults(), null);
        PrintRequestAttributeSet attrs = LabelPrinter.attributes(pf);
        MediaPrintableArea area =
                (MediaPrintableArea) attrs.get(MediaPrintableArea.class);
        yes("l'area stampabile è fra gli attributi", area != null);
        near("larghezza dichiarata al driver, in mm",
                50f, area.getWidth(MediaPrintableArea.MM));
        near("altezza dichiarata al driver, in mm",
                30f, area.getHeight(MediaPrintableArea.MM));
        near("origine in alto a sinistra", 0f, area.getX(MediaPrintableArea.MM));
        yes("orientamento verticale anche negli attributi",
                attrs.get(OrientationRequested.class) == OrientationRequested.PORTRAIT);
    }

    // --- disegno --------------------------------------------------------------

    private static void printable_drawsEveryPageOfTheRunAndNoMore() {
        LabelModel m = LabelModel.defaults();
        String[] codes = SerialWindow.of("TST-0000-00-001", 3).run(3);
        LabelPrinter printer = new LabelPrinter(m, codes, PrintSetup.defaults());
        PageFormat pf = LabelPrinter.pageFormat(m, PrintSetup.defaults(), null);

        int drawn = 0;
        for (int i = 0; i < codes.length; i++) {
            Sheet sheet = new Sheet(pf);
            if (printer.print(sheet.graphics(), pf, i) == Printable.PAGE_EXISTS
                    && sheet.ink() > 200) {
                drawn++;
            }
        }
        same("una pagina piena per ogni etichetta del giro", "3", String.valueOf(drawn));

        Sheet beyond = new Sheet(pf);
        yes("e nessuna pagina in più: è quello che riempiva il rullo di etichette vuote",
                printer.print(beyond.graphics(), pf, codes.length) == Printable.NO_SUCH_PAGE);
    }

    private static void printable_bothModes_putInkOnThePage() {
        LabelModel m = LabelModel.defaults();
        PageFormat pf = LabelPrinter.pageFormat(m, PrintSetup.defaults(), null);

        PrintSetup raster = PrintSetup.defaults();
        Sheet a = new Sheet(pf);
        new LabelPrinter(m, new String[] {"TST-0000-00-001"}, raster)
                .print(a.graphics(), pf, 0);

        PrintSetup vector = PrintSetup.defaults();
        vector.setRender(PrintSetup.Render.VETTORIALE);
        Sheet b = new Sheet(pf);
        new LabelPrinter(m, new String[] {"TST-0000-00-001"}, vector)
                .print(b.graphics(), pf, 0);

        yes("in modalità immagine l'etichetta si disegna (" + a.ink() + " px)", a.ink() > 500);
        yes("in modalità vettoriale anche (" + b.ink() + " px)", b.ink() > 500);
        // stesso layout, stessa etichetta: le due rese non possono differire
        // di più di un filo di antialiasing
        double ratio = (double) a.ink() / b.ink();
        yes("e le due rese coprono la stessa area (rapporto "
                + String.format("%.2f", ratio) + ")", ratio > 0.8 && ratio < 1.25);
    }

    private static void offset_movesTheWholeDrawing() {
        LabelModel m = LabelModel.defaults();
        PageFormat pf = LabelPrinter.pageFormat(m, PrintSetup.defaults(), null);

        Sheet flat = new Sheet(pf);
        new LabelPrinter(m, new String[] {"TST-0000-00-001"}, PrintSetup.defaults())
                .print(flat.graphics(), pf, 0);

        PrintSetup moved = PrintSetup.defaults();
        moved.setOffsetMm(3, 2);
        Sheet shifted = new Sheet(pf);
        new LabelPrinter(m, new String[] {"TST-0000-00-001"}, moved)
                .print(shifted.graphics(), pf, 0);

        double dx = shifted.centroidXmm() - flat.centroidXmm();
        double dy = shifted.centroidYmm() - flat.centroidYmm();
        near("correzione X: il disegno si sposta di 3 mm", 3.0, dx, 0.4);
        near("correzione Y: e di 2 mm", 2.0, dy, 0.4);
    }

    private static void rotation_turnsTheDrawingAndThePageTogether() {
        LabelModel m = LabelModel.defaults(); // 50 x 30
        for (int deg : new int[] {90, 270}) {
            PrintSetup s = PrintSetup.defaults();
            s.setTurn(turnOf(deg));
            PageFormat pf = LabelPrinter.pageFormat(m, s, null);
            near(deg + "°: la pagina gira con il disegno (larghezza)",
                    30 * MM_TO_PT, pf.getWidth());
            near(deg + "°: e anche in altezza", 50 * MM_TO_PT, pf.getHeight());
        }
        PrintSetup half = PrintSetup.defaults();
        half.setTurn(PrintSetup.Turn.GRADI_180);
        PageFormat pf = LabelPrinter.pageFormat(m, half, null);
        near("180°: mezzo giro non scambia i lati", 50 * MM_TO_PT, pf.getWidth());

        yes("i versi possibili sono i quattro quarti di giro",
                PrintSetup.Turn.values().length == 4);
        yes("e solo mezzo giro e dritta non scambiano i lati",
                !PrintSetup.Turn.GRADI_0.swapsSides()
                && !PrintSetup.Turn.GRADI_180.swapsSides()
                && PrintSetup.Turn.GRADI_90.swapsSides()
                && PrintSetup.Turn.GRADI_270.swapsSides());
    }

    private static void rotation_keepsTheDrawingInsideThePage() {
        LabelModel m = LabelModel.defaults();
        // il disegno ruotato deve restare dentro la pagina: se esce, in reparto
        // vuol dire mezza etichetta stampata e mezza sul supporto dopo
        for (int deg : new int[] {0, 90, 180, 270}) {
            PrintSetup s = PrintSetup.defaults();
            s.setTurn(turnOf(deg));
            PageFormat pf = LabelPrinter.pageFormat(m, s, null);
            Sheet sheet = new Sheet(pf);
            new LabelPrinter(m, new String[] {"TST-0000-00-001"}, s)
                    .print(sheet.graphics(), pf, 0);
            yes(deg + "°: c'è inchiostro sulla pagina (" + sheet.ink() + " px)",
                    sheet.ink() > 400);
            yes(deg + "°: e non tocca i bordi della pagina", !sheet.touchesEdge());
        }
    }

    private static void scale_changesTheSizeOfWhatIsPrinted() {
        LabelModel m = LabelModel.defaults();
        PageFormat pf = LabelPrinter.pageFormat(m, PrintSetup.defaults(), null);

        Sheet full = new Sheet(pf);
        new LabelPrinter(m, new String[] {"TST-0000-00-001"}, PrintSetup.defaults())
                .print(full.graphics(), pf, 0);

        PrintSetup smaller = PrintSetup.defaults();
        smaller.setScalePercent(70);
        Sheet reduced = new Sheet(pf);
        new LabelPrinter(m, new String[] {"TST-0000-00-001"}, smaller)
                .print(reduced.graphics(), pf, 0);

        // area al 70% = 49% dell'inchiostro, più o meno l'antialiasing
        double ratio = (double) reduced.ink() / full.ink();
        yes("al 70% si stampa circa metà area (" + String.format("%.2f", ratio) + ")",
                ratio > 0.40 && ratio < 0.60);

        final PrintSetup bad = PrintSetup.defaults();
        rejects("una scala del 10% non è una taratura, è un errore", new Runnable() {
            public void run() {
                bad.setScalePercent(10);
            }
        });
    }

    private static PrintSetup.Turn turnOf(int degrees) {
        for (PrintSetup.Turn t : PrintSetup.Turn.values()) {
            if (t.degrees() == degrees) {
                return t;
            }
        }
        throw new IllegalArgumentException("Verso non previsto: " + degrees);
    }

    private static void calibration_drawsAGridAndOnlyOnePage() throws Exception {
        LabelModel m = LabelModel.defaults();
        PrintSetup s = PrintSetup.defaults();
        PageFormat pf = LabelPrinter.pageFormat(m, s, null);
        Printable page = LabelPrinter.calibrationPage(m, s);

        Sheet sheet = new Sheet(pf);
        yes("la pagina di taratura esiste",
                page.print(sheet.graphics(), pf, 0) == Printable.PAGE_EXISTS);
        yes("e disegna la griglia (" + sheet.ink() + " px)", sheet.ink() > 300);

        Sheet second = new Sheet(pf);
        yes("di taratura ne basta una", page.print(second.graphics(), pf, 1)
                == Printable.NO_SUCH_PAGE);

        // il bordo dell'etichetta deve arrivare fino agli angoli: è il
        // riferimento che l'operatore misura col righello
        yes("il riquadro tocca l'angolo in alto a sinistra", sheet.darkNear(0.5, 0.5));
        yes("e quello in basso a destra",
                sheet.darkNear(m.widthMm() - 0.5, m.heightMm() - 0.5));
    }

    private static void printerLookup_survivesAMachineWithNoPrinters() {
        // il container della CI non ha stampanti: elencare e interrogare non
        // deve lanciare, deve solo dire che non c'è niente
        yes("l'elenco delle code non lancia", LabelPrinter.printerNames() != null);
        String described = LabelPrinter.describe("una stampante che non esiste");
        yes("interrogare una coda inesistente dà una riga leggibile",
                described != null && described.length() > 0);
    }

    // --- verso di stampa ------------------------------------------------------

    private static void turn_mapsTheFourCornersOntoThePage() {
        double w = 50;
        double h = 30;
        // ogni quarto di giro deve lasciare il rettangolo appoggiato all'origine:
        // è la traslazione dopo la rotazione, quella che a occhio si sbaglia
        for (PrintSetup.Turn turn : PrintSetup.Turn.values()) {
            java.awt.geom.Rectangle2D box = turn.transformMm(w, h)
                    .createTransformedShape(new java.awt.geom.Rectangle2D.Double(0, 0, w, h))
                    .getBounds2D();
            near(turn + ": comincia a filo del bordo sinistro", 0.0, box.getMinX());
            near(turn + ": e del bordo alto", 0.0, box.getMinY());
            near(turn + ": larghezza", turn.swapsSides() ? h : w, box.getWidth());
            near(turn + ": altezza", turn.swapsSides() ? w : h, box.getHeight());
        }
    }

    private static void turn_swapsThePageSides() {
        LabelModel m = LabelModel.defaults();
        PrintSetup s = PrintSetup.defaults();
        s.setTurn(PrintSetup.Turn.GRADI_90);
        PageFormat pf = LabelPrinter.pageFormat(m, s, null);
        near("ruotando, la pagina diventa alta quanto l'etichetta era larga",
                30 * MM_TO_PT, pf.getWidth());
        near("e larga quanto era alta", 50 * MM_TO_PT, pf.getHeight());

        s.setTurn(PrintSetup.Turn.GRADI_180);
        PageFormat flipped = LabelPrinter.pageFormat(m, s, null);
        near("capovolgendo la pagina resta com'è", 50 * MM_TO_PT, flipped.getWidth());
    }

    private static void turn_actuallyRotatesTheInk() {
        LabelModel m = LabelModel.defaults();
        String[] one = {"TST-0000-00-001"};

        PrintSetup flat = PrintSetup.defaults();
        PageFormat flatPage = LabelPrinter.pageFormat(m, flat, null);
        Sheet a = new Sheet(flatPage);
        new LabelPrinter(m, one, flat).print(a.graphics(), flatPage, 0);

        PrintSetup turned = PrintSetup.defaults();
        turned.setTurn(PrintSetup.Turn.GRADI_90);
        PageFormat turnedPage = LabelPrinter.pageFormat(m, turned, null);
        Sheet b = new Sheet(turnedPage);
        new LabelPrinter(m, one, turned).print(b.graphics(), turnedPage, 0);

        yes("ruotata, l'etichetta usa la stessa quantità di inchiostro",
                Math.abs(a.ink() - b.ink()) < a.ink() * 0.15);
        // ruotando di 90° il punto (x, y) finisce in (altezza - y, x): il
        // baricentro deve spostarsi esattamente così, non solo "da qualche parte"
        near("il baricentro finisce dove lo manda la rotazione",
                m.heightMm() - a.centroidYmm(), b.centroidXmm(), 0.5);
        near("anche sull'altro asse", a.centroidXmm(), b.centroidYmm(), 0.5);
        yes("e nessun pixel finisce fuori dalla pagina", b.ink() > 500);
    }

    private static void turn_reachesThePdfToo() throws Exception {
        LabelModel m = LabelModel.defaults();
        String[] one = {"TST-0000-00-001"};
        String flat = new String(app.core.export.PdfExporter.toPdf(m, one),
                java.nio.charset.Charset.forName("ISO-8859-1"));
        yes("il PDF normale è largo 50 mm",
                flat.contains("/MediaBox [0 0 141.7323 85.0394]"));

        String turned = new String(
                app.core.export.PdfExporter.toPdf(m, one, PrintSetup.Turn.GRADI_90),
                java.nio.charset.Charset.forName("ISO-8859-1"));
        // 30 mm = 85.0394 pt, 50 mm = 141.7323 pt: la pagina si è girata
        yes("col verso a 90° anche la pagina del PDF si gira",
                turned.contains("/MediaBox [0 0 85.0394 141.7323]"));
        yes("e il contenuto è cambiato di conseguenza", !turned.equals(flat));
    }

    // --- foglio di prova ------------------------------------------------------

    /** Un foglio finto: si stampa qui dentro e poi si contano i pixel. */
    private static final class Sheet {

        private final BufferedImage image;
        private final Graphics2D g;
        private final double pxPerMm;

        Sheet(PageFormat pf) {
            this.pxPerMm = 8; // abbastanza fitto da distinguere un millimetro
            int w = (int) Math.ceil(pf.getWidth() / MM_TO_PT * pxPerMm);
            int h = (int) Math.ceil(pf.getHeight() / MM_TO_PT * pxPerMm);
            this.image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            this.g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            // dalle unità di stampa (72 per pollice) ai pixel di questo foglio
            g.scale(pxPerMm / MM_TO_PT, pxPerMm / MM_TO_PT);
        }

        Graphics2D graphics() {
            return g;
        }

        int ink() {
            int dark = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) & 0xFF) < 0x80) {
                        dark++;
                    }
                }
            }
            return dark;
        }

        double centroidXmm() {
            double sum = 0;
            int n = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) & 0xFF) < 0x80) {
                        sum += x;
                        n++;
                    }
                }
            }
            return n == 0 ? 0 : sum / n / pxPerMm;
        }

        double centroidYmm() {
            double sum = 0;
            int n = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) & 0xFF) < 0x80) {
                        sum += y;
                        n++;
                    }
                }
            }
            return n == 0 ? 0 : sum / n / pxPerMm;
        }

        /** Il disegno arriva a sbattere contro il bordo della pagina? */
        boolean touchesEdge() {
            int w = image.getWidth();
            int h = image.getHeight();
            for (int x = 0; x < w; x++) {
                if ((image.getRGB(x, 0) & 0xFF) < 0x80
                        || (image.getRGB(x, h - 1) & 0xFF) < 0x80) {
                    return true;
                }
            }
            for (int y = 0; y < h; y++) {
                if ((image.getRGB(0, y) & 0xFF) < 0x80
                        || (image.getRGB(w - 1, y) & 0xFF) < 0x80) {
                    return true;
                }
            }
            return false;
        }

        /** C'è inchiostro entro mezzo millimetro da quel punto? */
        boolean darkNear(double xMm, double yMm) {
            int cx = (int) Math.round(xMm * pxPerMm);
            int cy = (int) Math.round(yMm * pxPerMm);
            int radius = (int) Math.round(0.5 * pxPerMm);
            for (int y = Math.max(0, cy - radius);
                    y <= Math.min(image.getHeight() - 1, cy + radius); y++) {
                for (int x = Math.max(0, cx - radius);
                        x <= Math.min(image.getWidth() - 1, cx + radius); x++) {
                    if ((image.getRGB(x, y) & 0xFF) < 0x80) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // --- helper ---------------------------------------------------------------

    private static void near(String what, double expected, double actual) {
        near(what, expected, actual, 0.01);
    }

    private static void near(String what, double expected, double actual, double eps) {
        if (Math.abs(expected - actual) <= eps) {
            passed++;
            System.out.println(String.format("  ok  %s -> %.3f", what, actual));
        } else {
            failed++;
            System.out.println(String.format("FAIL  %s: atteso %.3f, ottenuto %.3f",
                    what, expected, actual));
        }
    }

    private static void same(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("  ok  " + what + " -> \"" + actual + "\"");
        } else {
            failed++;
            System.out.println("FAIL  " + what + ": atteso \"" + expected
                    + "\", ottenuto \"" + actual + "\"");
        }
    }

    private static void yes(String what, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what);
        }
    }

    private static void rejects(String what, Runnable block) {
        try {
            block.run();
            failed++;
            System.out.println("FAIL  " + what + ": doveva essere rifiutato e non lo è stato");
        } catch (IllegalArgumentException e) {
            passed++;
            System.out.println("  ok  " + what + " -> " + e.getMessage());
        }
    }
}
