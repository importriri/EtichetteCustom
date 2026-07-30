package app.ui;

import app.config.AppTheme;
import app.config.LogTarget;
import app.config.SettingsManager;
import app.config.UiScale;
import app.core.LabelModel;
import app.core.QrCode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;

/**
 * Le impostazioni che si toccano una volta e poi si dimenticano: dove finisce
 * il registro, come rendere il QR, di che colore è l'applicazione, e i due
 * manuali.
 *
 * <p>Quello che si tocca ogni giorno — codice, quantità, misura, stampa — non
 * sta qui: sta nella riga in cima alla finestra principale. La divisione è
 * questa e non ce ne sono altre.
 */
public final class SettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final LabelModel model;
    private final PrinterDialog.StatusSink status;

    private final JTextField logDir = AppTheme.field(22);
    private final JTextField logPattern = AppTheme.field(14);
    private final JLabel logStatus = new JLabel(" ");
    private final JSpinner dpi = AppTheme.spinnerInt(203, 72, 1200);
    private final JComboBox<String> dpiPreset =
            AppTheme.combo(new String[] {"203 (termica standard)", "300 (termica fine)",
                                         "600 (alta risoluzione)", "personalizzato"});
    private final JComboBox<QrCode.Ecc> ecc = AppTheme.combo(QrCode.Ecc.values());
    private final JSpinner moduleWarn = AppTheme.spinnerDouble(0.30, 0.05, 2.0, 0.05);
    private final JSpinner minQr = AppTheme.spinnerDouble(0, 0, 100, 0.5);
    private final JComboBox<String> flavor = AppTheme.combo(new String[] {"latte", "mocha"});
    private final ManualPane manual = new ManualPane();

    public SettingsDialog(Window owner, LabelModel model, PrinterDialog.StatusSink status) {
        super(owner, "Impostazioni", Dialog.ModalityType.APPLICATION_MODAL);
        this.model = model;
        this.status = status;

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.BASE);
        JScrollPane scroll = new JScrollPane(buildForm(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        AppTheme.styleScroll(scroll);
        root.add(scroll, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);

        load();
        checkLogDirectory(false);
        pack();
        setSize(getWidth(), Math.min(UiScale.px(640), getHeight()));
        setLocationRelativeTo(owner);
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(
                UiScale.px(14), UiScale.px(16), UiScale.px(8), UiScale.px(16)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(UiScale.px(4), UiScale.px(4), UiScale.px(4), UiScale.px(4));
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        section(p, c, "Registro giornaliero");
        JPanel dirRow = flow(null);
        logDir.setEditable(false);
        dirRow.add(logDir);
        JButton browse = AppTheme.ghost("Sfoglia\u2026");
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseDirectory();
            }
        });
        dirRow.add(browse);
        row(p, c, "Cartella", dirRow);
        row(p, c, "Nome file", logPattern);
        wide(p, c, AppTheme.hint("%s viene sostituito con la data: un file nuovo ogni giorno, "
                + "nessuna riga sovrascritta. Se la cartella diventa irraggiungibile il "
                + "programma non si ferma, scrive in locale e lo dice nella barra di stato.",
                420));
        logStatus.setFont(AppTheme.UI_SMALL);
        wide(p, c, logStatus);

        section(p, c, "Stampa e QR");
        row(p, c, "Risoluzione", dpiPreset);
        row(p, c, "dpi", dpi);
        wide(p, c, AppTheme.hint("La risoluzione della <b>tua</b> stampante, quella scritta "
                + "sul manuale o nelle proprietà del driver. 203 dpi è il valore più comune "
                + "sulle termiche da etichette.", 420));
        row(p, c, "Correzione errore QR", ecc);
        row(p, c, "Soglia modulo (mm)", moduleWarn);
        row(p, c, "Lato minimo QR (mm)", minQr);
        wide(p, c, AppTheme.hint("Alza la correzione se le etichette si sporcano o si "
                + "graffiano. Il <b>lato minimo</b> serve quando il cliente lo impone da "
                + "capitolato: sotto quella misura compare l'avviso. Zero = nessun minimo.",
                420));

        section(p, c, "Aspetto");
        row(p, c, "Variante colori", flavor);
        wide(p, c, AppTheme.hint("<b>latte</b> è chiara, per il capannone illuminato; "
                + "<b>mocha</b> è scura. Il cambio si vede al prossimo avvio.", 420));

        section(p, c, "Manuale");
        wide(p, c, manual);
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
        JButton save = AppTheme.button("Salva e chiudi", AppTheme.GREEN);
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (apply()) {
                    dispose();
                }
            }
        });
        JButton close = AppTheme.ghost("Chiudi");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        bar.add(close);
        bar.add(save);
        return bar;
    }


    // --- stato ----------------------------------------------------------------

    private void load() {
        SettingsManager s = SettingsManager.get();
        File dir = s.logDirectory();
        logDir.setText(dir != null ? dir.getAbsolutePath()
                : SettingsManager.fallbackLogDirectory().getAbsolutePath());
        logPattern.setText(s.getString(SettingsManager.KEY_LOG_PATTERN, LogTarget.DEFAULT_PATTERN));
        dpi.setValue(model.dpi());
        selectDpiPreset(model.dpi());
        ecc.setSelectedItem(model.ecc());
        moduleWarn.setValue(model.moduleWarnMm());
        minQr.setValue(model.minQrSideMm());
        flavor.setSelectedItem(s.getString(SettingsManager.KEY_UI_FLAVOR, "latte"));

        dpiPreset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = dpiPreset.getSelectedIndex();
                int[] values = {203, 300, 600};
                if (index >= 0 && index < values.length) {
                    dpi.setValue(values[index]);
                }
                dpi.setEnabled(index == 3);
            }
        });
    }

    private void selectDpiPreset(int value) {
        int[] values = {203, 300, 600};
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                dpiPreset.setSelectedIndex(i);
                dpi.setEnabled(false);
                return;
            }
        }
        dpiPreset.setSelectedIndex(3);
        dpi.setEnabled(true);
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(logDir.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Cartella del registro giornaliero");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            logDir.setText(chooser.getSelectedFile().getAbsolutePath());
            checkLogDirectory(true);
        }
    }

    /**
     * Prova davvero a scrivere nella cartella scelta.
     *
     * @param loud {@code true} per avvisare con una finestra: in fabbrica un
     *             percorso di rete sparisce senza dire niente, e la differenza
     *             fra scoprirlo qui e scoprirlo a metà giro è tutta
     */
    private boolean checkLogDirectory(boolean loud) {
        File dir = new File(logDir.getText());
        try {
            LogTarget.assertUsable(dir);
            logStatus.setForeground(AppTheme.GREEN);
            logStatus.setText("Cartella scrivibile.");
            return true;
        } catch (IOException e) {
            logStatus.setForeground(AppTheme.PEACH);
            logStatus.setText(e.getMessage());
            if (loud) {
                JOptionPane.showMessageDialog(this, e.getMessage()
                        + "\n\nSe confermi comunque, il registro verrà scritto nella cartella "
                        + "locale di ripiego.", "Cartella del registro",
                        JOptionPane.WARNING_MESSAGE);
            }
            return false;
        }
    }

    private boolean apply() {
        SettingsManager s = SettingsManager.get();
        checkLogDirectory(false);
        s.setString(SettingsManager.KEY_LOG_DIR, logDir.getText());
        s.setString(SettingsManager.KEY_LOG_PATTERN, logPattern.getText().trim());
        s.setString(SettingsManager.KEY_UI_FLAVOR, (String) flavor.getSelectedItem());
        try {
            model.setDpi((Integer) dpi.getValue());
            model.setEcc((QrCode.Ecc) ecc.getSelectedItem());
            model.setModuleWarnMm(num(moduleWarn));
            model.setMinQrSideMm(num(minQr));
        } catch (RuntimeException invalid) {
            JOptionPane.showMessageDialog(this, invalid.getMessage(),
                    "Impostazioni", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        s.setString(SettingsManager.KEY_LAYOUT, model.toStorage());
        boolean saved = s.save();
        say(saved ? "Impostazioni salvate."
                  : "Impostazioni non salvate: cartella di configurazione non scrivibile.",
                saved ? 0 : 2);
        return true;
    }

    private void say(String message, int severity) {
        if (status != null) {
            status.say(message, severity);
        }
    }

    private static double num(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    // --- helper di layout -----------------------------------------------------

    private static JPanel flow(Component first) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiScale.px(8), UiScale.px(3)));
        row.setOpaque(false);
        if (first != null) {
            row.add(first);
        }
        return row;
    }

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
