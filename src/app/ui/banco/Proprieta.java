package app.ui.banco;

import app.codice.Code128;
import app.codice.Correzione;
import app.codice.Qr;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Serie;
import app.modello.Tipo;
import app.stile.Stile;
import app.ui.comp.Bottone;
import app.ui.comp.CodiceView;
import app.ui.comp.Scheda;
import app.ui.dati.NomiDati;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

/** Properties panel: operational data first, technical precision second. */
public class Proprieta extends JPanel implements javax.swing.Scrollable {
    private final Runnable dopo;
    private final Runnable prima;
    private boolean silenzio;

    public Proprieta(Runnable prima, Runnable dopo) {
        this.prima=prima; this.dopo=dopo;
        setBackground(Stile.BASE); setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(Stile.px(18),Stile.px(16),Stile.px(18),Stile.px(16)));
    }
    @Override public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
    @Override public int getScrollableUnitIncrement(java.awt.Rectangle r,int o,int d){return Stile.px(24);}
    @Override public int getScrollableBlockIncrement(java.awt.Rectangle r,int o,int d){return Stile.px(220);}
    @Override public boolean getScrollableTracksViewportWidth(){return true;}
    @Override public boolean getScrollableTracksViewportHeight(){return false;}

    public void mostra(final Etichetta eti, final Elemento e) {
        removeAll();
        if(e==null){
            JLabel h=new JLabel("Seleziona un elemento"); h.setFont(Stile.forte()); h.setForeground(Stile.SUB0); h.setAlignmentX(Component.LEFT_ALIGNMENT); add(h);
            JLabel n=new JLabel("Poi modifica contenuto, comportamento e posizione da qui."); n.setFont(Stile.piccolo()); n.setForeground(Stile.OV1); n.setAlignmentX(Component.LEFT_ALIGNMENT); add(Scheda.spazio(6)); add(n);
            revalidate(); repaint(); return;
        }
        silenzio=true;
        try{
            add(titolo(eti,e)); add(Scheda.spazio(14));
            if(e.tipo()!=Tipo.LINEA){ add(dati(eti,e)); add(Scheda.spazio(12)); }
            if(e.tipo().scritto()){ add(aCapo(e)); add(Scheda.spazio(12)); }
            if(e.tipo()==Tipo.QR){ add(lettura(eti,e)); add(Scheda.spazio(12)); }
            if(e.tipo()==Tipo.BARCODE){ add(barre(eti,e)); add(Scheda.spazio(12)); }
            add(posizione(eti,e)); add(javax.swing.Box.createVerticalGlue());
        } finally { silenzio=false; }
        revalidate(); repaint();
    }

    private Component titolo(Etichetta eti, Elemento e){
        JPanel wrap=new JPanel(); wrap.setOpaque(false); wrap.setLayout(new BoxLayout(wrap,BoxLayout.Y_AXIS)); wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,Stile.px(8),0)); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel n=new JLabel(e.nome()); n.setFont(Stile.titolo()); n.setForeground(Stile.TESTO); p.add(n); p.add(new Badge(e.tipo().etichetta(),Stile.BLU_SOFT,Stile.BLU)); wrap.add(p);
        Campo c=eti.campo(e.campo());
        if(c!=null){ JTextArea sub=testoSecondario(NomiDati.nome(eti,c)+"  ·  "+NomiDati.uso(eti,c));
            sub.setBorder(BorderFactory.createEmptyBorder(Stile.px(5),Stile.px(2),0,0));
            wrap.add(sub); }
        return wrap;
    }

    private Component dati(final Etichetta eti, final Elemento e){
        Scheda s=new Scheda("Dati"); s.setAlignmentX(Component.LEFT_ALIGNMENT);
        final Campo c=eti.campo(e.campo());
        if(c==null){ s.nota("Questo elemento non ha ancora un dato collegato."); return s; }

        final JTextField valore=new JTextField(valoreVisibile(c),18); valore.setFont(Stile.normale()); valore.setToolTipText("Il contenuto che finisce davvero nell'elemento");
        Runnable salvaValore=new Runnable(){ public void run(){ if(silenzio)return; applicaValore(eti,e,c,valore); }};
        valore.addActionListener(a->salvaValore.run());
        valore.addFocusListener(new FocusAdapter(){ @Override public void focusLost(FocusEvent ev){salvaValore.run();}});
        s.campo(c.comportamento()==Comportamento.PROGRESSIVO?"Codice iniziale":"Contenuto",valore);

        final JComboBox<Campo> collega=new JComboBox<Campo>();
        for(Campo x:eti.campi()) collega.addItem(x);
        collega.setSelectedItem(c); collega.setFont(Stile.normale()); collega.setRenderer(new CampoRenderer(eti));
        collega.setPreferredSize(new Dimension(Stile.px(210),Stile.px(34)));
        collega.setToolTipText("Scegli quale dato deve leggere questo elemento");
        collega.addActionListener(a->{ if(!silenzio && collega.getSelectedItem()!=null){ segna(); Campo x=(Campo)collega.getSelectedItem(); e.campo(x.nome()); mostra(eti,e); cambiato(); }});
        s.campo("Dato collegato",collega);

        final JComboBox<Comportamento> come=new JComboBox<Comportamento>(Comportamento.values());
        come.setSelectedItem(c.comportamento()); come.setFont(Stile.normale()); come.setPreferredSize(new Dimension(Stile.px(210),Stile.px(34)));
        come.addActionListener(a->{ if(!silenzio){ segna(); Comportamento nuovo=(Comportamento)come.getSelectedItem();
            if(nuovo==Comportamento.PROGRESSIVO){
                try{ Serie old=c.serie(); int cif=old==null?3:old.cifre(); c.serie(new Serie(valore.getText().trim(),cif)); }
                catch(RuntimeException ex){ c.comportamento(nuovo); }
            } else c.comportamento(nuovo);
            mostra(eti,e); cambiato(); }});
        s.campo("Come cambia",come);

        if(c.comportamento()==Comportamento.PROGRESSIVO){
            Serie serie=c.serie();
            if(serie!=null){
                CodiceView cv=new CodiceView(serie.prefisso(),serie.finestra(serie.prossimo())); cv.corpo(12); s.largo(cv);
                final JSpinner cifre=new JSpinner(new SpinnerNumberModel(serie.cifre(),1,9,1)); cifre.setFont(Stile.normale()); cifre.setPreferredSize(new Dimension(Stile.px(78),Stile.px(32)));
                cifre.addChangeListener(a->{if(!silenzio){int n=((Number)cifre.getValue()).intValue(); segna(); eti.cambiaFinestra(c.nome(),n); mostra(eti,e); cambiato();}});
                s.riga("Cifre mobili",cifre);
                s.nota("Solo queste cifre avanzano. Il prefisso resta identico.");
            } else s.nota("Inserisci un codice che termini con cifre numeriche: da quelle nasce il progressivo.");
        } else if(c.comportamento()==Comportamento.CHIESTO) {
            s.nota("Questo valore viene chiesto nella schermata Prepara il giro, prima della stampa.");
        }

        if(eti.elementiPerCampo(c).size()>1){
            Bottone indip=Bottone.normale("Rendi indipendente");
            indip.setToolTipText("Crea un nuovo dato solo per questo elemento");
            indip.addActionListener(a->{ if(!silenzio){segna(); eti.rendiIndipendente(e); mostra(eti,e); cambiato();}});
            s.largo(indip);
            s.nota("Ora questo dato e' condiviso: "+NomiDati.uso(eti,c)+". Separalo solo se devono mostrare codici diversi.");
        }
        return s;
    }

    private static JTextArea testoSecondario(String testo){
        JTextArea t=new JTextArea(testo);
        t.setEditable(false);t.setFocusable(false);t.setOpaque(false);
        t.setLineWrap(true);t.setWrapStyleWord(true);
        t.setFont(Stile.piccolo());t.setForeground(Stile.SUB0);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);t.setColumns(24);t.setRows(2);
        Dimension d=t.getPreferredSize();
        t.setPreferredSize(new Dimension(Stile.px(245),d.height));
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
        return t;
    }

    private static String valoreVisibile(Campo c){ return c.serie()!=null?c.serie().codice(c.serie().prossimo()):c.valore(); }
    private void applicaValore(Etichetta eti,Elemento e,Campo c,JTextField f){
        String v=f.getText().trim();
        try{
            segna();
            if(c.comportamento()==Comportamento.PROGRESSIVO){ int n=c.serie()==null?3:c.serie().cifre(); c.serie(new Serie(v,n)); }
            else c.valore(v);
            cambiato();
        }catch(RuntimeException ex){ f.setToolTipText(ex.getMessage()); java.awt.Toolkit.getDefaultToolkit().beep(); }
    }

    private Component aCapo(final Elemento e){
        Scheda s=new Scheda("Testo"); s.setAlignmentX(Component.LEFT_ALIGNMENT);
        final JSpinner righe=new JSpinner(new SpinnerNumberModel(e.massimoRighe(),1,6,1)); righe.setFont(Stile.normale()); righe.setPreferredSize(new Dimension(Stile.px(78),Stile.px(32)));
        righe.addChangeListener(a->{if(!silenzio){segna();e.massimoRighe(((Number)righe.getValue()).intValue());cambiato();}}); s.riga("Righe massime",righe);
        JCheckBox auto=new JCheckBox("Riduci solo se serve",true);auto.setEnabled(false);auto.setOpaque(false);auto.setFont(Stile.piccolo());auto.setForeground(Stile.SUB0);s.largo(auto);
        final JSpinner corpo=new JSpinner(new SpinnerNumberModel(e.corpo(),1.0,40.0,0.1)); corpo.setFont(Stile.normale());corpo.setPreferredSize(new Dimension(Stile.px(78),Stile.px(32)));
        corpo.addChangeListener(a->{if(!silenzio){segna();e.corpo(((Number)corpo.getValue()).doubleValue());cambiato();}});s.riga("Corpo (mm)",corpo);return s;
    }

    private Component lettura(final Etichetta eti,final Elemento e){
        Scheda s=new Scheda("Qualità QR"); s.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<Correzione> livello=new JComboBox<Correzione>(Correzione.values());livello.setSelectedItem(e.correzione());livello.setFont(Stile.normale());livello.setPreferredSize(new Dimension(Stile.px(120),Stile.px(32)));
        livello.addActionListener(a->{if(!silenzio){segna();e.correzione((Correzione)livello.getSelectedItem());mostra(eti,e);cambiato();}});s.riga("Correzione",livello);
        String contenuto=eti.contenuto(e,0); int lato;
        try{lato=Qr.codifica(contenuto,e.correzione()).length;}catch(RuntimeException ex){s.largo(new Stato("QR non valido",Stile.ROSSO,Stile.PESCA_SOFT));s.nota(ex.getMessage());return s;}
        double modulo=e.larghezza()/lato; s.riga("Matrice",etichettina(lato+" x "+lato)); s.riga("Modulo",etichettina(mm(modulo)));
        double q=4*modulo; boolean quiet=e.x()>=q && e.y()>=q && eti.larghezza()-e.x()-e.larghezza()>=q && eti.altezza()-e.y()-e.larghezza()>=q;
        if(modulo>=.25 && quiet) s.largo(new Stato("Ottima leggibilità",Stile.VERDE,Stile.VERDE_SOFT));
        else if(modulo<.25) {s.largo(new Stato("QR troppo piccolo",Stile.PESCA,Stile.PESCA_SOFT));s.nota("A 203 dpi usa almeno 0,25 mm per modulo.");}
        else {s.largo(new Stato("Zona bianca ridotta",Stile.PESCA,Stile.PESCA_SOFT));s.nota("Lascia piu' spazio bianco attorno al QR per lettori meno tolleranti.");}
        return s;
    }

    private Component barre(final Etichetta eti,final Elemento e){
        Scheda s=new Scheda("Qualità barcode");s.setAlignmentX(Component.LEFT_ALIGNMENT);String contenuto=eti.contenuto(e,0);
        try{int mod=Code128.moduli(contenuto);double m=e.larghezza()/mod;s.riga("Formato",etichettina("Code 128"));s.riga("Barra minima",etichettina(mm(m)));
            s.largo(new Stato(m>=.25?"Pronto per la stampa":"Barre troppo sottili",m>=.25?Stile.VERDE:Stile.PESCA,m>=.25?Stile.VERDE_SOFT:Stile.PESCA_SOFT));}
        catch(RuntimeException ex){s.largo(new Stato("Contenuto non valido",Stile.ROSSO,Stile.PESCA_SOFT));s.nota(ex.getMessage());}return s;
    }

    private Component posizione(final Etichetta eti,final Elemento e){
        Scheda s=new Scheda("Posizione");s.setAlignmentX(Component.LEFT_ALIGNMENT);s.riga("X (mm)",misura(e.x(),0,eti.larghezza(),v->e.x(v)));s.riga("Y (mm)",misura(e.y(),0,eti.altezza(),v->e.y(v)));s.riga("Larghezza",misura(e.larghezza(),1,Math.max(eti.larghezza(),eti.altezza()),v->e.larghezza(v)));
        JPanel rot=new JPanel(new FlowLayout(FlowLayout.RIGHT,Stile.px(3),0));rot.setOpaque(false);ButtonGroup gruppo=new ButtonGroup();int[] gradi={0,90,180,270};
        for(final int gr:gradi){JToggleButton b=new JToggleButton(gr+"°");b.setFont(Stile.minuscolo());b.setSelected(e.rotazione()==gr);b.setFocusPainted(false);b.setMargin(new java.awt.Insets(4,5,4,5));b.addActionListener(a->{if(!silenzio){segna();e.rotazione(gr);cambiato();}});gruppo.add(b);rot.add(b);}s.riga("Rotazione",rot);return s;
    }
    private interface Posa{void applica(double v);} private JSpinner misura(double val,double min,double max,final Posa p){final JSpinner sp=new JSpinner(new SpinnerNumberModel(val,min,max,.1));sp.setFont(Stile.normale());sp.setPreferredSize(new Dimension(Stile.px(82),Stile.px(32)));sp.addChangeListener(a->{if(!silenzio){segna();p.applica(((Number)sp.getValue()).doubleValue());cambiato();}});return sp;}
    private static JLabel etichettina(String s){JLabel l=new JLabel(s);l.setFont(Stile.normale());l.setForeground(Stile.TESTO);return l;} private static String mm(double v){return String.valueOf(Math.round(v*100)/100.0).replace('.',',')+" mm";}
    private void segna(){if(!silenzio&&prima!=null)prima.run();} private void cambiato(){if(dopo!=null)dopo.run();}

    private static final class CampoRenderer extends DefaultListCellRenderer{
        private final Etichetta eti; CampoRenderer(Etichetta e){eti=e;}
        @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean sel,boolean focus){super.getListCellRendererComponent(list,value,index,sel,focus);if(value instanceof Campo)setText(NomiDati.nome(eti,(Campo)value));return this;}
    }
    private static final class Badge extends JLabel{private final Color bg,border;Badge(String t,Color bg,Color border){super(t.toUpperCase());this.bg=bg;this.border=border;setFont(Stile.minuscolo().deriveFont(java.awt.Font.BOLD));setForeground(border);setBorder(BorderFactory.createEmptyBorder(3,7,3,7));}@Override protected void paintComponent(Graphics g){Graphics2D g2=Stile.liscio(g);try{Stile.riquadro(g2,0,0,getWidth(),getHeight(),Stile.px(7),bg,border);}finally{g2.dispose();}super.paintComponent(g);}}
    private static final class Stato extends JLabel{private final Color c,bg;Stato(String t,Color c,Color bg){super("●  "+t);this.c=c;this.bg=bg;setFont(Stile.piccolo().deriveFont(java.awt.Font.BOLD));setForeground(c);setBorder(BorderFactory.createEmptyBorder(7,9,7,9));}@Override protected void paintComponent(Graphics g){Graphics2D g2=Stile.liscio(g);try{Stile.riquadro(g2,0,0,getWidth(),getHeight(),Stile.px(8),bg,null);}finally{g2.dispose();}super.paintComponent(g);}}
}
