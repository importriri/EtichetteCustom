package prove;

import app.modello.*;
import app.stile.Stile;
import app.ui.banco.Proprieta;
import app.ui.finestre.Finestre;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

public final class ProvaScala {
    private static int ok;
    private static int ko;
    private static int size;

    public static void main(String[] args) throws Exception {
        size = Integer.parseInt(args.length == 0 ? "15" : args[0]);
        UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, size));
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try { ProvaScala.run(); } catch (Exception e) { throw new RuntimeException(e); }
            }
        });
        System.out.println(ok + " scale checks, " + ko + " failed at " + size + "px base");
        if (ko != 0) System.exit(1);
    }

    private static void run() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta e = new Etichetta("Etichetta lunga per audit DPI", 50, 30);
        Campo a = new Campo("codice", Comportamento.PROGRESSIVO, "SERIE-CLIENTE-LUNGHISSIMA-000001");
        a.serie(new Serie("SERIE-CLIENTE-LUNGHISSIMA-000001", 6));
        Campo b = new Campo("lotto produzione molto lungo", Comportamento.CHIESTO, "LOTTO-MOLTO-LUNGO");
        e.aggiungi(a).aggiungi(b);
        Elemento qr = new Elemento("QR principale", Tipo.QR, "codice", 2, 2, 12);
        e.aggiungi(qr);
        e.aggiungi(new Elemento("Testo principale", Tipo.CODICE, "codice", 17, 3, 25));

        Proprieta p = new Proprieta(new Runnable(){public void run(){}}, new Runnable(){public void run(){}});
        p.mostra(e, qr);
        p.setSize(new Dimension(Stile.px(310), Stile.px(780)));
        layout(p);
        List<Component> all = new ArrayList<Component>();
        collectAll(p, all);
        int widestCombo = 0;
        for (Component c : all) if (c instanceof JComboBox) widestCombo = Math.max(widestCombo, c.getWidth());
        check("linked-data combo remains readable", widestCombo >= Stile.px(190));

        Method generale = Finestre.class.getDeclaredMethod("generale", Component.class, JTextField.class, JTextField.class);
        generale.setAccessible(true);
        JTextField p1 = new JTextField("C:\\Users\\operator\\Documents\\Etichette Custom\\modelli\\cliente-molto-lungo\\linea-A\\etichette");
        JTextField p2 = new JTextField("C:\\Users\\operator\\Documents\\Etichette Custom\\registro\\2026\\turno-serale\\produzione");
        JComponent general = (JComponent) generale.invoke(null, null, p1, p2);
        general.setSize(Stile.px(700), Stile.px(480));
        layout(general);
        check("path controls scale with UI", Math.min(p1.getWidth(), p2.getWidth()) >= Stile.px(390));

        Method manuale = Finestre.class.getDeclaredMethod("manuale", boolean.class);
        manuale.setAccessible(true);
        JComponent manual = (JComponent) manuale.invoke(null, Boolean.TRUE);
        manual.setSize(Stile.px(700), Stile.px(480));
        layout(manual);
        List<Component> m = new ArrayList<Component>();
        collectAll(manual, m);
        JScrollPane scroll = null;
        for (Component c : m) if (c instanceof JScrollPane) { scroll = (JScrollPane)c; break; }
        check("manual keeps a large viewport", scroll != null && scroll.getWidth() >= Stile.px(500));
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents()) if (x instanceof Container) layout((Container)x);
    }
    private static void collectAll(Container root, List<Component> out) {
        for (Component c : root.getComponents()) {
            out.add(c);
            if (c instanceof Container) collectAll((Container)c, out);
        }
    }
    private static void check(String name, boolean pass) {
        if (pass) { ok++; System.out.println("  ok   " + name); }
        else { ko++; System.out.println("  FAIL " + name); }
    }
}
