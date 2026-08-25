package app.ui.comp;

import app.stile.Stile;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

/** Application button with a clear action hierarchy and a comfortable hit target. */
public class Bottone extends JButton {
    public enum Peso { NORMALE, PRIMARIO, PIATTO }
    private final Peso peso;
    private boolean selezionato;

    public Bottone(String testo, Peso peso) {
        super(testo);
        this.peso = peso;
        setFont(peso == Peso.PRIMARIO ? Stile.forte() : Stile.normale());
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }
    public static Bottone primario(String t) { return new Bottone(t, Peso.PRIMARIO); }
    public static Bottone normale(String t)  { return new Bottone(t, Peso.NORMALE); }
    public static Bottone piatto(String t)   { return new Bottone(t, Peso.PIATTO); }

    public Bottone selezionato(boolean valore) {
        selezionato = valore;
        repaint();
        return this;
    }

    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int ox = peso == Peso.PRIMARIO ? Stile.px(28) : Stile.px(18);
        return new Dimension(Math.max(Stile.px(86), d.width + ox), Stile.px(peso == Peso.PRIMARIO ? 38 : 34));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = Stile.liscio(g);
        try {
            boolean over = getModel().isRollover();
            boolean down = getModel().isPressed();
            boolean focus = isFocusOwner();
            Color fill = null, border = null, text;
            if (peso == Peso.PRIMARIO) {
                fill = down ? Stile.BLU.darker() : (over ? Stile.tinta(Stile.BLU, .10) : Stile.BLU);
                border = fill; text = Color.WHITE;
            } else if (peso == Peso.PIATTO) {
                fill = selezionato ? Stile.BLU_SOFT : (over ? Color.WHITE : null);
                border = selezionato ? Stile.BLU : (over ? Stile.S0 : null);
                text = selezionato ? Stile.BLU : (over ? Stile.TESTO : Stile.SUB0);
            } else {
                fill = down ? Stile.MANTLE : Color.WHITE;
                border = over ? Stile.BLU : Stile.S1;
                text = Stile.TESTO;
            }
            if (focus && peso != Peso.PRIMARIO) border = Stile.BLU;
            Stile.riquadro(g2, 1, 1, getWidth() - 2, getHeight() - 2, Stile.px(9), fill, border);
            g2.setColor(isEnabled() ? text : Stile.OV0);
            g2.setFont(getFont());
            int tw = g2.getFontMetrics().stringWidth(getText());
            int base = (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(getText(), (getWidth() - tw) / 2, base);
        } finally { g2.dispose(); }
    }
}
