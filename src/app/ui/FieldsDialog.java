package app.ui;

import app.config.AppTheme;
import app.config.UiScale;
import app.core.LabelField;
import app.core.LabelModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * I campi dell'etichetta: i valori con un nome che gli elementi richiamano.
 *
 * <p>È la finestra che rende disegnabile qualunque etichetta. Un campo fisso è
 * un numero di disegno che cambia una volta per commessa; un progressivo è il
 * seriale che avanza; un campo chiesto è il lotto che l'operatore digita
 * quando lancia la stampa. Un'etichetta può averne quanti ne vuole, e due
 * progressivi diversi avanzano insieme senza che il programma sappia niente
 * di loro.
 */
public final class FieldsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final LabelModel model;
    private final DefaultListModel<LabelField> listModel = new DefaultListModel<LabelField>();
    private final JList<LabelField> list = new JList<LabelField>(listModel);

    private final JTextField name = AppTheme.field(14);
    private final JComboBox<LabelField.Type> type = AppTheme.combo(LabelField.Type.values());
    private final JTextField value = AppTheme.field(16);
    private final JSpinner digits = AppTheme.spinnerInt(3, 1, 9);
    private final JLabel windowPreview = new JLabel(" ");
    private final JLabel capacity = new JLabel(" ");
    private final JLabel tokenLabel = new JLabel(" ");

    private boolean updating;
    private boolean changed;

    public FieldsDialog(Window owner, LabelModel model) {
        super(owner, "Campi dell'etichetta", Dialog.ModalityType.APPLICATION_MODAL);
        this.model = model;

        JPanel root = new JPanel(new BorderLayout(UiScale.px(12), 0));
        root.setBackground(AppTheme.BASE);
        root.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(14), UiScale.px(16), UiScale.px(8), UiScale.px(16)));
        root.add(buildList(), BorderLayout.WEST);
        root.add(buildForm(), BorderLayout.CENTER);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(AppTheme.BASE);
        outer.add(root, BorderLayout.CENTER);
        outer.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(outer);

        reload();
        wire();
        pack();
        setLocationRelativeTo(owner);
    }

    /** {@code true} se l'operatore ha toccato qualcosa. */
    public boolean changed() {
        return changed;
    }

    private JPanel buildList() {
        JPanel p = new JPanel(new BorderLayout(0, UiScale.px(6)));
        p.setOpaque(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(AppTheme.LATTE ? AppTheme.PAPER : AppTheme.MANTLE);
        list.setForeground(AppTheme.TEXT);
        list.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i,
                    boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                LabelField f = (LabelField) v;
                setIcon(AppTheme.dot(f.type() == LabelField.Type.SEQUENZIALE
                        ? AppTheme.MAUVE
                        : f.type() == LabelField.Type.CHIESTO ? AppTheme.PEACH : AppTheme.GREEN));
                setText("{" + f.name() + "}");
                setFont(AppTheme.MONO_FONT);
                setBackground(sel ? AppTheme.blend(AppTheme.MAUVE, AppTheme.BASE, 0.25f)
                        : (AppTheme.LATTE ? AppTheme.PAPER : AppTheme.MANTLE));
                setForeground(AppTheme.TEXT);
                setBorder(BorderFactory.createEmptyBorder(
                        UiScale.px(4), UiScale.px(7), UiScale.px(4), UiScale.px(7)));
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        AppTheme.styleScroll(scroll);
        scroll.setPreferredSize(new Dimension(UiScale.px(180), UiScale.px(220)));
        scroll.setBorder(new AppTheme.RoundBorder(AppTheme.SURFACE1, UiScale.px(8),
                new Insets(2, 2, 2, 2)));
        p.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, UiScale.px(6), 0));
        buttons.setOpaque(false);
        JButton add = AppTheme.ghost("+ Campo");
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addField();
            }
        });
        JButton remove = AppTheme.ghost("Elimina");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeField();
            }
        });
        buttons.add(add);
        buttons.add(remove);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(UiScale.px(4), UiScale.px(4), UiScale.px(4), UiScale.px(4));
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        section(p, c, "Campo selezionato");
        row(p, c, "Nome", name);
        tokenLabel.setFont(AppTheme.MONO_BOLD);
        tokenLabel.setForeground(AppTheme.MAUVE);
        wide(p, c, tokenLabel);
        wide(p, c, AppTheme.hint("Questo è il segnaposto da scrivere dentro un elemento per "
                + "far comparire il valore. Lo stesso campo può finire in dieci elementi "
                + "diversi.", 380));
        row(p, c, "Tipo", type);
        row(p, c, "Valore", value);
        row(p, c, "Cifre che avanzano", digits);

        windowPreview.setFont(AppTheme.MONO_BIG);
        wide(p, c, windowPreview);
        capacity.setFont(AppTheme.UI_SMALL);
        capacity.setForeground(AppTheme.SUBTEXT0);
        wide(p, c, capacity);
        wide(p, c, AppTheme.hint("<b>Fisso</b>: sempre uguale, tipo il numero di disegno. "
                + "<b>Progressivo</b>: le ultime cifre avanzano di uno a ogni etichetta, "
                + "contate da destra. <b>Chiesto</b>: il valore viene domandato quando "
                + "lanci la stampa, per il lotto o il numero d'ordine.", 380));

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(javax.swing.Box.createVerticalGlue(), c);
        return p;
    }

    private JPanel buildFooter() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiScale.px(8), UiScale.px(9)));
        bar.setBackground(AppTheme.MANTLE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.SURFACE0));
        JButton close = AppTheme.button("Fatto", AppTheme.GREEN);
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        bar.add(close);
        return bar;
    }

    // --- stato ----------------------------------------------------------------

    private void reload() {
        updating = true;
        LabelField wanted = list.getSelectedValue();
        listModel.clear();
        for (LabelField f : model.fields()) {
            listModel.addElement(f);
        }
        if (wanted != null && model.fields().contains(wanted)) {
            list.setSelectedValue(wanted, true);
        } else if (!model.fields().isEmpty()) {
            list.setSelectedIndex(0);
        }
        updating = false;
        loadSelected();
    }

    private void wire() {
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && !updating) {
                    loadSelected();
                }
            }
        });
        DocumentListener text = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            public void changedUpdate(DocumentEvent e) {
                apply();
            }
        };
        name.getDocument().addDocumentListener(text);
        value.getDocument().addDocumentListener(text);
        digits.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                apply();
            }
        });
        type.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                apply();
            }
        });
    }

    private void loadSelected() {
        LabelField f = list.getSelectedValue();
        boolean any = f != null;
        name.setEnabled(any);
        type.setEnabled(any);
        value.setEnabled(any);
        digits.setEnabled(any && f.type() == LabelField.Type.SEQUENZIALE);
        if (!any) {
            tokenLabel.setText(" ");
            windowPreview.setText(" ");
            capacity.setText(" ");
            return;
        }
        updating = true;
        name.setText(f.name());
        type.setSelectedItem(f.type());
        value.setText(f.value());
        digits.setValue(f.digits());
        updating = false;
        refreshPreview(f);
    }

    private void refreshPreview(LabelField f) {
        tokenLabel.setText(f.token());
        if (f.type() != LabelField.Type.SEQUENZIALE) {
            windowPreview.setText(" ");
            capacity.setForeground(AppTheme.SUBTEXT0);
            capacity.setText(f.type() == LabelField.Type.CHIESTO
                    ? "Il valore qui sopra è quello proposto la prossima volta."
                    : "Valore fisso: non cambia da un'etichetta all'altra.");
            return;
        }
        try {
            app.core.SerialWindow w = app.core.SerialWindow.of(f.value(), f.digits());
            windowPreview.setText("<html><span style='color:" + hex(AppTheme.SUBTEXT0) + "'>"
                    + escape(w.prefix()) + "</span><span style='color:" + hex(AppTheme.MAUVE)
                    + "'><b>" + w.tail() + "</b></span></html>");
            capacity.setForeground(AppTheme.SUBTEXT0);
            capacity.setText("Restano " + w.remaining() + " etichette, l'ultima è "
                    + w.last() + ".");
        } catch (RuntimeException e) {
            windowPreview.setText("<html><span style='color:" + hex(AppTheme.RED)
                    + "'>\u2014</span></html>");
            capacity.setForeground(AppTheme.RED);
            capacity.setText(e.getMessage());
        }
    }

    private void apply() {
        if (updating) {
            return;
        }
        LabelField f = list.getSelectedValue();
        if (f == null) {
            return;
        }
        try {
            String wanted = name.getText().trim();
            if (!wanted.isEmpty() && !wanted.equalsIgnoreCase(f.name())) {
                LabelField clash = model.field(wanted.toLowerCase(java.util.Locale.ITALIAN));
                if (clash != null && clash != f) {
                    return; // nome già preso: si aspetta che finisca di scrivere
                }
                f.setName(wanted);
            }
            f.setType((LabelField.Type) type.getSelectedItem());
            f.setValue(value.getText());
            f.setDigits((Integer) digits.getValue());
        } catch (RuntimeException whileTyping) {
            return;
        }
        changed = true;
        digits.setEnabled(f.type() == LabelField.Type.SEQUENZIALE);
        refreshPreview(f);
        list.repaint();
    }

    private void addField() {
        String base = "campo";
        int n = 2;
        String candidate = base;
        while (model.field(candidate) != null) {
            candidate = base + n++;
        }
        LabelField f = model.addField(LabelField.fixed(candidate, "valore"));
        changed = true;
        reload();
        list.setSelectedValue(f, true);
        name.requestFocusInWindow();
    }

    private void removeField() {
        LabelField f = list.getSelectedValue();
        if (f == null) {
            return;
        }
        // un campo cancellato lascia orfani i segnaposto che lo usavano: si dice
        // prima, invece di far scoprire {lotto} stampato sul supporto
        int used = 0;
        for (app.core.LabelElement e : model.elements()) {
            if (e.uses(f.name())) {
                used++;
            }
        }
        if (used > 0) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Il campo {" + f.name() + "} è usato da " + used
                    + (used == 1 ? " elemento" : " elementi") + ".\n"
                    + "Eliminandolo il segnaposto resterà scritto così com'è sull'etichetta.\n\n"
                    + "Eliminare lo stesso?",
                    "Campo in uso", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
        }
        model.removeField(f);
        changed = true;
        reload();
    }

    private static String hex(java.awt.Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // --- helper di layout -----------------------------------------------------

    private static void section(JPanel p, GridBagConstraints c, String title) {
        c.gridx = 0;
        c.gridwidth = 2;
        p.add(AppTheme.section(title), c);
        c.gridy++;
        c.gridwidth = 1;
    }

    private static void row(JPanel p, GridBagConstraints c, String label, Component field) {
        c.gridx = 0;
        c.weightx = 0;
        p.add(AppTheme.label(label), c);
        c.gridx = 1;
        c.weightx = 1;
        p.add(field, c);
        c.gridy++;
    }

    private static void wide(JPanel p, GridBagConstraints c, Component comp) {
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        p.add(comp, c);
        c.gridy++;
        c.gridwidth = 1;
    }
}
