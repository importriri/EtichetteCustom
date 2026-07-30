package app.core.export;

import app.core.LabelLayout;
import app.core.LabelModel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Rasterizza l'etichetta al DPI impostato e la scrive in PNG. */
public final class PngExporter {

    private PngExporter() {
    }

    /** L'immagine dell'etichetta, sfondo bianco, alla risoluzione del modello. */
    public static BufferedImage toImage(LabelModel model, String code) {
        return toImage(model, values(code));
    }

    /** Come sopra, ma con tutti i campi dell'etichetta. */
    public static BufferedImage toImage(LabelModel model, java.util.Map<String, String> fields) {
        double pxPerMm = model.pxPerMm();
        int w = Math.max(1, (int) Math.round(model.widthMm() * pxPerMm));
        int h = Math.max(1, (int) Math.round(model.heightMm() * pxPerMm));
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(Color.BLACK);
            LabelLayout.render(model, fields, new Graphics2DCanvas(g, pxPerMm));
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * Scrive il PNG su file, con il DPI dichiarato nel blocco {@code pHYs}.
     *
     * <p>Senza, il file si apre a 72 o 96 dpi e un'etichetta da 50 mm diventa
     * larga mezzo schermo appena la si trascina in Word: i pixel sono gli
     * stessi, ma il PNG deve dire quanto sono grandi.
     */
    public static void write(LabelModel model, String code, File target) throws IOException {
        write(model, values(code), target);
    }

    /** Un solo campo, quello di serie: comodo per le prove e per i test. */
    static java.util.Map<String, String> values(String code) {
        java.util.Map<String, String> one = new java.util.HashMap<String, String>();
        one.put(app.core.LabelField.DEFAULT_NAME, code == null ? "" : code);
        return one;
    }

    public static void write(LabelModel model, java.util.Map<String, String> fields, File target)
            throws IOException {
        BufferedImage img = toImage(model, fields);
        java.util.Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new IOException("Nessun encoder PNG disponibile in questa JRE.");
        }
        ImageWriter writer = writers.next();
        ImageOutputStream out = ImageIO.createImageOutputStream(target);
        try {
            writer.setOutput(out);
            IIOMetadata meta = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(img), writer.getDefaultWriteParam());
            String format = "javax_imageio_png_1.0";
            IIOMetadataNode root = new IIOMetadataNode(format);
            IIOMetadataNode phys = new IIOMetadataNode("pHYs");
            // il pHYs vuole pixel per metro, non per pollice
            String perMeter = String.valueOf(Math.round(model.dpi() / 0.0254));
            phys.setAttribute("pixelsPerUnitXAxis", perMeter);
            phys.setAttribute("pixelsPerUnitYAxis", perMeter);
            phys.setAttribute("unitSpecifier", "meter");
            root.appendChild(phys);
            meta.mergeTree(format, root);
            writer.write(null, new IIOImage(img, null, meta), writer.getDefaultWriteParam());
        } finally {
            writer.dispose();
            out.close();
        }
    }
}
