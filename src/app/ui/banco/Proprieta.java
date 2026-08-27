package app.ui.banco;

import app.codice.Code128;
import app.codice.Correzione;
import app.codice.Qr;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Serie;
import app.modello.Tipo;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.CodiceView;
import app.ui.comp.Scheda;
import app.ui.dati.NomiDati;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/** Contextual inspector that reveals secondary choices only when they are needed. */
public class Proprieta extends JPanel implements javax.swing.Scrollable {
    private final Runnable after;
    private final Runnable before;
    private boolean silent;
    private boolean showPrecision;
    private boolean showContentOptions;
    private boolean showLinkPicker;
    private boolean showQrOptions;

    public Proprieta(Runnable before, Runnable after) {
        this.before = before;
        this.after = after;
        setBackground(Stile.BASE);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(
                Stile.px(18), Stile.px(16), Stile.px(18), Stile.px(16)));
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return Stile.px(24); }
    @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return Stile.px(220); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }

    public void mostra(final Etichetta label, final Elemento element) {
        removeAll();
        if (element == null) {
            showEmptyState();
            return;
        }

        silent = true;
        try {
            add(title(label, element));
            add(Scheda.spazio(14));
            if (element.tipo() != Tipo.LINEA) {
                add(content(label, element));
                add(Scheda.spazio(12));
            }
            if (element.tipo().scritto()) {
                add(text(element));
                add(Scheda.spazio(12));
            }
            if (element.tipo() == Tipo.QR) {
                add(qr(label, element));
                add(Scheda.spazio(12));
            }
            if (element.tipo() == Tipo.BARCODE) {
                add(barcode(label, element));
                add(Scheda.spazio(12));
            }
            add(position(label, element));
            add(javax.swing.Box.createVerticalGlue());
        } finally {
            silent = false;
        }
        revalidate();
        repaint();
    }

    private void showEmptyState() {
        JLabel heading = new JLabel("Seleziona un elemento");
        heading.setFont(Stile.forte());
        heading.setForeground(Stile.SUB0);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(heading);

        JTextArea hint = secondaryText("Trascinalo per spostarlo. Usa le maniglie blu per ridimensionarlo.");
        hint.setBorder(BorderFactory.createEmptyBorder(Stile.px(6), 0, 0, 0));
        add(hint);
        revalidate();
        repaint();
    }

    private Component title(Etichetta label, Elemento element) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(8), 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel(element.nome());
        name.setFont(Stile.titolo());
        name.setForeground(Stile.TESTO);
        row.add(name);
        row.add(new Badge(element.tipo().etichetta(), Stile.BLU_SOFT, Stile.BLU));
        wrap.add(row);

        Campo field = label.campo(element.campo());
        if (field != null && label.elementiPerCampo(field).size() > 1) {
            JTextArea shared = secondaryText("Collegato a " + NomiDati.uso(label, field));
            shared.setBorder(BorderFactory.createEmptyBorder(Stile.px(5), Stile.px(2), 0, 0));
            wrap.add(shared);
        }
        return wrap;
    }

    private Component content(final Etichetta label, final Elemento element) {
        Scheda card = new Scheda("Contenuto");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        final Campo field = label.campo(element.campo());
        if (field == null) {
            card.nota("Questo elemento non ha ancora un contenuto.");
            return card;
        }

        final JTextField value = new JTextField(visibleValue(field), 18);
        prepareField(value);
        value.setToolTipText("Valore completo usato da questo elemento");
        Runnable saveValue = new Runnable() {
            @Override public void run() {
                if (!silent) applyValue(label, field, value);
            }
        };
        value.addActionListener(e -> saveValue.run());
        value.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) { saveValue.run(); }
        });
        card.campo(field.comportamento() == Comportamento.PROGRESSIVO
                ? "Codice iniziale" : "Valore", value);

        card.largo(new Stato(behaviorLabel(field.comportamento()),
                behaviorColor(field.comportamento()), behaviorBackground(field.comportamento())));

        if (field.comportamento() == Comportamento.PROGRESSIVO) {
            addSequenceControls(card, label, element, field);
        } else if (field.comportamento() == Comportamento.CHIESTO) {
            card.nota("Questo valore verra' chiesto soltanto quando prepari la stampa.");
        }

        JToggleButton behavior = choice(showContentOptions ? "Nascondi opzioni" : "Cambia comportamento…",
                showContentOptions);
        behavior.addActionListener(e -> {
            showContentOptions = behavior.isSelected();
            mostra(label, element);
        });
        card.largo(behavior);

        if (showContentOptions) {
            card.riga("Durante la stampa", behaviorChoices(label, element, field, value));
        }

        int users = label.elementiPerCampo(field).size();
        if (users > 1) {
            JLabel shared = new JLabel("🔗  " + NomiDati.uso(label, field));
            shared.setFont(Stile.piccolo().deriveFont(java.awt.Font.BOLD));
            shared.setForeground(Stile.BLU);
            card.largo(shared);

            Bottone detach = Bottone.normale("Separa questo elemento");
            detach.setToolTipText("Crea un contenuto indipendente solo per questo elemento");
            detach.addActionListener(e -> {
                if (!silent) {
                    mark();
                    label.rendiIndipendente(element);
                    showLinkPicker = false;
                    mostra(label, element);
                    changed();
                }
            });
            card.largo(detach);
        } else if (label.campi().size() > 1) {
            JToggleButton link = choice(showLinkPicker ? "Annulla collegamento" : "Collega a un altro elemento…",
                    showLinkPicker);
            link.addActionListener(e -> {
                showLinkPicker = link.isSelected();
                mostra(label, element);
            });
            card.largo(link);
            if (showLinkPicker) {
                card.campo("Usa il contenuto di", linkPicker(label, element, field));
            }
        }
        return card;
    }

    private void addSequenceControls(Scheda card, final Etichetta label,
                                     final Elemento element, final Campo field) {
        Serie series = field.serie();
        if (series == null) {
            card.nota("Il codice deve terminare con almeno una cifra per poter aumentare.");
            return;
        }

        CodiceView preview = new CodiceView(series.prefisso(), series.finestra(series.prossimo()));
        preview.corpo(12);
        card.largo(preview);

        Integer[] options = new Integer[9];
        for (int i = 0; i < options.length; i++) options[i] = Integer.valueOf(i + 1);
        final JComboBox<Integer> digits = new JComboBox<Integer>(options);
        digits.setSelectedItem(Integer.valueOf(series.cifre()));
        digits.setFont(Stile.normale());
        digits.setPreferredSize(new Dimension(Stile.px(82), Stile.px(34)));
        digits.addActionListener(e -> {
            if (!silent && digits.getSelectedItem() != null) {
                mark();
                label.cambiaFinestra(field.nome(), ((Integer) digits.getSelectedItem()).intValue());
                mostra(label, element);
                changed();
            }
        });
        card.riga("Ultime cifre che aumentano", digits);
    }

    private Component behaviorChoices(final Etichetta label, final Elemento element,
                                      final Campo field, final JTextField value) {
        JPanel panel = compactGroup();
        ButtonGroup group = new ButtonGroup();
        Comportamento[] values = {
            Comportamento.FISSO,
            Comportamento.PROGRESSIVO,
            Comportamento.CHIESTO
        };
        String[] labels = {"Non cambia", "Aumenta", "Chiedi"};

        for (int i = 0; i < values.length; i++) {
            final Comportamento target = values[i];
            JToggleButton button = choice(labels[i], field.comportamento() == target);
            button.addActionListener(e -> {
                if (!silent) {
                    mark();
                    if (target == Comportamento.PROGRESSIVO) {
                        try {
                            int digits = field.serie() == null ? 3 : field.serie().cifre();
                            field.serie(new Serie(value.getText().trim(), digits));
                        } catch (RuntimeException ex) {
                            java.awt.Toolkit.getDefaultToolkit().beep();
                            value.setToolTipText(ex.getMessage());
                            return;
                        }
                    } else {
                        field.comportamento(target);
                    }
                    showContentOptions = false;
                    mostra(label, element);
                    changed();
                }
            });
            group.add(button);
            panel.add(button);
        }
        return panel;
    }

    private Component linkPicker(final Etichetta label, final Elemento element, Campo current) {
        final JComboBox<Campo> combo = new JComboBox<Campo>();
        for (Campo candidate : label.campi()) {
            if (candidate != current) combo.addItem(candidate);
        }
        combo.setFont(Stile.normale());
        combo.setRenderer(new CampoRenderer(label));
        combo.setPreferredSize(new Dimension(Stile.px(210), Stile.px(36)));
        combo.setToolTipText("Scegli un contenuto gia' usato nell'etichetta");
        combo.addActionListener(e -> {
            if (!silent && combo.getSelectedItem() != null) {
                mark();
                element.campo(((Campo) combo.getSelectedItem()).nome());
                showLinkPicker = false;
                mostra(label, element);
                changed();
            }
        });
        return combo;
    }

    private Component text(final Elemento element) {
        Scheda card = new Scheda("Testo");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.riga("Allineamento", alignment(element));
        card.riga("Righe", rows(element));

        final JCheckBox separators = new JCheckBox("Mostra punti e simboli", element.mostraSeparatori());
        separators.setOpaque(false);
        separators.setFont(Stile.piccolo());
        separators.setForeground(Stile.SUB1);
        separators.setFocusPainted(false);
        separators.addActionListener(e -> {
            if (!silent) {
                mark();
                element.mostraSeparatori(separators.isSelected());
                changed();
            }
        });
        card.largo(separators);
        return card;
    }

    private Component alignment(final Elemento element) {
        JPanel panel = compactGroup();
        ButtonGroup group = new ButtonGroup();
        String[] labels = {"Sinistra", "Centro", "Destra"};
        for (int i = 0; i < labels.length; i++) {
            final int value = i;
            JToggleButton button = choice(labels[i], element.allineamento() == i);
            button.addActionListener(e -> {
                if (!silent) {
                    mark();
                    element.allineamento(value);
                    changed();
                }
            });
            group.add(button);
            panel.add(button);
        }
        return panel;
    }

    private Component rows(final Elemento element) {
        JPanel panel = compactGroup();
        ButtonGroup group = new ButtonGroup();
        String[] labels = {"Auto", "1", "2", "3"};
        int current = element.righePreferite();
        for (int i = 0; i < labels.length; i++) {
            final int value = i;
            JToggleButton button = choice(labels[i], current == i);
            button.addActionListener(e -> {
                if (!silent) {
                    mark();
                    element.righePreferite(value);
                    element.massimoRighe(3);
                    changed();
                }
            });
            group.add(button);
            panel.add(button);
        }
        return panel;
    }

    private Component qr(final Etichetta label, final Elemento element) {
        Scheda card = new Scheda("QR");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        String value = label.contenuto(element, 0);
        int side;
        try {
            side = Qr.codifica(value, element.correzione()).length;
        } catch (RuntimeException ex) {
            card.largo(new Stato("QR non valido", Stile.ROSSO, Stile.PESCA_SOFT));
            card.nota(ex.getMessage());
            return card;
        }

        double module = element.larghezza() / side;
        double quiet = 4 * module;
        boolean hasQuietZone = element.x() >= quiet && element.y() >= quiet
                && label.larghezza() - element.x() - element.larghezza() >= quiet
                && label.altezza() - element.y() - element.larghezza() >= quiet;

        if (module >= .25 && hasQuietZone) {
            card.largo(new Stato("Pronto da leggere", Stile.VERDE, Stile.VERDE_SOFT));
        } else if (module < .25) {
            card.largo(new Stato("Ingrandisci il QR", Stile.PESCA, Stile.PESCA_SOFT));
            card.nota("Allarga il QR trascinando una maniglia blu.");
        } else {
            card.largo(new Stato("Serve piu' spazio bianco", Stile.PESCA, Stile.PESCA_SOFT));
            card.nota("Spostalo un po' piu' lontano dai bordi.");
        }

        JToggleButton options = choice(showQrOptions ? "Nascondi opzioni QR" : "Opzioni QR…", showQrOptions);
        options.addActionListener(e -> {
            showQrOptions = options.isSelected();
            mostra(label, element);
        });
        card.largo(options);

        if (showQrOptions) {
            final JComboBox<Correzione> correction = new JComboBox<Correzione>(Correzione.values());
            correction.setSelectedItem(element.correzione());
            correction.setFont(Stile.normale());
            correction.setPreferredSize(new Dimension(Stile.px(120), Stile.px(34)));
            correction.addActionListener(e -> {
                if (!silent) {
                    mark();
                    element.correzione((Correzione) correction.getSelectedItem());
                    mostra(label, element);
                    changed();
                }
            });
            card.riga("Robustezza", correction);
            if (showPrecision) {
                card.riga("Matrice", smallLabel(side + " x " + side));
                card.riga("Modulo", smallLabel(mm(module)));
            }
        }
        return card;
    }

    private Component barcode(final Etichetta label, final Elemento element) {
        Scheda card = new Scheda("Barcode");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        String value = label.contenuto(element, 0);
        try {
            int modules = Code128.moduli(value);
            double module = element.larghezza() / modules;
            card.largo(new Stato(module >= .25 ? "Pronto da leggere" : "Allarga il barcode",
                    module >= .25 ? Stile.VERDE : Stile.PESCA,
                    module >= .25 ? Stile.VERDE_SOFT : Stile.PESCA_SOFT));
            if (showPrecision) {
                card.riga("Formato", smallLabel("Code 128"));
                card.riga("Barra minima", smallLabel(mm(module)));
            }
        } catch (RuntimeException ex) {
            card.largo(new Stato("Contenuto non valido", Stile.ROSSO, Stile.PESCA_SOFT));
            card.nota(ex.getMessage());
        }
        return card;
    }

    private Component position(final Etichetta label, final Elemento element) {
        Scheda card = new Scheda("Posizione");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rotation = compactGroup();
        ButtonGroup group = new ButtonGroup();
        int[] degrees = {0, 90, 180, 270};
        for (final int degree : degrees) {
            JToggleButton button = choice(degree + "°", element.rotazione() == degree);
            button.addActionListener(e -> {
                if (!silent) {
                    mark();
                    element.rotazione(degree);
                    changed();
                }
            });
            group.add(button);
            rotation.add(button);
        }
        card.riga("Ruota", rotation);

        JToggleButton precision = choice(showPrecision ? "Nascondi misure" : "Misure precise…",
                showPrecision);
        precision.addActionListener(e -> {
            showPrecision = precision.isSelected();
            mostra(label, element);
        });
        card.largo(precision);

        if (showPrecision) {
            card.riga("X", measure(element.x(), 0, label.larghezza(), v -> element.x(v)));
            card.riga("Y", measure(element.y(), 0, label.altezza(), v -> element.y(v)));
            card.riga("Larghezza", measure(element.larghezza(), 1,
                    Math.max(label.larghezza(), label.altezza()), v -> element.larghezza(v)));
            if (element.tipo() == Tipo.BARCODE || element.tipo() == Tipo.LINEA) {
                card.riga("Altezza", measure(element.altezza(), .4,
                        Math.max(label.larghezza(), label.altezza()), v -> element.altezza(v)));
            }
            if (element.tipo().scritto()) {
                card.riga("Testo", measure(element.corpo(), 1, 40, v -> element.corpo(v)));
            }
        }
        return card;
    }

    private interface Setter { void set(double value); }

    private Component measure(final double value, final double min,
                              final double max, final Setter setter) {
        JPanel row = new JPanel(new java.awt.BorderLayout(Stile.px(6), 0));
        row.setOpaque(false);
        final JTextField field = new JTextField(number(value), 5);
        prepareField(field);
        field.setHorizontalAlignment(JTextField.RIGHT);
        JLabel unit = new JLabel("mm");
        unit.setFont(Stile.piccolo());
        unit.setForeground(Stile.SUB0);
        row.add(field, java.awt.BorderLayout.CENTER);
        row.add(unit, java.awt.BorderLayout.EAST);

        Runnable save = new Runnable() {
            @Override public void run() {
                if (silent) return;
                try {
                    double parsed = Double.parseDouble(field.getText().trim().replace(',', '.'));
                    parsed = Math.max(min, Math.min(max, parsed));
                    mark();
                    setter.set(parsed);
                    field.setText(number(parsed));
                    changed();
                } catch (NumberFormatException ex) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    field.setText(number(value));
                }
            }
        };
        field.addActionListener(e -> save.run());
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) { save.run(); }
        });
        return row;
    }

    private static JPanel compactGroup() {
        JPanel panel = new JPanel(new GridLayout(1, 0, Stile.px(4), 0));
        panel.setOpaque(false);
        return panel;
    }

    private static JToggleButton choice(String text, boolean selected) {
        JToggleButton button = new JToggleButton(text);
        button.setFont(Stile.minuscolo());
        button.setSelected(selected);
        button.setFocusPainted(false);
        button.setMargin(new java.awt.Insets(
                Stile.px(6), Stile.px(7), Stile.px(6), Stile.px(7)));
        return button;
    }

    private static void prepareField(JTextField field) {
        field.setFont(Stile.normale());
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, Stile.px(36)));
    }

    private static JTextArea secondaryText(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Stile.piccolo());
        area.setForeground(Stile.SUB0);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setColumns(24);
        area.setRows(2);
        Dimension preferred = area.getPreferredSize();
        area.setPreferredSize(new Dimension(Stile.px(245), preferred.height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return area;
    }

    private static String visibleValue(Campo field) {
        return field.serie() != null
                ? field.serie().codice(field.serie().prossimo())
                : field.valore();
    }

    private void applyValue(Etichetta label, Campo field, JTextField input) {
        String value = input.getText().trim();
        try {
            mark();
            if (field.comportamento() == Comportamento.PROGRESSIVO) {
                int digits = field.serie() == null ? 3 : field.serie().cifre();
                field.serie(new Serie(value, digits));
            } else {
                field.valore(value);
            }
            changed();
        } catch (RuntimeException ex) {
            input.setToolTipText(ex.getMessage());
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    private static String behaviorLabel(Comportamento behavior) {
        if (behavior == Comportamento.PROGRESSIVO) return "Aumenta automaticamente";
        if (behavior == Comportamento.CHIESTO) return "Chiesto alla stampa";
        return "Non cambia";
    }

    private static Color behaviorColor(Comportamento behavior) {
        if (behavior == Comportamento.PROGRESSIVO) return Stile.PESCA;
        if (behavior == Comportamento.CHIESTO) return Stile.BLU;
        return Stile.SUB1;
    }

    private static Color behaviorBackground(Comportamento behavior) {
        if (behavior == Comportamento.PROGRESSIVO) return Stile.PESCA_SOFT;
        if (behavior == Comportamento.CHIESTO) return Stile.BLU_SOFT;
        return Stile.MANTLE;
    }

    private static JLabel smallLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Stile.normale());
        label.setForeground(Stile.TESTO);
        return label;
    }

    private static String number(double value) {
        return String.valueOf(Math.round(value * 10.0) / 10.0).replace('.', ',');
    }

    private static String mm(double value) {
        return String.valueOf(Math.round(value * 100) / 100.0).replace('.', ',') + " mm";
    }

    private void mark() {
        if (!silent && before != null) before.run();
    }

    private void changed() {
        if (after != null) after.run();
    }

    private static final class CampoRenderer extends DefaultListCellRenderer {
        private final Etichetta label;

        CampoRenderer(Etichetta label) { this.label = label; }

        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focus) {
            super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof Campo) setText(NomiDati.nome(label, (Campo) value));
            return this;
        }
    }

    private static final class Badge extends JLabel {
        private final Color background;
        private final Color border;

        Badge(String text, Color background, Color border) {
            super(text.toUpperCase());
            this.background = background;
            this.border = border;
            setFont(Stile.minuscolo().deriveFont(java.awt.Font.BOLD));
            setForeground(border);
            setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Stile.liscio(g);
            try {
                Stile.riquadro(g2, 0, 0, getWidth(), getHeight(),
                        Stile.px(7), background, border);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class Stato extends JLabel {
        private final Color background;

        Stato(String text, Color foreground, Color background) {
            super("●  " + text);
            this.background = background;
            setFont(Stile.piccolo().deriveFont(java.awt.Font.BOLD));
            setForeground(foreground);
            setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Stile.liscio(g);
            try {
                Stile.riquadro(g2, 0, 0, getWidth(), getHeight(),
                        Stile.px(8), background, null);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
