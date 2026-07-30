package app.ui;

import app.config.AppTheme;
import app.config.UiScale;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * La barra degli strumenti che galleggia sopra l'etichetta.
 *
 * <p>Sta sul disegno invece che in una colonna per una ragione pratica: le
 * icone restano sempre alla stessa distanza dall'etichetta, qualunque sia la
 * larghezza della finestra, e la mano non attraversa mezzo schermo per
 * ruotare un elemento.
 *
 * <p>Sette pulsanti e basta. Ogni cosa che si può fare da qui non si può fare
 * da nessun'altra parte con un altro nome: è la regola che ha tolto di mezzo
 * le tre rotazioni doppie della versione precedente.
 */
public final class ToolRail extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Che cosa ha premuto l'operatore. */
    public enum Action {
        SELEZIONA, TESTO, QR, DUPLICA, ELIMINA, GRIGLIA
    }

    /** Chi ascolta i pulsanti della barra. */
    public interface Listener {
        void toolPressed(Action action);
    }

    private final Listener listener;
    private final java.util.Map<Action, Tool> buttons =
            new java.util.EnumMap<Action, Tool>(Action.class);

    public ToolRail(Listener listener) {
        this.listener = listener;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(6), UiScale.px(6), UiScale.px(6), UiScale.px(6)));

        add(tool(Action.SELEZIONA, AppTheme.glyph("\u271A", "sel"), "Seleziona e sposta"));
        add(tool(Action.TESTO, "T", "Aggiungi una riga di testo"));
        add(tool(Action.QR, AppTheme.glyph("\u25A6", "QR"), "Aggiungi un QR"));
        add(separator());
        add(tool(Action.DUPLICA, AppTheme.glyph("\u29C9", "\u25A1\u25A1"), "Duplica l'elemento selezionato"));
        add(tool(Action.ELIMINA, AppTheme.glyph("\u2715", "X"), "Elimina l'elemento selezionato"));
        add(separator());
        add(tool(Action.GRIGLIA, AppTheme.glyph("\u229E", "#"), "Griglia da 5 mm e aggancio"));

        setActive(Action.SELEZIONA, true);
        setActive(Action.GRIGLIA, true);
    }

    private JComponent separator() {
        JComponent line = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.SURFACE1);
                g.fillRect(UiScale.px(5), getHeight() / 2, getWidth() - UiScale.px(10), 1);
            }
        };
        Dimension d = new Dimension(UiScale.px(38), UiScale.px(7));
        line.setPreferredSize(d);
        line.setMaximumSize(d);
        return line;
    }

    private Tool tool(Action action, String glyph, String tip) {
        Tool t = new Tool(action, glyph, tip);
        buttons.put(action, t);
        return t;
    }

    /** Accende o spegne un pulsante a interruttore (selezione, griglia). */
    public void setActive(Action action, boolean active) {
        Tool t = buttons.get(action);
        if (t != null) {
            t.active = active;
            t.repaint();
        }
    }

    /** Spegne i pulsanti che non hanno senso senza un elemento selezionato. */
    public void setHasSelection(boolean any) {
        for (Action a : new Action[] {Action.DUPLICA, Action.ELIMINA}) {
            Tool t = buttons.get(a);
            if (t != null) {
                t.setEnabled(any);
                t.repaint();
            }
        }
    }

    /** Un pulsante quadrato della barra. */
    private final class Tool extends JComponent {

        private static final long serialVersionUID = 1L;

        private final Action action;
        private final String glyph;
        private boolean over;
        private boolean active;

        Tool(Action action, String glyph, String tip) {
            this.action = action;
            this.glyph = glyph;
            setToolTipText(tip);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            Dimension d = new Dimension(UiScale.px(38), UiScale.px(38));
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    over = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    over = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled() && listener != null) {
                        listener.toolPressed(Tool.this.action);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = UiScale.px(9);
                Color fg;
                if (!isEnabled()) {
                    fg = AppTheme.blend(AppTheme.OVERLAY0, AppTheme.BASE, 0.45f);
                } else if (active) {
                    g.setColor(AppTheme.MAUVE);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                    fg = AppTheme.ON_ACCENT;
                } else if (over) {
                    g.setColor(AppTheme.blend(AppTheme.MAUVE, AppTheme.PAPER, 0.12f));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                    fg = AppTheme.MAUVE;
                } else {
                    fg = AppTheme.SUBTEXT0;
                }
                g.setColor(fg);
                g.setFont(AppTheme.UI_TITLE);
                java.awt.FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(glyph)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(glyph, x, y);
            } finally {
                g.dispose();
            }
        }
    }

    /** Il riquadro bianco arrotondato con l'ombra, sotto le icone. */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = UiScale.px(13);
            g.setColor(new Color(0, 0, 0, 26));
            g.fillRoundRect(UiScale.px(2), UiScale.px(3),
                    getWidth() - UiScale.px(3), getHeight() - UiScale.px(3), arc, arc);
            g.setColor(AppTheme.LATTE ? AppTheme.PAPER : AppTheme.SURFACE0);
            g.fillRoundRect(0, 0, getWidth() - UiScale.px(4), getHeight() - UiScale.px(4),
                    arc, arc);
            g.setColor(AppTheme.SURFACE1);
            g.drawRoundRect(0, 0, getWidth() - UiScale.px(4), getHeight() - UiScale.px(4),
                    arc, arc);
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    /** Registra una scorciatoia da tastiera su tutta la finestra. */
    public static void bind(JComponent root, String keyStroke, final Listener target,
                            final Action action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(keyStroke), action.name());
        root.getActionMap().put(action.name(), new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                target.toolPressed(action);
            }
        });
    }

}
