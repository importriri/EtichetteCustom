package app.render;

import app.codice.Code128;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.stile.Stile;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;

/** Unico renderer condiviso da anteprima, export e stampa. */
public final class Disegno {
    private Disegno(){}
    private static final String[] PREFERITE={"DejaVu Sans","Verdana","Tahoma","Arial"};
    private static String famiglia;
    public static synchronized String famiglia(){if(famiglia==null){famiglia="SansSerif";try{String[] presenti=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();for(int i=0;i<PREFERITE.length&&famiglia.equals("SansSerif");i++)for(String p:presenti)if(p.equalsIgnoreCase(PREFERITE[i])){famiglia=p;break;}}catch(Throwable t){famiglia="SansSerif";}}return famiglia;}
    public static void disegna(Graphics2D g,Etichetta eti,double mmPx,SorgenteQr qr,int copia){int w=(int)Math.round(eti.larghezza()*mmPx),h=(int)Math.round(eti.altezza()*mmPx);g.setColor(Stile.CARTA);g.fillRect(0,0,w,h);for(Elemento e:eti.elementi()){Graphics2D g2=(Graphics2D)g.create();try{java.awt.geom.Rectangle2D.Double base=Ingombri.base(g2,eti,e,mmPx,copia);g2.translate(e.x()*mmPx,e.y()*mmPx);switch(e.rotazione()){case 90:g2.translate(base.height*mmPx,0);g2.rotate(Math.PI/2);break;case 180:g2.translate(base.width*mmPx,base.height*mmPx);g2.rotate(Math.PI);break;case 270:g2.translate(0,base.width*mmPx);g2.rotate(3*Math.PI/2);break;default:break;}disegnaElemento(g2,eti,e,mmPx,qr,copia);}finally{g2.dispose();}}}
    private static void disegnaElemento(Graphics2D g,Etichetta eti,Elemento e,double mmPx,SorgenteQr qr,int copia){g.setColor(Stile.INCHIOSTRO);switch(e.tipo()){case QR:disegnaQr(g,qr,eti.contenuto(e,copia),e.correzione(),0,0,e.larghezza()*mmPx);break;case BARCODE:disegnaBarcode(g,eti.contenuto(e,copia),0,0,e.larghezza()*mmPx,e.altezza()*mmPx);break;case LINEA:g.fillRect(0,0,(int)Math.round(e.larghezza()*mmPx),Math.max(1,(int)Math.round(e.altezza()*mmPx)));break;default:disegnaScritta(g,eti,e,mmPx,copia);break;}}
    private static void disegnaScritta(Graphics2D g,Etichetta eti,Elemento e,double mmPx,int copia){String testo=testoVisuale(eti.contenuto(e,copia),e.mostraSeparatori());Misuratore m=misuratore(g,mmPx);Testo.Esito esito=Testo.componi(testo,e.larghezza(),e.corpo(),e.massimoRighe(),e.righePreferite(),e.grassetto(),m);Font f=font(esito.corpo()*mmPx,e.grassetto());g.setFont(f);FontMetrics fm=g.getFontMetrics();double inter=fm.getAscent()+fm.getDescent(),linea=fm.getAscent(),box=e.larghezza()*mmPx;for(String riga:esito.righe()){int rw=fm.stringWidth(riga);double x=0;if(e.allineamento()==1)x=Math.max(0,(box-rw)/2);else if(e.allineamento()==2)x=Math.max(0,box-rw);g.drawString(riga,(int)Math.round(x),(int)Math.round(linea));linea+=inter;}}
    static String testoVisuale(String raw,boolean mostra){if(mostra)return raw==null?"":raw;if(raw==null||raw.isEmpty())return "";StringBuilder b=new StringBuilder(raw.length());boolean spazio=false;for(int i=0;i<raw.length();i++){char c=raw.charAt(i);if(c=='.'||c=='_'||c=='-'||c=='/'||c==':'||c==';'||c==','){if(b.length()>0&&!spazio){b.append(' ');spazio=true;}}else{b.append(c);spazio=false;}}while(b.length()>0&&b.charAt(b.length()-1)==' ')b.setLength(b.length()-1);return b.toString();}
    private static void disegnaQr(Graphics2D g,SorgenteQr qr,String contenuto,app.codice.Correzione livello,double x,double y,double lato){if(qr==null)return;boolean[][] m=qr.matrice(contenuto,livello);if(m==null||m.length==0)return;int n=m.length;double passo=lato/n;for(int r=0;r<n;r++){int c=0;while(c<n){if(m[r][c]){int larghi=1;while(c+larghi<n&&m[r][c+larghi])larghi++;int px=(int)Math.round(x+c*passo),py=(int)Math.round(y+r*passo),pw=(int)Math.round(x+(c+larghi)*passo)-px,ph=(int)Math.round(y+(r+1)*passo)-py;g.fillRect(px,py,Math.max(1,pw),Math.max(1,ph));c+=larghi;}else c++;}}}
    private static void disegnaBarcode(Graphics2D g,String contenuto,double x,double y,double larghezza,double altezza){int[] tratti;try{tratti=Code128.tratti(contenuto);}catch(RuntimeException ex){g.drawRect((int)Math.round(x),(int)Math.round(y),(int)Math.round(larghezza),(int)Math.round(altezza));return;}int totale=0;for(int t:tratti)totale+=t;double u=larghezza/totale,cx=x;for(int i=0;i<tratti.length;i++){if(i%2==0)g.fillRect((int)Math.round(cx),(int)Math.round(y),Math.max(1,(int)Math.round(cx+tratti[i]*u)-(int)Math.round(cx)),(int)Math.round(altezza));cx+=tratti[i]*u;}}
    public static double moduloBarcodeMm(String contenuto,double larghezzaMm){int totale=0;for(int t:Code128.tratti(contenuto))totale+=t;return larghezzaMm/totale;}
    public static Font font(double corpoPx,boolean grassetto){return new Font(famiglia(),grassetto?Font.BOLD:Font.PLAIN,1).deriveFont((float)Math.max(1,corpoPx));}
    public static Misuratore misuratore(final Graphics2D g,final double mmPx){return new Misuratore(){@Override public double larghezza(String testo,double corpoMm,boolean grassetto){return g.getFontMetrics(font(corpoMm*mmPx,grassetto)).stringWidth(testo)/mmPx;}};}
}
