package app.render;

import app.codice.Code128;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.stile.Stile;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;

/**
 * L'unico posto in cui un'etichetta diventa pixel.
 *
 * Lo usano la tessera della vetrina, il foglio grande del banco e l'export:
 * cambia solo quanti pixel vale un millimetro. Se la tessera e la stampa
 * fossero disegnate da due codici diversi, prima o poi mostrerebbero due
 * cose diverse; cosi' non possono.
 */
public final class Disegno {

    private Disegno() { }

    private static final String[] PREFERITE = { "DejaVu Sans", "Verdana", "Tahoma", "Arial" };
    private static String famiglia;

    /**
     * Il carattere della stampa. Si sceglie una volta sola fra quelli
     * davvero installati: su Windows di reparto DejaVu spesso non c'e',
     * e un font mancante vuol dire etichette diverse da quelle viste a video.
     */
    public static synchronized String famiglia() {
        if (famiglia == null) {
            famiglia = "SansSerif";
            try {
                String[] presenti = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames();
                for (int i = 0; i < PREFERITE.length && famiglia.equals("SansSerif"); i++) {
                    for (String p : presenti) {
                        if (p.equalsIgnoreCase(PREFERITE[i])) {
                            famiglia = p;
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                famiglia = "SansSerif";
            }
        }
        return famiglia;
    }

    /** Disegna l'etichetta con l'angolo in alto a sinistra nell'origine di g. */
    public static void disegna(Graphics2D g, Etichetta eti, double mmPx,
                               SorgenteQr qr, int copia) {
        int w = (int) Math.round(eti.larghezza() * mmPx);
        int h = (int) Math.round(eti.altezza() * mmPx);
        g.setColor(Stile.CARTA);
        g.fillRect(0, 0, w, h);
        for (Elemento e : eti.elementi()) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                java.awt.geom.Rectangle2D.Double base = Ingombri.base(g2, eti, e, mmPx, copia);
                g2.translate(e.x() * mmPx, e.y() * mmPx);
                switch (e.rotazione()) {
                    case 90:
                        g2.translate(base.height * mmPx, 0);
                        g2.rotate(Math.PI / 2);
                        break;
                    case 180:
                        g2.translate(base.width * mmPx, base.height * mmPx);
                        g2.rotate(Math.PI);
                        break;
                    case 270:
                        g2.translate(0, base.width * mmPx);
                        g2.rotate(3 * Math.PI / 2);
                        break;
                    default:
                        break;
                }
                disegnaElemento(g2, eti, e, mmPx, qr, copia);
            } finally {
                g2.dispose();
            }
        }
    }

    private static void disegnaElemento(Graphics2D g, Etichetta eti, Elemento e,
                                        double mmPx, SorgenteQr qr, int copia) {
        g.setColor(Stile.INCHIOSTRO);
        double x = 0;
        double y = 0;
        switch (e.tipo()) {
            case QR:
                disegnaQr(g, qr, eti.contenuto(e, copia), e.correzione(),
                        x, y, e.larghezza() * mmPx);
                break;
            case BARCODE:
                disegnaBarcode(g, eti.contenuto(e, copia), x, y,
                        e.larghezza() * mmPx, e.altezza() * mmPx);
                break;
            case LINEA:
                g.fillRect(0, 0, (int) Math.round(e.larghezza() * mmPx),
                        Math.max(1, (int) Math.round(e.altezza() * mmPx)));
                break;
            default:
                disegnaScritta(g, eti, e, mmPx, copia, x, y);
                break;
        }
    }

    private static void disegnaScritta(Graphics2D g, Etichetta eti, Elemento e,
                                       double mmPx, int copia, double x, double y) {
        String testo = eti.contenuto(e, copia);
        Misuratore m = misuratore(g, mmPx);
        Testo.Esito esito = Testo.componi(testo, e.larghezza(), e.corpo(),
                e.massimoRighe(), e.grassetto(), m);

        Font f = font(esito.corpo() * mmPx, e.grassetto());
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        double interlinea = fm.getAscent() + fm.getDescent();
        double linea = y + fm.getAscent();
        for (String riga : esito.righe()) {
            g.drawString(riga, (int) Math.round(x), (int) Math.round(linea));
            linea += interlinea;
        }
    }

    private static void disegnaQr(Graphics2D g, SorgenteQr qr, String contenuto,
                                  app.codice.Correzione livello,
                                  double x, double y, double lato) {
        if (qr == null) {
            return;
        }
        boolean[][] m = qr.matrice(contenuto, livello);
        if (m == null || m.length == 0) {
            return;
        }
        int n = m.length;
        double passo = lato / n;
        for (int r = 0; r < n; r++) {
            int c = 0;
            while (c < n) {
                if (m[r][c]) {
                    int larghi = 1;
                    while (c + larghi < n && m[r][c + larghi]) {
                        larghi++;
                    }
                    int px = (int) Math.round(x + c * passo);
                    int py = (int) Math.round(y + r * passo);
                    int pw = (int) Math.round(x + (c + larghi) * passo) - px;
                    int ph = (int) Math.round(y + (r + 1) * passo) - py;
                    g.fillRect(px, py, Math.max(1, pw), Math.max(1, ph));
                    c += larghi;
                } else {
                    c++;
                }
            }
        }
    }

    private static void disegnaBarcode(Graphics2D g, String contenuto, double x, double y,
                                       double larghezza, double altezza) {
        int[] tratti;
        try {
            tratti = Code128.tratti(contenuto);
        } catch (RuntimeException nonCodificabile) {
            /* meglio una casella vuota che barre che dicono un'altra cosa */
            g.drawRect((int) Math.round(x), (int) Math.round(y),
                    (int) Math.round(larghezza), (int) Math.round(altezza));
            return;
        }
        int totale = 0;
        for (int t : tratti) {
            totale += t;
        }
        double u = larghezza / totale;
        double cx = x;
        for (int i = 0; i < tratti.length; i++) {
            if (i % 2 == 0) {
                g.fillRect((int) Math.round(cx), (int) Math.round(y),
                        Math.max(1, (int) Math.round(cx + tratti[i] * u) - (int) Math.round(cx)),
                        (int) Math.round(altezza));
            }
            cx += tratti[i] * u;
        }
    }

    /**
     * Larghezza del modulo piu' stretto, in millimetri. Sotto i due punti
     * di stampa il lettore comincia a fare fatica: a 203 dpi vuol dire
     * circa 0,25 mm. Serve per avvisare prima di stampare mille etichette
     * che nessuno riesce a leggere.
     */
    public static double moduloBarcodeMm(String contenuto, double larghezzaMm) {
        int totale = 0;
        for (int t : Code128.tratti(contenuto)) {
            totale += t;
        }
        return larghezzaMm / totale;
    }

    /** Il carattere della stampa, a una data dimensione in pixel. */
    public static Font font(double corpoPx, boolean grassetto) {
        return new Font(famiglia(), grassetto ? Font.BOLD : Font.PLAIN, 1)
                .deriveFont((float) Math.max(1.0, corpoPx));
    }

    /** Misuratore vero, costruito sulle metriche del font di stampa. */
    public static Misuratore misuratore(final Graphics2D g, final double mmPx) {
        return new Misuratore() {
            @Override
            public double larghezza(String testo, double corpoMm, boolean grassetto) {
                FontMetrics fm = g.getFontMetrics(font(corpoMm * mmPx, grassetto));
                return fm.stringWidth(testo) / mmPx;
            }
        };
    }
}
