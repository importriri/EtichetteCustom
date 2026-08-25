package app.ui.vetrina;

import app.modello.Etichetta;
import app.modello.Serie;
import app.render.Disegno;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.comp.CodiceView;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/**
 * Una tessera della vetrina.
 *
 * Non mostra un'icona ne' il nome di un modello: mostra l'etichetta vera,
 * disegnata dallo stesso codice che disegna il foglio grande e la stampa.
 * Quello che vedi nella tessera e' quello che esce dalla stampante.
 */
public class Tessera extends JComponent {

    public interface Apri {
        void apri(Etichetta e);
    }

    /** Il puntino dei tre punti, o il tasto destro: le cose che si fanno all'etichetta. */
    public interface Menu {
        void mostra(Etichetta e, int x, int y);
    }

    private final Etichetta etichetta;
    private final SorgenteQr qr;
    private final boolean nuova;
    private boolean sopra;
    private Apri apri;
    private Menu menu;

    public Tessera(Etichetta etichetta, SorgenteQr qr) {
        this(etichetta, qr, false);
    }

    private Tessera(Etichetta etichetta, SorgenteQr qr, boolean nuova) {
        this.etichetta = etichetta;
        this.qr = qr;
        this.nuova = nuova;
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        setToolTipText(nuova ? "Parti da un foglio vuoto"
                : etichetta.nome() + " \u2014 " + misura(etichetta));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sopra = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sopra = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (Tessera.this.etichetta == null) {
                    if (apri != null) {
                        apri.apri(null);
                    }
                    return;
                }
                if (menu != null && (e.isPopupTrigger()
                        || javax.swing.SwingUtilities.isRightMouseButton(e)
                        || suiPuntini(e))) {
                    menu.mostra(Tessera.this.etichetta, e.getX(), e.getY());
                    return;
                }
                if (apri != null) {
                    apri.apri(Tessera.this.etichetta);
                }
            }
        });
    }

    /** La tessera "+": stessa forma delle altre, un foglio vuoto dentro. */
    public static Tessera nuova(Apri apri) {
        Tessera t = new Tessera(null, null, true);
        t.apri = apri;
        return t;
    }

    public Tessera azione(Apri a) {
        apri = a;
        return this;
    }

    public Tessera menu(Menu m) {
        menu = m;
        return this;
    }

    /** Il quadratino in alto a destra della tessera. */
    private boolean suiPuntini(MouseEvent e) {
        int lato = Stile.px(26);
        return e.getX() > getWidth() - lato && e.getY() < lato;
    }

    public Etichetta etichetta() {
        return etichetta;
    }

    private static String misura(Etichetta e) {
        return num(e.larghezza()) + " \u00d7 " + num(e.altezza()) + " mm";
    }

    private static String num(double v) {
        String s = String.valueOf(Math.round(v * 10) / 10.0);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s.replace('.', ',');
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Stile.px(226), Stile.px(224));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Stile.liscio(g);
        try {
            int w = getWidth();
            int h = getHeight();
            int r = Stile.px(9);

            if (nuova) {
                g2.setColor(sopra ? Stile.BLU : Stile.S1);
                g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT,
                        java.awt.BasicStroke.JOIN_MITER, 10f,
                        new float[] { Stile.px(5), Stile.px(4) }, 0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.setColor(sopra ? Stile.BLU : Stile.SUB0);
                g2.setFont(Stile.font(24, Font.PLAIN));
                centra(g2, "+", w, h / 2 - Stile.px(12));
                g2.setFont(Stile.normale());
                centra(g2, "Nuova etichetta", w, h / 2 + Stile.px(16));
                g2.setFont(Stile.piccolo());
                g2.setColor(Stile.OV0);
                centra(g2, "parti da un foglio vuoto", w, h / 2 + Stile.px(32));
                return;
            }

            Stile.riquadro(g2, 0, 0, w, h, r, Stile.MANTLE, sopra ? Stile.BLU : Stile.S0);

            if (sopra) {
                int lato = Stile.px(22);
                int bx = w - lato - Stile.px(8);
                int by = Stile.px(8);
                Stile.riquadro(g2, bx, by, lato, lato, Stile.px(5), Stile.BASE, Stile.S1);
                g2.setColor(Stile.SUB0);
                for (int i = 0; i < 3; i++) {
                    g2.fillOval(bx + Stile.px(5) + i * Stile.px(5), by + lato / 2 - 1, 2, 2);
                }
            }

            /* la vetrinetta: il banco su cui poggia la carta */
            int m = Stile.px(11);
            int vh = Stile.px(120);
            Stile.riquadro(g2, m, m, w - 2 * m, vh, Stile.px(6), Stile.BANCO, null);
            disegnaCarta(g2, m, m, w - 2 * m, vh);

            int y = m + vh + Stile.px(18);
            g2.setFont(Stile.forte());
            g2.setColor(Stile.TESTO);
            g2.drawString(etichetta.nome(), m, y);

            y += Stile.px(15);
            g2.setFont(Stile.piccolo());
            g2.setColor(Stile.OV0);
            int campi = etichetta.campi().size();
            g2.drawString(misura(etichetta) + " \u00b7 " + campi
                    + (campi == 1 ? " campo" : " campi"), m, y);

            y += Stile.px(9);
            g2.setColor(Stile.S0);
            g2.drawLine(m, y, w - m, y);

            y += Stile.px(15);
            Serie s = etichetta.serie();
            if (s == null) {
                g2.setFont(Stile.piccolo());
                g2.setColor(Stile.OV0);
                g2.drawString("codice chiesto a ogni stampa", m, y);
            } else {
                String pre = s.prefisso();
                if (pre.length() > 6) {
                    pre = "\u2026" + pre.substring(pre.length() - 6);
                }
                Font f = Stile.mono(11);
                int larg = CodiceView.disegna(g2, m, y, pre, s.finestra(s.prossimo()), f);
                g2.setFont(Stile.piccolo());
                g2.setColor(Stile.SUB0);
                g2.drawString(" il prossimo", m + larg + Stile.px(3), y);
            }
        } finally {
            g2.dispose();
        }
    }

    private void centra(Graphics2D g, String s, int w, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (w - fm.stringWidth(s)) / 2, baseline);
    }

    private void disegnaCarta(Graphics2D g, int bx, int by, int bw, int bh) {
        double mmPx = Math.min((bw - Stile.px(14)) / etichetta.larghezza(),
                (bh - Stile.px(14)) / etichetta.altezza());
        int cw = (int) Math.round(etichetta.larghezza() * mmPx);
        int ch = (int) Math.round(etichetta.altezza() * mmPx);
        int cx = bx + (bw - cw) / 2;
        int cy = by + (bh - ch) / 2;

        g.setColor(new Color(0, 0, 0, 28));
        g.fillRect(cx + 1, cy + 2, cw, ch);

        Graphics2D g2 = (Graphics2D) g.create(cx, cy, cw, ch);
        try {
            Disegno.disegna(g2, etichetta, mmPx, qr, 0);
        } finally {
            g2.dispose();
        }
        g.setColor(Stile.S1);
        g.drawRect(cx, cy, cw - 1, ch - 1);
    }
}
