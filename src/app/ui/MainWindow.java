package app.ui;

import app.config.AppTheme;
import app.config.LogTarget;
import app.config.SettingsManager;
import app.config.UiScale;
import app.core.DayLog;
import app.core.LabelElement;
import app.core.LabelField;
import app.core.LabelModel;
import app.core.LabelPrinter;
import app.core.LayoutStore;
import app.core.PrintSetup;
import app.core.Templates;
import app.core.export.PdfExporter;
import app.core.export.PngExporter;
import app.core.export.SvgExporter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * La finestra: una schermata sola, senza schede.
 *
 * <p>Tre zone e nient'altro. In cima la <b>riga del giro</b>, cioè quello che
 * l'operatore tocca cento volte al giorno: codice, quantità, l'intervallo che
 * uscirà, la misura del supporto e Stampa. Al centro l'etichetta con i
 * righelli e la barra strumenti che le galleggia sopra. A destra, e solo
 * quando c'è qualcosa di selezionato, le proprietà di quell'elemento.
 *
 * <p>Stampante e impostazioni sono due finestrelle dietro altrettante icone:
 * si aprono una volta al mese e non meritano di rubare spazio ogni giorno.
 *
 * <p>La regola che tiene insieme il disegno è una: <b>ogni cosa si fa da un
 * posto solo</b>. Se un comando esiste nella barra strumenti, non esiste anche
 * come pulsante nel pannello; se una misura si scrive nella riga del giro, non
 * si riscrive altrove. La versione precedente aveva tre modi di ruotare un
 * elemento e due di ruotare l'etichetta, uno dei quali non funzionava
 * nemmeno — ed è così che ci si accorge che erano troppi.
 */
