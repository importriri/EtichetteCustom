package app.render;

import java.util.ArrayList;
import java.util.List;

/** Impaginazione equilibrata dei codici lunghi. */
public final class Testo {
    private Testo(){}
    private static final String SEPARATORI=" _-./:;,";
    public static final class Esito{private final String[] righe;private final double corpo;private final boolean rimpicciolito;Esito(String[] r,double c,boolean x){righe=r;corpo=c;rimpicciolito=x;}public String[] righe(){return righe;}public double corpo(){return corpo;}public boolean rimpicciolito(){return rimpicciolito;}public int quanteRighe(){return righe.length;}}
    public static Esito componi(String t,double w,double c,int max,boolean b,Misuratore m){return componi(t,w,c,max,0,b,m);}
    public static Esito componi(String t,double w,double c,int max,int preferite,boolean b,Misuratore m){if(t==null)t="";if(w<=0||c<=0)throw new IllegalArgumentException("larghezza e corpo devono essere positivi");max=Math.max(1,Math.min(3,max));preferite=Math.max(0,Math.min(max,preferite));double min=c*.4;for(double corpo=c;corpo+1e-4>=min;corpo-=c*.05){if(preferite>0){String[] r=bilancia(t,preferite,w,corpo,b,m,true);if(r!=null)return new Esito(r,arrotonda(corpo),corpo<c-1e-4);}else{Candidato best=null;for(int n=1;n<=max;n++){String[] r=bilancia(t,n,w,corpo,b,m,true);if(r==null)continue;double s=score(r,corpo,b,m)+n*.08;if(best==null||s<best.score)best=new Candidato(r,s);}if(best!=null)return new Esito(best.righe,arrotonda(corpo),corpo<c-1e-4);}}int n=preferite>0?preferite:max;return new Esito(senzaVincolo(t,n),arrotonda(min),true);}
    static String[] dividi(String t,int righe,double w,double c,boolean b,Misuratore m){int max=Math.max(1,Math.min(3,righe));for(int n=1;n<=max;n++){String[] r=bilancia(t,n,w,c,b,m,true);if(r!=null)return r;}return null;}
    private static String[] bilancia(String t,int n,double w,double c,boolean bold,Misuratore m,boolean limita){if(t.isEmpty())return new String[]{""};if(n<=1)return !limita||m.larghezza(t,c,bold)<=w?new String[]{t}:null;if(t.length()<n)return null;Taglio best=null;if(n==2){for(int a=1;a<t.length();a++){String[] r={t.substring(0,a),t.substring(a)};Taglio x=valuta(r,new int[]{a},w,c,bold,m,limita,t);if(x!=null&&(best==null||x.score<best.score))best=x;}}else{for(int a=1;a<t.length()-1;a++)for(int z=a+1;z<t.length();z++){String[] r={t.substring(0,a),t.substring(a,z),t.substring(z)};Taglio x=valuta(r,new int[]{a,z},w,c,bold,m,limita,t);if(x!=null&&(best==null||x.score<best.score))best=x;}}return best==null?null:best.righe;}
    private static Taglio valuta(String[] g,int[] tagli,double max,double corpo,boolean bold,Misuratore m,boolean limita,String orig){String[] r=new String[g.length];double[] ws=new double[g.length];double sum=0;for(int i=0;i<g.length;i++){r[i]=trim(g[i]);if(r[i].isEmpty())return null;ws[i]=m.larghezza(r[i],corpo,bold);if(limita&&ws[i]>max+1e-4)return null;sum+=ws[i];}double avg=sum/r.length,s=0;for(double x:ws){double d=x-avg;s+=d*d;}for(int cut:tagli)s+=naturale(orig,cut)?-2:25;return new Taglio(r,s);}
    private static boolean naturale(String s,int p){return p>0&&SEPARATORI.indexOf(s.charAt(p-1))>=0||p<s.length()&&SEPARATORI.indexOf(s.charAt(p))>=0;}
    private static String trim(String s){int a=0,z=s.length();while(a<z&&s.charAt(a)==' ')a++;while(z>a&&s.charAt(z-1)==' ')z--;return s.substring(a,z);}
    private static String[] senzaVincolo(String t,int n){if(t==null||t.isEmpty())return new String[]{""};n=Math.max(1,Math.min(3,Math.min(n,t.length())));if(n==1)return new String[]{t};List<String> out=new ArrayList<String>();int da=0;for(int r=n;r>1;r--){int rest=t.length()-da,ideale=da+(int)Math.round(rest/(double)r),cut=vicino(t,da+1,t.length()-(r-1),ideale);out.add(trim(t.substring(da,cut)));da=cut;}out.add(trim(t.substring(da)));return out.toArray(new String[out.size()]);}
    private static int vicino(String s,int min,int max,int ideale){int best=Math.max(min,Math.min(max,ideale)),dist=Integer.MAX_VALUE;for(int i=min;i<=max;i++)if(naturale(s,i)&&Math.abs(i-ideale)<dist){best=i;dist=Math.abs(i-ideale);}return best;}
    private static double score(String[] r,double c,boolean b,Misuratore m){double sum=0;double[] ws=new double[r.length];for(int i=0;i<r.length;i++){ws[i]=m.larghezza(r[i],c,b);sum+=ws[i];}double avg=sum/r.length,s=0;for(double x:ws){double d=x-avg;s+=d*d;}return s;}
    private static double arrotonda(double v){return Math.round(v*100.0)/100.0;}
    private static final class Candidato{final String[] righe;final double score;Candidato(String[] r,double s){righe=r;score=s;}}
    private static final class Taglio{final String[] righe;final double score;Taglio(String[] r,double s){righe=r;score=s;}}
}
