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
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Basic Swing layout checks that can run without operator interaction. */
public final class ProvaInterfaccia {
    private static int ok;
    private static int ko;

    private ProvaInterfaccia() { }

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
        System.exit(ko == 0 ? 0 : 1);
    }

    private static void run() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta label = new Etichetta("Etichetta con nome volutamente lungo", 50, 30);
        Campo primary = new Campo("codice", Comportamento.PROGRESSIVO, "CLIENTE-LUNGO-000001");
        primary.serie(new Serie("CLIENTE-LUNGO-000001", 6));
        Campo secondary = new Campo("codice 2", Comportamento.PROGRESSIVO,
                "ALTRO-PROGRESSIVO-0900");
        secondary.serie(new Serie("ALTRO-PROGRESSIVO-0900", 4));
        Campo lot = new Campo("lotto produzione molto lungo", Comportamento.CHIESTO,
                "LOTTO-2026-08-25-A");
        label.aggiungi(primary).aggiungi(secondary).aggiungi(lot);

        Elemento qr = new Elemento("QR cliente principale", Tipo.QR, "codice", 2.5, 2.5, 12);
        label.aggiungi(qr);
        label.aggiungi(new Elemento("Testo cliente", Tipo.CODICE, "codice", 17, 3, 28));
        label.aggiungi(new Elemento("QR secondario", Tipo.QR, "codice 2", 2.5, 16, 10));

        Proprieta inspector = new Proprieta(
                new Runnable() { @Override public void run() { } },
                new Runnable() { @Override public void run() { } });
        inspector.mostra(label, qr);
        inspector.setSize(new Dimension(Stile.px(286), Stile.px(780)));
        layout(inspector);

        List<Component> allInspector = components(inspector);
        check("progressive details stay hidden by default", countCombos(allInspector) == 0);
        AbstractButton behavior = findNamedButton(allInspector, "content-behavior");
        AbstractButton shared = findNamedButton(allInspector, "shared-content");
        check("behavior has one compact disclosure action", behavior != null);
        check("shared content has one compact disclosure action", shared != null);
        check("detach action stays hidden until shared content is opened",
                findButton(allInspector, "Rendi indipendente") == null);

        if (behavior != null) behavior.doClick();
        layout(inspector);
        allInspector = components(inspector);
        JComboBox<?> behaviorChoice = findNamedCombo(allInspector, "content-behavior-choice");
        check("behavior chooser appears only on request",
                behaviorChoice != null && behaviorChoice.getItemCount() == 3);
        check("behavior chooser keeps readable width",
                behaviorChoice != null && behaviorChoice.getWidth() >= Stile.px(180));
        check("progressive digit choice appears only on request", countCombos(allInspector) == 2);

        inspector.mostra(label, qr);
        inspector.setSize(new Dimension(Stile.px(286), Stile.px(780)));
        layout(inspector);
        allInspector = components(inspector);
        shared = findNamedButton(allInspector, "shared-content");
        if (shared != null) shared.doClick();
        layout(inspector);
        allInspector = components(inspector);
        check("shared-content details appear only on request",
                findButton(allInspector, "Rendi indipendente") != null);

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
        List<Component> allManual = components(manualPanel);
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

    private static List<Component> components(Container root) {
        List<Component> all = new ArrayList<Component>();
        collectAll(root, all);
        return all;
    }

    private static int countCombos(List<Component> all) {
        int count = 0;
        for (Component component : all) if (component instanceof JComboBox) count++;
        return count;
    }

    private static JComboBox<?> findNamedCombo(List<Component> all, String name) {
        for (Component component : all) {
            if (component instanceof JComboBox && name.equals(component.getName())) {
                return (JComboBox<?>) component;
            }
        }
        return null;
    }

    private static AbstractButton findNamedButton(List<Component> all, String name) {
        for (Component component : all) {
            if (component instanceof AbstractButton && name.equals(component.getName())) {
                return (AbstractButton) component;
            }
        }
        return null;
    }

    private static AbstractButton findButton(List<Component> all, String text) {
        for (Component component : all) {
            if (component instanceof AbstractButton
                    && text.equals(((AbstractButton) component).getText())) {
                return (AbstractButton) component;
            }
        }
        return null;
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
