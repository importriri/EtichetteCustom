package app.ui.banco;

import app.archivio.Archivio;
import app.archivio.Registro;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Impostazioni;
import app.modello.Storia;
import app.modello.Tipo;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.Voce;
import app.ui.finestre.Finestre;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Direct-manipulation layout editor with a deliberately small default surface. */
public class Banco extends JPanel {
    private final Etichetta label;
    private final SorgenteQr qr;
    private final Impostazioni settings;
    private final Archivio archive;
    private final Runnable preparePrint;
    private final Storia history = new Storia();

    private final Foglio canvas;
    private final Proprieta inspector;
    private final JPanel elementList = new JPanel();
    private final JLabel selectionState = new JLabel();
    private final JLabel sizeLabel = new JLabel();
    private final JLabel saveState = new JLabel();
    private final JLabel zoomValue = new JLabel();

    public Banco(Etichetta label, SorgenteQr qr, Impostazioni settings,
                 Archivio archive, Registro log, final Runnable back) {
        this(label, qr, settings, archive, log, back, back);
    }

    public Banco(Etichetta label, SorgenteQr qr, Impostazioni settings,
                 Archivio archive, Registro log, final Runnable back,
                 Runnable preparePrint) {
        super(new BorderLayout());
        this.label = label;
        this.qr = qr;
        this.settings = settings;
        this.archive = archive;
        this.preparePrint = preparePrint == null ? back : preparePrint;
        setBackground(Stile.BASE);

        canvas = new Foglio(label, qr);
        inspector = new Proprieta(new Runnable() {
            @Override public void run() { history.segna(Banco.this.label); }
        }, new Runnable() {
            @Override public void run() {
                canvas.repaint();
                refreshElements();
                refreshStatus();
            }
        });

        add(header(back), BorderLayout.NORTH);
        add(toolColumn(), BorderLayout.WEST);

        final JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBorder(null);
        canvasScroll.getViewport().setBackground(Stile.BANCO);
        canvasScroll.getVerticalScrollBar().setUnitIncrement(Stile.px(20));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Stile.BANCO);
        center.add(canvasScroll, BorderLayout.CENTER);
        center.add(zoomBar(canvasScroll), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JScrollPane inspectorScroll = new JScrollPane(inspector,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inspectorScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Stile.S0));
        inspectorScroll.setPreferredSize(new Dimension(Stile.px(348), 10));
        inspectorScroll.getVerticalScrollBar().setUnitIncrement(Stile.px(20));
        add(inspectorScroll, BorderLayout.EAST);

        add(statusBar(), BorderLayout.SOUTH);

        canvas.ascolto(new Foglio.Ascolto() {
            @Override public void selezionato(Elemento element) {
                inspector.mostra(Banco.this.label, element);
                refreshElements();
                refreshStatus();
            }

            @Override public void staPerCambiare() { history.segna(Banco.this.label); }
            @Override public void modificato() { refreshStatus(); }
        });

