package app.core;

import app.core.export.PdfExporter;
import app.core.export.PngExporter;
import app.core.export.SvgExporter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Suite per gli exporter PNG, SVG e PDF. Runner a mano, niente JUnit, exit 1 se fallisce.
 *
 * <p>Il controllo più importante è l'ultimo: l'immagine rasterizzata viene
 * campionata al centro di ogni modulo e confrontata con la matrice di
 * {@link QrCode}. Se il renderer sposta o scala il QR anche di poco, qui salta
 * fuori — senza bisogno di un decoder esterno.
 */
public final class ExportTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        png_sizeFollowsTheDpi();
        png_declaresItsDpiInTheFile();
        png_inkIsBlackOnWhite();
        svg_isWellFormed_andMeasuredInMm();
        pdf_structureIsExact();
        theScreenTintNeverLeavesTheScreen();
        pdf_onePagePerCode();
        raster_matchesTheQrMatrix_moduleByModule();

        System.out.println("Export: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void png_sizeFollowsTheDpi() {
        BufferedImage img = PngExporter.toImage(LabelModel.defaults(), "TST-0000-00-001");
        // 50 mm a 203 dpi = 399.6 px -> 400; 30 mm = 239.76 -> 240
        yes("50x30 mm a 203 dpi -> 400x240 px",
                img.getWidth() == 400 && img.getHeight() == 240);

        LabelModel m300 = LabelModel.defaults();
        m300.setDpi(300);
        BufferedImage hi = PngExporter.toImage(m300, "TST-0000-00-001");
        yes("stessa etichetta a 300 dpi -> 591x354 px",
                hi.getWidth() == 591 && hi.getHeight() == 354);
    }

    private static void png_declaresItsDpiInTheFile() throws Exception {
        File f = File.createTempFile("etichetta-", ".png");
        f.deleteOnExit();
        PngExporter.write(LabelModel.defaults(), "TST-0000-00-001", f);

        ImageInputStream in = ImageIO.createImageInputStream(f);
        ImageReader reader = ImageIO.getImageReaders(in).next();
        reader.setInput(in);
        IIOMetadata meta = reader.getImageMetadata(0);
        Node root = meta.getAsTree("javax_imageio_png_1.0");
        long perMeter = -1;
        NodeList kids = root.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            if ("pHYs".equals(kids.item(i).getNodeName())) {
                perMeter = Long.parseLong(((Element) kids.item(i))
                        .getAttribute("pixelsPerUnitXAxis"));
            }
        }
        reader.dispose();
        in.close();
        // 203 dpi = 7992 pixel per metro
        yes("il PNG dichiara 203 dpi nel blocco pHYs (" + perMeter + " px/m)",
                perMeter == Math.round(203 / 0.0254));
    }

    private static void png_inkIsBlackOnWhite() {
        LabelModel m = LabelModel.defaults();
        BufferedImage img = PngExporter.toImage(m, "TST-0000-00-001");
        yes("angolo in alto a sinistra bianco", (img.getRGB(0, 0) & 0xFFFFFF) == 0xFFFFFF);
        // il cuore del finder in alto a sinistra del QR: pieno nero garantito
        LabelElement qrElement = firstQr(m);
        QrCode qr = QrCode.encode("TST-0000-00-001", m.ecc());
        double module = qrElement.sizeMm() / qr.size();
        int px = (int) Math.round((qrElement.xMm() + 3.5 * module) * m.pxPerMm());
        int py = (int) Math.round((qrElement.yMm() + 3.5 * module) * m.pxPerMm());
        yes("cuore del finder nero pieno", (img.getRGB(px, py) & 0xFFFFFF) == 0x000000);
    }

    private static void svg_isWellFormed_andMeasuredInMm() throws Exception {
        LabelModel m = LabelModel.defaults();
        m.add(LabelElement.text("Sigla", "qualità", 26, 26, 3)); // l'accento deve
        // sopravvivere al parser XML
        String svg = SvgExporter.toSvg(m, "TST-0000-00-001");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(svg.getBytes(Charset.forName("UTF-8"))));
        Element root = doc.getDocumentElement();
        same("radice", "svg", root.getTagName());
        same("larghezza in millimetri", "50mm", root.getAttribute("width"));
        same("viewBox in millimetri", "0 0 50 30", root.getAttribute("viewBox"));
        NodeList paths = root.getElementsByTagName("path");
        yes("un solo tracciato, non vuoto", paths.getLength() == 1
                && ((Element) paths.item(0)).getAttribute("d").length() > 100);
    }

    private static void pdf_structureIsExact() throws Exception {
        byte[] pdf = PdfExporter.toPdf(LabelModel.defaults(),
                new String[] {"TST-0000-00-001", "TST-2026-07-002", "TST-2026-07-003"});
        String text = new String(pdf, Charset.forName("ISO-8859-1"));
        yes("comincia con %PDF-1.4", text.startsWith("%PDF-1.4"));
        // senza questa riga Acrobat e Chrome "adattano al foglio" per conto loro,
        // ed è così che un'etichetta da 50 mm finisce riscalata su un A4
        yes("il file chiede la stampa a dimensione reale",
                text.contains("/PrintScaling /None"));

        int sx = text.lastIndexOf("startxref");
        int xref = Integer.parseInt(text.substring(sx + 9, text.indexOf("%%EOF", sx)).trim());
        yes("startxref punta alla tabella xref", text.startsWith("xref", xref));

        // ogni offset dichiarato deve cadere esattamente su "N 0 obj"
        boolean allExact = true;
        java.util.regex.Matcher entry = java.util.regex.Pattern
                .compile("(\\d{10}) 00000 n").matcher(text.substring(xref));
        int obj = 0;
        while (entry.find()) {
            obj++;
            int at = Integer.parseInt(entry.group(1));
            if (!text.startsWith(obj + " 0 obj", at)) {
                allExact = false;
            }
        }
        yes("tutti i " + obj + " offset dell'xref sono esatti al byte", obj >= 8 && allExact);

        // 50 mm = 141.7323 pt, 30 mm = 85.0394 pt
        yes("MediaBox alla misura esatta dell'etichetta",
                text.contains("/MediaBox [0 0 141.7323 85.0394]"));
    }

    private static void pdf_onePagePerCode() throws Exception {
        byte[] pdf = PdfExporter.toPdf(LabelModel.defaults(),
                new String[] {"X001", "X002", "X003"});
        String text = new String(pdf, Charset.forName("ISO-8859-1"));
        yes("il conteggio pagine dichiara 3", text.contains("/Count 3"));
        int pages = 0;
        int at = -1;
        while ((at = text.indexOf("/Type /Page /", at + 1)) >= 0) {
            pages++;
        }
        same("una pagina per codice", "3", String.valueOf(pages));

        rejects("nessun codice da esportare", new Runnable() {
            public void run() {
                try {
                    PdfExporter.toPdf(LabelModel.defaults(), new String[0]);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private static void raster_matchesTheQrMatrix_moduleByModule() {
        LabelModel m = LabelModel.defaults();
        String code = "TST-A04-DEMO-000001";
        BufferedImage img = PngExporter.toImage(m, code);
        LabelElement qrElement = firstQr(m);
        QrCode qr = QrCode.encode(code, m.ecc());
        double module = qrElement.sizeMm() / qr.size();
        int wrong = 0;
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                int px = (int) Math.round((qrElement.xMm() + (x + 0.5) * module) * m.pxPerMm());
                int py = (int) Math.round((qrElement.yMm() + (y + 0.5) * module) * m.pxPerMm());
                boolean darkPixel = (img.getRGB(px, py) & 0xFF) < 0x80;
                if (darkPixel != qr.module(x, y)) {
                    wrong++;
                }
            }
        }
        yes("il centro di ognuno dei " + (qr.size * qr.size)
                + " moduli combacia con la matrice (" + wrong + " sbagliati)", wrong == 0);
    }

    // --- helper ---------------------------------------------------------------

    /** Il primo QR del modello: gli exporter non sanno più di "il" QR, ce ne stanno quanti se ne vuole. */
    private static LabelElement firstQr(LabelModel m) {
        for (LabelElement e : m.elements()) {
            if (e.kind() == LabelElement.Kind.QR) {
                return e;
            }
        }
        throw new IllegalStateException("Il modello non ha nessun QR.");
    }

    private static void same(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("  ok  " + what + " -> \"" + actual + "\"");
        } else {
            failed++;
            System.out.println("FAIL  " + what + ": atteso \"" + expected + "\", ottenuto \"" + actual + "\"");
        }
    }

    /**
     * L'anteprima disegna la carta smorzata e l'inchiostro non pieno perché uno
     * schermo scuro non è un foglio. Quel tono non deve raggiungere nessun
     * file: qui si guarda il constant pool degli esportatori, che è dove il
     * riferimento comparirebbe il giorno in cui qualcuno riusa la costante
     * comoda invece di quella giusta. Un controllo sui pixel direbbe solo che
     * oggi va bene; questo dice che non può andare male.
     */
    private static void theScreenTintNeverLeavesTheScreen() throws Exception {
        String[] mustStayPure = {
            "app/core/export/PngExporter",
            "app/core/export/SvgExporter",
            "app/core/export/PdfExporter",
            "app/core/export/Graphics2DCanvas",
            "app/core/LabelPrinter",
            "app/core/LabelLayout",
        };
        for (String name : mustStayPure) {
            byte[] bytecode = classFile(name);
            String pool = new String(bytecode, Charset.forName("ISO-8859-1"));
            yes(name.substring(name.lastIndexOf('/') + 1) + " non conosce il tema",
                    !pool.contains("app/config/AppTheme") && !pool.contains("app/ui/"));
        }

        // e il bianco che finisce nei file resta quello vero, non il tono
        BufferedImage img = PngExporter.toImage(LabelModel.defaults(), "TST-0000-00-001");
        yes("il PNG esce su bianco pieno, non sul tono dell'anteprima",
                (img.getRGB(0, 0) & 0xFFFFFF) == 0xFFFFFF);
        String svg = SvgExporter.toSvg(LabelModel.defaults(), "TST-0000-00-001");
        yes("l'SVG dichiara #ffffff e #000000",
                svg.contains("#ffffff") && svg.contains("#000000"));
    }

    private static byte[] classFile(String internalName) throws Exception {
        java.io.InputStream in = ExportTest.class.getClassLoader()
                .getResourceAsStream(internalName + ".class");
        if (in == null) {
            throw new IllegalStateException("classe non trovata sul classpath: " + internalName);
        }
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
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
        } catch (IllegalStateException e) {
            passed++;
            System.out.println("  ok  " + what + " -> " + e.getCause().getMessage());
        }
    }
}
