package app.core;

import app.core.export.Graphics2DCanvas;
import app.core.export.PngExporter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrinterResolution;

/**
 * Stampa il giro di etichette sulla coda scelta dall'operatore.
 *
 * <p><b>Perché questa classe è fatta così.</b> La versione precedente passava
 * il proprio {@link PageFormat} a {@code setPrintable} e poi apriva
 * {@code printDialog()}. La finestra nativa di Windows però restituisce il
 * formato pagina <i>del driver</i>, non quello che le si è dato: il lavoro
 * partiva su una pagina A4 con l'etichetta disegnata in un angolo, la
 * stampante mandava avanti il supporto per un'intera A4, e il risultato erano
 * etichette vuote a raffica con il disegno a cavallo fra due supporti. È
 * esattamente il difetto che si vede in reparto, e il motivo per cui invece il
 * PDF veniva perfetto: nel PDF la pagina la decidiamo noi e nessuno la tocca.
 *
 * <p>Le tre difese, tutte insieme:
 * <ol>
 *   <li>un {@link Book}: il formato pagina viene richiesto per ogni pagina e
 *       resta quello nostro, qualunque cosa faccia la finestra di stampa;</li>
 *   <li>gli attributi {@link MediaPrintableArea} e {@link Media}, così anche il
 *       driver riceve la misura giusta e non solo il disegno;</li>
 *   <li>la modalità immagine: si rasterizza al DPI della stampante e si manda
 *       un'immagine 1:1, che nessun driver può reinterpretare.</li>
 * </ol>
 *
 * <p>E siccome un supporto tarato male non dà errori ma solo etichette storte,
 * c'è {@link #calibrationPage}: una griglia millimetrata che si stampa, si
 * misura con il righello e si traduce in due numeri di correzione.
 */
public final class LabelPrinter implements Printable {

    private static final double MM_TO_PT = 72.0 / 25.4;

    private final LabelModel model;
    private final java.util.List<java.util.Map<String, String>> run;
    private final PrintSetup setup;

    /** Un giro con tutti i campi già risolti, un'etichetta per elemento della lista. */
    public LabelPrinter(LabelModel model, java.util.List<java.util.Map<String, String>> run,
                        PrintSetup setup) {
        if (model == null || run == null || run.isEmpty()) {
            throw new IllegalArgumentException("Niente da stampare.");
        }
        this.model = model;
        this.run = new java.util.ArrayList<java.util.Map<String, String>>(run);
        this.setup = setup == null ? PrintSetup.defaults() : setup;
    }

    /** Comodo per le prove e per i test: un giro con il solo campo di serie. */
    public LabelPrinter(LabelModel model, String[] codes, PrintSetup setup) {
        this(model, toRun(codes), setup);
    }

    private static java.util.List<java.util.Map<String, String>> toRun(String[] codes) {
        if (codes == null || codes.length == 0) {
            throw new IllegalArgumentException("Niente da stampare.");
        }
        java.util.List<java.util.Map<String, String>> out =
                new java.util.ArrayList<java.util.Map<String, String>>();
        for (String code : codes) {
            java.util.Map<String, String> one = new java.util.HashMap<String, String>();
            one.put(LabelField.DEFAULT_NAME, code);
            out.add(one);
        }
        return out;
    }

    // --- stampanti disponibili ------------------------------------------------

