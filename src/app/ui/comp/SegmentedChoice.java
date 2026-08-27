package app.ui.comp;

import app.stile.Stile;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JToggleButton;

/** Compact toggle used for small mutually-exclusive choices in the inspector. */
public final class SegmentedChoice extends JToggleButton {
    public SegmentedChoice(String text, boolean selected) {
        super(text, selected);
        setFont(Stile.minuscolo());
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        setMargin(new java.awt.Insets(
                Stile.px(6), Stile.px(8), Stile.px(6), Stile.px(8)));
    }

    @Override public Dimension getPreferredSize() {
        Dimension base = super.getPreferredSize();
        return new Dimension(Math.max(Stile.px(46), base.width + Stile.px(8)),
                Math.max(Stile.px(32), base.height + Stile.px(4)));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = Stile.liscio(g);
        try {
            boolean selected = isSelected();
            boolean rollover = getModel().isRollover();
            boolean pressed = getModel().isPressed();
            Color fill;
            Color border;
            Color text;
            if (selected) {
                fill = pressed ? Stile.tinta(Stile.BLU, .82) : Stile.BLU_SOFT;
                border = Stile.BLU;
                text = Stile.BLU;
            } else {
                fill = pressed ? Stile.MANTLE : (rollover ? Stile.BASE : Color.WHITE);
                border = rollover ? Stile.S1 : Stile.S0;
                text = Stile.SUB1;
            }
            Stile.riquadro(g2, 1, 1, getWidth() - 2, getHeight() - 2,
                    Stile.px(8), fill, border);
            g2.setFont(getFont());
            g2.setColor(isEnabled() ? text : Stile.OV0);
            int textWidth = g2.getFontMetrics().stringWidth(getText());
            int baseline = (getHeight() + g2.getFontMetrics().getAscent()
                    - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(getText(), Math.max(Stile.px(4), (getWidth() - textWidth) / 2), baseline);
        } finally {
            g2.dispose();
        }
    }
}