        shortcuts();
        if (!label.elementi().isEmpty()) {
            canvas.selezione(label.elementi().get(0));
        } else {
            inspector.mostra(label, null);
        }
        refreshElements();
        refreshStatus();
        refreshSize();
        refreshZoom();

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                canvas.adattaEImpagina(canvasScroll.getViewport().getExtentSize());
                refreshZoom();
                canvas.requestFocusInWindow();
            }
        });
    }

    public void salva() {
        try {
            archive.salva(label);
            saveState.setText("salvato");
        } catch (Exception ex) {
            Finestre.guaio(this, "Salvataggio",
                    "Non sono riuscito a salvare l'etichetta: " + ex.getMessage());
        }
    }

    private JComponent header(final Runnable back) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorWhite.VALUE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Stile.S0),
                BorderFactory.createEmptyBorder(
                        Stile.px(10), Stile.px(14), Stile.px(10), Stile.px(14))));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(10), 0));
        left.setOpaque(false);
        Bottone backButton = Bottone.piatto("‹  Vetrina");
        backButton.addActionListener(e -> {
            salva();
            back.run();
        });
        left.add(backButton);

        JPanel name = new JPanel();
        name.setOpaque(false);
        name.setLayout(new BoxLayout(name, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(label.nome());
        title.setFont(Stile.forte());
        title.setForeground(Stile.TESTO);
        sizeLabel.setFont(Stile.piccolo());
        sizeLabel.setForeground(Stile.OV1);
        name.add(title);
        name.add(sizeLabel);
        left.add(name);

        Bottone swap = Bottone.piatto("⇄");
        swap.setToolTipText("Scambia larghezza e altezza");
        swap.addActionListener(e -> {
            history.segna(label);
            canvas.ruotaEtichetta();
            refreshSize();
            inspector.mostra(label, canvas.selezione());
        });
        left.add(swap);
        panel.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Stile.px(7), 0));
        right.setOpaque(false);
        Bottone undo = Bottone.piatto("↶");
        undo.setToolTipText("Annulla  Ctrl+Z");
        undo.addActionListener(e -> undo());
        Bottone redo = Bottone.piatto("↷");
        redo.setToolTipText("Ripeti  Ctrl+Y");
        redo.addActionListener(e -> redo());
        Bottone settingsButton = Bottone.piatto("⚙");
        settingsButton.setToolTipText("Impostazioni");
        settingsButton.addActionListener(e -> {
            Finestre.impostazioni(this, settings);
            canvas.repaint();
        });
        Bottone print = Bottone.primario("Anteprima e stampa");
        print.addActionListener(e -> {
            salva();
            preparePrint.run();
        });
        right.add(undo);
        right.add(redo);
        right.add(settingsButton);
        right.add(print);
        panel.add(right, BorderLayout.EAST);

        refreshSize();
        return panel;
    }

    private void shortcuts() {
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask()), "undo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { undo(); }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMask()), "redo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { redo(); }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                shortcutMask() | java.awt.event.InputEvent.SHIFT_MASK), "redo-shift", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { redo(); }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_D, shortcutMask()), "duplicate", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { duplicate(); }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { deleteSelected(); }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask()), "save", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { salva(); }
        });
    }

    private static int shortcutMask() {
        try {
            return java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        } catch (Throwable headless) {
            return java.awt.event.InputEvent.CTRL_MASK;
        }
    }

    private void bind(KeyStroke key, String name, AbstractAction action) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, name);
        getActionMap().put(name, action);
    }

    private void undo() {
        if (history.annulla(label)) {
            afterHistoryChange();
        } else {
            saveState.setText("niente da annullare");
        }
    }

    private void redo() {
        if (history.ripeti(label)) {
            afterHistoryChange();
        } else {
            saveState.setText("niente da ripetere");
        }
    }

    private void afterHistoryChange() {
        canvas.selezione(label.elementi().isEmpty() ? null : label.elementi().get(0));
        canvas.revalidate();
        canvas.repaint();
        refreshElements();
        refreshSize();
        refreshStatus();
        inspector.mostra(label, canvas.selezione());
    }

    private JComponent zoomBar(final JScrollPane scroll) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(4), Stile.px(6)));
        panel.setBackground(Stile.BANCO);

        Bottone minus = Bottone.normale("−");
        minus.addActionListener(e -> {
            canvas.zoomPasso(-1);
            refreshZoom();
        });
        Bottone plus = Bottone.normale("+");
        plus.addActionListener(e -> {
            canvas.zoomPasso(1);
            refreshZoom();
        });
        Bottone fit = Bottone.piatto("Adatta");
        fit.addActionListener(e -> {
            canvas.adattaEImpagina(scroll.getViewport().getExtentSize());
            refreshZoom();
        });

        zoomValue.setFont(Stile.mono(11));
        zoomValue.setForeground(Stile.SUB0);
        zoomValue.setHorizontalAlignment(JLabel.CENTER);
        zoomValue.setPreferredSize(new Dimension(Stile.px(56), Stile.px(22)));
        panel.add(minus);
        panel.add(zoomValue);
        panel.add(plus);
        panel.add(fit);
        return panel;
    }

    private void refreshZoom() {
        zoomValue.setText(canvas.percentuale() + " %");
    }

    private JComponent toolColumn() {
        JPanel panel = new JPanel();
        panel.setBackground(Stile.BASE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Stile.S0),
                BorderFactory.createEmptyBorder(
                        Stile.px(12), Stile.px(9), Stile.px(12), Stile.px(9))));
        panel.setPreferredSize(new Dimension(Stile.px(168), 10));

        panel.add(group("Aggiungi"));
        panel.add(new Voce("T", "Testo").azione(() -> addElement(Tipo.TESTO)));
        panel.add(new Voce("▦", "QR").azione(() -> addElement(Tipo.QR)));
        panel.add(new Voce("|||", "Barcode").azione(() -> addElement(Tipo.BARCODE)));
        panel.add(new Voce("─", "Linea").azione(() -> addElement(Tipo.LINEA)));

        panel.add(group("Elementi"));
        elementList.setOpaque(false);
        elementList.setLayout(new BoxLayout(elementList, BoxLayout.Y_AXIS));
        elementList.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(elementList);

        panel.add(group("Azioni"));
        panel.add(new Voce("⧉", "Duplica").coda("Ctrl+D").azione(this::duplicate));
        panel.add(new Voce("⌦", "Elimina").coda("Canc").azione(this::deleteSelected));

        panel.add(Box.createVerticalGlue());
        panel.add(group("Altro"));
        panel.add(new Voce("≡", "Dati avanzati…").azione(() -> {
            history.segna(label);
            Finestre.campi(this, label);
            refreshElements();
            canvas.repaint();
            inspector.mostra(label, canvas.selezione());
        }));
        return panel;
    }

    private JComponent group(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(Stile.minuscolo());
        label.setForeground(Stile.OV0);
        label.setBorder(BorderFactory.createEmptyBorder(
                Stile.px(14), Stile.px(6), Stile.px(6), 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void addElement(Tipo type) {
        history.segna(label);
        String elementName = uniqueName(type.etichetta());
        String fieldName = null;

        if (type != Tipo.LINEA) {
            Elemento selected = canvas.selezione();
            Campo selectedField = selected == null ? null : label.campo(selected.campo());
            if (selectedField != null) {
                fieldName = selectedField.nome();
            } else {
                String base = type == Tipo.TESTO ? "testo" : "codice";
                String newFieldName = label.nomeCampoUnico(base);
                Comportamento behavior = type == Tipo.TESTO
                        ? Comportamento.FISSO : Comportamento.CHIESTO;
                Campo newField = new Campo(newFieldName, behavior, "");
                label.aggiungi(newField);
                fieldName = newField.nome();
            }
        }

        Elemento element = new Elemento(elementName, type, fieldName, 2, 2,
                type == Tipo.QR
                        ? Math.min(12, Math.max(1, label.larghezza() - 4))
                        : Math.min(30, Math.max(1, label.larghezza() - 4)));
        if (type == Tipo.BARCODE || type == Tipo.LINEA) {
            element.altezza(type == Tipo.LINEA ? 0.4 : 8);
        }
        label.aggiungi(element);
        canvas.rientra(element);
        canvas.selezione(element);
        canvas.repaint();
        refreshElements();
    }

    private void duplicate() {
        Elemento selected = canvas.selezione();
        if (selected == null) return;
        history.segna(label);
        Elemento copy = selected.copia();
        copy.nome(uniqueName(selected.nome()));
        copy.x(selected.x() + 2);
        copy.y(selected.y() + 2);
        label.aggiungi(copy);
        canvas.rientra(copy);
        canvas.selezione(copy);
        canvas.repaint();
    }

    private String uniqueName(String base) {
        String root = base == null || base.trim().isEmpty() ? "Elemento" : base.trim();
        if (!nameUsed(root)) return root;
        int n = 2;
        while (nameUsed(root + " " + n)) n++;
        return root + " " + n;
    }

    private boolean nameUsed(String name) {
        for (Elemento element : label.elementi()) {
            if (name.equals(element.nome())) return true;
        }
        return false;
    }

    private void deleteSelected() {
        Elemento selected = canvas.selezione();
        if (selected == null) return;
        history.segna(label);
        label.rimuovi(selected);
        canvas.selezione(label.elementi().isEmpty() ? null : label.elementi().get(0));
        canvas.repaint();
    }

    private void refreshElements() {
        elementList.removeAll();
        for (final Elemento element : label.elementi()) {
            Voce row = new Voce(element.tipo().glifo(), element.nome());
            Campo field = label.campo(element.campo());
            if (field != null && field.comportamento() == Comportamento.PROGRESSIVO) {
                row.coda("+1");
            } else if (field != null && field.comportamento() == Comportamento.CHIESTO) {
                row.coda("?");
            }
            row.attiva(element == canvas.selezione());
            row.azione(() -> canvas.selezione(element));
            elementList.add(row);
        }
        elementList.revalidate();
        elementList.repaint();
    }

    private void refreshSize() {
        sizeLabel.setText(num(label.larghezza()) + " × " + num(label.altezza()) + " mm");
    }

    private void refreshStatus() {
        Elemento selected = canvas.selezione();
        if (selected == null) {
            selectionState.setText("Seleziona un elemento per modificarlo");
        } else {
            selectionState.setText(selected.nome() + " selezionato  ·  trascina per spostare");
        }
    }

    private JComponent statusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Stile.MANTLE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Stile.S0),
                BorderFactory.createEmptyBorder(
                        Stile.px(4), Stile.px(12), Stile.px(4), Stile.px(12))));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(10), 0));
        left.setOpaque(false);
        left.add(new ReadyDot());
        selectionState.setFont(Stile.piccolo());
        selectionState.setForeground(Stile.OV1);
        left.add(selectionState);
        panel.add(left, BorderLayout.WEST);

        saveState.setFont(Stile.piccolo());
        saveState.setForeground(Stile.OV0);
        panel.add(saveState, BorderLayout.EAST);
        return panel;
    }

    private static String num(double value) {
        String text = String.valueOf(Math.round(value * 10) / 10.0);
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        return text.replace('.', ',');
    }

    private static final class ColorWhite {
        private static final java.awt.Color VALUE = java.awt.Color.WHITE;
        private ColorWhite() { }
    }

    private static class ReadyDot extends JComponent {
        @Override public Dimension getPreferredSize() {
            return new Dimension(Stile.px(8), Stile.px(8));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Stile.liscio(g);
            try {
                g2.setColor(Stile.VERDE);
                g2.fillOval(0, getHeight() / 2 - Stile.px(4), Stile.px(8), Stile.px(8));
            } finally {
                g2.dispose();
            }
        }
    }
}
