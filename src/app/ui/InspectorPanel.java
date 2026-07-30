package app.ui;

import app.config.AppTheme;
import app.config.UiScale;
import app.core.LabelElement;
import app.core.LabelLayout;
import app.core.LabelModel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Il pannello di destra: due facce della stessa cosa.
 *
 * <p>Se hai selezionato un elemento mostra <b>quell'elemento</b>. Se non hai
 * selezionato niente mostra <b>l'etichetta</b>: misura, campi, salvataggio del
 * layout. Nella versione precedente il pannello spariva del tutto e le
 * proprietà dell'etichetta stavano nella riga in alto — cioè la stessa
 * categoria di cose si cercava in due posti diversi a seconda di dove ti
 * trovavi.
 *
 * <p>La rotazione compare qui e <b>solo</b> qui, con un pulsante solo. Prima si
 * poteva ruotare da tre posti — l'icona nella barra, il pulsante grande e due
 * bottoncini per gli angoli — che è esattamente il genere di doppione che rende
 * un'applicazione difficile invece che completa.
 */
public final class InspectorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Quello che il pannello chiede alla finestra di fare. */
    public interface Listener {
        void elementEdited();

        void labelEdited();

        void fieldsRequested();

        void saveLayoutRequested();

        void openLayoutRequested();
    }

    private static final String CARD_LABEL = "etichetta";
    private static final String CARD_ELEMENT = "elemento";

    private final Listener listener;
    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);

    private LabelModel model;
    private LabelElement element;
    private boolean updating;

    // --- faccia "etichetta" ---
    private final JSpinner labelWidth = AppTheme.spinnerDouble(50, 5, 300, 1);
    private final JSpinner labelHeight = AppTheme.spinnerDouble(30, 5, 300, 1);
    private final JLabel fieldsSummary = new JLabel(" ");
    private final JLabel layoutName = new JLabel(" ");

    // --- faccia "elemento" ---
    private final JLabel title = new JLabel();
    private final JTextField content = AppTheme.field(14);
    private final JLabel tokenHint = AppTheme.hint("", 200);
    private final JLabel angle = new JLabel(" ");
    private final JSpinner x = AppTheme.spinnerDouble(0, -200, 500, 1);
    private final JSpinner y = AppTheme.spinnerDouble(0, -200, 500, 1);
    private final JSpinner size = AppTheme.spinnerDouble(4, 0.5, 300, 0.5);
    private final JSpinner wrapWidth = AppTheme.spinnerDouble(0, 0, 300, 1);
    private final JLabel wrapInfo = new JLabel(" ");
    private final JComboBox<LabelElement.Align> align =
            AppTheme.combo(LabelElement.Align.values());
    private final JCheckBox bold = new JCheckBox("Grassetto");
    private final JPanel textOnly = new JPanel(new GridBagLayout());

    public InspectorPanel(Listener listener) {
        this.listener = listener;
        setLayout(new BorderLayout());
        setBackground(AppTheme.MANTLE);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, AppTheme.SURFACE0));
        // 300 e non 262: con le etichette scritte per esteso e gli spinner a
        // fianco, sotto questa misura Windows tronca le parole in "Posizio…"
        setPreferredSize(new Dimension(UiScale.px(300), 10));

        deck.setOpaque(false);
        deck.add(scroll(buildLabelCard()), CARD_LABEL);
        deck.add(scroll(buildElementCard()), CARD_ELEMENT);
        add(deck, BorderLayout.CENTER);
        wire();
    }

    private JScrollPane scroll(JComponent inner) {
        JScrollPane sp = new JScrollPane(inner,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        AppTheme.styleScroll(sp);
        return sp;
    }

    // --- faccia "etichetta" ---------------------------------------------------

    private JPanel buildLabelCard() {
        JPanel p = card();
        GridBagConstraints c = constraints();

        p.add(AppTheme.section("Etichetta"), c);
        c.gridy++;
        p.add(pair("Misura", labelWidth, labelHeight, "mm"), c);
        c.gridy++;

        JButton swap = AppTheme.ghost("Scambia i lati");
        swap.setToolTipText("50 \u00D7 30 diventa 30 \u00D7 50");
        swap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (model != null) {
                    model.swapSides();
                    refresh();
                    fireLabel();
                }
            }
        });
        p.add(flow(swap), c);
        c.gridy++;
        p.add(AppTheme.hint("Un'etichetta verticale è semplicemente 30 \u00D7 50 invece di "
                + "50 \u00D7 30. Gli elementi restano dove sono.", 200), c);
        c.gridy++;

        p.add(AppTheme.section("Campi"), c);
        c.gridy++;
        fieldsSummary.setFont(AppTheme.MONO_FONT);
        fieldsSummary.setForeground(AppTheme.SUBTEXT0);
        p.add(fieldsSummary, c);
        c.gridy++;
        JButton fields = AppTheme.button("Gestisci i campi\u2026", AppTheme.MAUVE);
        fields.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (listener != null) {
                    listener.fieldsRequested();
                }
            }
        });
        p.add(fields, c);
        c.gridy++;
        p.add(AppTheme.hint("I valori con un nome che gli elementi richiamano scrivendo "
                + "<b>{nome}</b>: fissi, progressivi, oppure chiesti quando lanci la stampa.",
                200), c);
        c.gridy++;

        p.add(AppTheme.section("Layout"), c);
        c.gridy++;
        layoutName.setFont(AppTheme.UI_BOLD);
        layoutName.setForeground(AppTheme.TEXT);
        p.add(layoutName, c);
        c.gridy++;
        JPanel buttons = flow(null);
        JButton save = AppTheme.ghost("Salva\u2026");
        save.setToolTipText("Salva questo disegno con un nome");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (listener != null) {
                    listener.saveLayoutRequested();
                }
            }
        });
        JButton open = AppTheme.ghost("Apri\u2026");
        open.setToolTipText("Riapre un disegno salvato");
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (listener != null) {
                    listener.openLayoutRequested();
                }
            }
        });
        buttons.add(save);
        buttons.add(open);
        p.add(buttons, c);
        c.gridy++;
        p.add(AppTheme.hint("Disegna l'etichetta una volta, salvala col nome del prodotto, e "
                + "da domani la riapri e stampi.", 200), c);
        c.gridy++;

        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(Box.createVerticalGlue(), c);
        return p;
    }

    // --- faccia "elemento" ----------------------------------------------------

    private JPanel buildElementCard() {
        JPanel p = card();
        GridBagConstraints c = constraints();

        title.setFont(AppTheme.UI_SMALL.deriveFont(java.awt.Font.BOLD));
        title.setForeground(AppTheme.MAUVE);
        p.add(title, c);
        c.gridy++;
        p.add(content, c);
        c.gridy++;
        p.add(tokenHint, c);
        c.gridy++;

        // l'unico posto da cui si ruota, in tutta l'applicazione
        JButton rotate = AppTheme.button(
                AppTheme.glyph("\u21BB   ", "") + "Ruota 90\u00B0", AppTheme.MAUVE);
        rotate.setToolTipText("Un quarto di giro in senso orario (tasto R)");
        rotate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (element != null) {
                    element.rotateQuarterTurn();
                    refresh();
                    fireElement();
                }
            }
        });
        c.insets = new Insets(UiScale.px(9), 0, UiScale.px(3), 0);
        p.add(rotate, c);
        c.gridy++;
        c.insets = new Insets(UiScale.px(3), 0, UiScale.px(3), 0);

        angle.setFont(AppTheme.UI_SMALL);
        angle.setForeground(AppTheme.OVERLAY0);
        p.add(angle, c);
        c.gridy++;

        p.add(pair("Posizione", x, y, "mm"), c);
        c.gridy++;
        p.add(sizeRow(), c);
        c.gridy++;

        buildTextOnly();
        p.add(textOnly, c);
        c.gridy++;

        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(Box.createVerticalGlue(), c);
        return p;
    }

    private void buildTextOnly() {
        textOnly.setOpaque(false);
        GridBagConstraints c = constraints();

        textOnly.add(AppTheme.section("Testo"), c);
        c.gridy++;
        JPanel wrapRow = flow(null);
        wrapRow.add(label("Va a capo a"));
        wrapRow.add(wrapWidth);
        wrapRow.add(unit("mm"));
        textOnly.add(wrapRow, c);
        c.gridy++;
        wrapInfo.setFont(AppTheme.UI_SMALL);
        wrapInfo.setForeground(AppTheme.OVERLAY0);
        textOnly.add(wrapInfo, c);
        c.gridy++;
        textOnly.add(AppTheme.hint("Stringi e il testo si dispone su due righe, poi tre. Il "
                + "carattere resta della stessa altezza. Zero = riga unica.", 200), c);
        c.gridy++;

        JPanel alignRow = flow(null);
        alignRow.add(label("Allinea"));
        alignRow.add(align);
        textOnly.add(alignRow, c);
        c.gridy++;

        bold.setFont(AppTheme.UI);
        bold.setForeground(AppTheme.TEXT);
        bold.setOpaque(false);
        textOnly.add(bold, c);
    }

    private JPanel sizeRow() {
        JPanel row = flow(null);
        row.add(label("Misura"));
        row.add(size);
        row.add(unit("mm"));
        row.add(step(AppTheme.glyph("\u2212", "-"), 1 / 1.1));
        row.add(step("+", 1.1));
        return row;
    }

    private JButton step(String glyph, final double factor) {
        JButton b = AppTheme.tool(glyph,
                factor > 1 ? "Ingrandisce del 10%" : "Rimpicciolisce del 10%");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (element != null) {
                    element.scaleBy(factor);
                    refresh();
                    fireElement();
                }
            }
        });
        return b;
    }

    // --- helper di layout -----------------------------------------------------

    private static JPanel card() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(12), UiScale.px(14), UiScale.px(12), UiScale.px(14)));
        return p;
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(UiScale.px(3), 0, UiScale.px(3), 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        return c;
    }

    /**
     * Una riga di comandi che va a capo se il pannello è stretto.
     *
     * <p>{@link WrapLayout} e non {@link FlowLayout}: quello di serie manda a
     * capo i componenti ma poi dichiara l'altezza di una riga sola, e il
     * risultato è una riga di roba tagliata a metà. Il pannello è largo 300
     * punti e a schermo ingrandito ci sta meno di quanto sembri.
     */
    private static JPanel flow(Component first) {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, UiScale.px(6), UiScale.px(2)));
        row.setOpaque(false);
        if (first != null) {
            row.add(first);
        }
        return row;
    }

    /** Un'etichetta di campo scritta per esteso: mai abbreviata con i puntini. */
    private static JLabel label(String text) {
        return AppTheme.label(text);
    }

    private static JLabel unit(String text) {
        JLabel u = new JLabel(text);
        u.setFont(AppTheme.UI_SMALL);
        u.setForeground(AppTheme.OVERLAY0);
        return u;
    }

    private static JPanel pair(String caption, JComponent a, JComponent b, String u) {
        JPanel row = flow(null);
        row.add(label(caption));
        row.add(a);
        row.add(unit("\u00D7"));
        row.add(b);
        row.add(unit(u));
        return row;
    }

    // --- collegamenti ---------------------------------------------------------

    private void wire() {
        DocumentListener text = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyElement();
            }

            public void removeUpdate(DocumentEvent e) {
                applyElement();
            }

            public void changedUpdate(DocumentEvent e) {
                applyElement();
            }
        };
        content.getDocument().addDocumentListener(text);

        ChangeListener elementNumbers = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                applyElement();
            }
        };
        x.addChangeListener(elementNumbers);
        y.addChangeListener(elementNumbers);
        size.addChangeListener(elementNumbers);
        wrapWidth.addChangeListener(elementNumbers);

        ActionListener choices = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyElement();
            }
        };
        align.addActionListener(choices);
        bold.addActionListener(choices);

        ChangeListener labelNumbers = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                applyLabel();
            }
        };
        labelWidth.addChangeListener(labelNumbers);
        labelHeight.addChangeListener(labelNumbers);
    }

    /** Mostra l'elemento, o l'etichetta se l'elemento è {@code null}. */
    public void show(LabelModel model, LabelElement element) {
        this.model = model;
        this.element = element;
        cards.show(deck, element == null ? CARD_LABEL : CARD_ELEMENT);
        refresh();
    }

    /** Ricarica i campi dal modello: dopo un trascinamento sull'anteprima. */
    public void refresh() {
        if (model == null) {
            return;
        }
        updating = true;
        labelWidth.setValue(model.widthMm());
        labelHeight.setValue(model.heightMm());
        fieldsSummary.setText(summaryOfFields());
        layoutName.setText(model.templateName());

        if (element != null) {
            boolean isText = element.kind() == LabelElement.Kind.TESTO;
            title.setText((isText ? "TESTO" : "QR") + " \u00B7 "
                    + element.name().toUpperCase());
            content.setText(element.content());
            x.setValue(element.xMm());
            y.setValue(element.yMm());
            size.setValue(element.sizeMm());
            angle.setText(String.format("adesso è a %.0f\u00B0", element.rotationDeg()));
            wrapWidth.setValue(element.wrapWidthMm());
            align.setSelectedItem(element.align());
            bold.setSelected(element.bold());
            textOnly.setVisible(isText);
            tokenHint.setText(hintFor(element));
            wrapInfo.setText(linesFor(element));
        }
        updating = false;
    }

    private String summaryOfFields() {
        if (model.fields().isEmpty()) {
            return "nessun campo";
        }
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < model.fields().size(); i++) {
            app.core.LabelField f = model.fields().get(i);
            sb.append(i > 0 ? "<br>" : "").append("{").append(f.name()).append("} = ")
                    .append(escape(f.value().isEmpty() ? "\u2014" : f.value()));
        }
        return sb.append("</html>").toString();
    }

    private String hintFor(LabelElement e) {
        StringBuilder sb = new StringBuilder("<html><body style='width:")
                .append(UiScale.px(200)).append("px'>");
        java.util.List<String> tokens = e.tokens();
        if (tokens.isEmpty()) {
            sb.append("Testo fisso. Scrivi <b>{nome}</b> per richiamare un campo.");
        } else {
            sb.append("Campi: ");
            for (int i = 0; i < tokens.size(); i++) {
                String name = tokens.get(i);
                boolean known = model != null && model.field(name) != null;
                sb.append(i > 0 ? ", " : "")
                        .append(known ? "<b>" : "<b style='color:#d20f39'>")
                        .append("{").append(name).append("}</b>");
            }
        }
        return sb.append("</body></html>").toString();
    }

    private String linesFor(LabelElement e) {
        if (e.kind() != LabelElement.Kind.TESTO) {
            return " ";
        }
        String sample = model == null ? e.content() : e.resolve(model.valuesAt(0));
        int lines = LabelLayout.lineCount(e, sample);
        return lines == 1 ? "Una riga sola." : "Adesso occupa " + lines + " righe.";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void applyElement() {
        if (updating || element == null) {
            return;
        }
        try {
            element.setContent(content.getText());
            element.setPosition(num(x), num(y));
            element.setSizeMm(num(size));
            element.setWrapWidthMm(num(wrapWidth));
            element.setAlign((LabelElement.Align) align.getSelectedItem());
            element.setBold(bold.isSelected());
        } catch (RuntimeException whileTyping) {
            // valore momentaneamente fuori range mentre si digita: si aspetta
        }
        tokenHint.setText(hintFor(element));
        wrapInfo.setText(linesFor(element));
        fireElement();
    }

    private void applyLabel() {
        if (updating || model == null) {
            return;
        }
        try {
            model.setSizeMm(num(labelWidth), num(labelHeight));
        } catch (RuntimeException whileTyping) {
            return;
        }
        fireLabel();
    }

    private void fireElement() {
        if (listener != null) {
            listener.elementEdited();
        }
    }

    private void fireLabel() {
        if (listener != null) {
            listener.labelEdited();
        }
    }

    private static double num(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }
}
