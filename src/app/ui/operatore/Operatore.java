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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/** La modalità operatore prepara il giro senza esporre la modifica del layout. */
public final class Operatore extends JPanel {
    private final Etichetta eti;
    private final SorgenteQr qr;
    private final Impostazioni imp;
    private final Archivio archivio;
    private final Registro registro;
    private final Runnable indietro;
    private final Runnable modifica;
    private final Anteprima anteprima;
    private final JSpinner copie=new JSpinner(new SpinnerNumberModel(12,1,100000,1));
    private final JLabel stato=new JLabel("Pronto per la stampa");
    private final JLabel dettaglio=new JLabel(" ");
    private final JLabel esito=new JLabel(" ");
    private final Bottone stampa=Bottone.primario("Stampa 12 etichette");
    private final Map<Campo,JTextField> valori=new LinkedHashMap<Campo,JTextField>();
    private final Map<Campo,JSpinner> cifre=new LinkedHashMap<Campo,JSpinner>();
    private final Map<Campo,CodiceView[]> intervalli=new LinkedHashMap<Campo,CodiceView[]>();
    private boolean aggiornando;

    public Operatore(Etichetta eti,SorgenteQr qr,Impostazioni imp,Archivio archivio,Registro registro,Runnable indietro,Runnable modifica){
        super(new BorderLayout()); this.eti=eti;this.qr=qr;this.imp=imp;this.archivio=archivio;this.registro=registro;this.indietro=indietro;this.modifica=modifica;
        setBackground(Stile.BASE); anteprima=new Anteprima(); add(testata(),BorderLayout.NORTH); add(corpo(),BorderLayout.CENTER); add(barraStato(),BorderLayout.SOUTH);
        copie.setFont(Stile.normale()); copie.addChangeListener(e->{if(!aggiornando)aggiorna();});
        stampa.addActionListener(e->mandaInStampa()); aggiorna();
    }

    public void salva(){ try{archivio.salva(eti);}catch(Exception ex){esito.setText("Salvataggio non riuscito: "+ex.getMessage());esito.setForeground(Stile.ROSSO);} }

