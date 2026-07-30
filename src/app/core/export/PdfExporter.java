package app.core.export;

import app.core.LabelLayout;
import app.core.LabelModel;
import app.core.PrintSetup;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Esporta l'etichetta in PDF senza librerie esterne.
 *
 * <p>Il file è un PDF 1.4 minimo: catalogo, pagina della misura esatta
 * dell'etichetta e un unico flusso di contenuto con il tracciato. Niente font
 * incorporati, perché i testi arrivano qui già come profili vettoriali — quindi
 * il PDF si apre uguale ovunque e pesa pochi kilobyte.
 *
 * <p>Attenzione a un dettaglio: nel PDF l'asse {@code y} cresce verso l'alto,
 * nell'etichetta verso il basso. La conversione avviene qui e in nessun altro posto.
 */
public final class PdfExporter implements LabelCanvas {

    private static final double MM_TO_PT = 72.0 / 25.4;

    private final StringBuilder content = new StringBuilder();
    private final double heightMm;

    private PdfExporter(double heightMm) {
        this.heightMm = heightMm;
    }

    /** Costruisce il PDF di una singola etichetta. */
    public static byte[] toPdf(LabelModel model, String code) throws IOException {
        return toPdf(model, new String[] {code});
    }

    /**
     * Il PDF del giro, già ruotato come uscirà dalla stampante.
     *
     * <p>In reparto il PDF si stampa dal browser, e per farlo uscire dritto
     * bisognava scegliere a mano "Orientamento orizzontale" nella finestra di
     * stampa. Se il verso è già dentro il file, quella scelta non serve più:
     * una cosa in meno da ricordare, e una in meno da sbagliare.
     */
    /** Un giro con tutti i campi risolti, un'etichetta per pagina. */
    public static byte[] toPdf(LabelModel model, java.util.List<java.util.Map<String, String>> run,
            PrintSetup.Turn turn) throws IOException {
        if (run == null || run.isEmpty()) {
            throw new IllegalArgumentException("Nessun codice da esportare.");
        }
        return build(model, null, run, turn == null ? PrintSetup.Turn.GRADI_0 : turn);
    }

    public static byte[] toPdf(LabelModel model, String[] codes, PrintSetup.Turn turn)
            throws IOException {
        return build(model, codes, null, turn == null ? PrintSetup.Turn.GRADI_0 : turn);
    }

    /**
     * Costruisce un PDF con una pagina per etichetta: è il formato comodo per
     * mandare un giro intero in stampa o per archiviarlo.
     */
    public static byte[] toPdf(LabelModel model, String[] codes) throws IOException {
        return build(model, codes, null, PrintSetup.Turn.GRADI_0);
    }

    /**
     * Il costruttore vero: riceve o un giro di codici semplici, o un giro di
     * campi risolti. Ne arriva sempre esattamente uno dei due, e il resto del
     * metodo non ha bisogno di sapere quale.
     */
    private static byte[] build(LabelModel model, String[] codes,
            List<java.util.Map<String, String>> run, PrintSetup.Turn turn) throws IOException {
        int count = codes != null ? codes.length : (run != null ? run.size() : 0);
        if (count == 0) {
            throw new IllegalArgumentException("Nessun codice da esportare.");
        }
        double pageWmm = turn.swapsSides() ? model.heightMm() : model.widthMm();
        double pageHmm = turn.swapsSides() ? model.widthMm() : model.heightMm();
        double wPt = pageWmm * MM_TO_PT;
        double hPt = pageHmm * MM_TO_PT;

        List<byte[]> objects = new ArrayList<byte[]>();
        int pageCount = count;
        int firstPageObj = 3;
        int firstContentObj = firstPageObj + pageCount;

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(firstPageObj + i).append(" 0 R ");
        }

