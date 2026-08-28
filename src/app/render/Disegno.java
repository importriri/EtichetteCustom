package app.render;

import app.codice.Code128;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.stile.Stile;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;

/** Shared renderer used by preview, export and print. */
public final class Disegno {
    private static final String[] PREFERITE = {"DejaVu Sans", "Verdana", "Tahoma", "Arial"};
    private static String famiglia;

    private Disegno() { }

    public static synchronized String famiglia() {
        if (famiglia == null) {
            famiglia = "SansSerif";
            try {
                String[] presenti = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames();
                for (int i = 0; i < PREFERITE.length && famiglia.equals("SansSerif"); i++) {
                    for (String presente : presenti) {
                        if (presente.equalsIgnoreCase(PREFERITE[i])) {
                            famiglia = presente;
                            break;
                        }
                    }
                }
            } catch (Throwable ignorato) {
                famiglia = "SansSerif";
            }
        }
        return famiglia;
    }

    public static void disegna(Graphics2D g, Etichetta etichetta, double mmPx,
                               SorgenteQr qr, int copia) {
        int larghezza = (int) Math.round(etichetta.larghezza() * mmPx);
        int altezza = (int) Math.round(etichetta.altezza() * mmPx);
        g.setColor(Stile.CARTA);
        g.fillRect(0, 0, larghezza, altezza);

        for (Elemento elemento : etichetta.elementi()) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                java.awt.geom.Rectangle2D.Double base =
                        Ingombri.base(g2, etichetta, elemento, mmPx, copia);
                g2.translate(elemento.x() * mmPx, elemento.y() * mmPx);
                switch (elemento.rotazione()) {
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
                disegnaElemento(g2, etichetta, elemento, mmPx, qr, copia);
            } finally {
                g2.dispose();
            }
        }
    }

    private static void disegnaElemento(Graphics2D g, Etichetta etichetta, Elemento elemento,
                                        double mmPx, SorgenteQr qr, int copia) {
        g.setColor(Stile.INCHIOSTRO);
        switch (elemento.tipo()) {
            case QR:
                disegnaQr(g, qr, etichetta.contenuto(elemento, copia), elemento.correzione(),
                        0, 0, elemento.larghezza() * mmPx);
                break;
            case BARCODE:
                disegnaBarcode(g, etichetta.contenuto(elemento, copia), 0, 0,
                        elemento.larghezza() * mmPx, elemento.altezza() * mmPx);
                break;
            case LINEA:
                g.fillRect(0, 0, (int) Math.round(elemento.larghezza() * mmPx),
                        Math.max(1, (int) Math.round(elemento.altezza() * mmPx)));
                break;
            default:
                disegnaScritta(g, etichetta, elemento, mmPx, copia);
                break;
        }
    }

    private static void disegnaScritta(Graphics2D g, Etichetta etichetta, Elemento elemento,
                                       double mmPx, int copia) {
        String testo = testoElemento(etichetta, elemento, copia);
        Misuratore misuratore = misuratore(g, mmPx);
        Testo.Esito esito = Testo.componi(testo, elemento.larghezza(), elemento.corpo(),
                elemento.massimoRighe(), elemento.righePreferite(), elemento.grassetto(), misuratore);
        Font font = font(esito.corpo() * mmPx, elemento.grassetto());
        g.setFont(font);
        FontMetrics metriche = g.getFontMetrics();
        double interlinea = metriche.getAscent() + metriche.getDescent();
        double linea = metriche.getAscent();
        double box = elemento.larghezza() * mmPx;
        for (String riga : esito.righe()) {
            int larghezzaRiga = metriche.stringWidth(riga);
            double x = 0;
            if (elemento.allineamento() == 1) x = Math.max(0, (box - larghezzaRiga) / 2);
            else if (elemento.allineamento() == 2) x = Math.max(0, box - larghezzaRiga);
            g.drawString(riga, (int) Math.round(x), (int) Math.round(linea));
            linea += interlinea;
        }
    }

    /** Presentation-only text. QR and barcode always keep the exact source value. */
    static String testoElemento(Etichetta etichetta, Elemento elemento, int copia) {
        String sorgente = etichetta.contenuto(elemento, copia);
        if (elemento.parteTesto() > 0) {
            sorgente = Testo.parte(sorgente, elemento.parteTesto());
        }
        return testoVisuale(sorgente, elemento.mostraSeparatori());
    }

    static String testoVisuale(String raw, boolean mostra) {
        if (mostra) return raw == null ? "" : raw;
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder risultato = new StringBuilder(raw.length());
        boolean spazio = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '.' || c == '_' || c == '-' || c == '/'
                    || c == ':' || c == ';' || c == ',') {
                if (risultato.length() > 0 && !spazio) {
                    risultato.append(' ');
                    spazio = true;
                }
            } else {
                risultato.append(c);
                spazio = false;
            }
        }
        while (risultato.length() > 0 && risultato.charAt(risultato.length() - 1) == ' ') {
            risultato.setLength(risultato.length() - 1);
        }
        return risultato.toString();
    }

    private static void disegnaQr(Graphics2D g, SorgenteQr qr, String contenuto,
                                  app.codice.Correzione livello, double x, double y,
                                  double lato) {
        if (qr == null) return;
        boolean[][] matrice = qr.matrice(contenuto, livello);
        if (matrice == null || matrice.length == 0) return;
        int n = matrice.length;
        double passo = lato / n;
        for (int riga = 0; riga < n; riga++) {
            int colonna = 0;
            while (colonna < n) {
                if (matrice[riga][colonna]) {
                    int larghi = 1;
                    while (colonna + larghi < n && matrice[riga][colonna + larghi]) larghi++;
                    int px = (int) Math.round(x + colonna * passo);
                    int py = (int) Math.round(y + riga * passo);
                    int pw = (int) Math.round(x + (colonna + larghi) * passo) - px;
                    int ph = (int) Math.round(y + (riga + 1) * passo) - py;
                    g.fillRect(px, py, Math.max(1, pw), Math.max(1, ph));
                    colonna += larghi;
                } else {
                    colonna++;
                }
            }
        }
    }

    private static void disegnaBarcode(Graphics2D g, String contenuto, double x, double y,
                                       double larghezza, double altezza) {
        int[] tratti;
        try {
            tratti = Code128.tratti(contenuto);
        } catch (RuntimeException ex) {
            g.drawRect((int) Math.round(x), (int) Math.round(y),
                    (int) Math.round(larghezza), (int) Math.round(altezza));
            return;
        }
        int totale = 0;
        for (int tratto : tratti) totale += tratto;
        double unita = larghezza / totale;
        double corrente = x;
        for (int i = 0; i < tratti.length; i++) {
            if (i % 2 == 0) {
                g.fillRect((int) Math.round(corrente), (int) Math.round(y),
                        Math.max(1, (int) Math.round(corrente + tratti[i] * unita)
                                - (int) Math.round(corrente)),
                        (int) Math.round(altezza));
            }
            corrente += tratti[i] * unita;
        }
    }

    public static double moduloBarcodeMm(String contenuto, double larghezzaMm) {
        int totale = 0;
        for (int tratto : Code128.tratti(contenuto)) totale += tratto;
        return larghezzaMm / totale;
    }

    public static Font font(double corpoPx, boolean grassetto) {
        return new Font(famiglia(), grassetto ? Font.BOLD : Font.PLAIN, 1)
                .deriveFont((float) Math.max(1, corpoPx));
    }

    public static Misuratore misuratore(final Graphics2D g, final double mmPx) {
        return new Misuratore() {
            @Override public double larghezza(String testo, double corpoMm, boolean grassetto) {
                return g.getFontMetrics(font(corpoMm * mmPx, grassetto)).stringWidth(testo) / mmPx;
            }
        };
    }
}
