package app.ui.finestre;

import app.esporta.Esportazione;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Etichetta;
import app.modello.Impostazioni;
import app.modello.Serie;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.CodiceView;
import app.ui.dati.NomiDati;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** Finestre secondarie per dati, impostazioni, export e conferme. */
public final class Finestre {
    private static final String GITHUB = "https://github.com/importriri/etichette-custom";

    private Finestre() { }

    public static void avviso(Component sopra, String titolo, String testo) {
        JOptionPane.showMessageDialog(sopra, testo, titolo, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void guaio(Component sopra, String titolo, String testo) {
        JOptionPane.showMessageDialog(sopra, testo, titolo, JOptionPane.WARNING_MESSAGE);
    }

    public static boolean conferma(Component sopra, String titolo, String domanda) {
        return JOptionPane.showConfirmDialog(sopra, domanda, titolo,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /* Le modifiche restano locali finché l’operatore non conferma. */
    public static void campi(Component sopra, Etichetta eti) {
        final List<Campo> originali = new ArrayList<Campo>(eti.campi());
        final List<JComboBox<Comportamento>> comportamenti = new ArrayList<JComboBox<Comportamento>>();
        final List<JTextField> valori = new ArrayList<JTextField>();
        final List<JSpinner> cifre = new ArrayList<JSpinner>();

        JPanel elenco = pannello();
        for (Campo c : originali) {
            JPanel card = card();
            JLabel titolo = new JLabel(NomiDati.nome(eti, c));
            titolo.setFont(Stile.forte());
            titolo.setForeground(Stile.TESTO);
            card.add(titolo);
            JLabel tecnico = new JLabel("ID layout: " + c.nome() + "  ·  " + NomiDati.uso(eti, c));
            tecnico.setFont(Stile.minuscolo());
            tecnico.setForeground(Stile.OV1);
            tecnico.setBorder(BorderFactory.createEmptyBorder(0, 0, Stile.px(8), 0));
            card.add(tecnico);

            JComboBox<Comportamento> come = new JComboBox<Comportamento>(Comportamento.values());
            come.setSelectedItem(c.comportamento());
            come.setFont(Stile.normale());
            JTextField valore = new JTextField(c.corrente());
            valore.setFont(Stile.normale());
            int n = c.serie() == null ? 3 : c.serie().cifre();
            JSpinner quante = new JSpinner(new SpinnerNumberModel(n, 1, 9, 1));
            quante.setFont(Stile.normale());

            card.add(campoVerticale("Come cambia", come));
            card.add(campoVerticale(c.comportamento() == Comportamento.PROGRESSIVO
                    ? "Codice iniziale" : "Valore", valore));
            card.add(campoVerticale("Cifre mobili (se progressivo)", quante));
            elenco.add(card);
            elenco.add(javax.swing.Box.createVerticalStrut(Stile.px(10)));
            comportamenti.add(come);
            valori.add(valore);
            cifre.add(quante);
        }

        JScrollPane sc = new JScrollPane(elenco);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getViewport().setBackground(Stile.BASE);
        sc.getVerticalScrollBar().setUnitIncrement(Stile.px(20));
        sc.setPreferredSize(new Dimension(Stile.px(620), Stile.px(430)));

        JPanel dentro = new JPanel(new BorderLayout());
        dentro.setBackground(Stile.BASE);
        dentro.add(sc, BorderLayout.CENTER);
        dentro.add(nota("I dati sono condivisi: se QR e testo leggono lo stesso dato, "
                + "in stampa compaiono una volta sola. Per separarli usa Rendi indipendente nell'editor."),
                BorderLayout.SOUTH);
        if (!mostra(sopra, "Dati etichetta", dentro, null)) return;

        List<Serie> validate = new ArrayList<Serie>();
        try {
            for (int i = 0; i < originali.size(); i++) {
                Comportamento come = (Comportamento) comportamenti.get(i).getSelectedItem();
                if (come == Comportamento.PROGRESSIVO) {
                    validate.add(new Serie(valori.get(i).getText().trim(),
                            ((Number) cifre.get(i).getValue()).intValue()));
                } else {
                    validate.add(null);
                }
            }
        } catch (RuntimeException ex) {
            guaio(sopra, "Dati etichetta", ex.getMessage());
            return;
        }

        for (int i = 0; i < originali.size(); i++) {
            Campo c = originali.get(i);
            Comportamento come = (Comportamento) comportamenti.get(i).getSelectedItem();
            if (come == Comportamento.PROGRESSIVO) {
                c.serie(validate.get(i));
            } else {
                c.comportamento(come);
                c.valore(valori.get(i).getText());
            }
        }
    }

    public static void stampante(Component sopra, Impostazioni imp) {
        impostazioni(sopra, imp, 1);
    }

    public static void impostazioni(Component sopra, Impostazioni imp) {
        impostazioni(sopra, imp, 0);
    }

    private static void impostazioni(final Component sopra, final Impostazioni imp, int scheda) {
        final JTextField etichette = pathField(imp.cartellaEtichette());
        final JTextField log = pathField(imp.cartellaLog());
        final JTextField nomeStampante = new JTextField(imp.stampante());
        nomeStampante.setFont(Stile.normale());
        final JSpinner dpi = new JSpinner(new SpinnerNumberModel(
                imp.risoluzioneDpi(), 100, 1200, 1));
        dpi.setFont(Stile.normale());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Stile.forte());
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.addTab("Generale", generale(sopra, etichette, log));
        tabs.addTab("Stampante", stampantePane(nomeStampante, dpi));
        tabs.addTab("Manuale", manuale(true));
        tabs.addTab("Info", informazioni());
        tabs.setSelectedIndex(Math.max(0, Math.min(scheda, tabs.getTabCount() - 1)));
        for (int i = 0; i < tabs.getTabCount(); i++) {
            JLabel t = new JLabel(tabs.getTitleAt(i));
            t.setFont(Stile.forte());
            t.setBorder(BorderFactory.createEmptyBorder(
                    Stile.px(6), Stile.px(12), Stile.px(6), Stile.px(12)));
            tabs.setTabComponentAt(i, t);
        }

        if (!mostraGrande(sopra, "Impostazioni", tabs, "Salva")) return;
        imp.cartellaEtichette(new File(etichette.getText().trim()));
        imp.cartellaLog(new File(log.getText().trim()));
        imp.stampante(nomeStampante.getText().trim());
        imp.risoluzioneDpi(((Number) dpi.getValue()).intValue());
        try {
            imp.salva();
        } catch (Exception ex) {
            guaio(sopra, "Impostazioni", "Le modifiche sono attive, ma non riesco a salvarle: "
                    + ex.getMessage());
        }
    }

    private static JComponent generale(final Component sopra, final JTextField etichette,
                                       final JTextField log) {
        JPanel p = pagina();
        p.add(sezione("Percorsi", "Dove l'app salva layout e registro giornaliero."));
        p.add(percorso(sopra, "Etichette", "Layout salvati e modelli della vetrina.", etichette));
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(12)));
        p.add(percorso(sopra, "Registro", "Storico dei giri stampati, leggibile anche senza l'app.", log));
        p.add(javax.swing.Box.createVerticalGlue());
        return scorriPagina(p);
    }

    private static JComponent stampantePane(JTextField nome, JSpinner dpi) {
        JPanel p = pagina();
        p.add(sezione("Stampante", "Valori usati per controlli, registro e leggibilità dei codici."));
        p.add(campoVerticale("Nome nel registro", nome));
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(12)));
        p.add(campoVerticale("Risoluzione (dpi)", dpi));
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(16)));
        p.add(nota("La coda di stampa reale si sceglie nella finestra di sistema quando premi Stampa. "
                + "Il valore DPI serve anche per gli avvisi sulla dimensione di QR e barcode."));
        p.add(javax.swing.Box.createVerticalGlue());
        return scorriPagina(p);
    }

    private static JComponent manuale(boolean italiano) {
        JPanel root = new JPanel(new BorderLayout(0, Stile.px(10)));
        root.setBackground(Stile.BASE);

        final CardLayout layout = new CardLayout();
        final JPanel pagine = new JPanel(layout);
        pagine.setBackground(Stile.BASE);
        pagine.add(manualPane(manualeItaliano()), "it");
        pagine.add(manualPane(manualeInglese()), "en");

        JPanel lingue = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(6), 0));
        lingue.setOpaque(false);
        final Bottone it = Bottone.piatto("Italiano");
        final Bottone en = Bottone.piatto("English");
        Runnable aggiorna = () -> {
            boolean mostraItaliano = it.isSelected();
            it.selezionato(mostraItaliano);
            en.selezionato(!mostraItaliano);
            layout.show(pagine, mostraItaliano ? "it" : "en");
        };
        it.addActionListener(e -> { it.setSelected(true); en.setSelected(false); aggiorna.run(); });
        en.addActionListener(e -> { en.setSelected(true); it.setSelected(false); aggiorna.run(); });
        it.setSelected(italiano);
        en.setSelected(!italiano);
        lingue.add(it);
        lingue.add(en);
        aggiorna.run();

        root.add(lingue, BorderLayout.NORTH);
        root.add(pagine, BorderLayout.CENTER);
        return root;
    }

    private static JComponent manualPane(String html) {
        JEditorPane testo = new JEditorPane("text/html", html);
        testo.setEditable(false);
        testo.setCaretPosition(0);
        testo.setBackground(Color.WHITE);
        testo.setBorder(BorderFactory.createEmptyBorder(
                Stile.px(18), Stile.px(22), Stile.px(18), Stile.px(22)));
        JScrollPane sp = new JScrollPane(testo);
        sp.setBorder(BorderFactory.createLineBorder(Stile.S0));
        sp.getVerticalScrollBar().setUnitIncrement(Stile.px(22));
        return sp;
    }

    private static JComponent informazioni() {
        JPanel p = pagina();
        JLabel titolo = new JLabel("Etichette Custom");
        titolo.setFont(Stile.font(22, java.awt.Font.BOLD));
        titolo.setForeground(Stile.BLU);
        titolo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(titolo);
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(8)));
        p.add(testoWrap("Disegna, prepara, esporta e stampa etichette serializzate con testo, "
                + "QR e barcode. Anteprima e stampa usano lo stesso renderer.", Stile.SUB0));
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(22)));
        JLabel repo = new JLabel("GitHub  ·  importriri/etichette-custom");
        repo.setFont(Stile.forte());
        repo.setForeground(Stile.TESTO);
        repo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(repo);
        JLabel url = new JLabel(GITHUB);
        url.setFont(Stile.piccolo());
        url.setForeground(Stile.BLU);
        url.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(url);
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(12)));
        Bottone apri = Bottone.normale("Apri GitHub");
        apri.setAlignmentX(Component.LEFT_ALIGNMENT);
        apri.addActionListener(e -> apriSito(p, GITHUB));
        p.add(apri);
        p.add(javax.swing.Box.createVerticalGlue());
        return scorriPagina(p);
    }



    private static void apriSito(Component sopra, String url) {
        try {
            if (!Desktop.isDesktopSupported()) throw new IllegalStateException("browser non disponibile");
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            avviso(sopra, "GitHub", url);
        }
    }

    private static String manualeItaliano() {
        return htmlManuale("Manuale operatore",
                "<h2>1. Vetrina</h2><p>Scegli l'etichetta dalla sua anteprima. Il click normale apre la modalità operatore; <b>Modifica layout</b> apre l'editor.</p>"
              + "<h2>2. Prepara il giro</h2><p>Inserisci i dati richiesti e il numero di copie. Ogni progressivo mostra l'intervallo che uscirà. Se QR e testo condividono lo stesso dato, lo inserisci una volta sola.</p>"
              + "<h2>3. Stampa</h2><p>Controlla il riepilogo verde e premi <b>Stampa</b>. Se annulli la finestra di sistema, nessun progressivo avanza.</p>"
              + "<h2>4. Modifica layout</h2><p>Trascina gli elementi sul foglio. <b>R</b> ruota, <b>Ctrl+D</b> duplica, <b>Canc</b> elimina, <b>Ctrl+Z</b> annulla e <b>Ctrl+Y</b> ripete.</p>"
              + "<h2>5. Dati condivisi</h2><p>Ogni elemento legge un dato. Due elementi collegati allo stesso dato mostrano lo stesso contenuto. Usa <b>Rendi indipendente</b> solo quando devono avere valori o progressivi diversi.</p>"
              + "<h2>6. QR e barcode</h2><p>Gli avvisi arancioni indicano dimensioni o zona bianca da controllare. Il rosso blocca un contenuto non valido; il verde indica un'impostazione pronta.</p>"
              + "<h2>7. Percorsi e stampante</h2><p>Da Impostazioni trovi insieme cartelle, DPI, manuale e collegamento GitHub. Le modifiche vengono salvate per il prossimo avvio.</p>");
    }

    private static String manualeInglese() {
        return htmlManuale("Operator manual",
                "<h2>1. Gallery</h2><p>Choose a label from its real preview. A normal click opens operator mode; <b>Edit layout</b> opens the editor.</p>"
              + "<h2>2. Prepare a run</h2><p>Enter requested data and copy count. Each sequence shows the exact outgoing range. QR and text elements sharing one data source are entered only once.</p>"
              + "<h2>3. Print</h2><p>Check the green preflight and press <b>Print</b>. Cancelling the system print dialog never consumes a sequence.</p>"
              + "<h2>4. Edit layout</h2><p>Drag elements on the label. <b>R</b> rotates, <b>Ctrl+D</b> duplicates, <b>Delete</b> removes, <b>Ctrl+Z</b> undoes and <b>Ctrl+Y</b> redoes.</p>"
              + "<h2>5. Shared data</h2><p>Every element reads one data source. Elements linked to the same source always show the same value. Use <b>Make independent</b> only when values or sequences must differ.</p>"
              + "<h2>6. QR and barcode</h2><p>Orange warnings flag size or quiet-zone concerns. Red means invalid content; green means the current setup is ready.</p>"
              + "<h2>7. Paths and printer</h2><p>Settings keeps folders, DPI, manuals and the GitHub link in one place. Changes are saved for the next start.</p>");
    }

    private static String htmlManuale(String titolo, String corpo) {
        int testo = Stile.px(13);
        int h1 = Stile.px(22);
        int h2 = Stile.px(15);
        return "<html><head><style>body{font-family:sans-serif;color:#283246;background:#ffffff;"
                + "font-size:" + testo + "px;line-height:1.45;margin:0;}"
                + "h1{color:#2563eb;font-size:" + h1 + "px;margin:0 0 14px 0;}"
                + "h2{color:#283246;font-size:" + h2 + "px;margin:18px 0 5px 0;}"
                + "p{margin:0 0 8px 0;}b{color:#283246;}</style></head><body><h1>"
                + titolo + "</h1>" + corpo + "</body></html>";
    }

    private static JTextField pathField(File f) {
        JTextField t = new JTextField(f == null ? "" : f.getAbsolutePath());
        t.setFont(Stile.mono(11.5));
        t.setToolTipText(t.getText());
        return t;
    }

    private static JPanel percorso(final Component sopra, String titolo, String aiuto,
                                   final JTextField campo) {
        JPanel p = card();
        JLabel h = new JLabel(titolo);
        h.setFont(Stile.forte()); h.setForeground(Stile.TESTO); p.add(h);
        p.add(testoWrap(aiuto, Stile.OV1));
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(8)));
        JPanel r = new JPanel(new BorderLayout(Stile.px(8), 0));
        r.setOpaque(false);
        r.add(campo, BorderLayout.CENTER);
        Bottone sfoglia = Bottone.normale("Sfoglia…");
        sfoglia.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(new File(campo.getText()));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(sopra) == JFileChooser.APPROVE_OPTION) {
                campo.setText(fc.getSelectedFile().getAbsolutePath());
                campo.setToolTipText(campo.getText());
            }
        });
        r.add(sfoglia, BorderLayout.EAST);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, Stile.px(38)));
        p.add(r);
        return p;
    }



    public static Etichetta nuovaEtichetta(Component sopra) {
        JPanel p = pannello();
        final JTextField nome = new JTextField("Etichetta nuova", 22);
        nome.setFont(Stile.normale());
        p.add(campoVerticale("Nome", nome));
        final JSpinner larga = new JSpinner(new SpinnerNumberModel(50.0, 5.0, 300.0, 1.0));
        final JSpinner alta = new JSpinner(new SpinnerNumberModel(30.0, 5.0, 300.0, 1.0));
        larga.setFont(Stile.normale()); alta.setFont(Stile.normale());
        p.add(campoVerticale("Larghezza (mm)", larga));
        p.add(campoVerticale("Altezza (mm)", alta));
        p.add(nota("Parte vuota: gli elementi si aggiungono dal banco di lavoro."));
        if (!mostra(sopra, "Nuova etichetta", p, "Crea")) return null;
        String n = nome.getText().trim();
        Etichetta e = new Etichetta(n.isEmpty() ? "Etichetta nuova" : n,
                ((Number) larga.getValue()).doubleValue(), ((Number) alta.getValue()).doubleValue());
        e.aggiungi(new Campo("codice", Comportamento.CHIESTO, ""));
        return e;
    }

    public static String rinomina(Component sopra, String nomeAttuale) {
        JPanel p = pannello();
        JTextField nome = new JTextField(nomeAttuale, 24);
        nome.setFont(Stile.normale());
        p.add(campoVerticale("Nome", nome));
        if (!mostra(sopra, "Rinomina", p, "Rinomina")) return null;
        String nuovo = nome.getText().trim();
        return nuovo.isEmpty() ? null : nuovo;
    }

    /** Conferma i dati del giro senza consumare alcun progressivo. */
    public static boolean stampa(Component sopra, Etichetta eti, int copie) {
        try {
            eti.validaGiro(copie);
        } catch (RuntimeException ex) {
            guaio(sopra, "Il giro non ci sta", ex.getMessage());
            return false;
        }
        JPanel p = pannello();
        List<Campo> chiesti = eti.daChiedere();
        List<JTextField> campi = new ArrayList<JTextField>();
        for (Campo c : chiesti) {
            JTextField t = new JTextField(c.valore(), 24);
            t.setFont(Stile.normale());
            p.add(campoVerticale(NomiDati.nome(eti, c), t));
            campi.add(t);
        }
        for (Campo c : eti.progressivi()) {
            Serie s = c.serie();
            if (s == null) continue;
            String[] giro = s.giro(copie);
            JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(6), 0));
            r.setOpaque(false);
            CodiceView da = new CodiceView(s.prefisso(), s.finestra(s.prossimo()));
            CodiceView a = new CodiceView(s.prefisso(), giro[giro.length - 1].substring(
                    Math.min(s.prefisso().length(), giro[giro.length - 1].length())));
            da.corpo(11); a.corpo(11);
            r.add(da); r.add(new JLabel("→")); r.add(a);
            p.add(campoVerticale(NomiDati.nome(eti, c), r));
        }
        if (!mostra(sopra, "Conferma il giro", p, "Continua")) return false;
        for (int i = 0; i < chiesti.size(); i++) chiesti.get(i).valore(campi.get(i).getText());
        return true;
    }

    public static List<File> esporta(Component sopra, Etichetta eti,
                                     app.render.SorgenteQr qr, int copieDelGiro) {
        JPanel p = pannello();
        JComboBox<Esportazione.Come> come = new JComboBox<Esportazione.Come>(Esportazione.Come.values());
        come.setFont(Stile.normale()); p.add(campoVerticale("Formato", come));
        JComboBox<String> quante = new JComboBox<String>(new String[] {
                "Solo questa etichetta", "Tutto il giro (" + copieDelGiro + ")" });
        quante.setFont(Stile.normale()); p.add(campoVerticale("Quante", quante));
        JSpinner dpi = new JSpinner(new SpinnerNumberModel(600, 150, 1200, 50));
        dpi.setFont(Stile.normale()); p.add(campoVerticale("Risoluzione (dpi)", dpi));
        p.add(nota("SVG è vettoriale. PDF mette tutto il giro in un solo file, una pagina per etichetta."));
        if (!mostra(sopra, "Esporta", p, "Esporta")) return new ArrayList<File>();
        Esportazione.Come formato = (Esportazione.Come) come.getSelectedItem();
        int copie = quante.getSelectedIndex() == 0 ? 1 : copieDelGiro;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(nomeSano(eti.nome()) + "." + formato.coda()));
        if (fc.showSaveDialog(sopra) != JFileChooser.APPROVE_OPTION) return new ArrayList<File>();
        try {
            List<File> scritti = Esportazione.esporta(fc.getSelectedFile(), eti, qr,
                    formato, copie, ((Number) dpi.getValue()).intValue());
            avviso(sopra, "Esporta", scritti.size() == 1
                    ? "Scritto " + scritti.get(0).getName()
                    : "Scritti " + scritti.size() + " file in "
                            + scritti.get(0).getParentFile().getAbsolutePath());
            return scritti;
        } catch (Exception ex) {
            guaio(sopra, "Esporta", "Non sono riuscito a scrivere: " + ex.getMessage());
            return new ArrayList<File>();
        }
    }

    private static String nomeSano(String nome) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < nome.length(); i++) {
            char c = Character.toLowerCase(nome.charAt(i));
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') b.append(c);
            else if (b.length() > 0 && b.charAt(b.length() - 1) != '-') b.append('-');
        }
        return b.length() == 0 ? "etichetta" : b.toString();
    }

    private static JPanel pagina() {
        JPanel p = pannello();
        p.setBorder(BorderFactory.createEmptyBorder(
                Stile.px(18), Stile.px(20), Stile.px(18), Stile.px(20)));
        return p;
    }

    private static JPanel pannello() {
        JPanel p = new JPanel();
        p.setBackground(Stile.BASE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JPanel card() {
        JPanel p = pannello();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Stile.S0),
                BorderFactory.createEmptyBorder(
                        Stile.px(14), Stile.px(16), Stile.px(14), Stile.px(16))));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private static JComponent sezione(String titolo, String aiuto) {
        JPanel p = pannello();
        JLabel h = new JLabel(titolo);
        h.setFont(Stile.titolo()); h.setForeground(Stile.TESTO); h.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(h);
        p.add(testoWrap(aiuto, Stile.SUB0));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, Stile.px(16), 0));
        return p;
    }

    private static JComponent campoVerticale(String etichetta, JComponent comando) {
        JPanel p = new JPanel(new BorderLayout(0, Stile.px(5)));
        p.setOpaque(false);
        JLabel l = new JLabel(etichetta);
        l.setFont(Stile.piccolo()); l.setForeground(Stile.SUB0);
        p.add(l, BorderLayout.NORTH); p.add(comando, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Math.max(Stile.px(56), p.getPreferredSize().height)));
        return p;
    }

    private static JComponent testoWrap(String testo, Color colore) {
        JTextArea t = new JTextArea(testo);
        t.setEditable(false); t.setFocusable(false); t.setOpaque(false);
        t.setLineWrap(true); t.setWrapStyleWord(true);
        t.setFont(Stile.piccolo()); t.setForeground(colore); t.setColumns(48);
        t.setRows(testo.length() > 100 ? 3 : 2);
        t.setBorder(null); t.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension d=t.getPreferredSize();
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
        return t;
    }

    private static JComponent nota(String testo) {
        JTextArea t = (JTextArea) testoWrap(testo, Stile.OV1);
        t.setBorder(BorderFactory.createEmptyBorder(Stile.px(8), 0, 0, 0));
        return t;
    }

    private static JComponent scorriPagina(JPanel p) {
        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Stile.BASE);
        sp.getVerticalScrollBar().setUnitIncrement(Stile.px(20));
        return sp;
    }

    private static boolean mostraGrande(Component sopra, String titolo, JComponent corpo,
                                        String conferma) {
        return mostraInterno(sopra, titolo, corpo, conferma, true);
    }

    private static boolean mostra(Component sopra, String titolo, JComponent corpo,
                                  String conferma) {
        return mostraInterno(sopra, titolo, corpo, conferma, false);
    }

    private static boolean mostraInterno(Component sopra, String titolo, JComponent corpo,
                                         String conferma, boolean grande) {
        final boolean[] ok = { false };
        Window w = sopra == null ? null : SwingUtilities.getWindowAncestor(sopra);
        final JDialog d = new JDialog(w instanceof java.awt.Frame ? (java.awt.Frame) w : null,
                titolo, true);
        JPanel radice = new JPanel(new BorderLayout());
        radice.setBackground(Stile.BASE);
        radice.setBorder(BorderFactory.createEmptyBorder(
                Stile.px(14), Stile.px(16), Stile.px(12), Stile.px(16)));
        radice.add(corpo, BorderLayout.CENTER);

        JPanel piede = new JPanel(new FlowLayout(FlowLayout.RIGHT, Stile.px(8), 0));
        piede.setOpaque(false);
        piede.setBorder(BorderFactory.createEmptyBorder(Stile.px(12), 0, 0, 0));
        Bottone annulla = Bottone.normale("Annulla");
        annulla.addActionListener(e -> d.dispose());
        Bottone salva = Bottone.primario(conferma == null ? "Salva" : conferma);
        salva.addActionListener(e -> { ok[0] = true; d.dispose(); });
        piede.add(annulla); piede.add(salva); radice.add(piede, BorderLayout.SOUTH);

        d.setContentPane(radice);
        d.getRootPane().setDefaultButton(salva);
        d.pack();
        Dimension schermo = Toolkit.getDefaultToolkit().getScreenSize();
        if (grande) {
            int w0 = Math.min(Stile.px(760), (int) (schermo.width * .82));
            int h0 = Math.min(Stile.px(620), (int) (schermo.height * .82));
            d.setSize(Math.max(d.getWidth(), w0), Math.max(d.getHeight(), h0));
        } else {
            d.setSize(Math.min(Math.max(d.getWidth(), Stile.px(430)), (int) (schermo.width * .85)),
                    Math.min(Math.max(d.getHeight(), Stile.px(220)), (int) (schermo.height * .85)));
        }
        d.setMinimumSize(new Dimension(Stile.px(420), Stile.px(240)));
        d.setLocationRelativeTo(sopra);
        d.setVisible(true);
        return ok[0];
    }
}
