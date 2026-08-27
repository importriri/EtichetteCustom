package prove;

import app.archivio.Archivio;
import app.archivio.Registro;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Impostazioni;
import app.modello.Libreria;
import app.render.QrVero;
import app.stile.Stile;
import app.ui.banco.Banco;
import app.ui.banco.Foglio;
import app.ui.operatore.Operatore;
import app.ui.vetrina.Vetrina;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Full-screen graphical audit for the first-run gallery, editor and print flow. */
public final class ProvaFlussoGrafico {
    private static int ok;
    private static int ko;
    private static int size;

    private ProvaFlussoGrafico() { }

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
        System.out.println(ok + " full-flow graphics checks, " + ko
                + " failed at base " + size);
        if (ko != 0) System.exit(1);
    }

    private static void runAudit() throws Exception {
        Stile.adottaFontDiSistema();
        Etichetta label = Libreria.esempio();
        QrVero qr = new QrVero();
        Impostazioni settings = new Impostazioni();
        File archiveDir = temporaryDirectory("etichette-ui-archive");
        File logDir = temporaryDirectory("etichette-ui-log");
        Archivio archive = new Archivio(archiveDir);
        Registro log = new Registro(logDir);

        List<Etichetta> labels = new ArrayList<Etichetta>();
        labels.add(label);
        Vetrina gallery = new Vetrina(labels, qr, commands(), log);
        auditScreen(gallery, "gallery", 1280, 760);
        List<Component> galleryComponents = components(gallery);
        check("gallery has no JSpinner", count(galleryComponents, JSpinner.class) == 0);
        check("new-label action fits without truncation",
                buttonFits(findButton(galleryComponents, "+  Nuova etichetta")));
        check("gallery exposes one new-label affordance",
                countTextContaining(galleryComponents, "Nuova etichetta") == 1);
        check("search stays hidden for one model",
                visibleCount(galleryComponents, JTextField.class) == 0);

        Banco editor = new Banco(label, qr, settings, archive, log,
                new Runnable() { @Override public void run() { } },
                new Runnable() { @Override public void run() { } });
        selectFirstText(editor, label);
        auditScreen(editor, "workspace", 1360, 820);
        List<Component> editorComponents = components(editor);
        check("workspace has no JSpinner", count(editorComponents, JSpinner.class) == 0);
        check("print action fits without truncation",
                buttonFits(findButton(editorComponents, "Anteprima e stampa")));
        check("alignment chooser remains comfortably visible",
                comboFits(findNamedCombo(editorComponents, "text-alignment")));
        check("line-count chooser remains comfortably visible",
                comboFits(findNamedCombo(editorComponents, "text-rows")));
        check("rotate action fits without truncation",
                buttonFits(findNamedButton(editorComponents, "rotate-90")));

        Operatore operator = new Operatore(label, qr, settings, archive, log,
                new Runnable() { @Override public void run() { } },
                new Runnable() { @Override public void run() { } });
        auditScreen(operator, "operator", 1280, 760);
        List<Component> operatorComponents = components(operator);
        check("operator mode has no JSpinner", count(operatorComponents, JSpinner.class) == 0);
        check("operator keeps sequence configuration out of the run",
                count(operatorComponents, JComboBox.class) == 0);
        check("fixed values stay out of print preparation",
                !hasTextFieldValue(operatorComponents, "D04")
                        && !hasTextFieldValue(operatorComponents, "03_01-02"));
        check("copy count is a plain text field",
                hasTextFieldValue(operatorComponents, "12"));
        check("print button fits without truncation",
                buttonFits(findButton(operatorComponents, "Stampa 12 etichette")));
    }

    private static void selectFirstText(Banco editor, Etichetta label) throws Exception {
        Field field = Banco.class.getDeclaredField("canvas");
        field.setAccessible(true);
        Foglio canvas = (Foglio) field.get(editor);
        for (Elemento element : label.elementi()) {
            if (element.tipo().scritto()) {
                canvas.selezione(element);
                return;
            }
        }
        throw new IllegalStateException("first-run label has no text element");
    }

    private static void auditScreen(Component screen, String name, int width, int height)
            throws Exception {
        layout(screen, width, height);
        save(screen, name);
        BufferedImage image = image(screen);
        check(name + " has visible structure", colorVariety(image) > 12);
    }

    private static void layout(Component component, int width, int height) {
        component.setSize(new Dimension(width, height));
        if (component instanceof Container) {
            Container container = (Container) component;
            container.doLayout();
            for (Component child : container.getComponents()) {
                if (child.getWidth() > 0 && child.getHeight() > 0) {
                    layout(child, child.getWidth(), child.getHeight());
                }
            }
        }
    }

    private static List<Component> components(Container root) {
        List<Component> all = new ArrayList<Component>();
        collect(root, all);
        return all;
    }

    private static void collect(Container root, List<Component> out) {
        for (Component component : root.getComponents()) {
            out.add(component);
            if (component instanceof Container) collect((Container) component, out);
        }
    }

    private static int count(List<Component> all, Class<?> type) {
        int count = 0;
        for (Component component : all) if (type.isInstance(component)) count++;
        return count;
    }

    private static int visibleCount(List<Component> all, Class<?> type) {
        int count = 0;
        for (Component component : all) {
            if (type.isInstance(component) && component.isVisible()) count++;
        }
        return count;
    }

    private static int countTextContaining(List<Component> all, String text) {
        int count = 0;
        for (Component component : all) {
            String value = null;
            if (component instanceof AbstractButton) value = ((AbstractButton) component).getText();
            if (component instanceof JLabel) value = ((JLabel) component).getText();
            if (value != null && value.contains(text)) count++;
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

    private static AbstractButton findNamedButton(List<Component> all, String name) {
        for (Component component : all) {
            if (component instanceof AbstractButton && name.equals(component.getName())) {
                return (AbstractButton) component;
            }
        }
        return null;
    }

    private static JComboBox<?> findNamedCombo(List<Component> all, String name) {
        for (Component component : all) {
            if (component instanceof JComboBox && name.equals(component.getName())) {
                return (JComboBox<?>) component;
            }
        }
        return null;
    }

    private static boolean buttonFits(AbstractButton button) {
        if (button == null || button.getWidth() <= 0) return false;
        int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
        return button.getWidth() >= textWidth + Stile.px(14);
    }

    private static boolean comboFits(JComboBox<?> combo) {
        return combo != null && combo.getWidth() >= Stile.px(120)
                && combo.getHeight() >= Stile.px(28);
    }

    private static boolean hasTextFieldValue(List<Component> all, String value) {
        for (Component component : all) {
            if (component instanceof JTextField
                    && value.equals(((JTextField) component).getText())) return true;
        }
        return false;
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
        File directory = new File("build/ui-audit");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("cannot create " + directory);
        }
        String os = System.getProperty("os.name", "os").toLowerCase()
                .replaceAll("[^a-z0-9]+", "-");
        ImageIO.write(image(component), "png",
                new File(directory, os + "-flow-" + name + "-" + size + ".png"));
    }

    private static int colorVariety(BufferedImage image) {
        java.util.HashSet<Integer> colors = new java.util.HashSet<Integer>();
        for (int y = 0; y < image.getHeight(); y += 6) {
            for (int x = 0; x < image.getWidth(); x += 6) {
                colors.add(Integer.valueOf(image.getRGB(x, y)));
                if (colors.size() > 64) return colors.size();
            }
        }
        return colors.size();
    }

    private static File temporaryDirectory(String prefix) {
        try {
            File file = File.createTempFile(prefix, "");
            if (!file.delete() || !file.mkdirs()) {
                throw new IllegalStateException("cannot prepare " + file);
            }
            file.deleteOnExit();
            return file;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Vetrina.Comandi commands() {
        return new Vetrina.Comandi() {
            @Override public void apri(Etichetta label) { }
            @Override public void modifica(Etichetta label) { }
            @Override public void nuova() { }
            @Override public void rinomina(Etichetta label) { }
            @Override public void duplica(Etichetta label) { }
            @Override public void elimina(Etichetta label) { }
            @Override public void stampante() { }
            @Override public void impostazioni() { }
        };
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