public final class MainWindow extends JFrame implements ToolRail.Listener,
        PreviewPanel.Listener, InspectorPanel.Listener, PrinterDialog.StatusSink {

    private static final long serialVersionUID = 1L;

    private final transient LabelModel model;
    private final transient DayLog log;

    private final PreviewPanel preview = new PreviewPanel();
    private final ToolRail tools = new ToolRail(this);
    private final InspectorPanel inspector = new InspectorPanel(this);

    private final JTextField codeField = AppTheme.field(16);
    private final JSpinner quantity = AppTheme.spinnerInt(1, 1, 100000);
    private final JLabel rangeLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel warningLabel = new JLabel(" ");

    private boolean updating;

    public MainWindow() {
        super("Etichette Custom");
        SettingsManager settings = SettingsManager.get();
        this.model = LabelModel.fromStorage(settings.getString(SettingsManager.KEY_LAYOUT, null));
        this.log = buildLog(settings);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.BASE);
        root.add(buildRunBar(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);

        preview.setListener(this);
        bindKeys(root);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveLayout();
                SettingsManager.get().save();
                log.append("Chiusura applicazione");
                dispose();
            }
        });

        loadFromModel();
        wire();
        refresh();

        // pack() prima e minimo dopo: la finestra nasce grande quanto le serve
        // davvero, invece di comprimere il contenuto dentro una misura decisa a
        // mano — che a schermo ingrandito tagliava i campi
        pack();
        Dimension packed = getSize();
        // la riga del giro, se nessuno le dice quanto è larga la finestra, si
        // dispone su una riga sola e chiede più spazio dello schermo. Qui la si
        // limita allo schermo vero: da lì in poi va a capo da sola
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = (int) (screen.width * 0.92);
        int maxH = (int) (screen.height * 0.90);
        setMinimumSize(new Dimension(
                Math.min(maxW, Math.max(UiScale.px(820), Math.min(packed.width, UiScale.px(1180)))),
                Math.min(maxH, Math.max(UiScale.px(560), packed.height))));
        setSize(getMinimumSize());
        setLocationRelativeTo(null);

        log.append("Avvio applicazione");
        if (log.isDegraded()) {
            setStatus("Registro non scrivibile nella cartella scelta: sto usando "
                    + log.currentFile().getParent(), 1);
        } else {
            restStatus();
        }
    }

    private static DayLog buildLog(SettingsManager settings) {
        String pattern = settings.getString(SettingsManager.KEY_LOG_PATTERN,
                LogTarget.DEFAULT_PATTERN);
        File chosen = settings.logDirectory();
        LogTarget primary = new LogTarget(
                chosen != null ? chosen : SettingsManager.fallbackLogDirectory(), pattern);
        return new DayLog(primary, new LogTarget(SettingsManager.fallbackLogDirectory(), pattern));
    }

    // --- riga del giro --------------------------------------------------------

    /**
     * La riga del giro: codice, quantità, che cosa esce, e stampa.
     *
     * <p>Qui c'è solo quello che l'operatore tocca <b>ogni giorno</b>. Misura
     * dell'etichetta, campi e layout stanno nel pannello di destra, che è dove
     * si disegna; la stampante e le impostazioni dietro le due icone in fondo.
     * Prima erano tutti qui insieme, e la riga era così lunga che su un monitor
     * di reparto non ci stava.
     */
    private JPanel buildRunBar() {
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, UiScale.px(16), UiScale.px(8)));
        bar.setBackground(AppTheme.MANTLE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.SURFACE0),
                BorderFactory.createEmptyBorder(
                        UiScale.px(9), UiScale.px(14), UiScale.px(9), UiScale.px(14))));

        bar.add(group("Codice", codeField));
        bar.add(group("Quantità", quantity));

        rangeLabel.setFont(AppTheme.MONO_BOLD);
        rangeLabel.setForeground(AppTheme.MAUVE);
        rangeLabel.setBorder(new AppTheme.RoundBorder(AppTheme.SURFACE0, UiScale.px(8),
                new Insets(UiScale.px(5), UiScale.px(10), UiScale.px(5), UiScale.px(10))));
        bar.add(group("Esce", rangeLabel));

        JPanel actions = flow();
        JButton print = AppTheme.button("Stampa", AppTheme.GREEN);
        print.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doPrint();
            }
        });
        actions.add(print);
        actions.add(exportButton("PDF", "pdf"));
        actions.add(exportButton("PNG", "png"));
        actions.add(exportButton("SVG", "svg"));
        actions.add(iconButton(AppTheme.glyph("\u2399", "Stampante"),
                "Stampante e taratura", new Runnable() {
                    public void run() {
                        openPrinter();
                    }
                }));
        actions.add(iconButton(AppTheme.glyph("\u2699", "Opzioni"),
                "Impostazioni e manuali", new Runnable() {
                    public void run() {
                        openSettings();
                    }
                }));
        bar.add(group(" ", actions));
        return bar;
    }

    private JPanel group(String caption, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, UiScale.px(4)));
        p.setOpaque(false);
        JLabel l = new JLabel(caption.toUpperCase());
        l.setFont(AppTheme.UI_SMALL.deriveFont(java.awt.Font.BOLD));
        l.setForeground(AppTheme.OVERLAY0);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static JPanel flow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, UiScale.px(5), 0));
        p.setOpaque(false);
        return p;
    }

    private static JLabel unit(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.UI_SMALL);
        l.setForeground(AppTheme.OVERLAY0);
        return l;
    }

    private JButton exportButton(String text, final String kind) {
        JButton b = AppTheme.ghost(text);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doExport(kind);
            }
        });
        return b;
    }

    private JButton iconButton(String glyph, String tip, final Runnable action) {
        JButton b = AppTheme.tool(glyph, tip);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return b;
    }

    // --- corpo ----------------------------------------------------------------

    /**
     * L'anteprima con la barra strumenti sopra.
     *
     * <p>La barra sta in un {@link JLayeredPane} invece che in una colonna
     * accanto: così resta sempre alla stessa distanza dall'etichetta,
     * qualunque sia la larghezza della finestra.
     */
    private JComponent buildBody() {
        final JLayeredPane layers = new JLayeredPane();
        layers.setLayout(null);
        layers.setBackground(AppTheme.MANTLE);
        layers.add(preview, JLayeredPane.DEFAULT_LAYER);
        layers.add(tools, JLayeredPane.PALETTE_LAYER);
        layers.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                preview.setBounds(0, 0, layers.getWidth(), layers.getHeight());
                Dimension t = tools.getPreferredSize();
                tools.setBounds(UiScale.px(14), UiScale.px(14), t.width, t.height);
            }
        });

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(AppTheme.MANTLE);
        center.add(layers, BorderLayout.CENTER);
        center.setPreferredSize(new Dimension(UiScale.px(620), UiScale.px(420)));

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(AppTheme.BASE);
        body.add(center, BorderLayout.CENTER);
        body.add(inspector, BorderLayout.EAST);
        return body;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(UiScale.px(16), 0));
        bar.setBackground(AppTheme.MANTLE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.SURFACE0),
                BorderFactory.createEmptyBorder(
                        UiScale.px(6), UiScale.px(14), UiScale.px(6), UiScale.px(14))));
        warningLabel.setFont(AppTheme.UI_SMALL);
        warningLabel.setForeground(AppTheme.GREEN);
        statusLabel.setFont(AppTheme.UI_SMALL);
        statusLabel.setForeground(AppTheme.SUBTEXT0);
        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        bar.add(warningLabel, BorderLayout.WEST);
        bar.add(statusLabel, BorderLayout.EAST);
        return bar;
    }

    /** Le scorciatoie: le stesse cose della barra, per chi ha le mani sulla tastiera. */
    private void bindKeys(JComponent root) {
        // R ruota l'elemento selezionato: la scorciatoia dello stesso comando
        // che sta nel pannello, non un secondo posto da cui farlo
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke("R"), "ruota");
        root.getActionMap().put("ruota", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                LabelElement sel = preview.selected();
                if (sel != null) {
                    sel.rotateQuarterTurn();
                    afterEdit();
                }
            }
        });
        bind(root, "DELETE", ToolRail.Action.ELIMINA);
        bind(root, "control D", ToolRail.Action.DUPLICA);
        bind(root, "control G", ToolRail.Action.GRIGLIA);
    }

    private void bind(JComponent root, String stroke, final ToolRail.Action action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(stroke), action.name());
        root.getActionMap().put(action.name(), new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                toolPressed(action);
            }
        });
    }

    // --- barra strumenti ------------------------------------------------------

    public void toolPressed(ToolRail.Action action) {
        LabelElement selected = preview.selected();
        switch (action) {
            case TESTO:
                addElement(LabelElement.text("Testo " + (model.elements().size() + 1),
                        "TESTO", 5, 10, 4));
                break;
            case QR:
                addElement(LabelElement.qr("QR " + (model.elements().size() + 1),
                        defaultToken(), 5, 5, 15));
                break;
            case DUPLICA:
                if (selected != null) {
                    LabelElement copy = selected.copy();
                    copy.setName(selected.name() + " (copia)");
                    copy.setPosition(selected.xMm() + 2, selected.yMm() + 2);
                    addElement(copy);
                }
                break;
            case ELIMINA:
                if (selected != null) {
                    model.remove(selected);
                    model.setTemplateName("Personalizzato");
                    preview.setSelected(null);
                    afterEdit();
                }
                break;
            case GRIGLIA:
                preview.setShowGrid(!preview.showGrid());
                tools.setActive(ToolRail.Action.GRIGLIA, preview.showGrid());
                break;
            default:
                break;
        }
    }

    private String defaultToken() {
        LabelField main = model.mainField();
        return main != null ? main.token() : LabelElement.CODE_TOKEN;
    }

    private void addElement(LabelElement element) {
        model.add(element);
        model.setTemplateName("Personalizzato");
        preview.setModel(model);
        preview.setSelected(element);
        elementSelected(element);
        afterEdit();
    }

    // --- anteprima ------------------------------------------------------------

    public void elementChanged(LabelElement element) {
        inspector.refresh();
        refreshWarnings();
        saveLayout();
        preview.repaint();
    }

    public void elementSelected(LabelElement element) {
        inspector.show(model, element);
        tools.setHasSelection(element != null);
        revalidate();
        repaint();
    }

    // --- pannello proprietà ---------------------------------------------------

    public void elementEdited() {
        afterEdit();
    }

    /** Il pannello ha cambiato una proprietà dell'etichetta, non di un elemento. */
    public void labelEdited() {
        preview.setModel(model);
        preview.repaint();
        refreshWarnings();
        saveLayout();
    }

    public void fieldsRequested() {
        openFields();
    }

    public void saveLayoutRequested() {
        String suggested = model.templateName().equals(Templates.NUOVA)
                ? "" : model.templateName();
        String name = JOptionPane.showInputDialog(this,
                "Nome del layout:\n(per esempio il codice del prodotto)", suggested);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        if (LayoutStore.exists(name) && JOptionPane.showConfirmDialog(this,
                "Esiste già un layout \"" + name.trim() + "\". Sovrascriverlo?",
                "Salva layout", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            LayoutStore.save(name, model);
            model.setTemplateName(name.trim());
            inspector.refresh();
            saveLayout();
            setStatus("Layout salvato: " + name.trim(), 0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Non riesco a salvare: " + e.getMessage(),
                    "Salva layout", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openLayoutRequested() {
        java.util.List<String> names = LayoutStore.names();
        if (names.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Non hai ancora salvato nessun layout.\n\nDisegna l'etichetta come ti "
                    + "serve, poi premi Salva: da lì in poi la ritrovi qui.",
                    "Apri layout", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object choice = JOptionPane.showInputDialog(this, "Quale layout vuoi aprire?",
                "Apri layout", JOptionPane.QUESTION_MESSAGE, null,
                names.toArray(), names.get(0));
        if (choice == null) {
            return;
        }
        try {
            model.copyFrom(LayoutStore.load(choice.toString()));
            model.setTemplateName(choice.toString());
            preview.setSelected(null);
            loadFromModel();
            elementSelected(null);
            refresh();
            setStatus("Layout aperto: " + choice, 0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Non riesco ad aprire: " + e.getMessage(),
                    "Apri layout", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void afterEdit() {
        preview.setModel(model);
        preview.repaint();
        inspector.refresh();
        refreshWarnings();
        saveLayout();
    }

    // --- finestrelle ----------------------------------------------------------

    private void openPrinter() {
        new PrinterDialog(this, model, this).setVisible(true);
        refreshWarnings();
    }

    private void openSettings() {
        new SettingsDialog(this, model, this).setVisible(true);
        loadFromModel();
        refresh();
    }

    private void openFields() {
        FieldsDialog dialog = new FieldsDialog(this, model);
        dialog.setVisible(true);
        if (dialog.changed()) {
            loadFromModel();
            refresh();
            saveLayout();
        }
    }

    // --- travaso --------------------------------------------------------------

    private void loadFromModel() {
        updating = true;
        LabelField main = model.mainField();
        codeField.setText(main == null ? "" : main.value());
        codeField.setEnabled(main != null);
        preview.setModel(model);
        inspector.show(model, preview.selected());
        tools.setHasSelection(preview.selected() != null);
        updating = false;
    }

    private void wire() {
        DocumentListener code = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyCode();
            }

            public void removeUpdate(DocumentEvent e) {
                applyCode();
            }

            public void changedUpdate(DocumentEvent e) {
                applyCode();
            }
        };
        codeField.getDocument().addDocumentListener(code);

        quantity.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refresh();
            }
        });

    }

    private void applyCode() {
        if (updating) {
            return;
        }
        LabelField main = model.mainField();
        if (main != null) {
            main.setValue(codeField.getText().trim());
        }
        refresh();
    }

    private void refresh() {
        if (updating) {
            return;
        }
        preview.setModel(model);
        preview.setValues(model.valuesAt(0));
        preview.repaint();
        refreshWarnings();
        saveLayout();
    }

    private void refreshWarnings() {
        int qty = (Integer) quantity.getValue();
        StringBuilder problems = new StringBuilder();

        try {
            model.checkRun(qty);
            List<Map<String, String>> run = model.run(qty);
            LabelField main = model.mainField();
            if (main == null) {
                rangeLabel.setText("\u2014");
            } else {
                String first = run.get(0).get(main.name());
                String last = run.get(run.size() - 1).get(main.name());
                rangeLabel.setText(qty == 1 ? first : first + "  \u2192  " + last);
            }
        } catch (RuntimeException e) {
            rangeLabel.setText("\u2014");
            problems.append(e.getMessage());
        }

        for (String w : model.warnings(model.valuesAt(0).get(
                model.mainField() == null ? "" : model.mainField().name()))) {
            if (problems.length() > 0) {
                problems.append("   ");
            }
            problems.append(w);
        }

        if (problems.length() == 0) {
            warningLabel.setForeground(AppTheme.GREEN);
            warningLabel.setText(AppTheme.glyph("\u2713", "OK") + " nessun avviso");
            warningLabel.setToolTipText(null);
        } else {
            warningLabel.setForeground(AppTheme.PEACH);
            String text = problems.toString();
            warningLabel.setText(text.length() > 110 ? text.substring(0, 108) + "\u2026" : text);
            warningLabel.setToolTipText("<html><body style='width:420px'>"
                    + text.replace("   ", "<br>") + "</body></html>");
        }
    }

    private void saveLayout() {
        SettingsManager.get().setString(SettingsManager.KEY_LAYOUT, model.toStorage());
    }

    private static double num(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    // --- azioni ---------------------------------------------------------------

    /** Il giro da stampare, chiedendo prima i campi che vanno chiesti. */
    private List<Map<String, String>> currentRun() {
        int qty = (Integer) quantity.getValue();
        for (LabelField f : model.fields()) {
            if (f.type() == LabelField.Type.CHIESTO) {
                String answer = JOptionPane.showInputDialog(this,
                        "Valore per {" + f.name() + "}:", f.value());
                if (answer == null) {
                    return null;
                }
                f.setValue(answer);
            }
        }
        try {
            List<Map<String, String>> run = model.run(qty);
            refreshWarnings();
            return run;
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Non si può procedere", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void doPrint() {
        List<Map<String, String>> run = currentRun();
        if (run == null) {
            return;
        }
        PrintSetup setup = PrinterDialog.current();
        try {
            String first = firstValue(run.get(0));
            if (new LabelPrinter(model, run, setup).print("Etichette " + first)) {
                log.logRun(first, firstValue(run.get(run.size() - 1)), run.size(), "STAMPA");
                setStatus(run.size() + " etichette mandate in stampa su "
                        + (setup.printerName().isEmpty()
                                ? "stampante di sistema" : setup.printerName()), 0);
            }
        } catch (Exception e) {
            log.append("ERRORE stampa: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "La stampa non è riuscita: " + e.getMessage(),
                    "Stampa", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String firstValue(Map<String, String> values) {
        LabelField main = model.mainField();
        String v = main == null ? null : values.get(main.name());
        return v == null || v.isEmpty() ? "etichetta" : v;
    }

    private void doExport(String kind) {
        List<Map<String, String>> run = currentRun();
        if (run == null) {
            return;
        }
        SettingsManager settings = SettingsManager.get();
        JFileChooser chooser = new JFileChooser(
                settings.getString(SettingsManager.KEY_LAST_EXPORT_DIR,
                        System.getProperty("user.home")));
        chooser.setFileFilter(new FileNameExtensionFilter(kind.toUpperCase(), kind));
        String suggested = firstValue(run.get(0)).replaceAll("[^A-Za-z0-9._-]", "_");
        chooser.setSelectedFile(new File(suggested + "." + kind));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith("." + kind)) {
            target = new File(target.getParentFile(), target.getName() + "." + kind);
        }
        settings.setString(SettingsManager.KEY_LAST_EXPORT_DIR, target.getParent());
        settings.save();

        try {
            if ("pdf".equals(kind)) {
                PdfExporter.write(model, run, target, PrinterDialog.current().turn());
            } else {
                writeSeries(run, target, kind);
            }
            log.logRun(firstValue(run.get(0)), firstValue(run.get(run.size() - 1)),
                    run.size(), kind.toUpperCase());
            setStatus(run.size() + " etichette in " + target.getName(), 0);
        } catch (Exception e) {
            log.append("ERRORE export " + kind + ": " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Esportazione non riuscita: " + e.getMessage(),
                    "Esportazione", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** PNG e SVG non hanno pagine: un file per etichetta, col codice nel nome. */
    private void writeSeries(List<Map<String, String>> run, File target, String kind)
            throws Exception {
        String base = target.getName().substring(0, target.getName().length() - 4);
        File dir = target.getParentFile();
        for (int i = 0; i < run.size(); i++) {
            Map<String, String> values = run.get(i);
            File f = run.size() == 1 ? target
                    : new File(dir, base + "-" + firstValue(values)
                            .replaceAll("[^A-Za-z0-9._-]", "_") + "." + kind);
            if ("png".equals(kind)) {
                PngExporter.write(model, values, f);
            } else {
                SvgExporter.write(model, values, f);
            }
        }
    }

    // --- barra di stato -------------------------------------------------------

    public void say(String message, int severity) {
        setStatus(message, severity);
    }

    /** Scrive nella barra di stato: 0 va bene, 1 attenzione, 2 errore. */
    public void setStatus(String message, int severity) {
        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
        statusLabel.setForeground(severity == 0 ? AppTheme.SUBTEXT0 : AppTheme.forStatus(severity));
        if (severity == 0) {
            Timer clear = new Timer(6000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    restStatus();
                }
            });
            clear.setRepeats(false);
            clear.start();
        }
    }

    /** Il messaggio a riposo: dove sta scrivendo il registro di oggi. */
    private void restStatus() {
        statusLabel.setForeground(AppTheme.SUBTEXT0);
        statusLabel.setText("registro di oggi \u00B7 " + log.currentFile().getName());
        statusLabel.setToolTipText(log.currentFile().getAbsolutePath());
    }

    /** Comodo per chi non ha il riferimento alla finestra. */
    public static void status(Component child, String message, int severity) {
        MainWindow w = (MainWindow) SwingUtilities.getAncestorOfClass(MainWindow.class, child);
        if (w != null) {
            w.setStatus(message, severity);
        }
    }
}
