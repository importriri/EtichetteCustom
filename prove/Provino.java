package prove;

import app.modello.Etichetta;
import app.archivio.Archivio;
import app.archivio.Registro;
import app.modello.Impostazioni;
import app.modello.Libreria;
import app.render.QrVero;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.banco.Banco;
import app.ui.vetrina.Vetrina;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Scrive un PNG con le due schermate, senza aprire finestre.
 * Serve per guardare il risultato dove Java non c'e' - e per accorgersi
 * subito se una schermata esce vuota.
 */
public final class Provino {

    private Provino() { }

    public static void main(String[] args) throws Exception {
        File fuori = new File(args.length > 0 ? args[0] : "provino.png");
        SorgenteQr qr = new QrVero();
        List<Etichetta> tutte = Libreria.iniziale();

        int w = 1280;
        int hV = 760;
        int hB = 760;
        int titolo = 34;

        java.io.File casa = java.io.File.createTempFile("provino", "");
        if (!casa.delete() || !casa.mkdirs()) {
            throw new IllegalStateException("non riesco a preparare " + casa);
        }
        casa.deleteOnExit();
        app.archivio.Registro registro = new app.archivio.Registro(casa);
        Etichetta primo = tutte.get(0);
        registro.annota(primo, primo.serie().giro(12), "Datamax E-4203");
        primo.serie().consuma(12);
        Etichetta secondo = tutte.get(1);
        registro.annota(secondo, secondo.serie().giro(8), "Datamax E-4203");
        secondo.serie().consuma(8);

        Vetrina v = new Vetrina(tutte, qr, comandiFinti(), registro);
        Banco b = new Banco(tutte.get(0), qr, new Impostazioni(),
                new Archivio(nuovaCartella()), new Registro(nuovaCartella()), () -> { });

        BufferedImage im = new BufferedImage(w, hV + hB + titolo * 2 + 24,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0xE9EBF0));
            g.fillRect(0, 0, im.getWidth(), im.getHeight());

            int y = 0;
            y = intestazione(g, "01  Vetrina", y, titolo);
            y = incolla(g, v, w, hV, y);
            y += 12;
            y = intestazione(g, "02  Banco di lavoro", y, titolo);
            incolla(g, b, w, hB, y);
        } finally {
            g.dispose();
        }
        ImageIO.write(im, "png", fuori);
        System.out.println("scritto " + fuori.getAbsolutePath()
                + "  (" + im.getWidth() + "x" + im.getHeight() + ")");
    }

    private static int intestazione(Graphics2D g, String testo, int y, int alto) {
        g.setColor(new Color(0x8C8FA1));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.drawString(testo, 8, y + alto - 12);
        return y + alto;
    }

    private static int incolla(Graphics2D g, java.awt.Component c, int w, int h, int y) {
        ProvaSchermate.impagina(c, w, h);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gc = im.createGraphics();
        try {
            gc.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            gc.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gc.setColor(Stile.BASE);
            gc.fillRect(0, 0, w, h);
            c.paint(gc);
        } finally {
            gc.dispose();
        }
        g.drawImage(im, 0, y, null);
        g.setColor(new Color(0xCCD0DA));
        g.drawRect(0, y, w - 1, h - 1);
        return y + h;
    }

    /** Comandi che non fanno niente: alle prove interessa il disegno, non i clic. */
    static Vetrina.Comandi comandiFinti() {
        return new Vetrina.Comandi() {
            @Override
            public void apri(app.modello.Etichetta e) { }

            @Override
            public void modifica(app.modello.Etichetta e) { }

            @Override
            public void nuova() { }

            @Override
            public void rinomina(app.modello.Etichetta e) { }

            @Override
            public void duplica(app.modello.Etichetta e) { }

            @Override
            public void elimina(app.modello.Etichetta e) { }

            @Override
            public void stampante() { }

            @Override
            public void impostazioni() { }
        };
    }

    /** Una cartella usa e getta, cosi' le prove non sporcano niente. */
    static java.io.File nuovaCartella() {
        try {
            java.io.File f = java.io.File.createTempFile("etichette-prova", "");
            if (!f.delete() || !f.mkdirs()) {
                throw new IllegalStateException("non riesco a preparare " + f);
            }
            f.deleteOnExit();
            return f;
        } catch (java.io.IOException rotta) {
            throw new IllegalStateException(rotta);
        }
    }
}
