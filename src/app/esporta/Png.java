package app.esporta;

import app.modello.Etichetta;
import app.render.Disegno;
import app.render.SorgenteQr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Raster export at the requested resolution. */
public final class Png {

    private Png() { }

    public static BufferedImage immagine(Etichetta eti, SorgenteQr qr, int dpi, int copia) {
        if (dpi < 50 || dpi > 2400) {
            throw new IllegalArgumentException("risoluzione fuori scala: " + dpi + " dpi");
        }
        double mmPx = dpi / 25.4;
        int w = Math.max(1, (int) Math.round(eti.larghezza() * mmPx));
        int h = Math.max(1, (int) Math.round(eti.altezza() * mmPx));
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Disegno.disegna(g, eti, mmPx, qr, copia);
        } finally {
            g.dispose();
        }
        return im;
    }

    public static void scrivi(File dove, Etichetta eti, SorgenteQr qr, int dpi, int copia)
            throws IOException {
        if (!ImageIO.write(immagine(eti, qr, dpi, copia), "png", dove)) {
            throw new IOException("nessun encoder PNG disponibile");
        }
    }
}
