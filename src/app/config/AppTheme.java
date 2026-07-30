package app.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.DefaultFormatter;

/**
 * Il sistema grafico dell'applicazione, sulla palette Catppuccin.
 *
 * <p>Due varianti — Latte chiara e Mocha scura — scelte nelle impostazioni e
 * applicate all'avvio. Fatto a mano, zero dipendenze: pannelli arrotondati,
 * stati di passaggio del mouse, campi e spinner ridisegnati. Le cornici delle
 * finestre restano quelle di sistema.
 *
 * <p>Gli accenti sono tre e ognuno ha un mestiere: <b>viola</b> è il fuoco e la
 * selezione, <b>verde</b> è l'azione che manda in stampa, <b>arancio</b> è
 * l'avviso che non blocca. Il rosso resta per gli errori veri e per questo si
 * vede poco.
 *
 * <p>Nessuna misura in pixel è scritta a mano senza passare da
 * {@link UiScale}: è la ragione per cui su uno schermo Windows al 125% i valori
 * degli spinner non escono più tagliati.
 */
public final class AppTheme {

    private AppTheme() {
    }

    /** Variante chiara o scura, letta una volta sola all'avvio. */
    public static final boolean LATTE = !"mocha".equalsIgnoreCase(
            SettingsManager.get().getString(SettingsManager.KEY_UI_FLAVOR, "latte"));

    private static Color pick(int mocha, int latte) {
        return new Color(LATTE ? latte : mocha);
    }

    // --- palette --------------------------------------------------------------

    public static final Color CRUST = pick(0x11111B, 0xDCE0E8);
    public static final Color MANTLE = pick(0x181825, 0xE6E9EF);
    public static final Color BASE = pick(0x1E1E2E, 0xEFF1F5);
    public static final Color SURFACE0 = pick(0x313244, 0xCCD0DA);
    public static final Color SURFACE1 = pick(0x45475A, 0xBCC0CC);
    public static final Color SURFACE2 = pick(0x585B70, 0xACB0BE);
    public static final Color TEXT = pick(0xCDD6F4, 0x4C4F69);
    public static final Color SUBTEXT0 = pick(0xA6ADC8, 0x6C6F85);
    public static final Color OVERLAY0 = pick(0x6C7086, 0x9CA0B0);
    public static final Color GREEN = pick(0xA6E3A1, 0x40A02B);
    public static final Color RED = pick(0xF38BA8, 0xD20F39);
    public static final Color PEACH = pick(0xFAB387, 0xFE640B);
    public static final Color BLUE = pick(0x89B4FA, 0x1E66F5);
    public static final Color MAUVE = pick(0xCBA6F7, 0x8839EF);
    public static final Color YELLOW = pick(0xF9E2AF, 0xDF8E1D);

    /** Colore del testo che si legge sopra gli accenti pieni. */
    public static final Color ON_ACCENT = pick(0x11111B, 0xEFF1F5);

    /** Il bianco pieno della UI: sfondi dei campi in Latte, contorni dei grip. */
    public static final Color PAPER = new Color(0xFFFFFF);

    // --- anteprima: colori che non escono mai dallo schermo --------------------

    /**
     * Il supporto disegnato nell'anteprima.
     *
     * <p>Su Mocha il foglio occupa metà finestra e a bianco pieno accanto a una
     * UI scura fa da lampada: il salto di luminanza fra {@link #BASE} e
     * {@code #FFFFFF} è di quindici a uno e dopo qualche minuto si legge male
     * tutto il resto. Il tono qui sotto toglie circa un quarto della luce e
     * resta inequivocabilmente carta. Su Latte non c'è niente da smorzare e il
     * bianco resta pieno.
     *
     * <p>Gli altri due toni provati, se un giorno serve cambiarlo: {@code
     * 0xF2EFE9} appena velato, {@code 0xC8C4BC} netto ma non legge più come
     * bianco.
     *
     * <p><b>Vale solo per lo schermo.</b> PNG, SVG, PDF e la coda di stampa
     * scrivono {@code Color.WHITE} e {@code Color.BLACK} loro, non passano da
     * questa classe, e {@code ExportTest} lo verifica leggendo il constant pool
     * degli esportatori.
     */
    public static final Color PREVIEW_PAPER = pick(0xDDD9D2, 0xFFFFFF);

