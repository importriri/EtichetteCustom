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
import app.ui.comp.CodiceView;
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
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** Il banco di lavoro: si apre al posto della vetrina quando scegli una tessera. */
public class Banco extends JPanel {

    private final Etichetta eti;
    private final SorgenteQr qr;
    private final Impostazioni imp;
    private final Archivio archivio;
    private final Runnable preparaStampa;
    private final Storia storia = new Storia();

    private final Foglio foglio;
    private final Proprieta proprieta;
    private final JPanel listaElementi = new JPanel();
    private final CodiceView serieDa = new CodiceView();
    private final CodiceView serieA = new CodiceView();
    private final JLabel statoSel = new JLabel();
    private final JLabel misuraLab = new JLabel();
    private final JLabel avvisoSalvato = new JLabel();
    private final Bottone stampa = Bottone.primario("Stampa");
    private final JLabel zoomVal = new JLabel();
    private final JSpinner copie = new JSpinner(new SpinnerNumberModel(12, 1, 9999, 1));

    public Banco(Etichetta eti, SorgenteQr qr, Impostazioni imp,
                 Archivio archivio, Registro registro, final Runnable indietro) {
        this(eti, qr, imp, archivio, registro, indietro, indietro);
    }

    public Banco(Etichetta eti, SorgenteQr qr, Impostazioni imp,
                 Archivio archivio, Registro registro, final Runnable indietro,
                 Runnable preparaStampa) {
        super(new BorderLayout());
        this.eti = eti;
        this.qr = qr;
        this.imp = imp;
        this.archivio = archivio;
        this.preparaStampa = preparaStampa == null ? indietro : preparaStampa;
        setBackground(Stile.BASE);

        foglio = new Foglio(eti, qr);
        proprieta = new Proprieta(new Runnable() {
            @Override
            public void run() {
                storia.segna(Banco.this.eti);
            }
        }, new Runnable() {
            @Override
            public void run() {
                foglio.repaint();
                aggiornaElenco();
                aggiornaStato();
            }
        });

        add(rigaDelGiro(indietro), BorderLayout.NORTH);
        add(colonnaComandi(), BorderLayout.WEST);

        final JScrollPane sp = new JScrollPane(foglio);
        sp.setBorder(null);
        sp.getViewport().setBackground(Stile.BANCO);
        sp.getVerticalScrollBar().setUnitIncrement(Stile.px(20));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Stile.BANCO);
        centro.add(sp, BorderLayout.CENTER);
        centro.add(barraZoom(sp), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        JScrollPane dx = new JScrollPane(proprieta,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dx.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Stile.S0));
        dx.setPreferredSize(new Dimension(Stile.px(318), 10));
        dx.getVerticalScrollBar().setUnitIncrement(Stile.px(20));
        add(dx, BorderLayout.EAST);

        add(barraDiStato(), BorderLayout.SOUTH);

        foglio.ascolto(new Foglio.Ascolto() {
            @Override
            public void selezionato(Elemento e) {
                proprieta.mostra(Banco.this.eti, e);
                aggiornaElenco();
                aggiornaStato();
            }

            @Override
            public void staPerCambiare() {
                storia.segna(Banco.this.eti);
            }

            @Override
            public void modificato() {
                aggiornaStato();
            }
        });

