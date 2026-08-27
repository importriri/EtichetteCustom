package prove;

import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Tipo;
import app.render.QrVero;
import app.stile.Stile;
import app.ui.banco.Foglio;
import app.ui.banco.Proprieta;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Audit grafico off-screen con PNG conservati come artifact CI. */
public final class ProvaEditorGrafico {
    private static int ok;
    private static int ko;
    private static int size;

    private ProvaEditorGrafico() { }

    public static void main(String[] args) throws Exception {
        size = Integer.parseInt(args.length == 0 ? "12" : args[0]);
        Stile.installaLookAndFeel();
        UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, size));
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                try { eseguiAudit(); } catch (Exception ex) { throw new RuntimeException(ex); }
            }
        });
        System.out.println(ok + " editor graphics checks, " + ko + " failed at base " + size);
        if (ko != 0) System.exit(1);
    }

    private static void eseguiAudit() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta eti = new Etichetta("Audit Windows", 50, 30);
        Campo c = new Campo("codice", Comportamento.FISSO,
                "210150.022_02-01.262350009");
        eti.aggiungi(c);
        Elemento qr = new Elemento("QR", Tipo.QR, "codice", 3, 2, 14);
        Elemento text = new Elemento("Testo", Tipo.CODICE, "codice", 3, 19, 44)
                .corpo(4.2, true);
        text.mostraSeparatori(false);
        eti.aggiungi(qr);
        eti.aggiungi(text);

        Proprieta p = new Proprieta(new Runnable(){public void run(){}},
                new Runnable(){public void run(){}});
        p.mostra(eti, text);
        p.setSize(new Dimension(Stile.px(330), Stile.px(820)));
        layout(p);
        List<Component> componenti = new ArrayList<Component>();
        collectAll(p, componenti);
        check("primary editor contains no JSpinner", conta(componenti, JSpinner.class) == 0);
        check("text alignment exposes Centro", trovaToggle(componenti, "Centro") != null);
        check("text alignment exposes Sinistra", trovaToggle(componenti, "Sinistra") != null);
        check("text alignment exposes Destra", trovaToggle(componenti, "Destra") != null);
        check("text rows expose Auto", trovaToggle(componenti, "Auto") != null);
        check("text rows expose 3", trovaToggle(componenti, "3") != null);
        check("separator visibility is directly exposed",
                trovaCheck(componenti, "Mostra punti e simboli") != null);
        check("right alignment stays inside inspector",
                dentroOrizzontale(p, trovaToggle(componenti, "Destra")));
        check("third row choice stays inside inspector",
                dentroOrizzontale(p, trovaToggle(componenti, "3")));
        check("270 degree choice stays inside inspector",
                dentroOrizzontale(p, trovaToggle(componenti, "270°")));
        int campiPrima = conta(componenti, JTextField.class);
        check("precision fields stay hidden by default", campiPrima <= 2);

        JToggleButton centro = trovaToggle(componenti, "Centro");
        centro.doClick();
        check("center button updates model", text.allineamento() == 1);
        JToggleButton tre = trovaToggle(componenti, "3");
        tre.doClick();
        check("three-line button updates model", text.righePreferite() == 3);

        JCheckBox simboli = trovaCheck(componenti, "Mostra punti e simboli");
        if (!simboli.isSelected()) simboli.doClick();
        simboli.doClick();
        check("separator toggle updates only presentation", !text.mostraSeparatori());
        check("source code remains exact",
                "210150.022_02-01.262350009".equals(eti.contenuto(text, 0)));

        p.mostra(eti, text);
        p.setSize(new Dimension(Stile.px(330), Stile.px(820)));
        layout(p);
        componenti.clear();
        collectAll(p, componenti);
        JToggleButton precisione = trovaToggle(componenti, "Misure precise");
        check("precision control exists", precisione != null);
        if (precisione != null) precisione.doClick();
        layout(p);
        componenti.clear();
        collectAll(p, componenti);
        check("precision fields appear only on request",
                conta(componenti, JTextField.class) > campiPrima);
        check("precision controls do not clip horizontally",
                tuttiDentroOrizzontale(p, componenti));
        salva(p, "inspector");

        Foglio f = new Foglio(eti, new QrVero());
        f.percentuale(180);
        f.setSize(new Dimension(780, 520));
        f.selezione(qr);
        salva(f, "editor");
        BufferedImage canvas = immagine(f);
        check("grid produces visible non-white structure", varietaCarta(canvas) > 8);

        Method oxm = Foglio.class.getDeclaredMethod("originaX");
        Method oym = Foglio.class.getDeclaredMethod("originaY");
        oxm.setAccessible(true);
        oym.setAccessible(true);
        int ox = ((Integer) oxm.invoke(f)).intValue();
        int oy = ((Integer) oym.invoke(f)).intValue();
        double z = f.zoom();
        int hx = ox + (int) Math.round((qr.x() + qr.larghezza()) * z) + 2;
        int hy = oy + (int) Math.round((qr.y() + qr.larghezza()) * z) + 2;
        double prima = qr.larghezza();
        f.dispatchEvent(new MouseEvent(f, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, hx, hy, 1, false));
        f.dispatchEvent(new MouseEvent(f, MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(), 0, hx + (int)Math.round(5*z),
                hy + (int)Math.round(5*z), 0, false));
        f.dispatchEvent(new MouseEvent(f, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, hx + (int)Math.round(5*z),
                hy + (int)Math.round(5*z), 1, false));
        check("corner drag resizes QR directly", qr.larghezza() > prima + 2.0);
    }

    private static BufferedImage immagine(Component c) {
        BufferedImage im = new BufferedImage(Math.max(1,c.getWidth()),
                Math.max(1,c.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        try { c.paint(g); } finally { g.dispose(); }
        return im;
    }

    private static void salva(Component c, String nome) throws Exception {
        File dir = new File("build/ui-audit");
        if (!dir.isDirectory() && !dir.mkdirs())
            throw new IllegalStateException("non posso creare " + dir);
        String os = System.getProperty("os.name", "os").toLowerCase()
                .replaceAll("[^a-z0-9]+", "-");
        ImageIO.write(immagine(c), "png",
                new File(dir, os + "-" + nome + "-" + size + ".png"));
    }

    private static int varietaCarta(BufferedImage im) {
        java.util.HashSet<Integer> colori = new java.util.HashSet<Integer>();
        int x0=im.getWidth()/4, x1=im.getWidth()*3/4;
        int y0=im.getHeight()/4, y1=im.getHeight()*3/4;
        for(int y=y0;y<y1;y+=2) for(int x=x0;x<x1;x+=2)
            colori.add(Integer.valueOf(im.getRGB(x,y)));
        return colori.size();
    }

    private static boolean dentroOrizzontale(Container root, Component c) {
        if (c == null || c.getWidth() <= 0 || c.getHeight() <= 0) return false;
        Point pt = SwingUtilities.convertPoint(c, 0, 0, root);
        return pt.x >= 0 && pt.x + c.getWidth() <= root.getWidth();
    }

    private static boolean tuttiDentroOrizzontale(Container root, List<Component> all) {
        for (Component c : all) {
            if (c.isVisible() && c.getWidth() > 0 && c.getHeight() > 0
                    && !dentroOrizzontale(root, c)) return false;
        }
        return true;
    }

    private static void layout(Container c) {
        c.doLayout();
        for(Component x:c.getComponents()) if(x instanceof Container) layout((Container)x);
    }
    private static void collectAll(Container root,List<Component> out) {
        for(Component c:root.getComponents()) {
            out.add(c); if(c instanceof Container) collectAll((Container)c,out);
        }
    }
    private static int conta(List<Component> all,Class<?> tipo) {
        int n=0; for(Component c:all) if(tipo.isInstance(c)) n++; return n;
    }
    private static JToggleButton trovaToggle(List<Component> all,String testo) {
        for(Component c:all) if(c instanceof JToggleButton
                && testo.equals(((JToggleButton)c).getText())) return (JToggleButton)c;
        return null;
    }
    private static JCheckBox trovaCheck(List<Component> all,String testo) {
        for(Component c:all) if(c instanceof JCheckBox
                && testo.equals(((JCheckBox)c).getText())) return (JCheckBox)c;
        return null;
    }
    private static void check(String name,boolean pass) {
        if(pass){ok++;System.out.println("  ok   "+name);}
        else{ko++;System.out.println("  FAIL "+name);}
    }
}
