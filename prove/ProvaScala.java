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
import java.awt.Font;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Enlarged-font UI audit for controls that must remain usable at Windows-like scales. */
public final class ProvaScala {
    private static int ok;
    private static int ko;
    private static int size;

    private ProvaScala() { }

    public static void main(String[] args) throws Exception {
        size = Integer.parseInt(args.length == 0 ? "15" : args[0]);
        UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, size));
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                try {
                    ProvaScala.run();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        });
        System.out.println(ok + " scale checks, " + ko + " failed at " + size + "px base");
        System.exit(ko == 0 ? 0 : 1);
    }

    private static void run() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta label = new Etichetta("Etichetta lunga per audit DPI", 50, 30);
        Campo primary = new Campo("codice", Comportamento.PROGRESSIVO,
                "SERIE-CLIENTE-LUNGHISSIMA-000001");
        primary.serie(new Serie("SERIE-CLIENTE-LUNGHISSIMA-000001", 6));
        Campo lot = new Campo("lotto produzione molto lungo", Comportamento.CHIESTO,
                "LOTTO-MOLTO-LUNGO");
        label.aggiungi(primary).aggiungi(lot);
        Elemento qr = new Elemento("QR principale", Tipo.QR, "codice", 2, 2, 12);
        label.aggiungi(qr);
        label.aggiungi(new Elemento("Testo principale", Tipo.CODICE, "codice", 17, 3, 25));
        Elemento lotQr = new Elemento("QR lotto", Tipo.QR, "lotto produzione molto lungo", 2, 16, 10);
        label.aggiungi(lotQr);

        Proprieta inspector = new Proprieta(
                new Runnable() { @Override public void run() { } },
                new Runnable() { @Override public void run() { } });
        inspector.mostra(label, lotQr);
        inspector.setSize(new Dimension(Stile.px(310), Stile.px(780)));
        layout(inspector);
        List<Component> all = components(inspector);
        check("content-link picker stays hidden by default", countCombos(all) == 0);

        AbstractButton reveal = findButton(all, "Usa contenuto esistente…");
        check("content-link disclosure action remains readable",
                reveal != null && reveal.getWidth() >= Stile.px(180));
        if (reveal != null) reveal.doClick();
        layout(inspector);
        all = components(inspector);
        int widestCombo = 0;
        for (Component component : all) {
            if (component instanceof JComboBox) {
                widestCombo = Math.max(widestCombo, component.getWidth());
            }
        }
        check("content-link picker remains readable after opening",
                widestCombo >= Stile.px(190));

        Method generalMethod = Finestre.class.getDeclaredMethod(
                "generale", Component.class, JTextField.class, JTextField.class);
        generalMethod.setAccessible(true);
        JTextField labelPath = new JTextField(
                "C:\\Users\\operator\\Documents\\Etichette Custom\\modelli\\cliente-molto-lungo\\linea-A\\etichette");
        JTextField logPath = new JTextField(
                "C:\\Users\\operator\\Documents\\Etichette Custom\\registro\\2026\\turno-serale\\produzione");
        JComponent general = (JComponent) generalMethod.invoke(null, null, labelPath, logPath);
        general.setSize(Stile.px(700), Stile.px(480));
        layout(general);
        check("path controls scale with UI",
                Math.min(labelPath.getWidth(), logPath.getWidth()) >= Stile.px(390));

        Method manualMethod = Finestre.class.getDeclaredMethod("manuale", boolean.class);
        manualMethod.setAccessible(true);
        JComponent manual = (JComponent) manualMethod.invoke(null, Boolean.TRUE);
        manual.setSize(Stile.px(700), Stile.px(480));
        layout(manual);
        List<Component> manualComponents = components(manual);
        JScrollPane scroll = null;
        for (Component component : manualComponents) {
            if (component instanceof JScrollPane) {
                scroll = (JScrollPane) component;
                break;
            }
        }
        check("manual keeps a large viewport",
                scroll != null && scroll.getWidth() >= Stile.px(500));
    }

    private static List<Component> components(Container root) {
        List<Component> all = new ArrayList<Component>();
        collectAll(root, all);
        return all;
    }

    private static int countCombos(List<Component> all) {
        int count = 0;
        for (Component component : all) {
            if (component instanceof JComboBox) count++;
        }
        return count;
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

    private static void check(String name, boolean pass) {
        if (pass) {
            ok++;
            System.out.println("  ok   " + name);
        } else {
            ko++;
            System.out.println("  FAIL " + name);
        }
    }
}
