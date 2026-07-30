package app.ui;

import app.config.AppTheme;
import app.config.SettingsManager;
import app.config.UiScale;
import app.core.LabelModel;
import app.core.LabelPrinter;
import app.core.PrintSetup;

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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * La finestra della stampante: tutto quello che decide se un'etichetta esce
 * centrata o storta, in chiaro e salvato.
 *
 * <p>Non sa che stampante hai. Legge le code installate, chiede al driver che
 * pagina dichiara e te lo dice in millimetri: da lì in poi sono quattro
 * manopole — misura della pagina, verso, tiro, scala — che bastano per
 * qualunque etichettatrice, termica o no.
 *
 * <p>La riga di diagnosi in cima è la prima da leggere quando le etichette
 * escono male. Se il driver dichiara 210 × 297 mm su una stampante di
 * etichette, il formato in Windows è ancora un A4 e nessuna correzione di tiro
 * potrà rimediare: è il difetto che riempiva il rullo di etichette vuote.
 */
public final class PrinterDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final LabelModel model;
    private final StatusSink status;

    private final JComboBox<String> printer = new JComboBox<String>();
    private final JLabel diagnosis = new JLabel(" ");
    private final JComboBox<PrintSetup.PageMode> pageMode =
            AppTheme.combo(PrintSetup.PageMode.values());
    private final JSpinner pageWidth = AppTheme.spinnerDouble(50, 5, 1000, 0.5);
    private final JSpinner pageHeight = AppTheme.spinnerDouble(30, 5, 1000, 0.5);
    private final JComboBox<PrintSetup.Turn> turn = AppTheme.combo(PrintSetup.Turn.values());
    private final JSpinner offsetX = AppTheme.spinnerDouble(0, -50, 50, 0.1);
    private final JSpinner offsetY = AppTheme.spinnerDouble(0, -50, 50, 0.1);
    private final JSpinner scalePercent = AppTheme.spinnerDouble(100, 50, 200, 1);
    private final JComboBox<PrintSetup.Render> render = AppTheme.combo(PrintSetup.Render.values());
    private final JCheckBox showDialog = new JCheckBox("Chiedi la stampante a ogni stampa");

    private boolean updating;

    /** Dove finiscono i messaggi: la barra di stato della finestra principale. */
    public interface StatusSink {
        void say(String message, int severity);
    }

    public PrinterDialog(Window owner, LabelModel model, StatusSink status) {
        super(owner, "Stampante", Dialog.ModalityType.APPLICATION_MODAL);
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
        wire();
        refreshDiagnosis();
        pack();
        setSize(getWidth(), Math.min(UiScale.px(680), getHeight()));
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

        section(p, c, "Coda di stampa");
        AppTheme.styleCombo(printer);
        row(p, c, "Stampante", printer);
        JButton rescan = AppTheme.ghost("Rileggi le stampanti");
        rescan.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reloadPrinters();
                refreshDiagnosis();
            }
        });
        wide(p, c, flow(rescan));
        diagnosis.setFont(AppTheme.UI_SMALL);
        diagnosis.setForeground(AppTheme.SUBTEXT0);
        wide(p, c, diagnosis);
        wide(p, c, AppTheme.hint("Questa riga è la prima da leggere quando le etichette escono "
                + "male: se il driver dichiara <b>210 × 297 mm</b> su una stampante di "
                + "etichette, in Windows il formato è ancora un A4 e la stampante manderà "
                + "avanti il supporto per un foglio intero a ogni etichetta.", 440));
        showDialog.setFont(AppTheme.UI);
        showDialog.setForeground(AppTheme.TEXT);
        showDialog.setOpaque(false);
        wide(p, c, showDialog);
        wide(p, c, AppTheme.hint("Tolto il segno di spunta si stampa dritto sulla coda scelta "
                + "qui sopra, senza finestre di mezzo: è come lavora la produzione.", 440));

        section(p, c, "Pagina mandata al driver");
        row(p, c, "Misura", pageMode);
        row(p, c, "Larghezza (mm)", pageWidth);
        row(p, c, "Altezza (mm)", pageHeight);
        wide(p, c, AppTheme.hint("<b>Come l'etichetta</b> va bene quasi sempre. "
                + "<b>Quella della stampante</b> se il formato in Windows è già tarato e non "
                + "lo si vuole toccare. <b>Personalizzata</b> quando il passo del supporto è "
                + "diverso dall'area stampata, per esempio con etichette affiancate.", 440));

        section(p, c, "Verso e taratura");
        row(p, c, "Verso di stampa", turn);
        wide(p, c, AppTheme.hint("Se l'etichetta esce coricata, qui la si rimette dritta "
                + "<b>senza toccare il disegno</b>. Vale anche per il PDF esportato, che esce "
                + "già girato: nella finestra di stampa del browser non serve più scegliere "
                + "l'orientamento a mano.", 440));
        row(p, c, "Correzione X (mm)", offsetX);
        row(p, c, "Correzione Y (mm)", offsetY);
        wide(p, c, AppTheme.hint("Positivo sposta a destra e in basso. Stampa la pagina di "
                + "taratura, misura col righello di quanto il bordo è fuori posto e scrivi "
                + "qui la differenza col segno cambiato.", 440));
        row(p, c, "Scala (%)", scalePercent);
        wide(p, c, AppTheme.hint("Solo se la stampa esce dritta ma più grande o più piccola "
                + "del vero: misura un quadrato della griglia, se non è 5,0 mm scrivi qui il "
                + "rapporto. 100 vuol dire non toccare niente.", 440));

        section(p, c, "Che cosa riceve la stampante");
        row(p, c, "Modalità", render);
        wide(p, c, AppTheme.hint("<b>Immagine</b> rasterizza al DPI dell'etichetta e manda un "
                + "disegno 1:1: è il default perché nessun driver può reinterpretarlo. "
                + "<b>Vettoriale</b> è più nitido dove il driver lo regge bene.", 440));

        section(p, c, "Prove");
        JPanel tests = flow(null);
        JButton calibration = AppTheme.button("Stampa la pagina di taratura", AppTheme.MAUVE);
        calibration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                printCalibration();
            }
        });
        JButton sample = AppTheme.ghost("Stampa una sola etichetta");
        sample.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                printSample();
            }
        });
        tests.add(calibration);
        tests.add(sample);
        wide(p, c, tests);
        wide(p, c, AppTheme.hint("La pagina di taratura ha una griglia da 5 mm, il bordo "
                + "dell'etichetta e le squadrette agli angoli: se una squadretta non esce "
                + "tutta, il tiro è sbagliato da quel lato. Sopra ci stampa la taratura "
                + "attiva, così sai da dove sei partito.", 440));

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
                if (apply(true)) {
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

    /** La taratura salvata sul disco. */
    public static PrintSetup current() {
        return PrintSetup.fromStorage(
                SettingsManager.get().getString(SettingsManager.KEY_PRINT_SETUP, null));
    }

    private void load() {
        updating = true;
        PrintSetup setup = current();
        reloadPrinters();
        if (!setup.printerName().isEmpty()) {
            printer.setSelectedItem(setup.printerName());
        }
        pageMode.setSelectedItem(setup.pageMode());
        pageWidth.setValue(setup.pageWidthMm());
        pageHeight.setValue(setup.pageHeightMm());
        turn.setSelectedItem(setup.turn());
        offsetX.setValue(setup.offsetXMm());
        offsetY.setValue(setup.offsetYMm());
        scalePercent.setValue(setup.scalePercent());
        render.setSelectedItem(setup.render());
        showDialog.setSelected(setup.showDialog());
        updating = false;
        updateEnabled();
    }

    private void reloadPrinters() {
        Object was = printer.getSelectedItem();
        boolean before = updating;
        updating = true;
        printer.removeAllItems();
        printer.addItem(""); // vuoto = stampante di sistema
        for (String name : LabelPrinter.printerNames()) {
            printer.addItem(name);
        }
        if (was != null) {
            printer.setSelectedItem(was);
        }
        updating = before;
    }

    private void wire() {
        ActionListener redraw = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!updating) {
                    updateEnabled();
                    refreshDiagnosis();
                }
            }
        };
        printer.addActionListener(redraw);
        pageMode.addActionListener(redraw);
        turn.addActionListener(redraw);
        render.addActionListener(redraw);
        ChangeListener numbers = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // nessun salvataggio a ogni battuta: si salva col pulsante
            }
        };
        pageWidth.addChangeListener(numbers);
        pageHeight.addChangeListener(numbers);
    }

    private void updateEnabled() {
        boolean custom = pageMode.getSelectedItem() == PrintSetup.PageMode.PERSONALIZZATA;
        pageWidth.setEnabled(custom);
        pageHeight.setEnabled(custom);
    }

    private void refreshDiagnosis() {
        String name = selectedPrinter();
        String text = LabelPrinter.describe(name);
        diagnosis.setText(text);
        // verde se il driver dichiara una pagina vicina all'etichetta, arancio se no
        diagnosis.setForeground(looksLikeALabelPage(text) ? AppTheme.GREEN : AppTheme.PEACH);
    }

    /**
     * La pagina dichiarata assomiglia all'etichetta?
     *
     * <p>Il confronto è largo: basta che i due lati stiano dentro il doppio di
     * quelli dell'etichetta. Serve solo a distinguere "il driver sa che sto
     * stampando etichette" da "il driver crede di avere un A4", che è la
     * differenza fra una stampa buona e un rullo sprecato.
     */
    private boolean looksLikeALabelPage(String description) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([0-9]+[.,][0-9]) x ([0-9]+[.,][0-9]) mm").matcher(description);
        if (!m.find()) {
            return false;
        }
        try {
            double w = Double.parseDouble(m.group(1).replace(',', '.'));
            double h = Double.parseDouble(m.group(2).replace(',', '.'));
            double maxW = Math.max(model.widthMm(), model.heightMm()) * 2 + 20;
            return w <= maxW && h <= maxW;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    private String selectedPrinter() {
        Object sel = printer.getSelectedItem();
        return sel == null ? "" : sel.toString();
    }

    private PrintSetup fromFields() {
        PrintSetup setup = PrintSetup.defaults();
        setup.setPrinterName(selectedPrinter());
        setup.setPageMode((PrintSetup.PageMode) pageMode.getSelectedItem());
        setup.setPageSizeMm(num(pageWidth), num(pageHeight));
        setup.setTurn((PrintSetup.Turn) turn.getSelectedItem());
        setup.setOffsetMm(num(offsetX), num(offsetY));
        setup.setScalePercent(num(scalePercent));
        setup.setRender((PrintSetup.Render) render.getSelectedItem());
        setup.setShowDialog(showDialog.isSelected());
        return setup;
    }

    private boolean apply(boolean announce) {
        try {
            PrintSetup setup = fromFields();
            SettingsManager.get().setString(SettingsManager.KEY_PRINT_SETUP, setup.toStorage());
            boolean saved = SettingsManager.get().save();
            if (announce) {
                say(saved ? "Taratura salvata: " + setup
                          : "Taratura non salvata: impostazioni non scrivibili", saved ? 0 : 2);
            }
            return true;
        } catch (RuntimeException invalid) {
            JOptionPane.showMessageDialog(this, invalid.getMessage(),
                    "Taratura", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    private void printCalibration() {
        try {
            if (LabelPrinter.printCalibration(model, fromFields())) {
                say("Pagina di taratura mandata in stampa: misurala e correggi il tiro", 0);
            }
        } catch (Exception e) {
            error("Stampa della taratura non riuscita: " + e.getMessage());
        }
    }

    private void printSample() {
        try {
            java.util.List<java.util.Map<String, String>> one = model.run(1);
            if (new LabelPrinter(model, one, fromFields()).print("Prova Etichette Custom")) {
                say("Etichetta di prova mandata in stampa", 0);
            }
        } catch (Exception e) {
            error("Stampa di prova non riuscita: " + e.getMessage());
        }
    }

    private void say(String message, int severity) {
        if (status != null) {
            status.say(message, severity);
        }
    }

    private void error(String message) {
        say(message, 2);
        JOptionPane.showMessageDialog(this, message, "Stampa", JOptionPane.ERROR_MESSAGE);
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