        // /PrintScaling /None è la riga che fa uscire l'etichetta della misura
        // giusta senza toccare niente nella finestra di stampa: dice al lettore
        // PDF di stampare 1:1 invece di "adatta alla pagina", che è il default di
        // Chrome e di Acrobat ed è il motivo per cui un'etichetta da 50 mm
        // finiva riscalata su tutto il foglio.
        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R "
                + "/ViewerPreferences << /PrintScaling /None /NumCopies 1 >> >>"));
        objects.add(ascii("<< /Type /Pages /Count " + pageCount + " /Kids [" + kids.toString().trim() + "] >>"));

        List<byte[]> streams = new ArrayList<byte[]>();
        for (int i = 0; i < pageCount; i++) {
            PdfExporter canvas = new PdfExporter(pageHmm);
            LabelCanvas turned = TransformedCanvas.wrap(canvas,
                    turn.transformMm(model.widthMm(), model.heightMm()));
            if (codes != null) {
                LabelLayout.render(model, codes[i], turned);
            } else {
                LabelLayout.render(model, run.get(i), turned);
            }
            streams.add(ascii(canvas.content.toString()));
            objects.add(ascii(String.format(Locale.ROOT,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %s %s] /Contents %d 0 R "
                    + "/Resources << >> >>",
                    fmt(wPt), fmt(hPt), firstContentObj + i)));
        }
        for (byte[] s : streams) {
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            obj.write(ascii("<< /Length " + s.length + " >>\nstream\n"));
            obj.write(s);
            obj.write(ascii("\nendstream"));
            objects.add(obj.toByteArray());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ascii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n"));
        int[] offsets = new int[objects.size() + 1];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = out.size();
            out.write(ascii((i + 1) + " 0 obj\n"));
            out.write(objects.get(i));
            out.write(ascii("\nendobj\n"));
        }
        int xref = out.size();
        StringBuilder table = new StringBuilder();
        table.append("xref\n0 ").append(objects.size() + 1).append("\n");
        table.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            table.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        }
        table.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        table.append("startxref\n").append(xref).append("\n%%EOF\n");
        out.write(ascii(table.toString()));
        return out.toByteArray();
    }

    /** Scrive il PDF su file. */
    /** Scrive su file un giro con tutti i campi risolti. */
    public static void write(LabelModel model, List<java.util.Map<String, String>> run,
            File target, PrintSetup.Turn turn) throws IOException {
        java.io.FileOutputStream out = new java.io.FileOutputStream(target);
        try {
            out.write(toPdf(model, run, turn));
        } finally {
            out.close();
        }
    }

    public static void write(LabelModel model, String[] codes, File target) throws IOException {
        write(model, codes, target, PrintSetup.Turn.GRADI_0);
    }

    /** Scrive il PDF su file, nel verso in cui va stampato. */
    public static void write(LabelModel model, String[] codes, File target,
            PrintSetup.Turn turn) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(target);
            os.write(toPdf(model, codes, turn));
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    @Override
    public void fill(Shape shapeMm) {
        double[] c = new double[6];
        PathIterator it = shapeMm.getPathIterator(null);
        boolean evenOdd = it.getWindingRule() == PathIterator.WIND_EVEN_ODD;
        boolean any = false;
        double curX = 0;
        double curY = 0;
        while (!it.isDone()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO:
                    moveOrLine(c[0], c[1], "m");
                    curX = c[0];
                    curY = c[1];
                    any = true;
                    break;
                case PathIterator.SEG_LINETO:
                    moveOrLine(c[0], c[1], "l");
                    curX = c[0];
                    curY = c[1];
                    any = true;
                    break;
                case PathIterator.SEG_QUADTO: {
                    // Il PDF non conosce le quadratiche: si alzano a cubiche, che è
                    // una conversione esatta, non un'approssimazione.
                    double c1x = curX + 2.0 / 3.0 * (c[0] - curX);
                    double c1y = curY + 2.0 / 3.0 * (c[1] - curY);
                    double c2x = c[2] + 2.0 / 3.0 * (c[0] - c[2]);
                    double c2y = c[3] + 2.0 / 3.0 * (c[1] - c[3]);
                    cubic(c1x, c1y, c2x, c2y, c[2], c[3]);
                    curX = c[2];
                    curY = c[3];
                    any = true;
                    break;
                }
                case PathIterator.SEG_CUBICTO:
                    cubic(c[0], c[1], c[2], c[3], c[4], c[5]);
                    curX = c[4];
                    curY = c[5];
                    any = true;
                    break;
                default:
                    content.append("h\n");
                    break;
            }
            it.next();
        }
        if (any) {
            content.append(evenOdd ? "f*\n" : "f\n");
        }
    }

    private void cubic(double x1, double y1, double x2, double y2, double x3, double y3) {
        content.append(pt(x1)).append(" ").append(ptY(y1)).append(" ")
               .append(pt(x2)).append(" ").append(ptY(y2)).append(" ")
               .append(pt(x3)).append(" ").append(ptY(y3)).append(" c\n");
    }

    private void moveOrLine(double x, double y, String op) {
        content.append(pt(x)).append(" ").append(ptY(y)).append(" ").append(op).append("\n");
    }

    private String pt(double mm) {
        return fmt(mm * MM_TO_PT);
    }

    private String ptY(double mm) {
        return fmt((heightMm - mm) * MM_TO_PT);
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
