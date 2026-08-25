package app.ui.comp;

import app.stile.Stile;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Scheda coerente usata per proprietà e dati del giro. */
public class Scheda extends JPanel {
    private final String titolo;
    private final JPanel dentro = new JPanel(new GridBagLayout());
    private int riga;

    public Scheda(String titolo) {
        super(new java.awt.BorderLayout());
        this.titolo = titolo;
        setOpaque(false);
        dentro.setOpaque(false);
        int p = Stile.px(13);
        dentro.setBorder(javax.swing.BorderFactory.createEmptyBorder(
                p + Stile.px(18), p, p, p));
        add(dentro, java.awt.BorderLayout.CENTER);
    }

    /** Compact row for short values. */
    public Scheda riga(String etichetta, Component comando) {
        int y = riga++;
        GridBagConstraints a = new GridBagConstraints();
        a.gridx = 0; a.gridy = y; a.anchor = GridBagConstraints.WEST;
        a.insets = new Insets(0, 0, Stile.px(9), Stile.px(10));
        JLabel l = new JLabel(etichetta);
        l.setFont(Stile.piccolo());
        l.setForeground(Stile.SUB0);
        dentro.add(l, a);

        GridBagConstraints b = new GridBagConstraints();
        b.gridx = 1; b.gridy = y; b.weightx = 1;
        b.fill = GridBagConstraints.HORIZONTAL;
        b.anchor = GridBagConstraints.EAST;
        b.insets = new Insets(0, 0, Stile.px(9), 0);
        minimo(comando, Stile.px(96));
        dentro.add(comando, b);
        return this;
    }

    /** Full-width field for values that must remain readable. */
    public Scheda campo(String etichetta, Component comando) {
        GridBagConstraints a = new GridBagConstraints();
        a.gridx = 0; a.gridy = riga++; a.gridwidth = 2;
        a.weightx = 1; a.fill = GridBagConstraints.HORIZONTAL;
        a.anchor = GridBagConstraints.WEST;
        a.insets = new Insets(0, 0, Stile.px(4), 0);
        JLabel l = new JLabel(etichetta);
        l.setFont(Stile.piccolo());
        l.setForeground(Stile.SUB0);
        dentro.add(l, a);

        GridBagConstraints b = new GridBagConstraints();
        b.gridx = 0; b.gridy = riga++; b.gridwidth = 2;
        b.weightx = 1; b.fill = GridBagConstraints.HORIZONTAL;
        b.insets = new Insets(0, 0, Stile.px(10), 0);
        minimo(comando, Stile.px(120));
        dentro.add(comando, b);
        return this;
    }

    public Scheda largo(Component comando) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = riga++; c.gridx = 0; c.gridwidth = 2;
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, Stile.px(9), 0);
        minimo(comando, Stile.px(80));
        dentro.add(comando, c);
        return this;
    }

    public Scheda nota(String testo) {
        JTextArea n = new JTextArea(testo);
        n.setEditable(false);
        n.setFocusable(false);
        n.setOpaque(false);
        n.setLineWrap(true);
        n.setWrapStyleWord(true);
        n.setFont(Stile.piccolo());
        n.setForeground(Stile.OV1);
        n.setColumns(22);
        n.setRows(testo.length() > 95 ? 3 : 2);
        n.setBorder(null);
        Dimension d = n.getPreferredSize();
        n.setMinimumSize(new Dimension(Stile.px(120), d.height));
        n.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        return largo(n);
    }

    private static void minimo(Component c, int larghezza) {
        Dimension d = c.getPreferredSize();
        c.setMinimumSize(new Dimension(Math.min(larghezza, Math.max(1, d.width)), d.height));
    }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = Stile.liscio(g);
        try {
            Stile.riquadro(g2, 0, 0, getWidth(), getHeight(), Stile.px(10), Color.WHITE, Stile.S0);
            g2.setFont(Stile.minuscolo().deriveFont(java.awt.Font.BOLD));
            g2.setColor(Stile.SUB0);
            g2.drawString(titolo.toUpperCase(), Stile.px(13), Stile.px(20));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    public static JPanel colonna() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.Y_AXIS));
        return p;
    }

    public static JComponent spazio(int h) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, Stile.px(h)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Stile.px(h)));
        return p;
    }
}
