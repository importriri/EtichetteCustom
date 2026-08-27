package app.ui.vetrina;

import app.modello.Etichetta;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.CodiceView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Home gallery: labels first, recent print runs second. */
public class Vetrina extends JPanel {
    private final FluidGrid grid = new FluidGrid();
    private final JLabel count = new JLabel();
    private final SorgenteQr qr;
    private final JTextField search = new JTextField();
    private List<Etichetta> all = new ArrayList<Etichetta>();

    public interface Comandi {
        void apri(Etichetta label);
        void modifica(Etichetta label);
        void nuova();
        void rinomina(Etichetta label);
        void duplica(Etichetta label);
        void elimina(Etichetta label);
        void stampante();
        void impostazioni();
    }

    private final Comandi commands;
    private final app.archivio.Registro log;
    private final JPanel recentRuns = new JPanel();

    public Vetrina(List<Etichetta> labels, SorgenteQr qr, Comandi commands) {
        this(labels, qr, commands, null);
    }

    public Vetrina(List<Etichetta> labels, SorgenteQr qr, Comandi commands,
                   app.archivio.Registro log) {
        super(new BorderLayout());
        this.commands = commands;
        this.qr = qr;
        this.log = log;
        setBackground(Stile.BASE);
        int padding = Stile.px(24);
        setBorder(BorderFactory.createEmptyBorder(
                padding, Stile.px(28), padding, Stile.px(28)));

        add(header(), BorderLayout.NORTH);

        Column content = new Column();
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(grid);
        JComponent recent = recentRunsBlock();
        recent.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(recent);
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Stile.BASE);
        scroll.getVerticalScrollBar().setUnitIncrement(Stile.px(24));
        add(scroll, BorderLayout.CENTER);

