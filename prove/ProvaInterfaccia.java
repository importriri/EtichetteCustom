package prove;

import app.modello.*;
import app.stile.Stile;
import app.ui.banco.Proprieta;
import app.ui.finestre.Finestre;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class ProvaInterfaccia {
    private static int ok;
    private static int ko;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try { ProvaInterfaccia.run(); } catch (Exception e) { throw new RuntimeException(e); }
            }
        });
        System.out.println(ok + " UI checks, " + ko + " failed");
        if (ko != 0) System.exit(1);
    }

    private static void run() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta e = new Etichetta("Etichetta con nome volutamente lungo", 50, 30);
        Campo a = new Campo("codice", Comportamento.PROGRESSIVO, "CLIENTE-LUNGO-000001");
        a.serie(new Serie("CLIENTE-LUNGO-000001", 6));
        Campo b = new Campo("codice 2", Comportamento.PROGRESSIVO, "ALTRO-PROGRESSIVO-0900");
        b.serie(new Serie("ALTRO-PROGRESSIVO-0900", 4));
        Campo lotto = new Campo("lotto produzione molto lungo", Comportamento.CHIESTO, "LOTTO-2026-08-25-A");
        e.aggiungi(a).aggiungi(b).aggiungi(lotto);
        Elemento qr = new Elemento("QR cliente principale", Tipo.QR, "codice", 2.5, 2.5, 12);
        e.aggiungi(qr);
        e.aggiungi(new Elemento("Testo cliente", Tipo.CODICE, "codice", 17, 3, 28));
        e.aggiungi(new Elemento("QR secondario", Tipo.QR, "codice 2", 2.5, 16, 10));

        Proprieta p = new Proprieta(new Runnable(){public void run(){}}, new Runnable(){public void run(){}});
        p.mostra(e, qr);
        p.setSize(new Dimension(Stile.px(286), Stile.px(780)));
        layout(p);
        List<Component> allInspector = new ArrayList<Component>();
        collectAll(p, allInspector);
        int comboCount = 0;
        int max = 0;
        for (Component c : allInspector) {
            if (c instanceof JComboBox) { comboCount++; max = Math.max(max, c.getWidth()); }
        }
        check("inspector contains linked-data combo", comboCount > 0);
        check("linked-data controls get full card width", max >= Stile.px(185));

        Method generale = Finestre.class.getDeclaredMethod("generale", Component.class, JTextField.class, JTextField.class);
        generale.setAccessible(true);
        JTextField ePath = new JTextField(new File(System.getProperty("user.home"),
                "EtichetteCustom/layout/cliente/reparto/linea-A/modelli/etichette").getAbsolutePath());
        JTextField lPath = new JTextField(new File(System.getProperty("user.home"),
                "EtichetteCustom/log/2026/produzione/turno-serale/registro").getAbsolutePath());
        JComponent g = (JComponent) generale.invoke(null, null, ePath, lPath);
        g.setSize(Stile.px(700), Stile.px(480));
        layout(g);
        check("settings path field stays wide", Math.max(ePath.getWidth(), lPath.getWidth()) >= Stile.px(400));

        Method manuale = Finestre.class.getDeclaredMethod("manuale", boolean.class);
        manuale.setAccessible(true);
        JComponent m = (JComponent) manuale.invoke(null, Boolean.TRUE);
        m.setSize(Stile.px(700), Stile.px(480));
        layout(m);
        List<Component> allManual = new ArrayList<Component>();
        collectAll(m, allManual);
        JScrollPane first = null;
        for (Component c : allManual) if (c instanceof JScrollPane) { first = (JScrollPane) c; break; }
        check("manual has its own readable scroll area", first != null);
        check("manual viewport is not tiny", first != null && first.getWidth() >= Stile.px(500));
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents()) if (x instanceof Container) layout((Container) x);
    }

    private static void collectAll(Container root, List<Component> out) {
        for (Component c : root.getComponents()) {
            out.add(c);
            if (c instanceof Container) collectAll((Container) c, out);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) { ok++; System.out.println("  ok   " + name); }
        else { ko++; System.out.println("  FAIL " + name); }
    }
}
