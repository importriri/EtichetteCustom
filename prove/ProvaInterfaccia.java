package prove;

import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Serie;
import app.modello.Tipo;
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
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Basic Swing layout checks that can run without operator interaction. */
public final class ProvaInterfaccia {
    private static int ok;
    private static int ko;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                try {
                    ProvaInterfaccia.run();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        System.out.println(ok + " UI checks, " + ko + " failed");
        if (ko != 0) System.exit(1);
    }

    private static void run() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta label = new Etichetta("Etichetta con nome volutamente lungo", 50, 30);
        Campo primary = new Campo("codice", Comportamento.PROGRESSIVO, "CLIENTE-LUNGO-000001");
        primary.serie(new Serie("CLIENTE-LUNGO-000001", 6));
        Campo secondary = new Campo("codice 2", Comportamento.PROGRESSIVO, "ALTRO-PROGRESSIVO-0900");
        secondary.serie(new Serie("ALTRO-PROGRESSIVO-0900", 4));
        Campo lot = new Campo("lotto produzione molto lungo", Comportamento.CHIESTO, "LOTTO-2026-08-25-A");
        label.aggiungi(primary).aggiungi(secondary).aggiungi(lot);

        Elemento qr = new Elemento("QR cliente principale", Tipo.QR, "codice", 2.5, 2.5, 12);
        label.aggiungi(qr);
        label.aggiungi(new Elemento("Testo cliente", Tipo.CODICE, "codice", 17, 3, 28));
        label.aggiungi(new Elemento("QR secondario", Tipo.QR, "codice 2", 2.5, 16, 10));

        Proprieta inspector = new Proprieta(new Runnable() {
            @Override public void run() { }
        }, new Runnable() {
            @Override public void run() { }
        });
        inspector.mostra(label, qr);
        inspector.setSize(new Dimension(Stile.px(286), Stile.px(780)));
        layout(inspector);

        List<Component> allInspector = new ArrayList<Component>();
        collectAll(inspector, allInspector);
        int comboCount = 0;
        for (Component component : allInspector) {
            if (component instanceof JComboBox) comboCount++;
        }
        check("secondary content choices stay hidden by default", comboCount == 0);
        check("shared content is summarized without a picker",
                containsLabel(allInspector, "🔗"));

        Method general = Finestre.class.getDeclaredMethod(
                "generale", Component.class, JTextField.class, JTextField.class);
        general.setAccessible(true);
        JTextField labelPath = new JTextField(new File(System.getProperty("user.home"),
                "EtichetteCustom/layout/cliente/reparto/linea-A/modelli/etichette").getAbsolutePath());
        JTextField logPath = new JTextField(new File(System.getProperty("user.home"),
                "EtichetteCustom/log/2026/produzione/turno-serale/registro").getAbsolutePath());
        JComponent settings = (JComponent) general.invoke(null, null, labelPath, logPath);
        settings.setSize(Stile.px(700), Stile.px(480));
        layout(settings);
        check("settings path field stays wide",
                Math.max(labelPath.getWidth(), logPath.getWidth()) >= Stile.px(400));

        Method manual = Finestre.class.getDeclaredMethod("manuale", boolean.class);
        manual.setAccessible(true);
        JComponent manualPanel = (JComponent) manual.invoke(null, Boolean.TRUE);
        manualPanel.setSize(Stile.px(700), Stile.px(480));
        layout(manualPanel);
        List<Component> allManual = new ArrayList<Component>();
        collectAll(manualPanel, allManual);
        JScrollPane first = null;
        for (Component component : allManual) {
            if (component instanceof JScrollPane) {
                first = (JScrollPane) component;
                break;
            }
        }
        check("manual has its own readable scroll area", first != null);
        check("manual viewport is not tiny", first != null && first.getWidth() >= Stile.px(500));
    }

    private static boolean containsLabel(List<Component> all, String text) {
        for (Component component : all) {
            if (component instanceof JLabel
                    && ((JLabel) component).getText() != null
                    && ((JLabel) component).getText().contains(text)) return true;
        }
        return false;
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) layout((Container) child);
        }
    }

    private static void collectAll(Container root, List<Component> out) {
        for (Component component : root.getComponents()) {
            out.add(component);
            if (component instanceof Container) collectAll((Container) component, out);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            ok++;
            System.out.println("  ok   " + name);
        } else {
            ko++;
            System.out.println("  FAIL " + name);
        }
    }
}
