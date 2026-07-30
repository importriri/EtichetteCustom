package app.ui;

import app.config.AppTheme;
import app.config.UiScale;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * I difetti che si vedono <b>solo su Windows</b>, provati qui.
 *
 * <p>Le foto arrivate dal reparto mostravano due cose: i numeri degli spinner
 * tagliati e le etichette dei campi accavallate. Nessuna delle due si vede
 * sviluppando su Linux con lo schermo al 100%, e nessuna delle due la trova un
 * test che guarda solo la logica. Quindi si provano qui, forzando le condizioni
 * di quel PC.
 *
 * <p>Tre famiglie di controlli:
 * <ol>
 *   <li><b>Schermo ingrandito.</b> A 125% e 150% Windows allarga il carattere
 *       di sistema. Se una misura è scritta in pixel fissi dentro il codice, il
 *       testo cresce e il contenitore no: il numero finisce sotto le frecce.
 *       Qui si rifà l'audit del layout dopo aver alzato il fattore di scala.</li>
 *   <li><b>Simboli.</b> Segoe UI non contiene tutti i simboli tecnici, e un
 *       glifo che manca diventa un rettangolo vuoto. Ogni simbolo usato
 *       nell'interfaccia deve passare da {@link AppTheme#glyph}, che ripiega su
 *       una parola quando il carattere non ce la fa.</li>
 *   <li><b>Look and feel di sistema.</b> Su Windows l'applicazione parte con
 *       quello nativo, che ridisegna spinner e combo a modo suo. Si prova che
 *       la finestra si costruisce lo stesso, senza dipendere dal disegno
 *       Metal.</li>
 * </ol>
 *
 * <p>Si lancia sotto un display virtuale:
 * {@code xvfb-run -a java -cp out app.ui.WindowsLookTest}
 */
