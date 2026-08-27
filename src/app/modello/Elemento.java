package app.modello;

import app.codice.Correzione;

/** Un elemento posato sulla carta; tutte le misure sono in millimetri. */
public class Elemento {
    private String nome;
    private final Tipo tipo;
    private String campo;
    private double x,y,larghezza,altezza;
    private double corpo=3.0;
    private boolean grassetto;
    private int rotazione;
    private int massimoRighe=3;
    private Correzione correzione=Correzione.M;
    private int allineamento;
    private boolean mostraSeparatori=true;
    private int righePreferite; // 0 auto, 1..3 numero desiderato

    public Elemento(String nome,Tipo tipo,String campo,double x,double y,double larghezza){this.nome=nome;this.tipo=tipo;this.campo=campo;this.x=x;this.y=y;this.larghezza=larghezza;this.altezza=larghezza;}
    public String nome(){return nome;} public void nome(String v){nome=v;}
    public Tipo tipo(){return tipo;}
    public String campo(){return campo;} public void campo(String v){campo=v;}
    public double x(){return x;} public void x(double v){x=arrotonda(v);}
    public double y(){return y;} public void y(double v){y=arrotonda(v);}
    public double larghezza(){return larghezza;} public void larghezza(double v){larghezza=Math.max(1.0,arrotonda(v));}
    public double altezza(){return altezza;} public void altezza(double v){altezza=Math.max(.4,arrotonda(v));}
    public double corpo(){return corpo;} public void corpo(double v){corpo=Math.max(1.0,arrotonda(v));}
    public boolean grassetto(){return grassetto;} public void grassetto(boolean v){grassetto=v;}
    public int rotazione(){return rotazione;}
    public void rotazione(int gradi){int g=((gradi%360)+360)%360;if(g!=0&&g!=90&&g!=180&&g!=270)throw new IllegalArgumentException("rotazione ammessa solo a 0, 90, 180, 270: "+gradi);rotazione=g;}
    public Correzione correzione(){return correzione;} public void correzione(Correzione c){correzione=c==null?Correzione.M:c;}
    public int massimoRighe(){return massimoRighe;} public void massimoRighe(int v){massimoRighe=Math.max(1,Math.min(3,v));if(righePreferite>massimoRighe)righePreferite=massimoRighe;}
    public int righePreferite(){return righePreferite;} public void righePreferite(int v){righePreferite=Math.max(0,Math.min(3,v));if(righePreferite>massimoRighe)massimoRighe=righePreferite;}
    public int allineamento(){return allineamento;} public void allineamento(int v){allineamento=Math.max(0,Math.min(2,v));}
    public boolean mostraSeparatori(){return mostraSeparatori;} public void mostraSeparatori(boolean v){mostraSeparatori=v;}
    public Elemento corpo(double v,boolean bold){corpo(v);grassetto(bold);return this;}
    public Elemento altezzaDi(double v){altezza(v);return this;}
    public Elemento presenta(int allineamento,boolean separatori,int righe){allineamento(allineamento);mostraSeparatori(separatori);massimoRighe(righe);return this;}
    public Elemento copia(){Elemento c=new Elemento(nome,tipo,campo,x,y,larghezza);c.altezza(altezza);c.corpo(corpo);c.grassetto(grassetto);c.rotazione(rotazione);c.massimoRighe(massimoRighe);c.righePreferite(righePreferite);c.correzione(correzione);c.allineamento(allineamento);c.mostraSeparatori(mostraSeparatori);return c;}
    private static double arrotonda(double v){return Math.round(v*10.0)/10.0;}
    @Override public String toString(){return nome+" ("+tipo+")";}
}
