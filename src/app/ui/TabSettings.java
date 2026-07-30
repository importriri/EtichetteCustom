package app.ui;

import app.config.AppTheme;
import app.config.LogTarget;
import app.config.SettingsManager;
import app.core.LabelModel;
import app.core.QrCode;
import app.core.SerialWindow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Impostazioni: dove finisce il log e quante cifre entrano nell'incremento.
 *
 * <p>Le cifre si contano da destra: le ultime N posizioni del codice sono la
 * finestra che avanza di uno a ogni etichetta, il resto è prefisso e non si
 * muove. L'anteprima qui sotto mostra il taglio sul codice vero, così non c'è
 * bisogno di spiegarlo a parole.
 */
public final class TabSettings extends JPanel {

    /** Avvisa la finestra principale che qualcosa è cambiato. */
    public interface Listener {
        void settingsApplied();
    }

    private final LabelModel model;
    private Listener listener;

    private final JTextField logDir = new JTextField(26);
    private final JTextField logPattern = new JTextField(18);
    private final JLabel logStatus = new JLabel(" ");

    private final JSpinner digits = new JSpinner(new SpinnerNumberModel(3, 1, 9, 1));
    private final JTextField sample = new JTextField(18);
    private final JLabel windowPreview = new JLabel(" ");
    private final JLabel capacityLabel = new JLabel(" ");

    private final JSpinner dpi = new JSpinner(new SpinnerNumberModel(203, 72, 1200, 1));
    private final JComboBox<QrCode.Ecc> ecc = new JComboBox<QrCode.Ecc>(QrCode.Ecc.values());

