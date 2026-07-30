import app.Main;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Genera gli screenshot e i fotogrammi della gif della documentazione
 * pilotando l'applicazione vera, non un mockup.
 *
 * <p>Non fa parte della build: sta in {@code verifica/} come gli altri
 * strumenti di collaudo. Le immagini nella repo devono mostrare il programma
 * che gira, altrimenti alla prima modifica raccontano una bugia.
 *
 * <pre>
 *   javac --release 8 -encoding UTF-8 -cp out -d verifica/bin verifica/Shots.java
 *   xvfb-run -a -s "-screen 0 1600x1000x24" java -cp out:verifica/bin Shots /tmp/shots
 * </pre>
 */
public final class Shots {

    private static File outputDir;
    private static int frame;

    public static void main(String[] args) throws Exception {
        outputDir = new File(args.length > 0 ? args[0] : "/tmp/shots");
        outputDir.mkdirs();
        new File(outputDir, "frames").mkdirs();

        Main.main(new String[0]);
        JFrame window = awaitFrame();
        Thread.sleep(700);

        JTextField code = (JTextField) find(window.getContentPane(), JTextField.class);

        // 1 — si scrive il codice, un carattere alla volta: l'anteprima segue
        String wanted = "DEMO-4410.07_A2-01_000001";
        for (int i = 1; i <= wanted.length(); i++) {
            set(code, wanted.substring(0, i));
            if (i % 3 == 0 || i == wanted.length()) {
                shoot(window);
            }
        }
        hold(window, 3);
        save(window, "screenshot-etichetta.png");

        // 2a — niente selezionato: il pannello mostra l'etichetta, con la
        // misura del supporto, i campi e i layout salvati
        openDialog(window, "Gestisci i campi", "screenshot-campi.png");
        hold(window, 2);

        // 2b — si sceglie un elemento: il pannello passa alle sue proprietà
        clickPreview(window);
        hold(window, 3);
        save(window, "screenshot-selezione.png");

        // 3 — lo si ruota di un quarto di giro alla volta, dal pulsante grande
        for (int i = 0; i < 4; i++) {
            pressTool(window, "Ruota");
            shoot(window);
            shoot(window);
        }
        hold(window, 2);

        // 4 — e si stringe il testo finché non va a capo da solo
        List<JSpinner> spinners = findAll(window.getContentPane(), JSpinner.class);
        if (spinners.size() >= 3) {
            JSpinner wrap = spinners.get(spinners.size() - 1);
            for (double mm : new double[] {40, 30, 22, 16, 0}) {
                set(wrap, mm);
                shoot(window);
                shoot(window);
            }
        }
        hold(window, 3);

        // 5 — le due finestrelle, quelle che si aprono una volta al mese
        openDialog(window, "Stampante", "screenshot-stampante.png");
        openDialog(window, "Impostazioni", "screenshot-impostazioni.png");
        openDialog(window, "Gestisci i campi", "screenshot-campi.png");
        hold(window, 4);

        System.out.println("immagini in " + outputDir.getAbsolutePath()
                + ", fotogrammi: " + frame);
        System.exit(0);
    }

    /** Un clic al centro dell'anteprima: seleziona l'elemento che sta lì. */
    private static void clickPreview(final JFrame window) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Component p = find(window.getContentPane(), app.ui.PreviewPanel.class);
                if (p == null) {
                    return;
                }
                java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(p,
                        java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        0, p.getWidth() / 2, p.getHeight() / 2, 1, false);
                for (java.awt.event.MouseListener l : p.getMouseListeners()) {
                    l.mousePressed(click);
                }
            }
        });
        Thread.sleep(120);
    }

    /** Preme il pulsante il cui testo o suggerimento contiene quella parola. */
    private static void pressTool(final Container root, final String word) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                pressIn(root, word);
            }
        });
        Thread.sleep(120);
    }

    private static boolean pressIn(Container root, String word) {
        for (Component c : root.getComponents()) {
            if (c instanceof javax.swing.AbstractButton) {
                javax.swing.AbstractButton b = (javax.swing.AbstractButton) c;
                String tip = b.getToolTipText();
                String text = b.getText();
                if ((tip != null && tip.contains(word))
                        || (text != null && text.contains(word))) {
                    b.doClick();
                    return true;
                }
            }
            if (c instanceof Container && pressIn((Container) c, word)) {
                return true;
            }
        }
        return false;
    }

    /** Apre una finestrella, la fotografa e la richiude. */
    private static void openDialog(final JFrame window, String word, String name)
            throws Exception {
        final String w = word;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pressIn(window.getContentPane(), w);
            }
        });
        Thread.sleep(900);
        java.awt.Window dialog = null;
        for (java.awt.Window candidate : java.awt.Window.getWindows()) {
            if (candidate instanceof javax.swing.JDialog && candidate.isShowing()) {
                dialog = candidate;
            }
        }
        if (dialog == null) {
            System.out.println("  (nessuna finestrella per \"" + word + "\")");
            return;
        }
        BufferedImage shot = new BufferedImage(dialog.getWidth(), dialog.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        final java.awt.Window d = dialog;
        final BufferedImage target = shot;
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Graphics2D g = target.createGraphics();
                d.printAll(g);
                g.dispose();
            }
        });
        ImageIO.write(shot, "png", new File(outputDir, name));
        System.out.println("  " + name);
        final java.awt.Window toClose = dialog;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                toClose.dispose();
            }
        });
        Thread.sleep(400);
    }

    // --- gesti ----------------------------------------------------------------

    private static void set(final JTextField field, final String text) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                field.setText(text);
            }
        });
        Thread.sleep(60);
    }

    private static void set(final JSpinner spinner, final double value) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                spinner.setValue(value);
            }
        });
        Thread.sleep(60);
    }

    private static void select(final JList<?> list, final int index) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                list.setSelectedIndex(index);
            }
        });
        Thread.sleep(80);
    }


    // --- cattura --------------------------------------------------------------

    private static void hold(JFrame window, int frames) throws Exception {
        for (int i = 0; i < frames; i++) {
            shoot(window);
        }
    }

    private static void shoot(JFrame window) throws Exception {
        BufferedImage img = paint(window);
        ImageIO.write(img, "png", new File(outputDir,
                String.format("frames/frame-%03d.png", frame++)));
    }

    private static void save(JFrame window, String name) throws Exception {
        ImageIO.write(paint(window), "png", new File(outputDir, name));
        System.out.println("  " + name);
    }

    private static BufferedImage paint(final JFrame window) throws Exception {
        final BufferedImage img = new BufferedImage(
                window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Graphics2D g = img.createGraphics();
                window.printAll(g);
                g.dispose();
            }
        });
        return img;
    }

    // --- ricerca nell'albero dei componenti -----------------------------------

    private static JFrame awaitFrame() throws Exception {
        for (int i = 0; i < 100; i++) {
            for (Frame f : Frame.getFrames()) {
                if (f instanceof JFrame && f.isShowing()) {
                    return (JFrame) f;
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("La finestra non è comparsa.");
    }

    private static Component find(Container root, Class<?> type) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                return c;
            }
            if (c instanceof Container) {
                Component inside = find((Container) c, type);
                if (inside != null) {
                    return inside;
                }
            }
        }
        return null;
    }

    private static <T> List<T> findAll(Container root, Class<T> type) {
        List<T> found = new ArrayList<T>();
        collect(root, type, found);
        return found;
    }

    private static <T> void collect(Container root, Class<T> type, List<T> found) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                found.add(type.cast(c));
            }
            if (c instanceof Container) {
                collect((Container) c, type, found);
            }
        }
    }
}
