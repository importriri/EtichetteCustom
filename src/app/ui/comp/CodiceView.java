package app.ui.comp;

import app.stile.Stile;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/**
 * La finestra dell'incremento, scritta.
 *
 * Il prefisso resta grigio, le cifre che si muovono stanno dentro un
 * riquadro arancione. Compare identica sulla tessera, nella riga del giro,
 * nel pannello proprieta' e nell'elenco di stampa: l'operatore non deve
 * mai contare le cifre a mano per capire che cosa fara' +1.
 *
 * Vive solo nell'interfaccia. Sulla carta il codice e' nero su bianco,
 * come esce dalla stampante.
 */
public class CodiceView extends JComponent {

    private String prefisso = "";
    private String finestra = "";
    private double corpo = 12.0;

    public CodiceView() {
    }

    public CodiceView(String prefisso, String finestra) {
        testo(prefisso, finestra);
    }

    public void testo(String prefisso, String finestra) {
        this.prefisso = prefisso == null ? "" : prefisso;
        this.finestra = finestra == null ? "" : finestra;
        revalidate();
        repaint();
    }

    public void corpo(double c) {
        corpo = c;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Font f = Stile.mono(corpo);
        FontMetrics fm = getFontMetrics(f);
        return new Dimension(fm.stringWidth(prefisso + finestra) + Stile.px(10),
                fm.getHeight() + Stile.px(4));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Stile.liscio(g);
        try {
            Font f = Stile.mono(corpo);
            FontMetrics fm = g2.getFontMetrics(f);
            int base = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            disegna(g2, 0, base, prefisso, finestra, f);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Disegna il codice e torna la larghezza occupata, cosi' chi lo usa
     * dentro un altro componente sa dove continuare.
     */
    public static int disegna(Graphics2D g, int x, int baseline,
                              String prefisso, String finestra, Font f) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        int cx = x;
        if (prefisso != null && !prefisso.isEmpty()) {
            g.setColor(Stile.OV1);
            g.drawString(prefisso, cx, baseline);
            cx += fm.stringWidth(prefisso);
        }
        if (finestra != null && !finestra.isEmpty()) {
            int w = fm.stringWidth(finestra);
            int pad = Math.max(2, Stile.px(3));
            g.setColor(Stile.PESCA);
            g.fillRoundRect(cx - pad / 2, baseline - fm.getAscent(),
                    w + pad, fm.getAscent() + fm.getDescent(), Stile.px(4), Stile.px(4));
            g.setColor(java.awt.Color.WHITE);
            g.drawString(finestra, cx, baseline);
            cx += w + pad / 2;
        }
        return cx - x;
    }
}