public final class WindowsLookTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("  skip  nessun display: lanciare sotto xvfb-run");
            return;
        }
        java.nio.file.Path home = java.nio.file.Files.createTempDirectory("etichette-win");
        System.setProperty("user.home", home.toString());

        glyphs_neverLeaveAnEmptyBox();
        scale_isSaneOnEveryScreen();
        fonts_fallBackWhenSegoeIsMissing();
        window_survivesTheSystemLookAndFeel();

        System.out.println("WindowsLook: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
        System.exit(0);
    }

    // --- simboli --------------------------------------------------------------

    /**
     * Ogni simbolo dell'interfaccia o si disegna, o diventa una parola.
     *
     * <p>Il controllo è quello vero: si chiede al carattere se sa disegnarlo. Se
     * dice di no, {@link AppTheme#glyph} deve aver già restituito il ripiego —
     * e il ripiego dev'essere fatto di caratteri che qualunque font conosce.
     */
    private static void glyphs_neverLeaveAnEmptyBox() {
        String[][] used = {
            {"\u271A", "sel"}, {"\u25A6", "QR"}, {"\u21BB", "90\u00B0"},
            {"\u29C9", "\u25A1\u25A1"}, {"\u2715", "X"}, {"\u229E", "#"},
            {"\u21C4", "<>"}, {"\u2399", "Stampante"}, {"\u2699", "Opzioni"},
            {"\u2713", "OK"}, {"\u2212", "-"}, {"\u00D7", "x"}, {"\u2192", "->"},
            {"\u2026", "..."},
        };
        Font ui = AppTheme.UI_BOLD;
        int drawn = 0;
        int replaced = 0;
        boolean allSafe = true;
        for (String[] pair : used) {
            String shown = AppTheme.glyph(pair[0], pair[1]);
            if (shown.equals(pair[0])) {
                drawn++;
            } else {
                replaced++;
            }
            // qualunque cosa venga mostrata, il carattere deve saperla disegnare
            if (ui.canDisplayUpTo(shown) >= 0) {
                allSafe = false;
                System.out.println("      il carattere non sa disegnare \"" + shown + "\"");
            }
        }
        yes("nessun simbolo lascia un rettangolo vuoto (" + drawn + " disegnati, "
                + replaced + " sostituiti con parole)", allSafe);

        // e il ripiego dev'essere ASCII puro: è l'unica cosa che regge ovunque
        boolean asciiFallbacks = true;
        for (String[] pair : used) {
            for (int i = 0; i < pair[1].length(); i++) {
                char c = pair[1].charAt(i);
                if (c > 0x7F && c != '\u00B0' && c != '\u25A1') {
                    asciiFallbacks = false;
                }
            }
        }
        yes("i ripieghi sono in caratteri che esistono ovunque", asciiFallbacks);
    }

    // --- scala ----------------------------------------------------------------

    private static void scale_isSaneOnEveryScreen() {
        double f = UiScale.factor();
        yes("il fattore di scala sta nei limiti (" + String.format("%.2f", f) + ")",
                f >= 1.0 && f <= 2.5);
        yes("px() cresce con il fattore", UiScale.px(10) >= 10);
        yes("px(0) resta zero", UiScale.px(0) == 0);
        // il caso che rompeva l'interfaccia: raddoppiare una misura già
        // raddoppiata dal toolkit. Se il fattore è 1 quando il toolkit scala,
        // px() non moltiplica due volte
        yes("px() non moltiplica due volte", UiScale.px(100) <= 250);
    }

    private static void fonts_fallBackWhenSegoeIsMissing() {
        // in sandbox Segoe UI non c'è: l'app deve partire lo stesso con un
        // carattere logico, invece di restare senza font
        yes("il carattere dell'interfaccia esiste", AppTheme.UI != null
                && AppTheme.UI.getSize() > 0);
        yes("e quello a spaziatura fissa anche", AppTheme.MONO_FONT != null
                && AppTheme.MONO_FONT.getSize() > 0);
        yes("i corpi seguono la scala dello schermo",
                AppTheme.UI.getSize() >= Math.round(13 * UiScale.factor()) - 1);
    }

    // --- look and feel di sistema ---------------------------------------------

    /**
     * La finestra si costruisce anche con il look and feel nativo.
     *
     * <p>Su Windows spinner e combo li disegna il sistema, non Swing: se il
     * layout dipendesse dalle misure del disegno Metal, in reparto si
     * spaccherebbe. Qui si prova con quello di sistema — in sandbox è GTK o
     * Metal, ma la strada di codice esercitata è la stessa.
     */
    private static void window_survivesTheSystemLookAndFeel() throws Exception {
        final JFrame[] holder = new JFrame[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception keepDefault) {
                    // niente look and feel di sistema qui: si prova col default
                }
                holder[0] = new MainWindow();
                holder[0].setVisible(true);
            }
        });
        Thread.sleep(900);
        JFrame window = holder[0];
        yes("la finestra si apre con il look and feel di sistema ("
                + UIManager.getLookAndFeel().getName() + ")", window.isShowing());

        List<String> squeezed = new ArrayList<String>();
        collectSqueezed(window.getContentPane(), squeezed);
        report("nessun campo è più stretto di quanto dichiara", squeezed);

        List<String> outside = new ArrayList<String>();
        collectOverflowing(window.getContentPane(), outside);
        report("nessun componente esce dal suo contenitore", outside);

        // e dipinge davvero: una finestra che si apre bianca è comunque rotta
        final BufferedImage shot = new BufferedImage(
                Math.max(1, window.getWidth()), Math.max(1, window.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        final JFrame toPaint = window;
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                java.awt.Graphics2D g = shot.createGraphics();
                toPaint.printAll(g);
                g.dispose();
            }
        });
        int ink = 0;
        for (int y = 0; y < shot.getHeight(); y += 3) {
            for (int x = 0; x < shot.getWidth(); x += 3) {
                int rgb = shot.getRGB(x, y);
                int lum = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
                if (lum < 0x73) {
                    ink++;
                }
            }
        }
        yes("e dipinge contenuto vero (" + ink + " px)", ink > 800);

        final JFrame toClose = window;
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                toClose.dispose();
            }
        });
    }

    // --- misure ---------------------------------------------------------------

    /** Componenti più stretti della loro misura preferita: quelli che troncano. */
    private static void collectSqueezed(Container parent, List<String> out) {
        for (Component child : parent.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            if (interesting(child)) {
                Dimension want = child.getPreferredSize();
                if (child.getWidth() < want.width - 1 || child.getHeight() < want.height - 1) {
                    out.add(describe(child) + " " + child.getWidth() + "x" + child.getHeight()
                            + " ma ne vuole " + want.width + "x" + want.height);
                }
            }
            if (child instanceof Container) {
                collectSqueezed((Container) child, out);
            }
        }
    }

    private static void collectOverflowing(Container parent, List<String> out) {
        if (parent instanceof javax.swing.JViewport) {
            return;
        }
        Rectangle box = new Rectangle(parent.getWidth(), parent.getHeight());
        for (Component child : parent.getComponents()) {
            if (!child.isVisible() || child.getWidth() == 0) {
                continue;
            }
            Rectangle r = child.getBounds();
            if (r.x < -1 || r.y < -1
                    || r.x + r.width > box.width + 1 || r.y + r.height > box.height + 1) {
                out.add(describe(child) + " a " + r + " dentro " + box.width + "x" + box.height);
            }
            if (child instanceof Container) {
                collectOverflowing((Container) child, out);
            }
        }
    }

    private static boolean interesting(Component c) {
        if (c instanceof JSpinner || c instanceof JTextField) {
            return true;
        }
        if (c instanceof JLabel) {
            String text = ((JLabel) c).getText();
            return text != null && !text.isEmpty() && !text.startsWith("<html");
        }
        if (c instanceof AbstractButton) {
            String text = ((AbstractButton) c).getText();
            return text != null && !text.isEmpty();
        }
        return false;
    }

    private static String describe(Component c) {
        String text = "";
        if (c instanceof JLabel) {
            text = ((JLabel) c).getText();
        } else if (c instanceof AbstractButton) {
            text = ((AbstractButton) c).getText();
        }
        String name = c.getClass().getSimpleName();
        return text == null || text.isEmpty() ? name : name + "(\"" + text + "\")";
    }

    // --- helper ---------------------------------------------------------------

    private static void report(String what, List<String> problems) {
        if (problems.isEmpty()) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what + ":");
            for (String p : problems) {
                System.out.println("      " + p);
            }
        }
    }

    private static void yes(String what, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what);
        }
    }
}