        popola(labels, qr);
    }

    public void popola(List<Etichetta> labels, SorgenteQr ignored) {
        all = new ArrayList<Etichetta>(labels);
        filter();
        refreshRuns();
    }

    private void filter() {
        String query = search.getText() == null
                ? "" : search.getText().trim().toLowerCase();
        grid.removeAll();
        int visible = 0;
        for (Etichetta label : all) {
            if (!query.isEmpty() && !label.nome().toLowerCase().contains(query)) continue;
            visible++;
            grid.add(new Tessera(label, qr)
                    .azione(item -> commands.apri(item))
                    .menu(this::showMenu));
        }
        grid.add(Tessera.nuova(item -> commands.nuova()));
        int n = query.isEmpty() ? all.size() : visible;
        count.setText(n + (n == 1 ? " modello" : " modelli"));
        grid.revalidate();
        grid.repaint();
    }

    private void showMenu(final Etichetta label, int x, int y) {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        menu.add(menuItem("Apri", () -> commands.apri(label)));
        menu.add(menuItem("Rinomina…", () -> commands.rinomina(label)));
        menu.add(menuItem("Duplica", () -> commands.duplica(label)));
        menu.addSeparator();
        javax.swing.JMenuItem delete = menuItem("Elimina", () -> commands.elimina(label));
        delete.setForeground(Stile.ROSSO);
        menu.add(delete);
        for (Component component : grid.getComponents()) {
            if (component instanceof Tessera && ((Tessera) component).etichetta() == label) {
                menu.show(component, x, y);
                return;
            }
        }
    }

    private javax.swing.JMenuItem menuItem(String text, final Runnable action) {
        javax.swing.JMenuItem item = new javax.swing.JMenuItem(text);
        item.setFont(Stile.normale());
        item.addActionListener(e -> action.run());
        return item;
    }

    private JComponent header() {
        JPanel panel = new JPanel(new BorderLayout(Stile.px(18), 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, Stile.px(18), 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(10), 0));
        left.setOpaque(false);
        JLabel title = new JLabel("Etichette");
        title.setFont(Stile.titolo());
        title.setForeground(Stile.TESTO);
        count.setFont(Stile.piccolo());
        count.setForeground(Stile.OV0);
        left.add(title);
        left.add(count);
        panel.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Stile.px(8), 0));
        right.setOpaque(false);
        search.setFont(Stile.normale());
        search.setToolTipText("Cerca etichetta");
        search.setPreferredSize(new Dimension(Stile.px(320), Stile.px(36)));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { filter(); }
            @Override public void removeUpdate(DocumentEvent event) { filter(); }
            @Override public void changedUpdate(DocumentEvent event) { filter(); }
        });

        Bottone newLabel = Bottone.primario("+  Nuova etichetta");
        newLabel.addActionListener(e -> commands.nuova());
        Bottone settings = Bottone.piatto("⚙");
        settings.setToolTipText("Impostazioni");
        settings.addActionListener(e -> commands.impostazioni());
        right.add(search);
        right.add(newLabel);
        right.add(settings);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JComponent recentRunsBlock() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(Stile.px(26), 0, 0, 0));

        JLabel title = new JLabel("Ultime stampe");
        title.setFont(Stile.forte());
        title.setForeground(Stile.TESTO);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(Stile.px(6)));

        recentRuns.setOpaque(false);
        recentRuns.setLayout(new BoxLayout(recentRuns, BoxLayout.Y_AXIS));
        recentRuns.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(recentRuns);
        refreshRuns();
        return panel;
    }

    private void refreshRuns() {
        recentRuns.removeAll();
        List<app.archivio.Registro.Giro> runs = log == null
                ? new ArrayList<app.archivio.Registro.Giro>()
                : log.ultimi(4);
        if (runs.isEmpty()) {
            JLabel empty = new JLabel("Nessuna stampa registrata finora");
            empty.setFont(Stile.piccolo());
            empty.setForeground(Stile.OV0);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(
                    Stile.px(6), Stile.px(8), 0, 0));
            recentRuns.add(empty);
        } else {
            for (app.archivio.Registro.Giro run : runs) {
                recentRuns.add(new RunRow(run.etichetta(), run.primo(), run.ultimo(),
                        run.quante() + (run.quante() == 1 ? " etichetta" : " etichette"),
                        run.quando()));
            }
        }
        recentRuns.revalidate();
        recentRuns.repaint();
    }

    private static class Column extends JPanel implements Scrollable {
        Column() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return Stile.px(24); }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Stile.px(200); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static class RunRow extends JComponent {
        private final String name;
        private final String first;
        private final String last;
        private final String amount;
        private final String when;

        RunRow(String name, String first, String last, String amount, String when) {
            this.name = name;
            this.first = first == null ? "" : first;
            this.last = last == null ? "" : last;
            this.amount = amount;
            this.when = when;
        }

        private int commonPrefix() {
            int i = 0;
            while (i < first.length() && i < last.length()
                    && first.charAt(i) == last.charAt(i)) i++;
            return i;
        }

        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, Stile.px(34));
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(Stile.px(700), Stile.px(34));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Stile.liscio(g);
            try {
                g2.setColor(Stile.S0);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                int baseline = Stile.px(22);
                g2.setFont(Stile.normale());
                g2.setColor(Stile.SUB1);
                g2.drawString(name, Stile.px(8), baseline);

                int split = commonPrefix();
                String prefix = first.substring(0, split);
                if (prefix.length() > 7) prefix = "…" + prefix.substring(prefix.length() - 6);
                java.awt.Font mono = Stile.mono(11);
                int x = Stile.px(210);
                x += CodiceView.disegna(g2, x, baseline, prefix, first.substring(split), mono);
                g2.setFont(mono);
                g2.setColor(Stile.OV1);
                g2.drawString("  →  ", x, baseline);
                x += g2.getFontMetrics().stringWidth("  →  ");
                CodiceView.disegna(g2, x, baseline, prefix, last.substring(split), mono);

                g2.setFont(Stile.normale());
                g2.setColor(Stile.SUB1);
                g2.drawString(amount, Stile.px(500), baseline);
                g2.setFont(Stile.piccolo());
                g2.setColor(Stile.OV0);
                int width = g2.getFontMetrics().stringWidth(when);
                g2.drawString(when, getWidth() - width - Stile.px(8), baseline);
            } finally {
                g2.dispose();
            }
        }
    }

    private static class FluidGrid extends JPanel implements Scrollable {
        FluidGrid() {
            setOpaque(false);
            setLayout(null);
        }

        private int step() { return Stile.px(258) + Stile.px(18); }
        private int columns(int width) {
            return Math.max(1, (width + Stile.px(18)) / step());
        }

        @Override public void doLayout() {
            int columns = columns(getWidth());
            int i = 0;
            for (Component component : getComponents()) {
                Dimension size = component.getPreferredSize();
                int row = i / columns;
                int column = i % columns;
                component.setBounds(column * step(),
                        row * (size.height + Stile.px(18)), size.width, size.height);
                i++;
            }
        }

        @Override public Dimension getPreferredSize() {
            int count = getComponentCount();
            if (count == 0) return new Dimension(Stile.px(258), Stile.px(236));
            int width = getWidth() > 0 ? getWidth() : Stile.px(1000);
            int columns = columns(width);
            int rows = (count + columns - 1) / columns;
            int height = getComponent(0).getPreferredSize().height;
            return new Dimension(width, rows * (height + Stile.px(18)));
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return Stile.px(24); }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Stile.px(200); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
