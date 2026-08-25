package app.ui.vetrina;

import app.modello.Etichetta;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.CodiceView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;

/** La schermata di partenza: le etichette in vetrina, e sotto gli ultimi giri. */
public class Vetrina extends JPanel {

    private final GrigliaFluida griglia = new GrigliaFluida();
    private final JLabel conteggio = new JLabel();
    private final SorgenteQr qr;
    private final JTextField cerca = new JTextField();
    private List<Etichetta> tutte = new ArrayList<Etichetta>();

    /** Tutto quello che dalla vetrina si puo' chiedere di fare. */
    public interface Comandi {
        void apri(Etichetta e);
        void modifica(Etichetta e);
        void nuova();
        void rinomina(Etichetta e);
        void duplica(Etichetta e);
        void elimina(Etichetta e);
        void stampante();
        void impostazioni();
    }

    private final Comandi comandi;
    private final app.archivio.Registro registro;
    private final JPanel giri = new JPanel();

    public Vetrina(List<Etichetta> etichette, SorgenteQr qr, Comandi comandi) {
        this(etichette, qr, comandi, null);
    }

    public Vetrina(List<Etichetta> etichette, SorgenteQr qr, Comandi comandi,
                   app.archivio.Registro registro) {
        super(new BorderLayout());
        this.comandi = comandi;
        this.qr = qr;
        this.registro = registro;
        setBackground(Stile.BASE);
        int p = Stile.px(24);
        setBorder(BorderFactory.createEmptyBorder(p, Stile.px(28), p, Stile.px(28)));

        add(intestazione(), BorderLayout.NORTH);

        Colonna dentro = new Colonna();
        griglia.setAlignmentX(Component.LEFT_ALIGNMENT);
        dentro.add(griglia);
        JComponent blocco = ultimiGiri();
        blocco.setAlignmentX(Component.LEFT_ALIGNMENT);
        dentro.add(blocco);
        dentro.add(Box.createVerticalGlue());

        JScrollPane sp = new JScrollPane(dentro,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(Stile.BASE);
        sp.getVerticalScrollBar().setUnitIncrement(Stile.px(24));
        add(sp, BorderLayout.CENTER);

        popola(etichette, qr);
    }

    public void popola(List<Etichetta> etichette, SorgenteQr qr) {
        tutte = new ArrayList<Etichetta>(etichette);
        filtra();
        aggiornaGiri();
    }

    private void filtra() {
        String q = cerca.getText() == null ? "" : cerca.getText().trim().toLowerCase();
        griglia.removeAll();
        int visibili = 0;
        for (Etichetta e : tutte) {
            if (!q.isEmpty() && !e.nome().toLowerCase().contains(q)) continue;
            visibili++;
            griglia.add(new Tessera(e, qr)
                    .azione(eti -> comandi.apri(eti))
                    .menu(this::menuDi));
        }
        griglia.add(Tessera.nuova(eti -> comandi.nuova()));
        int n = q.isEmpty() ? tutte.size() : visibili;
        conteggio.setText(n + (n == 1 ? " modello" : " modelli"));
        griglia.revalidate();
        griglia.repaint();
    }

    private void menuDi(final Etichetta e, int x, int y) {
        javax.swing.JPopupMenu m = new javax.swing.JPopupMenu();
        m.add(voceMenu("Apri", () -> comandi.apri(e)));
        m.add(voceMenu("Rinomina\u2026", () -> comandi.rinomina(e)));
        m.add(voceMenu("Duplica", () -> comandi.duplica(e)));
        m.addSeparator();
        javax.swing.JMenuItem via = voceMenu("Elimina", () -> comandi.elimina(e));
        via.setForeground(Stile.ROSSO);
        m.add(via);
        for (java.awt.Component c : griglia.getComponents()) {
            if (c instanceof Tessera && ((Tessera) c).etichetta() == e) {
                m.show(c, x, y);
                return;
            }
        }
    }

    private javax.swing.JMenuItem voceMenu(String testo, final Runnable azione) {
        javax.swing.JMenuItem i = new javax.swing.JMenuItem(testo);
        i.setFont(Stile.normale());
        i.addActionListener(a -> azione.run());
        return i;
    }

    private JComponent intestazione() {
        JPanel p = new JPanel(new BorderLayout(Stile.px(16), 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, Stile.px(14), 0));

        JPanel sx = new JPanel(new FlowLayout(FlowLayout.LEFT, Stile.px(10), 0));
        sx.setOpaque(false);
        JLabel t = new JLabel("Etichette");
        t.setFont(Stile.titolo());
        t.setForeground(Stile.TESTO);
        conteggio.setFont(Stile.piccolo());
        conteggio.setForeground(Stile.OV0);
        sx.add(t); sx.add(conteggio);
        p.add(sx, BorderLayout.WEST);

        JPanel dx = new JPanel(new BorderLayout(Stile.px(8), 0));
        dx.setOpaque(false);
        cerca.setFont(Stile.normale());
        cerca.setToolTipText("Cerca etichetta");
        cerca.setPreferredSize(new Dimension(Stile.px(430), Stile.px(34)));
        cerca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtra(); }
            public void removeUpdate(DocumentEvent e) { filtra(); }
            public void changedUpdate(DocumentEvent e) { filtra(); }
        });
        Bottone settings = Bottone.normale("⚙  Impostazioni…");
        settings.addActionListener(e -> comandi.impostazioni());
        dx.add(cerca, BorderLayout.CENTER);
        dx.add(settings, BorderLayout.EAST);
        p.add(dx, BorderLayout.CENTER);
        return p;
    }

    private JComponent ultimiGiri() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(Stile.px(22), 0, 0, 0));

        JLabel t = new JLabel("Ultimi giri");
        t.setFont(Stile.forte());
        t.setForeground(Stile.TESTO);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(javax.swing.Box.createVerticalStrut(Stile.px(6)));

        giri.setOpaque(false);
        giri.setLayout(new BoxLayout(giri, BoxLayout.Y_AXIS));
        giri.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(giri);
        aggiornaGiri();
        return p;
    }

    /**
     * Quello che c'e' scritto nel registro, non quello che farebbe scena.
     * Se non si e' ancora stampato niente, lo dice.
     */
    private void aggiornaGiri() {
        giri.removeAll();
        java.util.List<app.archivio.Registro.Giro> ultimi = registro == null
                ? new java.util.ArrayList<app.archivio.Registro.Giro>()
                : registro.ultimi(4);
        if (ultimi.isEmpty()) {
            JLabel vuoto = new JLabel("Nessuna stampa registrata finora");
            vuoto.setFont(Stile.piccolo());
            vuoto.setForeground(Stile.OV0);
            vuoto.setAlignmentX(Component.LEFT_ALIGNMENT);
            vuoto.setBorder(BorderFactory.createEmptyBorder(Stile.px(6), Stile.px(8), 0, 0));
            giri.add(vuoto);
        } else {
            for (app.archivio.Registro.Giro g : ultimi) {
                giri.add(new RigaGiro(g.etichetta(), g.primo(), g.ultimo(),
                        g.quante() + (g.quante() == 1 ? " etichetta" : " etichette"),
                        g.quando()));
            }
        }
        giri.revalidate();
        giri.repaint();
    }



    /**
     * Colonna che segue la larghezza della finestra: cosi' le tessere e
     * gli ultimi giri scorrono insieme e non resta un buco fra le due cose.
     */
    private static class Colonna extends JPanel implements Scrollable {

        Colonna() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return Stile.px(24);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return Stile.px(200);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Una riga del registro.
     *
     * I due codici agli estremi del giro non vengono scritti per intero:
     * la parte che hanno in comune e' il prefisso fermo, quella che cambia
     * e' la finestra dell'incremento. Si evidenzia quella - la stessa
     * lettura che l'operatore trova ovunque nel programma - e la si ricava
     * dai codici stessi, senza doverla chiedere a nessuno.
     */
    private static class RigaGiro extends JComponent {

        private final String nome;
        private final String primo;
        private final String ultimo;
        private final String quante;
        private final String quando;

        RigaGiro(String nome, String primo, String ultimo, String quante, String quando) {
            this.nome = nome;
            this.primo = primo == null ? "" : primo;
            this.ultimo = ultimo == null ? "" : ultimo;
            this.quante = quante;
            this.quando = quando;
        }

        private int comune() {
            int i = 0;
            while (i < primo.length() && i < ultimo.length()
                    && primo.charAt(i) == ultimo.charAt(i)) {
                i++;
            }
            return i;
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, Stile.px(30));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Stile.px(600), Stile.px(30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Stile.liscio(g);
            try {
                g2.setColor(Stile.S0);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                int base = Stile.px(19);
                g2.setFont(Stile.normale());
                g2.setColor(Stile.SUB1);
                g2.drawString(nome, Stile.px(8), base);

                int taglio = comune();
                String prefisso = primo.substring(0, taglio);
                if (prefisso.length() > 7) {
                    prefisso = "\u2026" + prefisso.substring(prefisso.length() - 6);
                }
                java.awt.Font mono = Stile.mono(11);
                int x = Stile.px(180);
                x += CodiceView.disegna(g2, x, base, prefisso, primo.substring(taglio), mono);
                g2.setFont(mono);
                g2.setColor(Stile.OV1);
                g2.drawString("  \u2192  ", x, base);
                x += g2.getFontMetrics().stringWidth("  \u2192  ");
                CodiceView.disegna(g2, x, base, prefisso, ultimo.substring(taglio), mono);

                g2.setFont(Stile.normale());
                g2.setColor(Stile.SUB1);
                g2.drawString(quante, Stile.px(440), base);
                g2.setFont(Stile.piccolo());
                g2.setColor(Stile.OV0);
                int w = g2.getFontMetrics().stringWidth(quando);
                g2.drawString(quando, getWidth() - w - Stile.px(8), base);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Le tessere vanno a capo da sole seguendo la larghezza della finestra,
     * senza barra orizzontale: e' una vetrina, non una tabella.
     */
    private static class GrigliaFluida extends JPanel implements Scrollable {

        GrigliaFluida() {
            setOpaque(false);
            setLayout(null);
        }

        private int passo() {
            return Stile.px(226) + Stile.px(16);
        }

        private int colonne(int larghezza) {
            return Math.max(1, (larghezza + Stile.px(16)) / passo());
        }

        @Override
        public void doLayout() {
            int col = colonne(getWidth());
            int i = 0;
            for (Component c : getComponents()) {
                Dimension d = c.getPreferredSize();
                int r = i / col;
                int q = i % col;
                c.setBounds(q * passo(), r * (d.height + Stile.px(16)), d.width, d.height);
                i++;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int n = getComponentCount();
            if (n == 0) {
                return new Dimension(Stile.px(226), Stile.px(224));
            }
            int larghezza = getWidth() > 0 ? getWidth() : Stile.px(1000);
            int col = colonne(larghezza);
            int righe = (n + col - 1) / col;
            int alt = getComponent(0).getPreferredSize().height;
            return new Dimension(larghezza, righe * (alt + Stile.px(16)));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return Stile.px(24);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return Stile.px(200);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

}
