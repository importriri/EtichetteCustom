package app.ui.operatore;

import app.archivio.Archivio;
import app.archivio.Registro;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Etichetta;
import app.modello.Impostazioni;
import app.modello.Serie;
import app.render.Disegno;
import app.render.SorgenteQr;
import app.stampa.StampaGiro;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.CodiceView;
import app.ui.dati.NomiDati;
import app.ui.finestre.Finestre;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.print.PrinterException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Protected print-preparation view with only run-time choices exposed. */
public final class Operatore extends JPanel {
    private final Etichetta label;
    private final SorgenteQr qr;
    private final Impostazioni settings;
    private final Archivio archive;
    private final Registro log;
    private final Runnable back;
    private final Runnable edit;
    private final Preview preview;

    private final JTextField copies = new JTextField("12", 6);
    private final JLabel state = new JLabel("Pronto per la stampa");
    private final JLabel detail = new JLabel(" ");
    private final JLabel result = new JLabel(" ");
    private final Bottone print = Bottone.primario("Stampa 12 etichette");

    private final Map<Campo, JTextField> values = new LinkedHashMap<Campo, JTextField>();
    private final Map<Campo, CodiceView[]> ranges = new LinkedHashMap<Campo, CodiceView[]>();
    private boolean updating;

    public Operatore(Etichetta label, SorgenteQr qr, Impostazioni settings,
                     Archivio archive, Registro log, Runnable back, Runnable edit) {
        super(new BorderLayout());
        this.label = label;
        this.qr = qr;
        this.settings = settings;
        this.archive = archive;
        this.log = log;
        this.back = back;
        this.edit = edit;

        setBackground(Stile.BASE);
        preview = new Preview();
        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        add(statusBar(), BorderLayout.SOUTH);

        copies.setFont(Stile.normale());
        copies.setHorizontalAlignment(JTextField.RIGHT);
        copies.setPreferredSize(new Dimension(Stile.px(92), Stile.px(36)));
        copies.addActionListener(e -> updateState());
        copies.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) { updateState(); }
        });

        print.addActionListener(e -> sendToPrinter());
        updateState();
    }

    public void salva() {
        try {
            archive.salva(label);
        } catch (Exception ex) {
            result.setText("Salvataggio non riuscito: " + ex.getMessage());
            result.setForeground(Stile.ROSSO);
        }
    }

    private javax.swing.JComponent header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Stile.S0),
                BorderFactory.createEmptyBorder(
                        Stile.px(12), Stile.px(18), Stile.px(12), Stile.px(18))));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(12), 0));
        left.setOpaque(false);
        Bottone backButton = Bottone.piatto("‹  Etichette");
        backButton.addActionListener(e -> back.run());
        left.add(backButton);

        JPanel name = new JPanel();
        name.setOpaque(false);
        name.setLayout(new BoxLayout(name, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(label.nome());
        title.setFont(Stile.titolo());
        title.setForeground(Stile.TESTO);
        JLabel format = new JLabel(num(label.larghezza()) + " × " + num(label.altezza()) + " mm");
        format.setFont(Stile.piccolo());
        format.setForeground(Stile.OV1);
        name.add(title);
        name.add(format);
        left.add(name);
        panel.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Stile.px(7), 0));
        right.setOpaque(false);
        Bottone settingsButton = Bottone.piatto("⚙");
        settingsButton.setToolTipText("Impostazioni");
        settingsButton.addActionListener(e -> Finestre.impostazioni(this, settings));
        Bottone editButton = Bottone.normale("Modifica layout");
        editButton.addActionListener(e -> edit.run());
        right.add(settingsButton);
        right.add(editButton);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private javax.swing.JComponent body() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Stile.BASE);
        panel.setBorder(BorderFactory.createEmptyBorder(
                Stile.px(18), Stile.px(18), Stile.px(18), Stile.px(18)));

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = .70;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, Stile.px(20));
        JPanel previewWrap = new JPanel(new BorderLayout());
        previewWrap.setBackground(Stile.BANCO);
        previewWrap.setBorder(BorderFactory.createLineBorder(Stile.S0));
        previewWrap.add(preview, BorderLayout.CENTER);
        panel.add(previewWrap, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = .30;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        panel.add(controls(), right);
        return panel;
    }

    private javax.swing.JComponent controls() {
        JPanel column = new JPanel(new BorderLayout());
        column.setBackground(Stile.BASE);

        JLabel title = new JLabel("Prepara la stampa");
        title.setFont(Stile.titolo());
        title.setForeground(Stile.TESTO);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, Stile.px(12), 0));
        column.add(title, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        values.clear();
        ranges.clear();

        int editableCount = 0;
        for (Campo field : label.campiUsati()) {
            if (field.comportamento() == Comportamento.FISSO) continue;
            list.add(dataCard(field));
            list.add(javax.swing.Box.createVerticalStrut(Stile.px(10)));
            editableCount++;
        }
        if (editableCount == 0) {
            JLabel none = new JLabel("Nessun dato da inserire.");
            none.setFont(Stile.piccolo());
            none.setForeground(Stile.SUB0);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(none);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Stile.BASE);
        scroll.getVerticalScrollBar().setUnitIncrement(Stile.px(18));
        column.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(Stile.px(12), 0, 0, 0));
        bottom.add(labeled("Copie", copies));
        bottom.add(javax.swing.Box.createVerticalStrut(Stile.px(10)));

        JPanel ready = new JPanel(new BorderLayout());
        ready.setBackground(Stile.VERDE_SOFT);
        ready.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Stile.VERDE_BORDO),
                BorderFactory.createEmptyBorder(
                        Stile.px(10), Stile.px(12), Stile.px(10), Stile.px(12))));
        state.setFont(Stile.forte());
        state.setForeground(Stile.VERDE);
        detail.setFont(Stile.piccolo());
        detail.setForeground(Stile.SUB0);
        ready.add(state, BorderLayout.NORTH);
        ready.add(detail, BorderLayout.SOUTH);
        bottom.add(ready);
        bottom.add(javax.swing.Box.createVerticalStrut(Stile.px(10)));

        print.setAlignmentX(Component.LEFT_ALIGNMENT);
        print.setMaximumSize(new Dimension(Integer.MAX_VALUE, Stile.px(44)));
        bottom.add(print);

        Bottone export = Bottone.piatto("Esporta…");
        export.setAlignmentX(Component.LEFT_ALIGNMENT);
        export.setMaximumSize(new Dimension(Integer.MAX_VALUE, Stile.px(34)));
        export.addActionListener(e -> {
            Integer count = copyCount();
            if (count != null) Finestre.esporta(this, label, qr, count.intValue());
        });
        bottom.add(javax.swing.Box.createVerticalStrut(Stile.px(4)));
        bottom.add(export);
        column.add(bottom, BorderLayout.SOUTH);
        return column;
    }

    private javax.swing.JComponent dataCard(final Campo field) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        Color accent = color(field);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, Stile.px(3), 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Stile.S0),
                        BorderFactory.createEmptyBorder(
                                Stile.px(11), Stile.px(12), Stile.px(11), Stile.px(12)))));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        JLabel title = new JLabel(NomiDati.nome(label, field));
        title.setFont(Stile.forte());
        title.setForeground(Stile.TESTO);
        card.add(title, c);

        c.gridy++;
        c.insets = new Insets(Stile.px(8), 0, Stile.px(4), 0);
        JLabel valueLabel = new JLabel(field.comportamento() == Comportamento.PROGRESSIVO
                ? "Codice iniziale" : "Valore");
        valueLabel.setFont(Stile.piccolo());
        valueLabel.setForeground(Stile.SUB0);
        card.add(valueLabel, c);

        JTextField value = new JTextField(field.serie() != null
                ? field.serie().codice(field.serie().prossimo())
                : field.valore());
        value.setFont(Stile.normale());
        value.setPreferredSize(new Dimension(Stile.px(180), Stile.px(36)));
        values.put(field, value);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(value, c);
        value.addActionListener(e -> saveInputsAndRefresh());
        value.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) { saveInputsAndRefresh(); }
        });

        if (field.comportamento() == Comportamento.PROGRESSIVO) {
            JPanel range = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(5), 0));
            range.setOpaque(false);
            CodiceView from = new CodiceView("", "");
            CodiceView to = new CodiceView("", "");
            from.corpo(11);
            to.corpo(11);
            ranges.put(field, new CodiceView[] { from, to });
            range.add(from);
            JLabel arrow = new JLabel("→");
            arrow.setForeground(Stile.OV1);
            range.add(arrow);
            range.add(to);
            c.gridy++;
            c.insets = new Insets(Stile.px(8), 0, 0, 0);
            card.add(range, c);
        }

        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private void saveInputsAndRefresh() {
        if (updating) return;
        saveInputs();
        updateState();
    }

    private void saveInputs() {
        for (Map.Entry<Campo, JTextField> entry : values.entrySet()) {
            Campo field = entry.getKey();
            String value = entry.getValue().getText().trim();
            try {
                if (field.comportamento() == Comportamento.PROGRESSIVO) {
                    int count = field.serie() == null ? 3 : field.serie().cifre();
                    field.serie(new Serie(value, count));
                } else {
                    field.valore(value);
                }
            } catch (RuntimeException ex) {
                result.setText(NomiDati.nome(label, field) + ": " + ex.getMessage());
                result.setForeground(Stile.ROSSO);
            }
        }
        preview.repaint();
    }

    private Integer copyCount() {
        try {
            int count = Integer.parseInt(copies.getText().trim());
            if (count < 1 || count > 100000) throw new NumberFormatException();
            return Integer.valueOf(count);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void updateState() {
        if (updating) return;
        updating = true;
        try {
            Integer count = copyCount();
            String error = null;
            if (count == null) {
                error = "Inserisci un numero di copie tra 1 e 100000";
            } else {
                try {
                    label.validaGiro(count.intValue());
                } catch (RuntimeException ex) {
                    error = ex.getMessage();
                }
            }

            if (count != null) {
                for (Map.Entry<Campo, CodiceView[]> entry : ranges.entrySet()) {
                    Campo field = entry.getKey();
                    if (field.serie() == null) continue;
                    Serie series = field.serie();
                    try {
                        String[] run = series.giro(count.intValue());
                        entry.getValue()[0].testo(series.prefisso(), series.finestra(series.prossimo()));
                        String last = run[run.length - 1];
                        entry.getValue()[1].testo(series.prefisso(),
                                last.substring(Math.min(series.prefisso().length(), last.length())));
                    } catch (RuntimeException ex) {
                        error = ex.getMessage();
                    }
                }
            }

            if (error == null && count != null) {
                int n = count.intValue();
                print.setText("Stampa " + n + (n == 1 ? " etichetta" : " etichette"));
                state.setText("✓  Pronto");
                state.setForeground(Stile.VERDE);
                detail.setText(n + (n == 1 ? " etichetta" : " etichette")
                        + " · " + num(label.larghezza()) + " × " + num(label.altezza()) + " mm");
                print.setEnabled(true);
            } else {
                print.setText("Stampa");
                state.setText("⚠  Controlla i dati");
                state.setForeground(Stile.PESCA);
                detail.setText(error == null ? "Controlla i valori" : error);
                print.setEnabled(false);
            }
            preview.repaint();
        } finally {
            updating = false;
        }
    }

    private void sendToPrinter() {
        saveInputs();
        Integer count = copyCount();
        if (count == null) {
            updateState();
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        }

        int n = count.intValue();
        try {
            label.validaGiro(n);
            String[] codes = label.codiciGiro(n);
            StampaGiro job = new StampaGiro(label, qr, n);
            if (!job.manda(label.nome())) {
                result.setText("Stampa annullata · nessun progressivo avanzato");
                result.setForeground(Stile.SUB0);
                return;
            }
            label.consumaProgressivi(n);
            archive.salva(label);
            log.annota(label, codes, settings.stampante());
            syncFieldsFromModel();
            updateState();
            result.setText("✓ Giro stampato e registrato");
            result.setForeground(Stile.VERDE);
        } catch (PrinterException ex) {
            result.setText("Stampante: " + ex.getMessage());
            result.setForeground(Stile.ROSSO);
        } catch (Exception ex) {
            result.setText("Stampa non completata: " + ex.getMessage());
            result.setForeground(Stile.ROSSO);
        }
    }

    private void syncFieldsFromModel() {
        updating = true;
        try {
            for (Map.Entry<Campo, JTextField> entry : values.entrySet()) {
                Campo field = entry.getKey();
                entry.getValue().setText(field.serie() != null
                        ? field.serie().codice(field.serie().prossimo())
                        : field.valore());
            }
        } finally {
            updating = false;
        }
    }

    private javax.swing.JComponent labeled(String name, javax.swing.JComponent component) {
        JPanel row = new JPanel(new BorderLayout(Stile.px(10), 0));
        row.setOpaque(false);
        JLabel label = new JLabel(name);
        label.setFont(Stile.piccolo());
        label.setForeground(Stile.SUB0);
        row.add(label, BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Stile.px(36)));
        return row;
    }

    private javax.swing.JComponent statusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Stile.S0),
                BorderFactory.createEmptyBorder(
                        Stile.px(7), Stile.px(18), Stile.px(7), Stile.px(18))));
        result.setFont(Stile.piccolo());
        result.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(result, BorderLayout.EAST);
        return panel;
    }

    private static Color color(Campo field) {
        if (field.comportamento() == Comportamento.PROGRESSIVO) return Stile.PESCA;
        if (field.comportamento() == Comportamento.CHIESTO) return Stile.LAVANDA;
        return Stile.CELESTE;
    }

    private static String num(double value) {
        return String.valueOf(Math.round(value * 10) / 10.0)
                .replace(".0", "").replace('.', ',');
    }

    private final class Preview extends javax.swing.JComponent {
        @Override public Dimension getPreferredSize() {
            return new Dimension(Stile.px(650), Stile.px(500));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Stile.liscio(g);
            try {
                g2.setColor(Stile.BANCO);
                g2.fillRect(0, 0, getWidth(), getHeight());
                double sx = (getWidth() - Stile.px(80)) / label.larghezza();
                double sy = (getHeight() - Stile.px(90)) / label.altezza();
                double mm = Math.max(.1, Math.min(sx, sy));
                int width = (int) Math.round(label.larghezza() * mm);
                int height = (int) Math.round(label.altezza() * mm);
                int x = (getWidth() - width) / 2;
                int y = (getHeight() - height) / 2;
                g2.setColor(new Color(0, 0, 0, 34));
                g2.fillRect(x + Stile.px(5), y + Stile.px(6), width, height);
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y, width, height);
                g2.setColor(Stile.S1);
                g2.drawRect(x, y, width, height);
                g2.translate(x, y);
                Disegno.disegna(g2, label, mm, qr, 0);
            } finally {
                g2.dispose();
            }
        }
    }
}
