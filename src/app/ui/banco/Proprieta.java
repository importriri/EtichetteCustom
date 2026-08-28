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
import app.render.Disegno;
import app.render.Misuratore;
import app.render.Testo;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.CodiceView;
import app.ui.comp.Scheda;
import app.ui.dati.NomiDati;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
    private boolean showSharedOptions;
    private boolean showQrOptions;
    private boolean showTextLayout;

    public Proprieta(Runnable before, Runnable after) {
        this.before = before;
        this.after = after;
        setBackground(Stile.BASE);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(
                Stile.px(18), Stile.px(16), Stile.px(18), Stile.px(16)));
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) {
        return Stile.px(24);
    }
    @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) {
        return Stile.px(220);
    }
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
            add(title(element));
            add(Scheda.spazio(14));
            if (element.tipo() != Tipo.LINEA) {
                add(content(label, element));
                add(Scheda.spazio(12));
            }
            if (element.tipo().scritto()) {
                add(text(label, element));
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

        JTextArea hint = secondaryText(
                "Trascinalo per spostarlo. Usa le maniglie blu per ridimensionarlo.");
        hint.setBorder(BorderFactory.createEmptyBorder(Stile.px(6), 0, 0, 0));
        add(hint);
        revalidate();
        repaint();
    }

    private Component title(Elemento element) {
        JLabel name = new JLabel(element.nome());
        name.setFont(Stile.titolo());
        name.setForeground(Stile.TESTO);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        return name;
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

        if (field.comportamento() == Comportamento.CHIESTO) {
            card.nota("Lo chiederemo solo quando prepari la stampa.");
        }

        JToggleButton behavior = choice(showContentOptions ? "Chiudi" : "Come cambia…",
                showContentOptions);
        behavior.setName("content-behavior");
        behavior.addActionListener(e -> {
            showContentOptions = behavior.isSelected();
            mostra(label, element);
        });
        card.largo(behavior);

        if (showContentOptions) {
            card.riga("Durante la stampa", behaviorChoice(label, element, field, value));
            if (field.comportamento() == Comportamento.PROGRESSIVO) {
                addSequenceControls(card, label, element, field);
            }
        }

        int users = label.elementiPerCampo(field).size();
        if (users > 1) {
            JToggleButton shared = choice("🔗  " + NomiDati.tipoUso(label, field), showSharedOptions);
            shared.setName("shared-content");
            shared.setToolTipText(NomiDati.uso(label, field));
            shared.addActionListener(e -> {
                showSharedOptions = shared.isSelected();
                mostra(label, element);
            });
            card.largo(shared);

            if (showSharedOptions) {
                Bottone detach = Bottone.normale("Rendi indipendente");
                detach.setToolTipText("Usa un contenuto separato solo per questo elemento");
                detach.addActionListener(e -> {
                    if (!silent) {
                        mark();
                        label.rendiIndipendente(element);
                        showSharedOptions = false;
                        showLinkPicker = false;
                        mostra(label, element);
                        changed();
                    }
                });
                card.largo(detach);
            }
        } else if (label.campi().size() > 1) {
            JToggleButton link = choice(showLinkPicker ? "Annulla" : "Usa contenuto esistente…",
                    showLinkPicker);
            link.addActionListener(e -> {
                showLinkPicker = link.isSelected();
                mostra(label, element);
            });
            card.largo(link);
            if (showLinkPicker) {
                card.campo("Contenuto", linkPicker(label, element, field));
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
        digits.setPreferredSize(new Dimension(Stile.px(120), Stile.px(36)));
        digits.addActionListener(e -> {
            if (!silent && digits.getSelectedItem() != null) {
                mark();
                label.cambiaFinestra(field.nome(), ((Integer) digits.getSelectedItem()).intValue());
                mostra(label, element);
                changed();
            }
        });
        card.riga("Cifre che aumentano", digits);
    }

    private Component behaviorChoice(final Etichetta label, final Elemento element,
                                     final Campo field, final JTextField value) {
        final String[] labels = {"Non cambia", "Aumenta", "Chiedi in stampa"};
        final Comportamento[] values = {
            Comportamento.FISSO,
            Comportamento.PROGRESSIVO,
            Comportamento.CHIESTO
        };
        final JComboBox<String> combo = new JComboBox<String>(labels);
        combo.setName("content-behavior-choice");
        combo.setFont(Stile.normale());
        combo.setSelectedIndex(behaviorIndex(field.comportamento()));
        combo.setPreferredSize(new Dimension(Stile.px(210), Stile.px(36)));
        combo.addActionListener(e -> {
            if (silent || combo.getSelectedIndex() < 0) return;
            final Comportamento target = values[combo.getSelectedIndex()];
            if (target == field.comportamento()) return;

            if (target == Comportamento.PROGRESSIVO) {
                try {
                    int digits = field.serie() == null ? 3 : field.serie().cifre();
                    Serie next = new Serie(value.getText().trim(), digits);
                    mark();
                    field.serie(next);
                } catch (RuntimeException ex) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    value.setToolTipText(ex.getMessage());
                    combo.setSelectedIndex(behaviorIndex(field.comportamento()));
                    return;
                }
            } else {
                mark();
                field.comportamento(target);
            }
            showContentOptions = false;
            mostra(label, element);
            changed();
        });
        return combo;
    }

    private static int behaviorIndex(Comportamento behavior) {
        if (behavior == Comportamento.PROGRESSIVO) return 1;
        if (behavior == Comportamento.CHIESTO) return 2;
        return 0;
    }

    private Component linkPicker(final Etichetta label, final Elemento element, Campo current) {
        final JComboBox<Campo> combo = new JComboBox<Campo>();
        for (Campo candidate : label.campi()) {
            if (candidate != current) combo.addItem(candidate);
        }
        combo.setFont(Stile.normale());
        combo.setRenderer(new CampoRenderer(label));
        combo.setPreferredSize(new Dimension(Stile.px(210), Stile.px(36)));
        combo.setToolTipText("Scegli un contenuto già usato nell'etichetta");
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

    private Component text(final Etichetta label, final Elemento element) {
        Scheda card = new Scheda("Testo");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.riga("Allinea", alignment(element));
        card.riga("Righe", rows(element));

        final JCheckBox separators = new JCheckBox(
                "Mostra punti e simboli", element.mostraSeparatori());
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

        final String[] parts = Testo.parti(label.contenuto(element, 0));
        if (parts.length > 1) {
            if (element.parteTesto() > 0) {
                int current = Math.min(parts.length, element.parteTesto());
                Stato partState = new Stato("Blocco " + current + " di " + parts.length,
                        Stile.SUB1, Stile.MANTLE);
                partState.setToolTipText(
                        "Questo blocco mostra solo una parte. Il QR continua a usare il codice intero.");
                card.largo(partState);
            }

            JToggleButton layout = choice(
                    showTextLayout ? "Chiudi disposizione" : "Organizza testo…", showTextLayout);
            layout.setName("text-layout");
            layout.addActionListener(e -> {
                showTextLayout = layout.isSelected();
                mostra(label, element);
            });
            card.largo(layout);

            if (showTextLayout) {
                card.riga("Mostra", textPartChoice(label, element, parts));
                if (element.parteTesto() == 0) {
                    Bottone split = Bottone.normale("Dividi in blocchi");
                    split.setName("split-text-parts");
                    split.setToolTipText(
                            "Crea blocchi spostabili che restano collegati allo stesso codice del QR.");
                    split.addActionListener(e -> {
                        if (!silent) {
                            mark();
                            splitText(label, element, parts);
                            showTextLayout = false;
                            mostra(label, element);
                            changed();
                        }
                    });
                    card.largo(split);
                }
            }
        }
        return card;
    }

    private Component textPartChoice(final Etichetta label, final Elemento element,
                                     final String[] parts) {
        String[] options = new String[parts.length + 1];
        options[0] = "Codice intero";
        for (int i = 0; i < parts.length; i++) {
            options[i + 1] = (i + 1) + " · " + abbreviate(parts[i], 20);
        }
        final JComboBox<String> combo = new JComboBox<String>(options);
        combo.setName("text-part");
        combo.setFont(Stile.normale());
        combo.setSelectedIndex(Math.max(0, Math.min(parts.length, element.parteTesto())));
        combo.setPreferredSize(new Dimension(Stile.px(210), Stile.px(36)));
        combo.addActionListener(e -> {
            if (!silent && combo.getSelectedIndex() >= 0
                    && combo.getSelectedIndex() != element.parteTesto()) {
                mark();
                element.parteTesto(combo.getSelectedIndex());
                if (element.parteTesto() > 0) element.mostraSeparatori(false);
                mostra(label, element);
                changed();
            }
        });
        return combo;
    }

    private static String abbreviate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(1, max - 1)) + "…";
    }

    /** Splits presentation only; every generated block keeps the same source reference. */
    private static void splitText(Etichetta label, Elemento element, String[] parts) {
        if (parts == null || parts.length < 2 || element.parteTesto() != 0) return;

        Elemento source = element.copia();
        double gap = .8;
        double rowGap = .6;
        double left = source.x();
        double available = Math.max(1.0, label.larghezza() - left);
        double requested = available;
        double[] widths = measuredBlockWidths(parts, source, requested);
        int rows = 1;
        double rowWidth = 0;
        for (int i = 0; i < parts.length; i++) {
            if (rowWidth > 0 && rowWidth + gap + widths[i] > requested + .001) {
                rows++;
                rowWidth = widths[i];
            } else {
                rowWidth += (rowWidth > 0 ? gap : 0) + widths[i];
            }
        }

        double rowHeight = Math.max(2.0, source.corpo() * 1.25);
        double totalHeight = rows * rowHeight + (rows - 1) * rowGap;
        double startY = Math.min(source.y(), Math.max(0, label.altezza() - totalHeight));
        double cursorX = left;
        double cursorY = startY;

        for (int i = 0; i < parts.length; i++) {
            if (cursorX > left && cursorX + widths[i] > left + requested + .001) {
                cursorX = left;
                cursorY += rowHeight + rowGap;
            }

            Elemento target = i == 0 ? element : source.copia();
            target.nome(source.nome() + " " + (i + 1));
            target.parteTesto(i + 1);
            target.mostraSeparatori(false);
            target.massimoRighe(1);
            target.righePreferite(1);
            target.allineamento(0);
            target.larghezza(widths[i]);
            target.x(cursorX);
            target.y(cursorY);
            cursorX += widths[i] + gap;
            if (i > 0) label.aggiungi(target);
        }
    }

    /** Measures split blocks with the same font metrics used by the renderer. */
    private static double[] measuredBlockWidths(String[] parts, Elemento source,
                                                double maximumWidth) {
        double[] widths = new double[parts.length];
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            Misuratore measurer = Disegno.misuratore(graphics, 10);
            for (int i = 0; i < parts.length; i++) {
                double measured = measurer.larghezza(
                        parts[i], source.corpo(), source.grassetto()) + 1.0;
                widths[i] = Math.min(maximumWidth, Math.max(3.0, measured));
            }
        } finally {
            graphics.dispose();
        }
        return widths;
    }

    private Component alignment(final Elemento element) {
        final JComboBox<String> combo = new JComboBox<String>(
                new String[] {"Sinistra", "Centro", "Destra"});
        combo.setName("text-alignment");
        combo.setFont(Stile.normale());
        combo.setSelectedIndex(Math.max(0, Math.min(2, element.allineamento())));
        combo.setPreferredSize(new Dimension(Stile.px(150), Stile.px(36)));
        combo.addActionListener(e -> {
            if (!silent && combo.getSelectedIndex() >= 0) {
                mark();
                element.allineamento(combo.getSelectedIndex());
                changed();
            }
        });
        return combo;
    }

    private Component rows(final Elemento element) {
        final JComboBox<String> combo = new JComboBox<String>(
                new String[] {"Automatiche", "1", "2", "3"});
        combo.setName("text-rows");
        combo.setFont(Stile.normale());
        combo.setSelectedIndex(Math.max(0, Math.min(3, element.righePreferite())));
        combo.setPreferredSize(new Dimension(Stile.px(150), Stile.px(36)));
        combo.addActionListener(e -> {
            if (!silent && combo.getSelectedIndex() >= 0) {
                mark();
                element.righePreferite(combo.getSelectedIndex());
                element.massimoRighe(3);
                changed();
            }
        });
        return combo;
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
            card.largo(new Stato("Serve più spazio bianco", Stile.PESCA, Stile.PESCA_SOFT));
            card.nota("Spostalo un po' più lontano dai bordi.");
        }

        JToggleButton options = choice(
                showQrOptions ? "Chiudi opzioni QR" : "Opzioni QR…", showQrOptions);
        options.addActionListener(e -> {
            showQrOptions = options.isSelected();
            mostra(label, element);
        });
        card.largo(options);

        if (showQrOptions) {
            final JComboBox<Correzione> correction =
                    new JComboBox<Correzione>(Correzione.values());
            correction.setSelectedItem(element.correzione());
            correction.setFont(Stile.normale());
            correction.setPreferredSize(new Dimension(Stile.px(150), Stile.px(36)));
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
        Scheda card = new Scheda("Disponi");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        Bottone rotate = Bottone.normale("↻  Ruota 90°");
        rotate.setName("rotate-90");
        rotate.setToolTipText("Ogni clic ruota l'elemento di 90 gradi.");
        rotate.addActionListener(e -> {
            if (!silent) {
                mark();
                element.rotazione((element.rotazione() + 90) % 360);
                changed();
            }
        });
        card.largo(rotate);

        JToggleButton precision = choice(
                showPrecision ? "Chiudi misure" : "Misure precise…", showPrecision);
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

    private static JToggleButton choice(String text, boolean selected) {
        return new DisclosureButton(text, selected);
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
            if (field.comportamento() == Comportamento.PROGRESSIVO) {
                int digits = field.serie() == null ? 3 : field.serie().cifre();
                Serie next = new Serie(value, digits);
                mark();
                field.serie(next);
            } else {
                mark();
                field.valore(value);
            }
            changed();
        } catch (RuntimeException ex) {
            input.setToolTipText(ex.getMessage());
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    private static String behaviorLabel(Comportamento behavior) {
        if (behavior == Comportamento.PROGRESSIVO) return "Aumenta da solo";
        if (behavior == Comportamento.CHIESTO) return "Da inserire in stampa";
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

    /** Lightweight disclosure control that renders consistently on Windows and Linux. */
    private static final class DisclosureButton extends JToggleButton {
        DisclosureButton(String text, boolean selected) {
            super(text, selected);
            setFont(Stile.piccolo());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(Stile.px(120), d.width + Stile.px(20)), Stile.px(36));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Stile.liscio(g);
            try {
                boolean selected = isSelected();
                boolean over = getModel().isRollover();
                boolean down = getModel().isPressed();
                Color fill = selected ? Stile.BLU_SOFT
                        : (down ? Stile.MANTLE : (over ? Color.WHITE : Stile.BASE));
                Color border = selected || isFocusOwner() ? Stile.BLU : Stile.S1;
                Color text = selected ? Stile.BLU : Stile.TESTO;
                Stile.riquadro(g2, 1, 1, getWidth() - 2, getHeight() - 2,
                        Stile.px(9), fill, border);
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? text : Stile.OV0);
                int width = g2.getFontMetrics().stringWidth(getText());
                int baseline = (getHeight() + g2.getFontMetrics().getAscent()
                        - g2.getFontMetrics().getDescent()) / 2;
                g2.drawString(getText(), Math.max(Stile.px(8), (getWidth() - width) / 2), baseline);
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class Stato extends JLabel {
        private final Color background;

        Stato(String text, Color foreground, Color background) {
            super("●  " + text);
            this.background = background;
            setFont(Stile.piccolo().deriveFont(java.awt.Font.BOLD));
            setForeground(foreground);
            setBorder(BorderFactory.createEmptyBorder(
                    Stile.px(7), Stile.px(9), Stile.px(7), Stile.px(9)));
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
