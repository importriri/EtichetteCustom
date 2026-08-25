package prove;

import app.modello.Etichetta;
import app.archivio.Archivio;
import app.archivio.Registro;
import app.modello.Impostazioni;
import app.modello.Libreria;
import app.render.QrVero;
import app.render.SorgenteQr;
import app.ui.banco.Banco;
import app.ui.vetrina.Tessera;
import app.ui.vetrina.Vetrina;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Le due schermate si costruiscono e si disegnano senza schermo.
 * Non e' una prova di bellezza: e' la prova che nulla esplode e che
 * quello che appare non e' un rettangolo vuoto.
 */
public final class ProvaSchermate {

    private ProvaSchermate() { }

    private static final SorgenteQr QR = new QrVero();

    public static void esegui() {
        Prove.suite("Schermate - vetrina e banco");

        List<Etichetta> tutte = Libreria.iniziale();

        Tessera t = new Tessera(tutte.get(0), QR);
        BufferedImage im = ritratto(t, t.getPreferredSize().width,
                t.getPreferredSize().height);
        Prove.vero("la tessera disegna qualcosa", colori(im) > 3);
        Prove.vero("la tessera mostra della carta bianca", contieneBianco(im));

        Vetrina v = new Vetrina(tutte, QR, comandiFinti());
        BufferedImage iv = ritratto(v, 1200, 700);
        Prove.vero("la vetrina non e' una pagina vuota", colori(iv) > 6);

        Banco b = new Banco(tutte.get(0), QR, new Impostazioni(),
                new Archivio(nuovaCartella()), new Registro(nuovaCartella()), () -> { });
        BufferedImage ib = ritratto(b, 1280, 760);
        Prove.vero("il banco non e' una pagina vuota", colori(ib) > 6);
        Prove.vero("il banco mostra la carta bianca", contieneBianco(ib));
    }

    private static BufferedImage ritratto(Component c, int w, int h) {
        impagina(c, w, h);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            c.paint(g);
        } finally {
            g.dispose();
        }
        return im;
    }

    /** Impagina a mano: senza finestra vera, validate() non farebbe nulla. */
    static void impagina(Component c, int w, int h) {
        c.setSize(w, h);
        if (c instanceof Container) {
            Container k = (Container) c;
            k.doLayout();
            for (Component figlio : k.getComponents()) {
                impagina(figlio, figlio.getWidth(), figlio.getHeight());
            }
        }
    }

    private static int colori(BufferedImage im) {
        java.util.Set<Integer> visti = new java.util.HashSet<Integer>();
        for (int y = 0; y < im.getHeight(); y += 3) {
            for (int x = 0; x < im.getWidth(); x += 3) {
                visti.add(im.getRGB(x, y));
                if (visti.size() > 64) {
                    return visti.size();
                }
            }
        }
        return visti.size();
    }

    private static boolean contieneBianco(BufferedImage im) {
        for (int y = 0; y < im.getHeight(); y += 2) {
            for (int x = 0; x < im.getWidth(); x += 2) {
                if (im.getRGB(x, y) == 0xFFFFFFFF) {
                    return true;
                }
            }
        }
        return false;
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
