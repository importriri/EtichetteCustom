package prove;

import app.esporta.Esportazione;
import app.esporta.Png;
import app.esporta.Svg;
import app.modello.Etichetta;
import app.modello.Libreria;
import app.render.QrVero;
import app.render.SorgenteQr;
import app.stampa.StampaGiro;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.util.List;

/**
 * Printing and export regression coverage. Consecutive pages in a progressive
 * run must render different payloads while repeat rendering stays deterministic.
 */
public final class ProvaStampa {

    private ProvaStampa() { }

    private static final SorgenteQr QR = new QrVero();

    public static void esegui() throws Exception {
        stampa();
        esporta();
    }

    private static void stampa() throws Exception {
        Prove.suite("Print pages and dimensions");

        Etichetta eti = Libreria.articolo();
        StampaGiro giro = new StampaGiro(eti, QR, 12);
        Prove.uguale("twelve copies produce twelve pages", 12, giro.getNumberOfPages());

        PageFormat f = giro.getPageFormat(0);
        Prove.vicino("page width matches the label in points",
                50 * 72 / 25.4, f.getWidth(), 0.01);
        Prove.vicino("page height matches the label", 30 * 72 / 25.4, f.getHeight(), 0.01);
        Prove.vicino("printable area starts at the page origin", 0, f.getImageableX(), 0.01);
        Prove.vicino("printable area has no horizontal margins",
                f.getWidth(), f.getImageableWidth(), 0.01);
        Prove.vicino("printable area has no vertical margins", f.getHeight(), f.getImageableHeight(), 0.01);

        BufferedImage prima = pagina(giro, f, 0);
        BufferedImage seconda = pagina(giro, f, 1);
        Prove.vero("the first page contains ink", ProvaDisegno.quotaInchiostro(prima) > 0.01);
        Prove.vero("the second page differs because the sequence advances",
                !ProvaDisegno.identiche(prima, seconda));

        BufferedImage ancoraPrima = pagina(giro, f, 0);
        Prove.vero("rendering the same page again is deterministic",
                ProvaDisegno.identiche(prima, ancoraPrima));

        Prove.uguale("pages beyond the run are rejected",
                Printable.NO_SUCH_PAGE, giro.print(nuovoContesto(10, 10), f, 12));

        Prove.esplode("a zero-copy print run is rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new StampaGiro(Libreria.articolo(), QR, 0);
                    }
                });
    }

    private static void esporta() throws Exception {
        Prove.suite("PNG, SVG and PDF export");

        Etichetta eti = Libreria.articolo();
        File dove = ProvaArchivio.temporanea("export");

        BufferedImage png = Png.immagine(eti, QR, 600, 0);
        Prove.uguale("a 50 mm label is 1181 px wide at 600 dpi", 1181, png.getWidth());
        Prove.uguale("the 30 mm height is 709 px at 600 dpi", 709, png.getHeight());
        Prove.vero("the PNG contains rendered ink", ProvaDisegno.quotaInchiostro(png) > 0.01);
        Prove.esplode("an unreasonable export resolution is rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        Png.immagine(Libreria.articolo(), QR, 100000, 0);
                    }
                });

        String svg = Svg.testo(eti, 0);
        Prove.vero("SVG output starts as XML", svg.startsWith("<?xml"));
        Prove.vero("SVG dimensions are expressed in millimetres",
                svg.contains("width=\"50mm\"") && svg.contains("height=\"30mm\""));
        Prove.vero("progressive readable text is present in SVG",
                svg.contains("584700349"));
        Prove.vero("QR output stays vector in SVG",
                svg.contains("<rect") && !svg.contains("<image"));
        Prove.vero("both readable text lines are present",
                conta(svg, "<tspan") >= 2);

        List<File> tuttiPng = Esportazione.esporta(new File(dove, "giro.png"), eti, QR,
                Esportazione.Come.PNG, 3, 300);
        Prove.uguale("three labels produce three PNG files", 3, tuttiPng.size());
        Prove.vero("PNG files are numbered in order", tuttiPng.get(0).getName().equals("giro-1.png"));
        Prove.vero("the final PNG is actually written", tuttiPng.get(2).length() > 100);

        List<File> pdf = Esportazione.esporta(new File(dove, "giro.pdf"), eti, QR,
                Esportazione.Come.PDF, 3, 300);
        Prove.uguale("one PDF contains the complete run", 1, pdf.size());
        byte[] dentro = byteDi(pdf.get(0));
        Prove.vero("output starts with the PDF signature", new String(dentro, 0, 8, "ISO-8859-1")
                .startsWith("%PDF-1."));
        String testa = new String(dentro, "ISO-8859-1");
        Prove.vero("PDF declares three pages", testa.contains("/Count 3"));
        Prove.vero("PDF page dimensions match the label",
                testa.contains("/MediaBox[0 0 141.732 85.039]"));
        Prove.vero("PDF image data is compressed", testa.contains("/FlateDecode"));
        Prove.vero("PDF ends with the EOF marker", testa.trim().endsWith("%%EOF"));
    }

    private static BufferedImage pagina(StampaGiro giro, PageFormat f, int indice) {
        int w = (int) Math.ceil(f.getWidth() * 4);
        int h = (int) Math.ceil(f.getHeight() * 4);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.scale(4, 4);
            giro.print(g, f, indice);
        } finally {
            g.dispose();
        }
        return im;
    }

    private static Graphics2D nuovoContesto(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).createGraphics();
    }

    private static int conta(String dove, String cosa) {
        int quanti = 0;
        int da = 0;
        while ((da = dove.indexOf(cosa, da)) >= 0) {
            quanti++;
            da += cosa.length();
        }
        return quanti;
    }

    private static byte[] byteDi(File f) throws Exception {
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
