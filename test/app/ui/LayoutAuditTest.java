package app.ui;

import app.Main;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

// ispezione della finestra vera, componente per componente.
//
// Nasce dalle foto arrivate dal reparto: su Windows con lo schermo ingrandito i
// valori degli spinner uscivano tagliati sotto le frecce e i pulsanti in fondo
// finivano mezzi fuori. A schermo sembra tutto a posto finché non ci si trova
// davanti quel PC — quindi invece di guardare, si misura: ogni componente deve
// stare dentro il suo contenitore e avere almeno la larghezza che dichiara di
// volere. Un campo più stretto della sua misura preferita è un campo che
// tronca, e questo è il difetto.
//
//   xvfb-run java -cp out app.ui.LayoutAuditTest
public final class LayoutAuditTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("  skip  nessun display: lanciare sotto xvfb-run");
            return;
        }
        Path home = Files.createTempDirectory("etichette-audit");
        System.setProperty("user.home", home.toString());

        Main.main(new String[0]);
        JFrame window = awaitFrame("Etichette Custom", 8000);
        yes("la finestra c'è", window != null);
        if (window == null) {
            report();
            return;
        }

        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        yes("la finestra sta dentro lo schermo (" + window.getWidth() + "x"
                + window.getHeight() + " su " + screen.width + "x" + screen.height + ")",
                window.getWidth() <= screen.width && window.getHeight() <= screen.height);

        // niente più schede da girare: la schermata è una sola. Si misura
        // quella, poi le due finestrelle, poi si seleziona un elemento per far
        // comparire il pannello delle proprietà e si rimisura tutto
        auditWindow(window, "schermata principale");

        // i campi si aprono dal pannello dell'etichetta, che c'è solo quando
        // non è selezionato niente: quindi prima quello, poi la selezione
        openAndAudit(window, "campi");

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                selectFirstElement(window);
            }
        });
        Thread.sleep(250);
        auditWindow(window, "con un elemento selezionato");

        for (final String which : new String[] {"stampante", "impostazioni"}) {
            // invokeLater e non invokeAndWait: le finestrelle sono modali, e
            // aprirle blocca il thread grafico finché non si chiudono
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    press(window, which);
                }
            });
            // si aspetta finché la finestrella non c'è davvero, invece di
            // scommettere su un tempo fisso: quella delle impostazioni deve
            // anche rendere il manuale, e su una macchina lenta ci mette di più
            java.awt.Window dialog = awaitDialog(6000);
            yes("la finestrella \"" + which + "\" si apre", dialog != null);
            if (dialog != null) {
                auditWindow(dialog, which);
                final java.awt.Window toClose = dialog;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toClose.dispose();
                    }
                });
                Thread.sleep(400);
            }
        }

        report();
    }

    /** Apre una finestrella, la misura e la richiude. */
    private static void openAndAudit(final JFrame window, final String which) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                press(window, which);
            }
        });
        Thread.sleep(900);
        java.awt.Window dialog = openDialog();
        yes("la finestrella \"" + which + "\" si apre", dialog != null);
        if (dialog == null) {
            return;
        }
        auditWindow(dialog, which);
        final java.awt.Window toClose = dialog;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                toClose.dispose();
            }
        });
        Thread.sleep(400);
    }

    /** Misura una finestra intera: niente sborda, niente è più stretto del dovuto. */
    private static void auditWindow(java.awt.Window window, String what) {
        List<String> outside = new ArrayList<String>();
        collectOverflowing(((javax.swing.RootPaneContainer) window).getContentPane(), outside);
        report("\"" + what + "\": nessun componente esce dal suo contenitore", outside);

        List<String> squeezed = new ArrayList<String>();
        collectSqueezed(((javax.swing.RootPaneContainer) window).getContentPane(), squeezed);
        report("\"" + what + "\": nessun campo è più stretto di quanto dichiara", squeezed);

        List<String> cut = new ArrayList<String>();
        collectTruncated(((javax.swing.RootPaneContainer) window).getContentPane(), cut);
        report("\"" + what + "\": nessuna scritta esce tagliata", cut);
    }

    /** Seleziona il primo elemento dell'etichetta, come farebbe un clic. */
    private static void selectFirstElement(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof app.ui.PreviewPanel) {
                app.ui.PreviewPanel p = (app.ui.PreviewPanel) c;
                java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(p,
                        java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        0, p.getWidth() / 2, p.getHeight() / 2, 1, false);
                for (java.awt.event.MouseListener l : p.getMouseListeners()) {
                    l.mousePressed(click);
                }
                return;
            }
            if (c instanceof Container) {
                selectFirstElement((Container) c);
            }
        }
    }

    /** Preme il pulsante il cui suggerimento contiene quella parola. */
    private static void press(Container root, String word) {
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                String tip = b.getToolTipText();
                String text = b.getText();
                if (!b.isShowing()) {
                    continue;
                }
                if ((tip != null && tip.toLowerCase().contains(word))
                        || (text != null && text.toLowerCase().contains(word))) {
                    b.doClick();
                    return;
                }
            }
            if (c instanceof Container) {
                press((Container) c, word);
            }
        }
    }

    /** Aspetta che compaia una finestrella, fino al tempo indicato. */
    private static java.awt.Window awaitDialog(int timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            java.awt.Window w = openDialog();
            if (w != null) {
                Thread.sleep(250); // il tempo di disporre il contenuto
                return w;
            }
            Thread.sleep(100);
        }
        return null;
    }

    private static java.awt.Window openDialog() {
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            if (w instanceof javax.swing.JDialog && w.isShowing()) {
                return w;
            }
        }
        return null;
    }

    /**
     * Testi che a schermo escono tagliati, con i puntini al posto delle lettere.
     *
     * <p>È il difetto fotografato in reparto: "Posizio…" al posto di
     * "Posizione". Non lo trova il controllo sulle misure preferite, perché un
     * componente a cui è stata <i>imposta</i> una larghezza piccola dichiara
     * quella come preferita ed è formalmente a posto. Qui invece si misura la
     * stringa vera con il carattere vero e la si confronta con lo spazio che ha:
     * se non ci sta, Swing ci metterà i puntini, e questo è un difetto punto e
     * basta.
     */
    private static void collectTruncated(Container parent, List<String> out) {
        for (Component child : parent.getComponents()) {
            if (!child.isVisible() || child.getWidth() == 0) {
                continue;
            }
            String text = null;
            java.awt.Font font = child.getFont();
            java.awt.Insets pad = new java.awt.Insets(0, 0, 0, 0);
            if (child instanceof JLabel) {
                text = ((JLabel) child).getText();
                if (((JLabel) child).getBorder() != null) {
                    pad = ((JLabel) child).getBorder().getBorderInsets(child);
                }
            } else if (child instanceof AbstractButton) {
                text = ((AbstractButton) child).getText();
                if (((AbstractButton) child).getBorder() != null) {
                    pad = ((AbstractButton) child).getBorder().getBorderInsets(child);
                }
            }
            // il testo in HTML si dispone da solo su più righe: non si tronca
            if (text != null && !text.isEmpty() && !text.startsWith("<html") && font != null) {
                int needed = child.getFontMetrics(font).stringWidth(text)
                        + pad.left + pad.right;
                if (needed > child.getWidth() + 1) {
                    out.add(describe(child) + ": servono " + needed
                            + " px, ne ha " + child.getWidth());
                }
            }
            if (child instanceof Container) {
                collectTruncated((Container) child, out);
            }
        }
    }

    /** Componenti che sbordano dal genitore: quelli che a schermo spariscono a metà. */
    private static void collectOverflowing(Container parent, List<String> out) {
        if (parent instanceof JViewport) {
            return; // dentro un'area di scorrimento sbordare è il suo mestiere
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

    /**
     * Componenti più stretti o più bassi della loro misura preferita: sono
     * quelli che troncano il testo con i puntini o nascondono il numero sotto
     * le frecce dello spinner.
     */
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

    private static boolean interesting(Component c) {
        if (c instanceof JSpinner || c instanceof JTextField) {
            return true;
        }
        if (c instanceof JLabel) {
            String text = ((JLabel) c).getText();
            // le etichette in HTML mandano a capo: restringerle è previsto
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

    private static JFrame awaitFrame(String title, int timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Frame f : Frame.getFrames()) {
                if (f instanceof JFrame && title.equals(f.getTitle()) && f.isShowing()) {
                    return (JFrame) f;
                }
            }
            Thread.sleep(100);
        }
        return null;
    }

    private static void report(String what, List<String> problems) {
        if (problems.isEmpty()) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what);
            for (int i = 0; i < Math.min(6, problems.size()); i++) {
                System.out.println("        " + problems.get(i));
            }
            if (problems.size() > 6) {
                System.out.println("        ... e altri " + (problems.size() - 6));
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

    private static void report() {
        System.out.println("LayoutAudit: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