    /** L'inchiostro nell'anteprima. Vedi {@link #PREVIEW_PAPER}: solo schermo. */
    public static final Color PREVIEW_INK = pick(0x11111B, 0x000000);

    /**
     * Il piano su cui appoggia il foglio. Un gradino in mezzo fra lo sfondo
     * della finestra e la carta: è il passe-partout dei programmi di disegno, e
     * fa più dello smorzare la carta.
     */
    public static final Color PREVIEW_MAT = pick(0x2A2A3C, 0xDCE0E8);

    public static Color blend(Color fg, Color bg, float a) {
        return new Color(
                Math.round(fg.getRed() * a + bg.getRed() * (1 - a)),
                Math.round(fg.getGreen() * a + bg.getGreen() * (1 - a)),
                Math.round(fg.getBlue() * a + bg.getBlue() * (1 - a)));
    }

    private static Color hover(Color c) {
        return LATTE ? blend(Color.BLACK, c, 0.10f) : blend(Color.WHITE, c, 0.14f);
    }

    /** Colore di stato: 0 va bene, 1 attenzione, 2 errore. */
    public static Color forStatus(int severity) {
        if (severity <= 0) {
            return GREEN;
        }
        return severity == 1 ? PEACH : RED;
    }

    // --- caratteri ------------------------------------------------------------

    private static final String SANS = firstInstalled("Segoe UI", Font.SANS_SERIF);
    private static final String MONO = firstInstalled("Consolas", Font.MONOSPACED);

