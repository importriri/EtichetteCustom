package prove;

import app.modello.Etichetta;
import app.modello.Libreria;
import app.render.Disegno;
import app.render.QrVero;
import app.render.SorgenteQr;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Renderer regression coverage. Gallery previews and the editor canvas must
 * show the same label geometry rather than separate approximations.
 */
public final class ProvaDisegno {

    private ProvaDisegno() { }

    private static final SorgenteQr QR = new QrVero();

    public static void esegui() {
        Prove.suite("Renderer geometry");

        List<Etichetta> tutte = Libreria.iniziale();
        for (Etichetta e : tutte) {
            BufferedImage im = rendi(e, 8);
            double inchiostro = quotaInchiostro(im);
            Prove.vero(e.nome() + ": contains rendered ink", inchiostro > 0.01);
            Prove.vero(e.nome() + ": paper is not filled black", inchiostro < 0.6);
            Prove.vero(e.nome() + ": top-left corner remains paper",
                    bianco(im.getRGB(0, 0)));
        }

        BufferedImage a = rendi(tutte.get(0), 8);
        BufferedImage b = rendi(tutte.get(0), 8);
        Prove.vero("two renders of the same label are identical", identiche(a, b));

        /* The physical ink footprint must stay stable across render scales. */
        for (Etichetta e : tutte) {
            double[] piccola = impronta(rendi(e, 4), 4);
            double[] grande = impronta(rendi(e, 14), 14);
            Prove.vicino(e.nome() + ": left ink offset",
                    piccola[0], grande[0], 1.0);
            Prove.vicino(e.nome() + ": top ink offset",
                    piccola[1], grande[1], 1.0);
            Prove.vicino(e.nome() + ": ink footprint width",
                    piccola[2], grande[2], 1.0);
            Prove.vicino(e.nome() + ": ink footprint height",
                    piccola[3], grande[3], 1.0);
        }
    }

    static BufferedImage rendi(Etichetta e, double mmPx) {
        int w = (int) Math.round(e.larghezza() * mmPx);
        int h = (int) Math.round(e.altezza() * mmPx);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            Disegno.disegna(g, e, mmPx, QR, 0);
        } finally {
            g.dispose();
        }
        return im;
    }

    /** Ink x, y, width and height in millimetres. */
    static double[] impronta(BufferedImage im, double mmPx) {
        int minX = im.getWidth();
        int minY = im.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                if (!bianco(im.getRGB(x, y))) {
                    if (x < minX) { minX = x; }
                    if (y < minY) { minY = y; }
                    if (x > maxX) { maxX = x; }
                    if (y > maxY) { maxY = y; }
                }
            }
        }
        if (maxX < 0) {
            return new double[] { 0, 0, 0, 0 };
        }
        return new double[] { minX / mmPx, minY / mmPx,
                (maxX - minX + 1) / mmPx, (maxY - minY + 1) / mmPx };
    }

    static double quotaInchiostro(BufferedImage im) {
        int scuri = 0;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                if (!bianco(im.getRGB(x, y))) {
                    scuri++;
                }
            }
        }
        return scuri / (double) (im.getWidth() * im.getHeight());
    }

    static boolean bianco(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + g + b) / 3 > 200;
    }

    static boolean identiche(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