        scorciatoie();
        if (!eti.elementi().isEmpty()) {
            foglio.selezione(eti.elementi().get(0));
        } else {
            proprieta.mostra(eti, null);
        }
        aggiornaSerie();
        aggiornaElenco();
        aggiornaStato();
        aggiornaZoom();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                foglio.adattaEImpagina(sp.getViewport().getExtentSize());
                aggiornaZoom();
                foglio.requestFocusInWindow();
            }
        });
    }

    /** Da chiamare quando si torna in vetrina: mette al sicuro il lavoro. */
    public void salva() {
        try {
            archivio.salva(eti);
            avvisoSalvato.setText("salvato");
        } catch (Exception rotta) {
            Finestre.guaio(this, "Salvataggio",
                    "Non sono riuscito a salvare l'etichetta: " + rotta.getMessage());
        }
    }

    /* ---- scorciatoie -------------------------------------------------- */

    private void scorciatoie() {
        lega(KeyStroke.getKeyStroke(KeyEvent.VK_Z, scorciatoia()), "annulla", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (storia.annulla(eti)) {
                    foglio.selezione(eti.elementi().isEmpty() ? null : eti.elementi().get(0));
                    foglio.revalidate();
                    foglio.repaint();
                    aggiornaSerie();
                    aggiornaElenco();
                    aggiornaMisura();
                } else {
                    avvisoSalvato.setText("niente da annullare");
                }
            }
        });
        lega(KeyStroke.getKeyStroke(KeyEvent.VK_Y, scorciatoia()), "ripeti", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (storia.ripeti(eti)) {
                    foglio.selezione(eti.elementi().isEmpty() ? null : eti.elementi().get(0));
                    foglio.revalidate(); foglio.repaint();
                    aggiornaSerie(); aggiornaElenco(); aggiornaMisura();
                    proprieta.mostra(eti, foglio.selezione());
                } else avvisoSalvato.setText("niente da ripetere");
            }
        });
        lega(KeyStroke.getKeyStroke(KeyEvent.VK_Z, scorciatoia() | java.awt.event.InputEvent.SHIFT_MASK), "ripeti2", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                getActionMap().get("ripeti").actionPerformed(e);
            }
        });
        lega(KeyStroke.getKeyStroke(KeyEvent.VK_D, scorciatoia()), "duplica", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                duplica();
            }
        });
        lega(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "elimina", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                elimina();
            }
        });
        lega(KeyStroke.getKeyStroke(KeyEvent.VK_S, scorciatoia()), "salva", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                salva();
            }
        });
    }

    /**
     * Il tasto delle scorciatoie: Ctrl su Windows, Cmd sul Mac.
     * Senza schermo il Toolkit non risponde, e le prove non devono
     * cadere per questo: si ripiega su Ctrl.
     */
    private static int scorciatoia() {
        try {
            return java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        } catch (Throwable senzaSchermo) {
            return java.awt.event.InputEvent.CTRL_MASK;
        }
    }

    private void lega(KeyStroke tasto, String nome, AbstractAction azione) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(tasto, nome);
        getActionMap().put(nome, azione);
    }

    /* ---- riga del giro ------------------------------------------------ */

    private JComponent rigaDelGiro(final Runnable indietro) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(14), Stile.px(8)));
        p.setBackground(Stile.BASE);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Stile.S0));

        Bottone back = Bottone.piatto("\u2039  Vetrina");
        back.addActionListener(e -> {
            salva();
            indietro.run();
        });
        p.add(back);
        p.add(separatore());

        misuraLab.setFont(Stile.normale());
        misuraLab.setForeground(Stile.TESTO);
        Bottone scambia = Bottone.piatto("\u21c4");
        scambia.setToolTipText("Scambia i lati dell'etichetta");
        scambia.addActionListener(e -> {
            storia.segna(eti);
            foglio.ruotaEtichetta();
            aggiornaMisura();
            proprieta.mostra(eti, foglio.selezione());
        });
        JPanel mis = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(4), 0));
        mis.setOpaque(false);
        mis.add(misuraLab);
        mis.add(scambia);
        p.add(campo("Etichetta", mis));
        p.add(separatore());

        copie.setFont(Stile.normale());
        copie.setPreferredSize(new Dimension(Stile.px(64), Stile.px(24)));
        copie.addChangeListener(e -> aggiornaSerie());
        p.add(campo("Copie", copie));

        JPanel serie = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(4), 0));
        serie.setOpaque(false);
        serieDa.corpo(11.5);
        serieA.corpo(11.5);
        JLabel freccia = new JLabel("\u2192");
        freccia.setForeground(Stile.OV1);
        serie.add(serieDa);
        serie.add(freccia);
        serie.add(serieA);
        p.add(campo("Serie di stampa", serie));

        p.add(Box.createHorizontalStrut(Stile.px(20)));

        Bottone esporta = Bottone.normale("Esporta\u2026");
        esporta.addActionListener(e -> Finestre.esporta(this, eti, qr, quanteCopie()));
        p.add(esporta);

        stampa.addActionListener(e -> { salva(); preparaStampa.run(); });
        p.add(stampa);

        aggiornaMisura();
        return p;
    }

    private int quanteCopie() {
        return ((Number) copie.getValue()).intValue();
    }

    private JComponent campo(String etichetta, JComponent comando) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(etichetta.toUpperCase());
        l.setFont(Stile.minuscolo());
        l.setForeground(Stile.OV1);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        comando.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(comando);
        return p;
    }

    private JComponent separatore() {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(1, Stile.px(26)));
        s.setBackground(Stile.S0);
        return s;
    }

    /* ---- zoom ---------------------------------------------------------- */

    private JComponent barraZoom(final JScrollPane dove) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(4), Stile.px(6)));
        p.setBackground(Stile.BANCO);

        Bottone meno = Bottone.normale("\u2212");
        meno.addActionListener(e -> {
            foglio.zoomPasso(-1);
            aggiornaZoom();
        });
        Bottone piu = Bottone.normale("+");
        piu.addActionListener(e -> {
            foglio.zoomPasso(1);
            aggiornaZoom();
        });
        Bottone adatta = Bottone.piatto("Adatta");
        adatta.addActionListener(e -> {
            foglio.adattaEImpagina(dove.getViewport().getExtentSize());
            aggiornaZoom();
        });

        zoomVal.setFont(Stile.mono(11));
        zoomVal.setForeground(Stile.SUB0);
        zoomVal.setHorizontalAlignment(JLabel.CENTER);
        zoomVal.setPreferredSize(new Dimension(Stile.px(56), Stile.px(22)));

        p.add(meno);
        p.add(zoomVal);
        p.add(piu);
        p.add(adatta);
        return p;
    }

    private void aggiornaZoom() {
        zoomVal.setText(foglio.percentuale() + " %");
    }

    /* ---- colonna comandi ---------------------------------------------- */

    private JComponent colonnaComandi() {
        JPanel p = new JPanel();
        p.setBackground(Stile.BASE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Stile.S0),
                BorderFactory.createEmptyBorder(Stile.px(14), Stile.px(10),
                        Stile.px(14), Stile.px(10))));
        p.setPreferredSize(new Dimension(Stile.px(190), 10));

        p.add(gruppo("Aggiungi"));
        p.add(new Voce("T", "Testo").azione(() -> aggiungi(Tipo.TESTO)));
        p.add(new Voce("#", "Codice").azione(() -> aggiungi(Tipo.CODICE)));
        p.add(new Voce("\u25a6", "QR").azione(() -> aggiungi(Tipo.QR)));
        p.add(new Voce("|||", "Barcode").azione(() -> aggiungi(Tipo.BARCODE)));
        p.add(new Voce("\u2500", "Linea").azione(() -> aggiungi(Tipo.LINEA)));

        p.add(gruppo("Elementi"));
        listaElementi.setOpaque(false);
        listaElementi.setLayout(new BoxLayout(listaElementi, BoxLayout.Y_AXIS));
        listaElementi.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(listaElementi);

        p.add(gruppo("Disponi"));
        p.add(new Voce("\u27f2", "Ruota 90\u00b0").coda("R").azione(() -> {
            Elemento e = foglio.selezione();
            if (e != null) {
                storia.segna(eti);
                e.rotazione(e.rotazione() + 90);
                foglio.rientra(e);
                foglio.repaint();
                proprieta.mostra(eti, e);
            }
        }));
        p.add(new Voce("\u29c9", "Duplica").coda("Ctrl+D").azione(this::duplica));
        p.add(new Voce("\u2326", "Elimina").coda("Canc").azione(this::elimina));
        p.add(new Voce("\u21b6", "Annulla").coda("Ctrl+Z").azione(() ->
                getActionMap().get("annulla").actionPerformed(
                        new ActionEvent(this, 0, "annulla"))));
        p.add(new Voce("\u21b7", "Ripeti").coda("Ctrl+Y").azione(() ->
                getActionMap().get("ripeti").actionPerformed(
                        new ActionEvent(this, 0, "ripeti"))));

        p.add(gruppo("Etichetta"));
        p.add(new Voce("\u2261", "Campi\u2026").azione(() -> {
            storia.segna(eti);
            Finestre.campi(this, eti);
            aggiornaSerie();
            aggiornaElenco();
            foglio.repaint();
            proprieta.mostra(eti, foglio.selezione());
        }));
        p.add(new Voce("\u2699", "Impostazioni\u2026").azione(() -> {
            Finestre.impostazioni(this, imp);
            foglio.repaint();
        }));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent gruppo(String t) {
        JLabel l = new JLabel(t.toUpperCase());
        l.setFont(Stile.minuscolo());
        l.setForeground(Stile.OV0);
        l.setBorder(BorderFactory.createEmptyBorder(Stile.px(15), Stile.px(6), Stile.px(6), 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void aggiungi(Tipo t) {
        storia.segna(eti);
        String nomeElemento = nomeUnico(t.etichetta());
        String campo = null;
        if (t != Tipo.LINEA) {
            String base = t == Tipo.TESTO ? "testo" : "codice";
            String nomeCampo = eti.nomeCampoUnico(base);
            Comportamento modo = (t == Tipo.QR || t == Tipo.CODICE || t == Tipo.BARCODE)
                    ? Comportamento.CHIESTO : Comportamento.FISSO;
            Campo nuovo = new Campo(nomeCampo, modo, "");
            eti.aggiungi(nuovo);
            campo = nuovo.nome();
        }
        Elemento e = new Elemento(nomeElemento, t, campo, 2, 2,
                t == Tipo.QR ? Math.min(12, Math.max(1, eti.larghezza() - 4))
                        : Math.min(30, Math.max(1, eti.larghezza() - 4)));
        if (t == Tipo.BARCODE || t == Tipo.LINEA) e.altezza(t == Tipo.LINEA ? 0.4 : 8);
        eti.aggiungi(e);
        foglio.rientra(e);
        foglio.selezione(e);
        foglio.repaint();
        aggiornaElenco();
        aggiornaSerie();
    }

    private void duplica() {
        Elemento e = foglio.selezione();
        if (e == null) {
            return;
        }
        storia.segna(eti);
        Elemento copia = e.copia();
        copia.nome(nomeUnico(e.nome()));
        copia.x(e.x() + 2);
        copia.y(e.y() + 2);
        eti.aggiungi(copia);
        foglio.rientra(copia);
        foglio.selezione(copia);
        foglio.repaint();
    }

    private String nomeUnico(String base) {
        String b = base == null || base.trim().isEmpty() ? "Elemento" : base.trim();
        if (!nomeUsato(b)) return b;
        int n = 2;
        while (nomeUsato(b + " " + n)) n++;
        return b + " " + n;
    }

    private boolean nomeUsato(String nome) {
        for (Elemento e : eti.elementi()) if (nome.equals(e.nome())) return true;
        return false;
    }

    private void elimina() {
        Elemento e = foglio.selezione();
        if (e == null) {
            return;
        }
        storia.segna(eti);
        eti.rimuovi(e);
        foglio.selezione(eti.elementi().isEmpty() ? null : eti.elementi().get(0));
        foglio.repaint();
    }

    /* ---- aggiornamenti ------------------------------------------------ */

    private void aggiornaElenco() {
        listaElementi.removeAll();
        for (final Elemento e : eti.elementi()) {
            Voce v = new Voce(e.tipo().glifo(), e.nome());
            Campo c = eti.campo(e.campo());
            if (c != null && c.comportamento() == Comportamento.PROGRESSIVO) {
                v.coda("+1");
            } else if (c != null && c.comportamento() == Comportamento.CHIESTO) {
                v.coda("?");
            }
            v.attiva(e == foglio.selezione());
            v.azione(() -> foglio.selezione(e));
            listaElementi.add(v);
        }
        listaElementi.revalidate();
        listaElementi.repaint();
    }

    private void aggiornaMisura() {
        misuraLab.setText(num(eti.larghezza()) + " \u00d7 " + num(eti.altezza()) + " mm");
    }

    private void aggiornaSerie() {
        int n = quanteCopie();
        int quanti = eti.progressivi().size();
        stampa.setText("Prepara " + n + (n == 1 ? " etichetta" : " etichette"));
        if (quanti == 0) {
            serieDa.testo("nessun progressivo", "");
            serieA.testo("", "");
        } else {
            serieDa.testo(quanti + (quanti == 1 ? " progressivo" : " progressivi"), "");
            serieA.testo("→ vedi stampa", "");
        }
    }

    private void aggiornaStato() {
        Elemento e = foglio.selezione();
        statoSel.setText(e == null ? "Nessun elemento scelto"
                : e.nome() + "  \u00b7  X " + num(e.x()) + "  Y " + num(e.y())
                        + "  \u00b7  " + num(e.larghezza()) + " mm di larghezza"
                        + (e.rotazione() == 0 ? "" : "  \u00b7  " + e.rotazione() + "\u00b0"));
    }

    private JComponent barraDiStato() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Stile.MANTLE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Stile.S0),
                BorderFactory.createEmptyBorder(Stile.px(4), Stile.px(12),
                        Stile.px(4), Stile.px(12))));

        JPanel sx = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(12), 0));
        sx.setOpaque(false);
        sx.add(new Pallino());
        JLabel st = new JLabel(imp.stampante());
        st.setFont(Stile.piccolo());
        st.setForeground(Stile.OV1);
        sx.add(st);
        statoSel.setFont(Stile.piccolo());
        statoSel.setForeground(Stile.OV1);
        sx.add(statoSel);
        p.add(sx, BorderLayout.WEST);

        avvisoSalvato.setFont(Stile.piccolo());
        avvisoSalvato.setForeground(Stile.OV0);
        p.add(avvisoSalvato, BorderLayout.EAST);
        return p;
    }

    private static String num(double v) {
        String s = String.valueOf(Math.round(v * 10) / 10.0);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s.replace('.', ',');
    }

    /** Il puntino verde della stampante pronta. */
    private static class Pallino extends JComponent {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Stile.px(8), Stile.px(8));
        }

        @Override
        protected void paintComponent(Graphics g) {
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