    private javax.swing.JComponent testata(){
        JPanel p=new JPanel(new BorderLayout());p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,Stile.S0),BorderFactory.createEmptyBorder(Stile.px(12),Stile.px(18),Stile.px(12),Stile.px(18))));
        JPanel sx=new JPanel(new FlowLayout(FlowLayout.LEFT,Stile.px(12),0));sx.setOpaque(false);Bottone back=Bottone.piatto("‹  Vetrina");back.addActionListener(e->indietro.run());sx.add(back);
        JPanel nome=new JPanel();nome.setOpaque(false);nome.setLayout(new BoxLayout(nome,BoxLayout.Y_AXIS));JLabel t=new JLabel(eti.nome());t.setFont(Stile.titolo());t.setForeground(Stile.TESTO);JLabel m=new JLabel(num(eti.larghezza())+" × "+num(eti.altezza())+" mm");m.setFont(Stile.piccolo());m.setForeground(Stile.OV1);nome.add(t);nome.add(m);sx.add(nome);p.add(sx,BorderLayout.WEST);
        JPanel dx=new JPanel(new FlowLayout(FlowLayout.RIGHT,Stile.px(7),0));dx.setOpaque(false);Bottone settings=Bottone.piatto("⚙  Impostazioni");settings.addActionListener(e->Finestre.impostazioni(this,imp));Bottone edit=Bottone.normale("Modifica layout");edit.addActionListener(e->modifica.run());dx.add(settings);dx.add(edit);p.add(dx,BorderLayout.EAST);return p;
    }

    private javax.swing.JComponent corpo(){
        JPanel p=new JPanel(new GridBagLayout());p.setBackground(Stile.BASE);p.setBorder(BorderFactory.createEmptyBorder(Stile.px(18),Stile.px(18),Stile.px(18),Stile.px(18)));
        GridBagConstraints a=new GridBagConstraints();a.gridx=0;a.gridy=0;a.weightx=.62;a.weighty=1;a.fill=GridBagConstraints.BOTH;a.insets=new Insets(0,0,0,Stile.px(18));
        JPanel previewWrap=new JPanel(new BorderLayout());previewWrap.setBackground(Stile.BANCO);previewWrap.setBorder(BorderFactory.createLineBorder(Stile.S0));previewWrap.add(anteprima,BorderLayout.CENTER);p.add(previewWrap,a);
        GridBagConstraints b=new GridBagConstraints();b.gridx=1;b.gridy=0;b.weightx=.38;b.weighty=1;b.fill=GridBagConstraints.BOTH;p.add(controlli(),b);return p;
    }

    private javax.swing.JComponent controlli(){
        JPanel col=new JPanel();col.setBackground(Stile.BASE);col.setLayout(new BorderLayout());
        JPanel header=new JPanel();header.setOpaque(false);header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));JLabel t=new JLabel("Prepara il giro");t.setFont(Stile.titolo());t.setForeground(Stile.TESTO);t.setAlignmentX(Component.LEFT_ALIGNMENT);header.add(t);JLabel sub=new JLabel("Controlla i dati. Il layout resta protetto.");sub.setFont(Stile.piccolo());sub.setForeground(Stile.SUB0);sub.setAlignmentX(Component.LEFT_ALIGNMENT);header.add(sub);header.setBorder(BorderFactory.createEmptyBorder(0,0,Stile.px(12),0));col.add(header,BorderLayout.NORTH);
        JPanel lista=new JPanel();lista.setOpaque(false);lista.setLayout(new BoxLayout(lista,BoxLayout.Y_AXIS));valori.clear();cifre.clear();intervalli.clear();for(Campo c:eti.campiUsati()){lista.add(schedaDato(c));lista.add(javax.swing.Box.createVerticalStrut(Stile.px(10)));}
        JScrollPane sp=new JScrollPane(lista);sp.setBorder(BorderFactory.createEmptyBorder());sp.getViewport().setBackground(Stile.BASE);sp.getVerticalScrollBar().setUnitIncrement(Stile.px(18));col.add(sp,BorderLayout.CENTER);
        JPanel bottom=new JPanel();bottom.setOpaque(false);bottom.setLayout(new BoxLayout(bottom,BoxLayout.Y_AXIS));bottom.setBorder(BorderFactory.createEmptyBorder(Stile.px(12),0,0,0));bottom.add(campo("Copie",copie));bottom.add(javax.swing.Box.createVerticalStrut(Stile.px(10)));
        JPanel pre=new JPanel(new BorderLayout());pre.setBackground(Stile.VERDE_SOFT);pre.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Stile.VERDE_BORDO),BorderFactory.createEmptyBorder(Stile.px(10),Stile.px(12),Stile.px(10),Stile.px(12))));stato.setFont(Stile.forte());stato.setForeground(Stile.VERDE);dettaglio.setFont(Stile.piccolo());dettaglio.setForeground(Stile.SUB0);pre.add(stato,BorderLayout.NORTH);pre.add(dettaglio,BorderLayout.SOUTH);bottom.add(pre);bottom.add(javax.swing.Box.createVerticalStrut(Stile.px(10)));stampa.setAlignmentX(Component.LEFT_ALIGNMENT);stampa.setMaximumSize(new Dimension(Integer.MAX_VALUE,Stile.px(42)));bottom.add(stampa);Bottone exp=Bottone.normale("Esporta…");exp.setAlignmentX(Component.LEFT_ALIGNMENT);exp.setMaximumSize(new Dimension(Integer.MAX_VALUE,Stile.px(36)));exp.addActionListener(e->Finestre.esporta(this,eti,qr,quanteCopie()));bottom.add(javax.swing.Box.createVerticalStrut(Stile.px(6)));bottom.add(exp);col.add(bottom,BorderLayout.SOUTH);return col;
    }

    private javax.swing.JComponent schedaDato(final Campo c){
        JPanel card=new JPanel(new GridBagLayout());card.setBackground(Color.WHITE);Color accent=colore(c);card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,Stile.px(3),0,0,accent),BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Stile.S0),BorderFactory.createEmptyBorder(Stile.px(10),Stile.px(12),Stile.px(10),Stile.px(12)))));
        GridBagConstraints x=new GridBagConstraints();x.gridx=0;x.gridy=0;x.gridwidth=2;x.weightx=1;x.fill=GridBagConstraints.HORIZONTAL;x.anchor=GridBagConstraints.WEST;JLabel title=new JLabel(NomiDati.nome(eti,c));title.setFont(Stile.forte());title.setForeground(Stile.TESTO);card.add(title,x);
        x.gridy++;JLabel meta=new JLabel(NomiDati.comportamento(c)+"  ·  "+NomiDati.tipoUso(eti,c)+"  ·  "+NomiDati.uso(eti,c));meta.setFont(Stile.minuscolo());meta.setForeground(Stile.OV1);x.insets=new Insets(2,0,Stile.px(8),0);card.add(meta,x);
        x.insets=new Insets(0,0,0,0);x.gridwidth=1;x.gridy++;x.gridx=0;x.weightx=.35;JLabel lab=new JLabel(c.comportamento()==Comportamento.PROGRESSIVO?"Codice iniziale":"Valore");lab.setFont(Stile.piccolo());lab.setForeground(Stile.SUB0);card.add(lab,x);
        JTextField v=new JTextField(c.serie()!=null?c.serie().codice(c.serie().prossimo()):c.valore());v.setFont(Stile.normale());valori.put(c,v);x.gridx=1;x.weightx=.65;x.fill=GridBagConstraints.HORIZONTAL;card.add(v,x);v.addActionListener(e->{cambiaDati();aggiorna();});v.addFocusListener(new FocusAdapter(){@Override public void focusLost(FocusEvent e){cambiaDati();aggiorna();}});
        if(c.comportamento()==Comportamento.PROGRESSIVO){
            x.gridy++;x.gridx=0;x.weightx=.35;x.fill=GridBagConstraints.NONE;JLabel cl=new JLabel("Cifre mobili");cl.setFont(Stile.piccolo());cl.setForeground(Stile.SUB0);card.add(cl,x);
            int n=c.serie()==null?3:c.serie().cifre();JSpinner cs=new JSpinner(new SpinnerNumberModel(n,1,9,1));cs.setFont(Stile.normale());cifre.put(c,cs);x.gridx=1;x.fill=GridBagConstraints.HORIZONTAL;card.add(cs,x);cs.addChangeListener(e->{if(!aggiornando){cambiaDati();aggiorna();}});
            JPanel range=new JPanel(new FlowLayout(FlowLayout.LEFT,Stile.px(5),0));range.setOpaque(false);CodiceView da=new CodiceView("","");CodiceView a=new CodiceView("","");da.corpo(11);a.corpo(11);intervalli.put(c,new CodiceView[]{da,a});range.add(da);JLabel arrow=new JLabel("→");arrow.setForeground(Stile.OV1);range.add(arrow);range.add(a);x.gridy++;x.gridx=0;x.gridwidth=2;x.weightx=1;x.fill=GridBagConstraints.HORIZONTAL;x.insets=new Insets(Stile.px(7),0,0,0);card.add(range,x);
        }
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,card.getPreferredSize().height));return card;
    }

    private void cambiaDati(){ if(aggiornando)return; for(Map.Entry<Campo,JTextField> e:valori.entrySet()){Campo c=e.getKey();String v=e.getValue().getText().trim();try{if(c.comportamento()==Comportamento.PROGRESSIVO){JSpinner sp=cifre.get(c);int n=sp==null?(c.serie()==null?3:c.serie().cifre()):((Number)sp.getValue()).intValue();c.serie(new Serie(v,n));}else c.valore(v);}catch(RuntimeException ex){esito.setText(NomiDati.nome(eti,c)+": "+ex.getMessage());esito.setForeground(Stile.ROSSO);}} anteprima.repaint(); }
    private int quanteCopie(){return ((Number)copie.getValue()).intValue();}

    private void aggiorna(){ if(aggiornando)return;aggiornando=true;try{cambiaDati();int n=quanteCopie();stampa.setText("Stampa "+n+(n==1?" etichetta":" etichette"));String errore=null;try{eti.validaGiro(n);}catch(RuntimeException ex){errore=ex.getMessage();}
        for(Map.Entry<Campo,CodiceView[]> e:intervalli.entrySet()){Campo c=e.getKey();if(c.serie()==null)continue;Serie s=c.serie();try{String[] g=s.giro(n);e.getValue()[0].testo(s.prefisso(),s.finestra(s.prossimo()));String last=g[g.length-1];e.getValue()[1].testo(s.prefisso(),last.substring(Math.min(s.prefisso().length(),last.length())));}catch(RuntimeException ex){errore=ex.getMessage();}}
        if(errore==null){stato.setText("✓  Pronto per la stampa");stato.setForeground(Stile.VERDE);dettaglio.setText(eti.campiUsati().size()+" dati · "+eti.progressivi().size()+" progressivi · formato "+num(eti.larghezza())+" × "+num(eti.altezza())+" mm");stampa.setEnabled(true);}else{stato.setText("⚠  Controlla i dati");stato.setForeground(Stile.PESCA);dettaglio.setText(errore);stampa.setEnabled(false);}anteprima.repaint();}finally{aggiornando=false;}}

    private void mandaInStampa(){cambiaDati();int n=quanteCopie();try{eti.validaGiro(n);String[] codici=eti.codiciGiro(n);StampaGiro job=new StampaGiro(eti,qr,n);if(!job.manda(eti.nome())){esito.setText("Stampa annullata · nessun progressivo avanzato");esito.setForeground(Stile.SUB0);return;}eti.consumaProgressivi(n);archivio.salva(eti);registro.annota(eti,codici,imp.stampante());sincronizzaCampiDaModello();aggiorna();esito.setText("✓ Giro stampato e registrato");esito.setForeground(Stile.VERDE);}catch(PrinterException ex){esito.setText("Stampante: "+ex.getMessage());esito.setForeground(Stile.ROSSO);}catch(Exception ex){esito.setText("Stampa non completata: "+ex.getMessage());esito.setForeground(Stile.ROSSO);}}
    private void sincronizzaCampiDaModello(){aggiornando=true;try{for(Map.Entry<Campo,JTextField> e:valori.entrySet())e.getValue().setText(e.getKey().serie()!=null?e.getKey().serie().codice(e.getKey().serie().prossimo()):e.getKey().valore());}finally{aggiornando=false;}}

    private javax.swing.JComponent campo(String nome,javax.swing.JComponent c){JPanel p=new JPanel(new BorderLayout(Stile.px(10),0));p.setOpaque(false);JLabel l=new JLabel(nome);l.setFont(Stile.piccolo());l.setForeground(Stile.SUB0);p.add(l,BorderLayout.WEST);p.add(c,BorderLayout.CENTER);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,Stile.px(36)));return p;}
    private javax.swing.JComponent barraStato(){JPanel p=new JPanel(new BorderLayout());p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,Stile.S0),BorderFactory.createEmptyBorder(Stile.px(7),Stile.px(18),Stile.px(7),Stile.px(18))));JLabel mode=new JLabel("MODALITA' OPERATORE  ·  layout protetto");mode.setFont(Stile.minuscolo());mode.setForeground(Stile.OV1);esito.setFont(Stile.piccolo());esito.setHorizontalAlignment(SwingConstants.RIGHT);p.add(mode,BorderLayout.WEST);p.add(esito,BorderLayout.EAST);return p;}
    private static Color colore(Campo c){if(c.comportamento()==Comportamento.PROGRESSIVO)return Stile.PESCA;if(c.comportamento()==Comportamento.CHIESTO)return Stile.LAVANDA;return Stile.CELESTE;}
    private static String num(double v){return String.valueOf(Math.round(v*10)/10.0).replace(".0","").replace('.',',');}

    private final class Anteprima extends javax.swing.JComponent{
        @Override public Dimension getPreferredSize(){return new Dimension(Stile.px(620),Stile.px(480));}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=Stile.liscio(g);try{g2.setColor(Stile.BANCO);g2.fillRect(0,0,getWidth(),getHeight());double sx=(getWidth()-Stile.px(80))/eti.larghezza();double sy=(getHeight()-Stile.px(90))/eti.altezza();double mm=Math.max(.1,Math.min(sx,sy));int w=(int)Math.round(eti.larghezza()*mm),h=(int)Math.round(eti.altezza()*mm);int x=(getWidth()-w)/2,y=(getHeight()-h)/2;g2.setColor(new Color(0,0,0,40));g2.fillRect(x+Stile.px(5),y+Stile.px(6),w,h);g2.setColor(Color.WHITE);g2.fillRect(x,y,w,h);g2.setColor(Stile.S1);g2.drawRect(x,y,w,h);g2.translate(x,y);Disegno.disegna(g2,eti,mm,qr,0);}finally{g2.dispose();}}
    }
}
