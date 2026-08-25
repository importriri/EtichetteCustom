package app.ui.banco;

import app.modello.Elemento;
import app.modello.Etichetta;
import app.render.Disegno;
import app.render.Ingombri;
import app.render.SorgenteQr;
import app.stile.Stile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Il foglio grande: carta, righelli in millimetri, griglia, zoom e selezione.
 *
 * La carta e' disegnata da {@link Disegno}, lo stesso codice delle tessere e
 * della stampa. Qui sopra ci va solo quello che serve a lavorarci: i righelli,
 * la griglia e le maniglie. Niente di tutto cio' finisce sull'etichetta.
 */
public class Foglio extends JComponent {

    public interface Ascolto {
        void selezionato(Elemento e);

        /** Chiamato PRIMA di ogni modifica: e' li' che si segna l'annullamento. */
        void staPerCambiare();

        void modificato();
    }

    private final SorgenteQr qr;
    private Etichetta eti;
    private Elemento selezione;
    private double mmPx = 6;
    private Ascolto ascolto;
    private Map<Elemento, Rectangle2D.Double> ingombri;

    private Point presa;
    private double presaX;
    private double presaY;
    private boolean adattato;
    private boolean trascinato;

    public Foglio(Etichetta eti, SorgenteQr qr) {
        this.eti = eti;
        this.qr = qr;
        setBackground(Stile.BANCO);
        setOpaque(true);
        setFocusable(true);
        mouse();
        tasti();
    }

    public void ascolto(Ascolto a) {
        ascolto = a;
    }

    public Etichetta etichetta() {
        return eti;
    }

    public void etichetta(Etichetta e) {
        eti = e;
        selezione = e.elementi().isEmpty() ? null : e.elementi().get(0);
        revalidate();
        repaint();
    }

    public Elemento selezione() {
        return selezione;
    }

    public void selezione(Elemento e) {
        selezione = e;
        if (ascolto != null) {
            ascolto.selezionato(e);
        }
        repaint();
    }

    public double zoom() {
        return mmPx;
    }

    /** Percentuale rispetto alla dimensione reale sullo schermo (96 dpi). */
    public int percentuale() {
        return (int) Math.round(mmPx / (96.0 / 25.4) * 100);
    }

    public void zoomPasso(int segno) {
        double p = percentuale() + segno * 20;
        percentuale(p);
    }

    public void percentuale(double p) {
        double limitata = Math.max(30, Math.min(600, p));
        mmPx = limitata / 100.0 * (96.0 / 25.4);
        revalidate();
        repaint();
    }

    /**
     * Zoom che fa entrare tutta l'etichetta nello spazio dato.
     * Non chiama revalidate: viene usato anche durante il disegno,
     * dove rimettere in discussione l'impaginazione farebbe un giro infinito.
     */
    public void adatta(Dimension spazio) {
        if (spazio == null || spazio.width <= 0 || spazio.height <= 0) {
            return;
        }
        double libW = spazio.width - 2 * margine() - righello();
        double libH = spazio.height - 2 * margine() - righello();
        double k = Math.min(libW / eti.larghezza(), libH / eti.altezza());
        if (k > 0.5) {
            mmPx = k;
            adattato = true;
        }
    }

    public void adattaEImpagina(Dimension spazio) {
        adatta(spazio);
        revalidate();
        repaint();
    }

    private int margine() {
        return Stile.px(26);
    }

    private int righello() {
        return Stile.px(18);
    }

    /**
     * La carta sta al centro dello spazio disponibile finche' ci sta;
     * appena lo zoom la fa crescere oltre, torna in alto a sinistra e
     * comincia a scorrere.
     */
    private int originaX() {
        int larga = (int) Math.round(eti.larghezza() * mmPx);
        int avanza = getWidth() - righello() - larga;
        return righello() + Math.max(margine(), avanza / 2);
    }

