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
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Off-screen graphical audit. PNG output is retained as CI evidence. */
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
                try {
                    runAudit();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        System.out.println(ok + " editor graphics checks, " + ko + " failed at base " + size);
        if (ko != 0) System.exit(1);
    }

    private static void runAudit() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta label = new Etichetta("Audit Windows", 50, 30);
        Campo field = new Campo("codice", Comportamento.FISSO,
                "210150.022_02-01.262350009");
        label.aggiungi(field);
        Elemento qr = new Elemento("QR", Tipo.QR, "codice", 3, 2, 14);
        Elemento text = new Elemento("Testo", Tipo.CODICE, "codice", 3, 19, 44)
                .corpo(4.2, true);
        text.mostraSeparatori(false);
        label.aggiungi(qr);
        label.aggiungi(text);

        Proprieta inspector = new Proprieta(new Runnable() {
            @Override public void run() { }
        }, new Runnable() {
            @Override public void run() { }
        });
        inspector.mostra(label, text);
        inspector.setSize(new Dimension(Stile.px(330), Stile.px(820)));
        layout(inspector);

        List<Component> components = new ArrayList<Component>();
        collectAll(inspector, components);
        check("primary editor contains no JSpinner", count(components, JSpinner.class) == 0);

        JComboBox<?> alignment = findNamedCombo(components, "text-alignment");
        JComboBox<?> rows = findNamedCombo(components, "text-rows");
        AbstractButton rotate = findNamedButton(components, "rotate-90");

        check("alignment uses one compact chooser", alignment != null && alignment.getItemCount() == 3);
        check("line count uses one compact chooser", rows != null && rows.getItemCount() == 4);
        check("rotation uses one direct 90 degree action", rotate != null);
        check("old four-angle buttons are gone",
                findButton(components, "0°") == null
                        && findButton(components, "90°") == null
                        && findButton(components, "180°") == null
                        && findButton(components, "270°") == null);
        check("separator visibility is directly exposed",
                findCheck(components, "Mostra punti e simboli") != null);
        check("secondary behavior choices stay hidden by default",
                findToggle(components, "Aumenta") == null);
        check("alignment chooser stays inside inspector",
                insideHorizontally(inspector, alignment));
        check("line chooser stays inside inspector",
                insideHorizontally(inspector, rows));
        check("rotate action stays inside inspector",
                insideHorizontally(inspector, rotate));

        int fieldsBefore = count(components, JTextField.class);
        check("precision fields stay hidden by default", fieldsBefore <= 2);

        if (alignment != null) alignment.setSelectedItem("Centro");
        check("alignment chooser updates model", text.allineamento() == 1);
        if (rows != null) rows.setSelectedItem("3");
        check("line chooser updates model", text.righePreferite() == 3);

        if (rotate != null) rotate.doClick();
        check("one rotate click advances exactly 90 degrees", text.rotazione() == 90);
        if (rotate != null) {
            rotate.doClick();
            rotate.doClick();
            rotate.doClick();
        }
        check("four rotate clicks return to zero", text.rotazione() == 0);

        JCheckBox separators = findCheck(components, "Mostra punti e simboli");
        if (!separators.isSelected()) separators.doClick();
        separators.doClick();
        check("separator toggle updates only presentation", !text.mostraSeparatori());
        check("source code remains exact",
                "210150.022_02-01.262350009".equals(label.contenuto(text, 0)));

        inspector.mostra(label, text);
        inspector.setSize(new Dimension(Stile.px(330), Stile.px(820)));
        layout(inspector);
        components.clear();
        collectAll(inspector, components);
        JToggleButton behavior = findToggle(components, "Cambia comportamento…");
        check("behavior options have one explicit entry point", behavior != null);
        if (behavior != null) behavior.doClick();
        layout(inspector);
        components.clear();
        collectAll(inspector, components);
        check("behavior choices appear only on request",
                findToggle(components, "Aumenta") != null
                        && findToggle(components, "Chiedi") != null);

        JToggleButton precision = findToggle(components, "Misure precise…");
        check("precision control exists", precision != null);
        if (precision != null) precision.doClick();
        layout(inspector);
        components.clear();
        collectAll(inspector, components);
        check("precision fields appear only on request",
                count(components, JTextField.class) > fieldsBefore);
        check("precision controls do not clip horizontally",
                allInsideHorizontally(inspector, components));
        save(inspector, "inspector");

        Foglio canvasPanel = new Foglio(label, new QrVero());
        canvasPanel.percentuale(180);
        canvasPanel.setSize(new Dimension(780, 520));
        canvasPanel.selezione(qr);
        save(canvasPanel, "editor");
        BufferedImage canvas = image(canvasPanel);
        check("grid produces visible non-white structure", paperVariety(canvas) > 8);

        Method xOriginMethod = Foglio.class.getDeclaredMethod("originaX");
        Method yOriginMethod = Foglio.class.getDeclaredMethod("originaY");
        xOriginMethod.setAccessible(true);
        yOriginMethod.setAccessible(true);
        int xOrigin = ((Integer) xOriginMethod.invoke(canvasPanel)).intValue();
        int yOrigin = ((Integer) yOriginMethod.invoke(canvasPanel)).intValue();
        double zoom = canvasPanel.zoom();
        int handleX = xOrigin + (int) Math.round((qr.x() + qr.larghezza()) * zoom) + 2;
        int handleY = yOrigin + (int) Math.round((qr.y() + qr.larghezza()) * zoom) + 2;
        double before = qr.larghezza();
        canvasPanel.dispatchEvent(new MouseEvent(canvasPanel, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, handleX, handleY, 1, false));
        canvasPanel.dispatchEvent(new MouseEvent(canvasPanel, MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(), 0,
                handleX + (int) Math.round(5 * zoom),
                handleY + (int) Math.round(5 * zoom), 0, false));
        canvasPanel.dispatchEvent(new MouseEvent(canvasPanel, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0,
                handleX + (int) Math.round(5 * zoom),
                handleY + (int) Math.round(5 * zoom), 1, false));
        check("corner drag resizes QR directly", qr.larghezza() > before + 2.0);
    }

    private static BufferedImage image(Component component) {
        BufferedImage image = new BufferedImage(Math.max(1, component.getWidth()),
                Math.max(1, component.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void save(Component component, String name) throws Exception {
        File dir = new File("build/ui-audit");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IllegalStateException("cannot create " + dir);
        }
        String os = System.getProperty("os.name", "os").toLowerCase()
                .replaceAll("[^a-z0-9]+", "-");
        ImageIO.write(image(component), "png",
                new File(dir, os + "-" + name + "-" + size + ".png"));
    }

    private static int paperVariety(BufferedImage image) {
        java.util.HashSet<Integer> colors = new java.util.HashSet<Integer>();
        int x0 = image.getWidth() / 4;
        int x1 = image.getWidth() * 3 / 4;
        int y0 = image.getHeight() / 4;
        int y1 = image.getHeight() * 3 / 4;
        for (int y = y0; y < y1; y += 2) {
            for (int x = x0; x < x1; x += 2) {
                colors.add(Integer.valueOf(image.getRGB(x, y)));
            }
        }
        return colors.size();
    }

    private static boolean insideHorizontally(Container root, Component component) {
        if (component == null || component.getWidth() <= 0 || component.getHeight() <= 0) return false;
        Point point = SwingUtilities.convertPoint(component, 0, 0, root);
        return point.x >= 0 && point.x + component.getWidth() <= root.getWidth();
    }

    private static boolean allInsideHorizontally(Container root, List<Component> all) {
        for (Component component : all) {
            if (component.isVisible() && component.getWidth() > 0 && component.getHeight() > 0
                    && !insideHorizontally(root, component)) return false;
        }
        return true;
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

    private static int count(List<Component> all, Class<?> type) {
        int count = 0;
        for (Component component : all) if (type.isInstance(component)) count++;
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

    private static JToggleButton findToggle(List<Component> all, String text) {
        for (Component component : all) {
            if (component instanceof JToggleButton
                    && text.equals(((JToggleButton) component).getText())) {
                return (JToggleButton) component;
            }
        }
        return null;
    }

    private static JCheckBox findCheck(List<Component> all, String text) {
        for (Component component : all) {
            if (component instanceof JCheckBox
                    && text.equals(((JCheckBox) component).getText())) {
                return (JCheckBox) component;
            }
        }
        return null;
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
