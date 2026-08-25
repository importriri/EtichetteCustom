package prove;

import app.modello.Impostazioni;
import app.stile.Stile;
import app.ui.finestre.Finestre;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class ProvaDialoghi {
    private static int failures;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Stile.adottaFontDiSistema();
                JFrame owner = new JFrame("RC owner");
                owner.setSize(600, 400);
                owner.setLocationRelativeTo(null);
                owner.setVisible(true);

                Impostazioni imp = new Impostazioni();
                imp.cartellaEtichette(new File("C:/Users/operator/Documents/Etichette Custom/modelli/cliente/reparto/linea-A"));
                imp.cartellaLog(new File("C:/Users/operator/Documents/Etichette Custom/registro/2026/produzione/turno-serale"));
                imp.stampante("Datamax E-4203 - Produzione Linea A");
                imp.risoluzioneDpi(203);

                Timer inspect = new Timer(700, e -> inspectAndClose());
                inspect.setRepeats(false);
                inspect.start();
                Finestre.impostazioni(owner, imp);
                owner.dispose();
            }
        });
        if (failures != 0) System.exit(1);
    }

    private static void inspectAndClose() {
        JDialog target = null;
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && w.isShowing() && "Impostazioni".equals(((JDialog)w).getTitle())) {
                target = (JDialog)w;
                break;
            }
        }
        if (target == null) {
            fail("settings dialog is visible");
            return;
        }
        pass("settings dialog is visible");
        Dimension d = target.getSize();
        check("settings dialog has release-size geometry", d.width >= Stile.px(700) && d.height >= Stile.px(560));
        JTabbedPane tabs = find(target, JTabbedPane.class);
        check("settings exposes four unified tabs", tabs != null && tabs.getTabCount() == 4);
        JTextField widest = widestTextField(target);
        check("settings contains a wide text field", widest != null && widest.getWidth() >= Stile.px(360));
        target.dispose();
    }

    private static JTextField widestTextField(Container root) {
        JTextField best = null;
        for (Component c : root.getComponents()) {
            if (c instanceof JTextField && (best == null || c.getWidth() > best.getWidth())) best = (JTextField)c;
            if (c instanceof Container) {
                JTextField x = widestTextField((Container)c);
                if (x != null && (best == null || x.getWidth() > best.getWidth())) best = x;
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) return (T)c;
            if (c instanceof Container) {
                T x = find((Container)c, type);
                if (x != null) return x;
            }
        }
        return null;
    }

    private static void check(String name, boolean value) { if (value) pass(name); else fail(name); }
    private static void pass(String name) { System.out.println("  ok   " + name); }
    private static void fail(String name) { failures++; System.out.println("  FAIL " + name); }
}