    private int originaY() {
        int alta = (int) Math.round(eti.altezza() * mmPx);
        int avanza = getHeight() - righello() - alta;
        return righello() + Math.max(margine(), avanza / 2);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(
                (int) Math.round(eti.larghezza() * mmPx) + righello() + 2 * margine(),
                (int) Math.round(eti.altezza() * mmPx) + righello() + 2 * margine());
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!adattato && getParent() != null && getParent().getWidth() > 0) {
            adatta(getParent().getSize());
        }
        Graphics2D g2 = Stile.liscio(g);
        try {
            g2.setColor(Stile.BANCO);
            g2.fillRect(0, 0, getWidth(), getHeight());

            int ox = originaX();
            int oy = originaY();
            int w = (int) Math.round(eti.larghezza() * mmPx);
            int h = (int) Math.round(eti.altezza() * mmPx);

            righelli(g2, ox, oy, w, h);

            /* la carta, con la sua ombra */
            g2.setColor(new Color(0, 0, 0, 34));
            g2.fillRect(ox + 2, oy + 3, w, h);
            Graphics2D gc = (Graphics2D) g2.create(ox, oy, w, h);
            try {
                Disegno.disegna(gc, eti, mmPx, qr, 0);
            } finally {
                gc.dispose();
            }
            g2.setColor(Stile.S1);
            g2.drawRect(ox, oy, w - 1, h - 1);

            griglia(g2, ox, oy, w, h);

            ingombri = Ingombri.calcola(g2, eti, mmPx, 0);
            if (selezione != null && ingombri.containsKey(selezione)) {
                maniglie(g2, ox, oy, ingombri.get(selezione));
            }
        } finally {
            g2.dispose();
        }
    }

    private void righelli(Graphics2D g, int ox, int oy, int w, int h) {
        int r = righello();
        g.setColor(Stile.MANTLE);
        g.fillRect(ox, oy - r, w, r);
        g.fillRect(ox - r, oy, r, h);
        g.setColor(Stile.S0);
        g.drawRect(ox, oy - r, w, r);
        g.drawRect(ox - r, oy, r, h);

        int passo = mmPx < 4 ? 5 : 1;
        g.setFont(Stile.mono(8));
        for (int mm = 0; mm <= (int) eti.larghezza(); mm += passo) {
            int x = ox + (int) Math.round(mm * mmPx);
            boolean grande = mm % 10 == 0;
            g.setColor(Stile.OV0);
            g.drawLine(x, oy - r, x, oy - r + (grande ? Stile.px(9) : Stile.px(5)));
            if (grande && mm > 0) {
                g.setColor(Stile.OV1);
                g.drawString(String.valueOf(mm), x + 2, oy - Stile.px(3));
            }
        }
        for (int mm = 0; mm <= (int) eti.altezza(); mm += passo) {
            int y = oy + (int) Math.round(mm * mmPx);
            boolean grande = mm % 10 == 0;
            g.setColor(Stile.OV0);
            g.drawLine(ox - r, y, ox - r + (grande ? Stile.px(9) : Stile.px(5)), y);
            if (grande && mm > 0) {
                g.setColor(Stile.OV1);
                g.drawString(String.valueOf(mm), ox - r + Stile.px(9), y + Stile.px(7));
            }
        }
    }

    private void griglia(Graphics2D g, int ox, int oy, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.clipRect(ox, oy, w, h);
            for (int mm = 1; mm < eti.larghezza(); mm++) {
                int x = ox + (int) Math.round(mm * mmPx);
                g2.setColor(mm % 5 == 0 ? new Color(30, 30, 46, 42) : new Color(30, 30, 46, 15));
                g2.drawLine(x, oy, x, oy + h);
            }
            for (int mm = 1; mm < eti.altezza(); mm++) {
                int y = oy + (int) Math.round(mm * mmPx);
                g2.setColor(mm % 5 == 0 ? new Color(30, 30, 46, 42) : new Color(30, 30, 46, 15));
                g2.drawLine(ox, y, ox + w, y);
            }
        } finally {
            g2.dispose();
        }
    }

    private void maniglie(Graphics2D g, int ox, int oy, Rectangle2D.Double r) {
        int x = ox + (int) Math.round(r.x * mmPx);
        int y = oy + (int) Math.round(r.y * mmPx);
        int w = (int) Math.round(r.width * mmPx);
        int h = (int) Math.round(r.height * mmPx);
        g.setColor(Stile.BLU);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(x - 2, y - 2, w + 4, h + 4);
        g.setStroke(new BasicStroke(1f));
        int m = Stile.px(7);
        maniglia(g, x - 2, y - 2, m);
        maniglia(g, x + w + 2, y - 2, m);
        maniglia(g, x - 2, y + h + 2, m);
        maniglia(g, x + w + 2, y + h + 2, m);
    }

    private void maniglia(Graphics2D g, int cx, int cy, int lato) {
        g.setColor(Stile.BLU);
        g.fillRect(cx - lato / 2, cy - lato / 2, lato, lato);
        g.setColor(Color.WHITE);
        g.drawRect(cx - lato / 2, cy - lato / 2, lato - 1, lato - 1);
    }

    /* ---- interazione -------------------------------------------------- */

    private void mouse() {
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Elemento sotto = sotto(e.getX(), e.getY());
                if (sotto != selezione) {
                    selezione(sotto);
                }
                if (sotto != null) {
                    presa = e.getPoint();
                    presaX = sotto.x();
                    presaY = sotto.y();
                    trascinato = false;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                presa = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (presa == null || selezione == null) {
                    return;
                }
                if (!trascinato) {
                    trascinato = true;
                    if (ascolto != null) {
                        ascolto.staPerCambiare();
                    }
                }
                double dx = (e.getX() - presa.x) / mmPx;
                double dy = (e.getY() - presa.y) / mmPx;
                muovi(presaX + dx, presaY + dy);
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    private void muovi(double nx, double ny) {
        if (selezione == null) return;
        Rectangle2D.Double r = ingombroCorrente(selezione);
        double maxX = Math.max(0, eti.larghezza() - r.width);
        double maxY = Math.max(0, eti.altezza() - r.height);
        selezione.x(Math.max(0, Math.min(maxX, nx)));
        selezione.y(Math.max(0, Math.min(maxY, ny)));
        if (ascolto != null) ascolto.modificato();
        repaint();
    }

    /** Ruota carta ed elementi come un unico layout, senza deformarne le misure. */
    public void ruotaEtichetta() {
        double vecchiaAltezza = eti.altezza();
        java.util.Map<Elemento, Rectangle2D.Double> prima =
                new java.util.LinkedHashMap<Elemento, Rectangle2D.Double>();
        for (Elemento e : eti.elementi()) prima.put(e, ingombroCorrente(e));
        eti.scambiaLati();
        for (Elemento e : eti.elementi()) {
            Rectangle2D.Double r = prima.get(e);
            e.x(vecchiaAltezza - (r.y + r.height));
            e.y(r.x);
            e.rotazione(e.rotazione() + 90);
            rientra(e);
        }
        revalidate();
        repaint();
    }

    /** Riporta l'ingombro visibile dell'elemento dentro la carta. */
    public void rientra(Elemento e) {
        if (e == null) return;
        Rectangle2D.Double r = ingombroCorrente(e);
        double maxX = Math.max(0, eti.larghezza() - r.width);
        double maxY = Math.max(0, eti.altezza() - r.height);
        e.x(Math.max(0, Math.min(maxX, e.x())));
        e.y(Math.max(0, Math.min(maxY, e.y())));
    }

    private Rectangle2D.Double ingombroCorrente(Elemento e) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(4, 4,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            return Ingombri.di(g, eti, e, Math.max(1, mmPx), 0);
        } finally {
            g.dispose();
        }
    }

    /** L'elemento sotto al puntatore, dall'alto verso il basso della pila. */
    private Elemento sotto(int px, int py) {
        if (ingombri == null) {
            return null;
        }
        double mx = (px - originaX()) / mmPx;
        double my = (py - originaY()) / mmPx;
        List<Elemento> pila = new ArrayList<Elemento>(eti.elementi());
        for (int i = pila.size() - 1; i >= 0; i--) {
            Rectangle2D.Double r = ingombri.get(pila.get(i));
            if (r != null && r.contains(mx, my)) {
                return pila.get(i);
            }
        }
        return null;
    }

    private void tasti() {
        lega("R", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selezione != null) {
                    if (ascolto != null) {
                        ascolto.staPerCambiare();
                    }
                    selezione.rotazione(selezione.rotazione() + 90);
                    rientra(selezione);
                    avvisa();
                }
            }
        });
        freccia("LEFT", -1, 0);
        freccia("RIGHT", 1, 0);
        freccia("UP", 0, -1);
        freccia("DOWN", 0, 1);
    }

    private void freccia(String tasto, final int dx, final int dy) {
        lega(tasto, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selezione != null) {
                    if (ascolto != null) {
                        ascolto.staPerCambiare();
                    }
                    muovi(selezione.x() + dx * 0.5, selezione.y() + dy * 0.5);
                }
            }
        });
        lega("shift " + tasto, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selezione != null) {
                    if (ascolto != null) {
                        ascolto.staPerCambiare();
                    }
                    muovi(selezione.x() + dx * 0.1, selezione.y() + dy * 0.1);
                }
            }
        });
    }

    private void lega(String tasto, AbstractAction a) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(tasto), tasto);
        getActionMap().put(tasto, a);
    }

    private void avvisa() {
        if (ascolto != null) {
            ascolto.modificato();
        }
        repaint();
    }
}