    public TabSettings(LabelModel model) {
        this.model = model;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(AppTheme.BASE);
        add(buildForm(), BorderLayout.NORTH);
        load();
        wire();
        refreshWindowPreview();
        checkLogDirectory(false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;

        section(p, c, "Registro giornaliero");
        JPanel dirRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dirRow.setOpaque(false);
        logDir.setEditable(false);
        logDir.setBackground(AppTheme.MANTLE);
        dirRow.add(logDir);
        JButton browse = new JButton("Sfoglia...");
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseDirectory();
            }
        });
        dirRow.add(browse);
        row(p, c, "Cartella", dirRow);
        row(p, c, "Nome file", logPattern);
        hint(p, c, "%s viene sostituito con la data: un file nuovo ogni giorno, "
                + "nessuna riga sovrascritta.");
        logStatus.setFont(AppTheme.UI_SMALL);
        c.gridx = 1;
        p.add(logStatus, c);
        c.gridy++;

        section(p, c, "Numerazione progressiva");
        row(p, c, "Cifre da incrementare", digits);
        row(p, c, "Codice di prova", sample);
        windowPreview.setFont(AppTheme.MONO_BOLD.deriveFont(16f));
        c.gridx = 1;
        p.add(windowPreview, c);
        c.gridy++;
        capacityLabel.setFont(AppTheme.UI_SMALL);
        capacityLabel.setForeground(AppTheme.SUBTEXT0);
        c.gridx = 1;
        p.add(capacityLabel, c);
        c.gridy++;
        hint(p, c, "Le cifre si contano da destra. Quelle evidenziate avanzano di uno "
                + "a ogni etichetta, il resto del codice non viene toccato.");

        section(p, c, "Stampa e QR");
        row(p, c, "Risoluzione (dpi)", dpi);
        row(p, c, "Correzione errore QR", ecc);
        hint(p, c, "203 dpi è la risoluzione nativa della Datamax E-Class. "
                + "Alza la correzione se le etichette si sporcano o si graffiano.");

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        JButton save = new JButton("Salva impostazioni");
        save.setFont(AppTheme.UI_BOLD);
        save.setBackground(AppTheme.BLUE);
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);
        save.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                apply();
            }
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        bottom.setOpaque(false);
        bottom.add(save);
        p.add(bottom, c);
        return p;
    }

    private static void section(JPanel p, GridBagConstraints c, String title) {
        c.gridx = 0;
        c.gridwidth = 2;
        JLabel l = new JLabel(title.toUpperCase());
        l.setFont(AppTheme.UI_SMALL.deriveFont(java.awt.Font.BOLD));
        l.setForeground(AppTheme.OVERLAY0);
        l.setBorder(BorderFactory.createEmptyBorder(14, 0, 2, 0));
        p.add(l, c);
        c.gridy++;
        c.gridwidth = 1;
    }

    private static void row(JPanel p, GridBagConstraints c, String label, java.awt.Component field) {
        c.gridx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(AppTheme.TEXT);
        p.add(l, c);
        c.gridx = 1;
        p.add(field, c);
        c.gridy++;
    }

    private static void hint(JPanel p, GridBagConstraints c, String text) {
        c.gridx = 1;
        JLabel l = new JLabel("<html><body style='width:340px'>" + text + "</body></html>");
        l.setFont(AppTheme.UI_SMALL);
        l.setForeground(AppTheme.SUBTEXT0);
        p.add(l, c);
        c.gridy++;
    }

    // --- stato ----------------------------------------------------------------

    private void load() {
        SettingsManager s = SettingsManager.get();
        File dir = s.logDirectory();
        logDir.setText(dir != null ? dir.getAbsolutePath()
                : SettingsManager.fallbackLogDirectory().getAbsolutePath());
        logPattern.setText(s.getString(SettingsManager.KEY_LOG_PATTERN, LogTarget.DEFAULT_PATTERN));
        digits.setValue(s.getInt(SettingsManager.KEY_SERIAL_DIGITS, 3));
        sample.setText(s.getString("label.sample", "TST-0000-00-001"));
        dpi.setValue(model.dpi());
        ecc.setSelectedItem(model.ecc());
    }

    private void wire() {
        ChangeListener ch = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refreshWindowPreview();
            }
        };
        digits.addChangeListener(ch);
        sample.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                refreshWindowPreview();
            }

            public void removeUpdate(DocumentEvent e) {
                refreshWindowPreview();
            }

            public void changedUpdate(DocumentEvent e) {
                refreshWindowPreview();
            }
        });
    }

    /** Mostra il taglio prefisso/finestra sul codice di prova. */
    private void refreshWindowPreview() {
        String code = sample.getText().trim();
        int n = (Integer) digits.getValue();
        if (code.isEmpty()) {
            windowPreview.setText(" ");
            capacityLabel.setText(" ");
            return;
        }
        try {
            SerialWindow w = SerialWindow.of(code, n);
            windowPreview.setText("<html><span style='color:#9ca0b0'>" + escape(w.prefix())
                    + "</span><span style='color:#1e66f5'><b>" + escape(w.tail())
                    + "</b></span></html>");
            capacityLabel.setText(String.format(
                    "Da qui restano %d etichette, l'ultima e' %s.", w.remaining(), w.last()));
        } catch (RuntimeException e) {
            windowPreview.setText("<html><span style='color:#d20f39'>"
                    + escape(code) + "</span></html>");
            capacityLabel.setText(e.getMessage());
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(logDir.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Cartella del registro giornaliero");
        if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this))
                == JFileChooser.APPROVE_OPTION) {
            logDir.setText(chooser.getSelectedFile().getAbsolutePath());
            checkLogDirectory(true);
        }
    }

    /**
     * Prova davvero a scrivere nella cartella scelta, adesso e non alla prima
     * stampa: un percorso di rete morto si scopre qui.
     */
    private boolean checkLogDirectory(boolean loud) {
        File dir = new File(logDir.getText());
        try {
            LogTarget.assertUsable(dir);
            logStatus.setText("Cartella scrivibile.");
            logStatus.setForeground(AppTheme.GREEN);
            return true;
        } catch (IOException e) {
            logStatus.setText(e.getMessage());
            logStatus.setForeground(AppTheme.RED);
            if (loud) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                        e.getMessage() + "\n\nSe confermi comunque, il registro verrà scritto in "
                        + SettingsManager.fallbackLogDirectory().getAbsolutePath() + ".",
                        "Cartella non utilizzabile", JOptionPane.WARNING_MESSAGE);
            }
            return false;
        }
    }

    /** Scrive le impostazioni su disco e avvisa il resto dell'app. */
    private void apply() {
        SettingsManager s = SettingsManager.get();
        checkLogDirectory(false);
        s.setString(SettingsManager.KEY_LOG_DIR, logDir.getText().trim());
        String pattern = logPattern.getText().trim();
        if (!pattern.contains("%s")) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Il nome del file deve contenere %s, che viene sostituito con la data.",
                    "Nome file non valido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        s.setString(SettingsManager.KEY_LOG_PATTERN, pattern);
        s.setInt(SettingsManager.KEY_SERIAL_DIGITS, (Integer) digits.getValue());
        s.setString("label.sample", sample.getText().trim());

        model.setDpi((Integer) dpi.getValue());
        model.setEcc((QrCode.Ecc) ecc.getSelectedItem());
        s.setString(SettingsManager.KEY_LAYOUT, model.toStorage());

        if (!s.save()) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Non sono riuscito a salvare le impostazioni in "
                    + SettingsManager.configDirectory().getAbsolutePath() + ".",
                    "Salvataggio non riuscito", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (listener != null) {
            listener.settingsApplied();
        }
        MainWindow.status(this, "Impostazioni salvate", 0);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(660, 520);
    }
}