    /** I nomi delle code di stampa installate. */
    public static List<String> printerNames() {
        List<String> names = new ArrayList<String>();
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
            names.add(s.getName());
        }
        return names;
    }

    /** La coda con quel nome, oppure {@code null} se non c'è più. */
    public static PrintService serviceNamed(String name) {
        if (name == null || name.isEmpty()) {
            return PrintServiceLookup.lookupDefaultPrintService();
        }
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (name.equals(s.getName())) {
                return s;
            }
        }
        return null;
    }

    /**
     * Che cosa dichiara il driver, in millimetri e in italiano.
     *
     * <p>È la riga da leggere quando le etichette escono storte: se qui compare
     * 210 x 297 mm, il formato in Windows è ancora l'A4 e nessuna correzione
     * di tiro potrà sistemare la stampa.
     */
    public static String describe(String printerName) {
        PrintService service = serviceNamed(printerName);
        if (service == null) {
            return "Nessuna stampante trovata con questo nome.";
        }
        StringBuilder sb = new StringBuilder(service.getName());
        PrinterJob job = PrinterJob.getPrinterJob();
        try {
            job.setPrintService(service);
            PageFormat def = job.defaultPage();
            sb.append(String.format(Locale.ITALIAN, " — pagina dichiarata %.1f x %.1f mm",
                    def.getWidth() / MM_TO_PT, def.getHeight() / MM_TO_PT));
            sb.append(String.format(Locale.ITALIAN, ", area stampabile %.1f x %.1f mm",
                    def.getImageableWidth() / MM_TO_PT, def.getImageableHeight() / MM_TO_PT));
        } catch (PrinterException e) {
            sb.append(" — impossibile interrogare il driver: ").append(e.getMessage());
        }
        Object res = service.getDefaultAttributeValue(PrinterResolution.class);
        if (res instanceof PrinterResolution) {
            sb.append(", ").append(((PrinterResolution) res)
                    .getFeedResolution(PrinterResolution.DPI)).append(" dpi");
        }
        return sb.toString();
    }

    // --- formato pagina -------------------------------------------------------

    /**
     * La pagina che verrà mandata al driver, secondo la modalità scelta.
     *
     * <p>Margini a zero: il driver di un'etichettatrice non ha niente da
     * riscalare, e ogni margine che ci mettiamo noi è un millimetro di
     * disallineamento in più da recuperare a mano.
     */
    public static PageFormat pageFormat(LabelModel model, PrintSetup setup, PrinterJob job) {
        double wMm;
        double hMm;
        switch (setup.pageMode()) {
            case STAMPANTE: {
                PageFormat def = job != null ? job.defaultPage() : new PageFormat();
                wMm = def.getWidth() / MM_TO_PT;
                hMm = def.getHeight() / MM_TO_PT;
                break;
            }
            case PERSONALIZZATA:
                wMm = setup.pageWidthMm();
                hMm = setup.pageHeightMm();
                break;
            default:
                // con un quarto di giro l'etichetta ruotata è alta quanto era
                // larga: la pagina deve seguirla, altrimenti il disegno finisce
                // fuori dal supporto — che è esattamente il difetto da cui si parte
                wMm = setup.turn().swapsSides() ? model.heightMm() : model.widthMm();
                hMm = setup.turn().swapsSides() ? model.widthMm() : model.heightMm();
                break;
        }
        Paper paper = new Paper();
        double w = wMm * MM_TO_PT;
        double h = hMm * MM_TO_PT;
        paper.setSize(w, h);
        paper.setImageableArea(0, 0, w, h);
        PageFormat pf = new PageFormat();
        pf.setOrientation(PageFormat.PORTRAIT);
        pf.setPaper(paper);
        return pf;
    }

    /** Gli attributi di stampa: la misura arriva al driver anche per questa strada. */
    public static PrintRequestAttributeSet attributes(PageFormat pf) {
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        float wMm = (float) (pf.getWidth() / MM_TO_PT);
        float hMm = (float) (pf.getHeight() / MM_TO_PT);
        attrs.add(new MediaPrintableArea(0f, 0f, wMm, hMm, MediaPrintableArea.MM));
        attrs.add(OrientationRequested.PORTRAIT);
        MediaSizeName named = MediaSize.findMedia(wMm, hMm, MediaSize.MM);
        if (named != null) {
            attrs.add(named);
        }
        return attrs;
    }

    // --- stampa ---------------------------------------------------------------

    /**
     * Manda in stampa il giro.
     *
     * @return {@code false} se l'operatore ha annullato la finestra di stampa
     */
    public boolean print(String jobName) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService service = serviceNamed(setup.printerName());
        if (service != null) {
            job.setPrintService(service);
        }
        job.setJobName(jobName);

        PageFormat pf = pageFormat(model, setup, job);
        Book book = new Book();
        book.append(this, pf, run.size());
        job.setPageable(book);

        PrintRequestAttributeSet attrs = attributes(pf);
        if (setup.showDialog() && !job.printDialog(attrs)) {
            return false;
        }
        // il Book viene ripassato dopo la finestra: se il dialogo ha cambiato
        // il formato pagina, questa riga rimette il nostro
        job.setPageable(book);
        job.print(attrs);
        return true;
    }

    /**
     * Porta il sistema di riferimento dall'angolo della pagina a quello del
     * disegno: margine del driver, correzione di tiro e scala.
     *
     * <p>Il <b>verso</b> non si applica qui: quello vive in
     * {@link PrintSetup.Turn} ed è lo stesso oggetto che usano il PDF e la
     * pagina di taratura. Tenerlo in un posto solo è ciò che impedisce alla
     * stampa diretta e al PDF di divergere — che è il difetto da cui è partito
     * tutto questo lavoro.
     */
    static void applyPageTransform(Graphics2D g, PageFormat pf, PrintSetup setup) {
        g.translate(pf.getImageableX(), pf.getImageableY());
        g.translate(setup.offsetXMm() * MM_TO_PT, setup.offsetYMm() * MM_TO_PT);
        double scale = setup.scaleFactor();
        if (scale != 1.0) {
            g.scale(scale, scale);
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= run.size()) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            applyPageTransform(g, pageFormat, setup);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.BLACK);
            g.transform(turnInPoints(model, setup));
            java.util.Map<String, String> values = run.get(pageIndex);
            if (setup.render() == PrintSetup.Render.IMMAGINE) {
                drawAsImage(g, values);
            } else {
                LabelLayout.render(model, values, new Graphics2DCanvas(g, MM_TO_PT));
            }
        } finally {
            g.dispose();
        }
        return PAGE_EXISTS;
    }

    /**
     * Il verso di stampa, portato dai millimetri alle unità di stampa.
     *
     * <p>La geometria non viene rifatta qui: è quella di {@link PrintSetup.Turn},
     * la stessa che usano il PDF e la pagina di taratura. Qui si cambia solo
     * l'unità di misura, con la scala davanti e la sua inversa dietro.
     */
    private static AffineTransform turnInPoints(LabelModel model, PrintSetup setup) {
        AffineTransform t = AffineTransform.getScaleInstance(MM_TO_PT, MM_TO_PT);
        t.concatenate(setup.turn().transformMm(model.widthMm(), model.heightMm()));
        t.concatenate(AffineTransform.getScaleInstance(1 / MM_TO_PT, 1 / MM_TO_PT));
        return t;
    }

    /**
     * Disegna l'etichetta come immagine già rasterizzata al DPI del modello.
     *
     * <p>Il fattore {@code 72/dpi} riporta i pixel dell'immagine alle unità di
     * stampa: l'etichetta esce della misura fisica giusta anche se la
     * risoluzione del driver è un'altra.
     */
    private void drawAsImage(Graphics2D g, java.util.Map<String, String> values) {
        BufferedImage img = PngExporter.toImage(model, values);
        double scale = 72.0 / model.dpi();
        g.drawImage(img, AffineTransform.getScaleInstance(scale, scale), null);
    }

    // --- pagina di taratura ---------------------------------------------------

    /**
     * La pagina di taratura: il bordo dell'etichetta, una griglia da 5 mm, i
     * riferimenti agli angoli e la correzione attualmente impostata.
     *
     * <p>Si stampa, si misura con un righello di quanto è fuori posto il bordo
     * e si scrivono i due numeri nella scheda Stampante. Due minuti, e la
     * taratura smette di essere un tentativo.
     */
    public static Printable calibrationPage(final LabelModel model, final PrintSetup setup) {
        return new Printable() {
            public int print(Graphics graphics, PageFormat pf, int pageIndex) {
                if (pageIndex > 0) {
                    return NO_SUCH_PAGE;
                }
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    applyPageTransform(g, pf, setup);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(Color.BLACK);
                    // la taratura esce nello stesso verso della stampa vera:
                    // misurare una griglia messa per storto non servirebbe a niente
                    g.transform(turnInPoints(model, setup));
                    double w = model.widthMm();
                    double h = model.heightMm();

                    g.setStroke(new BasicStroke(0.3f));
                    for (double x = 0; x <= w + 0.001; x += 5) {
                        g.draw(new Line2D.Double(x * MM_TO_PT, 0, x * MM_TO_PT, h * MM_TO_PT));
                    }
                    for (double y = 0; y <= h + 0.001; y += 5) {
                        g.draw(new Line2D.Double(0, y * MM_TO_PT, w * MM_TO_PT, y * MM_TO_PT));
                    }
                    g.setStroke(new BasicStroke(1.0f));
                    g.draw(new Rectangle2D.Double(0, 0, w * MM_TO_PT, h * MM_TO_PT));

                    // squadretta d'angolo: se non esce tutta, il tiro è sbagliato di lì
                    double tick = 4 * MM_TO_PT;
                    g.setStroke(new BasicStroke(2.0f));
                    g.draw(new Line2D.Double(0, 0, tick, 0));
                    g.draw(new Line2D.Double(0, 0, 0, tick));
                    g.draw(new Line2D.Double(w * MM_TO_PT - tick, h * MM_TO_PT,
                            w * MM_TO_PT, h * MM_TO_PT));
                    g.draw(new Line2D.Double(w * MM_TO_PT, h * MM_TO_PT - tick,
                            w * MM_TO_PT, h * MM_TO_PT));

                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 6));
                    g.drawString(String.format(Locale.ITALIAN, "%.0fx%.0f mm  %d dpi",
                            w, h, model.dpi()), (float) (2 * MM_TO_PT), (float) (5 * MM_TO_PT));
                    g.drawString(String.format(Locale.ITALIAN,
                            "correzione %+.1f / %+.1f mm   %d°   %.0f%%",
                            setup.offsetXMm(), setup.offsetYMm(),
                            setup.turn().degrees(), setup.scalePercent()),
                            (float) (2 * MM_TO_PT), (float) (9 * MM_TO_PT));
                    g.drawString("griglia 5 mm — misura un quadrato: se non è 5,0 correggi la scala",
                            (float) (2 * MM_TO_PT), (float) (13 * MM_TO_PT));
                } finally {
                    g.dispose();
                }
                return PAGE_EXISTS;
            }
        };
    }

    /** Stampa la pagina di taratura sulla coda configurata. */
    public static boolean printCalibration(LabelModel model, PrintSetup setup)
            throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService service = serviceNamed(setup.printerName());
        if (service != null) {
            job.setPrintService(service);
        }
        job.setJobName("Taratura Etichette Custom");
        PageFormat pf = pageFormat(model, setup, job);
        Book book = new Book();
        book.append(calibrationPage(model, setup), pf, 1);
        job.setPageable(book);
        PrintRequestAttributeSet attrs = attributes(pf);
        if (setup.showDialog() && !job.printDialog(attrs)) {
            return false;
        }
        job.setPageable(book);
        job.print(attrs);
        return true;
    }
}