    private static String firstInstalled(String preferred, String logical) {
        if (GraphicsEnvironment.isHeadless()) {
            return logical;
        }
        try {
            for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()) {
                if (f.equalsIgnoreCase(preferred)) {
                    return preferred;
                }
            }
        } catch (RuntimeException noFonts) {
            return logical;
        }
        return logical;
    }

    private static int size(float base) {
        return Math.round(UiScale.pt(base));
    }

    public static final Font UI = new Font(SANS, Font.PLAIN, size(13));
    public static final Font UI_BOLD = new Font(SANS, Font.BOLD, size(13));
    public static final Font UI_SMALL = new Font(SANS, Font.PLAIN, size(11));
    public static final Font UI_TITLE = new Font(SANS, Font.BOLD, size(15));
    public static final Font MONO_FONT = new Font(MONO, Font.PLAIN, size(13));
    public static final Font MONO_BOLD = new Font(MONO, Font.BOLD, size(13));
    public static final Font MONO_BIG = new Font(MONO, Font.BOLD, size(18));

    /**
     * Il simbolo se il carattere lo sa disegnare, altrimenti una parola.
     *
     * <p>Nasce da un rischio concreto su Windows: Segoe UI non contiene tutti i
     * simboli tecnici, e un glifo che manca non è invisibile — diventa un
     * rettangolo vuoto. Un pulsante con dentro un rettangolo vuoto è un
     * pulsante che l'operatore non usa.
     *
     * <p>Si controlla a colpo sicuro con {@code canDisplayUpTo}, una volta
     * all'avvio, e se il simbolo non c'è si ripiega su un'abbreviazione in
     * lettere. Brutta, ma leggibile su qualunque PC di reparto.
     */
    public static String glyph(String symbol, String fallback) {
        Font f = UI_BOLD;
        return f.canDisplayUpTo(symbol) < 0 ? symbol : fallback;
    }

    // --- bordi e superfici ----------------------------------------------------

    /** Bordo arrotondato con imbottitura, l'unico bordo usato nell'app. */
    public static final class RoundBorder extends AbstractBorder {

        private static final long serialVersionUID = 1L;

        private final Color color;
        private final int arc;
        private final Insets pad;

        public RoundBorder(Color color, int arc, Insets pad) {
            this.color = color;
            this.arc = arc;
            this.pad = pad;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return pad;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            return pad;
        }
    }

    public static Border fieldBorder(Color line) {
        return new RoundBorder(line, UiScale.px(10),
                new Insets(UiScale.px(4), UiScale.px(8), UiScale.px(4), UiScale.px(8)));
    }

    /** Un pannello dipinto come una scheda arrotondata: il piano di lavoro. */
    public static JPanel card() {
        JPanel p = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LATTE ? MANTLE : SURFACE0);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UiScale.px(14), UiScale.px(14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        return p;
    }

    // --- pulsanti -------------------------------------------------------------

    private static final class RoundButton extends JButton {

        private static final long serialVersionUID = 1L;

        private final Color bg;
        private final boolean ghost;
        private boolean over;

        RoundButton(String text, Color bg, boolean ghost) {
            super(text);
            this.bg = bg;
            this.ghost = ghost;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(ghost ? TEXT : ON_ACCENT);
            setFont(UI_BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(
                    UiScale.px(7), UiScale.px(14), UiScale.px(7), UiScale.px(14)));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    over = true;
                    if (RoundButton.this.ghost) {
                        setForeground(MAUVE);
                    }
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    over = false;
                    if (RoundButton.this.ghost) {
                        setForeground(TEXT);
                    }
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = UiScale.px(12);
            if (ghost) {
                Color line = !isEnabled() ? blend(SURFACE2, BASE, 0.45f)
                        : over ? MAUVE : SURFACE2;
                g2.setColor(line);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else {
                Color fill = !isEnabled() ? blend(bg, BASE, 0.40f)
                        : over ? hover(bg) : bg;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Pulsante pieno: l'azione vera. */
    public static JButton button(String text, Color bg) {
        return new RoundButton(text, bg, false);
    }

    /** Pulsante di contorno: c'è, ma non è la leva. */
    public static JButton ghost(String text) {
        return new RoundButton(text, BASE, true);
    }

    /** Pulsantino quadrato per le icone testuali (rotazioni, più e meno). */
    public static JButton tool(String glyph, String tooltip) {
        JButton b = ghost(glyph);
        b.setFont(UI_BOLD);
        b.setToolTipText(tooltip);
        b.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(4), UiScale.px(8), UiScale.px(4), UiScale.px(8)));
        return b;
    }

    // --- campi ----------------------------------------------------------------

    public static JTextField field(int columns) {
        final JTextField t = new JTextField(columns);
        t.setFont(MONO_FONT);
        t.setBackground(LATTE ? PAPER : MANTLE);
        t.setForeground(TEXT);
        t.setCaretColor(MAUVE);
        t.setSelectionColor(blend(MAUVE, BASE, 0.35f));
        t.setSelectedTextColor(TEXT);
        t.setBorder(fieldBorder(SURFACE1));
        t.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                t.setBorder(fieldBorder(MAUVE));
            }

            @Override
            public void focusLost(FocusEvent e) {
                t.setBorder(fieldBorder(SURFACE1));
            }
        });
        return t;
    }

    public static JSpinner spinnerInt(int value, int min, int max) {
        int v = Math.max(min, Math.min(max, value));
        JSpinner sp = new JSpinner(new SpinnerNumberModel(v, min, max, 1));
        styleSpinner(sp, 5);
        return sp;
    }

    public static JSpinner spinnerDouble(double value, double min, double max, double step) {
        double v = Math.max(min, Math.min(max, value));
        JSpinner sp = new JSpinner(new SpinnerNumberModel(v, min, max, step));
        styleSpinner(sp, 6);
        return sp;
    }

    /**
     * Uno spinner leggibile e che consegna davvero quello che ci si scrive.
     *
     * <p>Due dettagli, entrambi nati da un difetto vero: la larghezza si
     * dichiara in <b>colonne</b> e non in pixel, altrimenti a schermo ingrandito
     * il numero finisce sotto le frecce; e {@code commitsOnValidEdit} fa
     * arrivare al modello un valore digitato anche quando l'operatore preme
     * subito Stampa senza uscire dal campo.
     */
    private static void styleSpinner(JSpinner sp, int columns) {
        sp.setFont(MONO_FONT);
        sp.setBorder(new RoundBorder(SURFACE1, UiScale.px(8),
                new Insets(UiScale.px(2), UiScale.px(4), UiScale.px(2), UiScale.px(2))));
        sp.setBackground(LATTE ? PAPER : MANTLE);
        JComponent ed = sp.getEditor();
        if (ed instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
            tf.setFont(MONO_FONT);
            tf.setBackground(LATTE ? PAPER : MANTLE);
            tf.setForeground(TEXT);
            tf.setCaretColor(MAUVE);
            tf.setColumns(columns);
            tf.setHorizontalAlignment(SwingConstants.RIGHT);
            tf.setBorder(BorderFactory.createEmptyBorder(
                    UiScale.px(3), UiScale.px(6), UiScale.px(3), UiScale.px(4)));
            if (tf.getFormatter() instanceof DefaultFormatter) {
                ((DefaultFormatter) tf.getFormatter()).setCommitsOnValidEdit(true);
            }
        }
        for (Component c : sp.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setBackground(SURFACE0);
                b.setForeground(SUBTEXT0);
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setFocusable(false);
            }
        }
    }

    public static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> cb = new JComboBox<T>(items);
        styleCombo(cb);
        return cb;
    }

    public static <T> void styleCombo(JComboBox<T> cb) {
        cb.setFont(UI);
        cb.setFocusable(false);
        cb.setBackground(LATTE ? PAPER : MANTLE);
        cb.setForeground(TEXT);
        cb.setBorder(new RoundBorder(SURFACE1, UiScale.px(8),
                new Insets(UiScale.px(2), UiScale.px(6), UiScale.px(2), UiScale.px(2))));
        cb.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object v,
                    int idx, boolean sel, boolean foc) {
                Component c = super.getListCellRendererComponent(list, v, idx, sel, foc);
                c.setBackground(sel ? blend(MAUVE, BASE, 0.25f) : (LATTE ? PAPER : MANTLE));
                c.setForeground(TEXT);
                setBorder(BorderFactory.createEmptyBorder(
                        UiScale.px(4), UiScale.px(8), UiScale.px(4), UiScale.px(8)));
                setFont(UI);
                return c;
            }
        });
    }

    // --- etichette e struttura ------------------------------------------------

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UI);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel hint(String text, int widthPx) {
        JLabel l = new JLabel("<html><body style='width:" + UiScale.px(widthPx) + "px'>"
                + text + "</body></html>");
        l.setFont(UI_SMALL);
        l.setForeground(OVERLAY0);
        return l;
    }

    /** Occhiello maiuscolo più filo di separazione: segna un gruppo vero. */
    public static JComponent section(String title) {
        JPanel p = new JPanel(new java.awt.BorderLayout(UiScale.px(8), 0));
        p.setOpaque(false);
        JLabel l = new JLabel(title.toUpperCase());
        l.setFont(UI_SMALL.deriveFont(Font.BOLD));
        l.setForeground(MAUVE);
        JComponent line = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(SURFACE1);
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        p.add(l, java.awt.BorderLayout.WEST);
        p.add(line, java.awt.BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(UiScale.px(10), 0, UiScale.px(3), 0));
        return p;
    }

    /** Striscia di stato a tutta larghezza. */
    public static JLabel banner() {
        JLabel b = new JLabel("", SwingConstants.CENTER);
        b.setOpaque(true);
        b.setForeground(ON_ACCENT);
        b.setFont(UI_BOLD);
        b.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(6), UiScale.px(12), UiScale.px(6), UiScale.px(12)));
        return b;
    }

    /** Barre di scorrimento sottili, in tinta con il resto. */
    public static void styleScroll(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(UiScale.px(10), 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, UiScale.px(10)));
        sp.getVerticalScrollBar().setUnitIncrement(UiScale.px(16));
        sp.setBackground(BASE);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setOpaque(false);
        sp.setOpaque(false);
    }

    private static final class SlimScrollBarUI extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            thumbColor = SURFACE2;
            trackColor = MANTLE;
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
            g.setColor(MANTLE);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? MAUVE : SURFACE2);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4,
                    UiScale.px(6), UiScale.px(6));
            g2.dispose();
        }
    }

    /** Icona quadrata piena, per le voci di lista degli elementi. */
    public static Icon dot(final Color color) {
        final int s = UiScale.px(10);
        return new Icon() {
            public int getIconWidth() {
                return s;
            }

            public int getIconHeight() {
                return s;
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(x, y, s, s, UiScale.px(3), UiScale.px(3));
                g2.dispose();
            }
        };
    }
}
