package app.ui.comp;

import app.stile.Stile;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/**
 * Una riga della colonna comandi. Niente riquadro intorno: la colonna e'
 * fatta di aria e di parole, i bordi li mette solo chi ne ha bisogno.
 */
public class Voce extends JComponent {

    public interface Azione {
        void esegui();
    }

    private final String glifo;
    private final String testo;
    private String coda;
    private boolean attiva;
    private boolean sopra;
    private Azione azione;

    public Voce(String glifo, String testo) {
        this.glifo = glifo == null ? "" : glifo;
        this.testo = testo;
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
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
                if (azione != null) {
                    azione.esegui();
                }
            }
        });
    }

    public Voce coda(String c) {
        coda = c;
        return this;
    }

    public Voce azione(Azione a) {
        azione = a;
        return this;
    }

    public void attiva(boolean v) {
        attiva = v;
        repaint();
    }

    public boolean attiva() {
        return attiva;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Stile.px(160), Stile.px(26));
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, Stile.px(26));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Stile.liscio(g);
        try {
            if (attiva || sopra) {
                Stile.riquadro(g2, 0, 0, getWidth(), getHeight(), Stile.px(6),
                        attiva ? Stile.S0 : Stile.MANTLE, null);
            }
            int base = (getHeight() + Stile.px(9)) / 2;
            g2.setFont(Stile.piccolo());
            g2.setColor(attiva ? Stile.BLU : Stile.OV1);
            g2.drawString(glifo, Stile.px(7), base);

            g2.setFont(Stile.normale());
            g2.setColor(attiva || sopra ? Stile.TESTO : Stile.SUB1);
            g2.drawString(testo, Stile.px(29), base);

            if (coda != null) {
                g2.setFont(Stile.minuscolo());
                g2.setColor(Stile.OV0);
                int w = g2.getFontMetrics().stringWidth(coda);
                g2.drawString(coda, getWidth() - w - Stile.px(7), base);
            }
        } finally {
            g2.dispose();
        }
    }
}
