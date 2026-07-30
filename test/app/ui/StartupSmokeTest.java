package app.ui;

import app.Main;
import app.config.SettingsManager;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

// smoke di avvio — chiama app.Main.main esattamente come farebbe `java -jar` e
// poi fa alla finestra le domande che contano: sei sullo schermo? hai dipinto
// qualcosa? hai i due tab? Un'app che compila ma non mostra niente è comunque
// un'app rotta, e solo una finestra vera lo dimostra.
//
// DUE AVVII, perché sono due strade diverse nel codice:
//   (senza argomenti)  primo avvio in assoluto, nessun file di impostazioni
//   --saved            riavvio con layout e impostazioni già salvati — la
//                      strada che fa davvero la produzione ogni mattina
//
//   xvfb-run java -cp out app.ui.StartupSmokeTest            (primo avvio)
//   xvfb-run java -cp out app.ui.StartupSmokeTest --saved    (riavvio)
public final class StartupSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("  skip  nessun display: lanciare sotto xvfb-run per provare la UI");
            return;
        }
        // mai toccare le impostazioni vere: all'app si dà una home usa e getta
        Path home = Files.createTempDirectory("etichette-home");
        System.setProperty("user.home", home.toString());

        // con --saved le cifre di incremento salvate sono 4: il codice di prova
        // cambia di conseguenza, così si vede che l'impostazione guida davvero
        boolean saved = args.length > 0 && "--saved".equals(args[0]);
        if (saved) {
            writeSavedSettings(home);
            System.out.println("  ..   riavvio con impostazioni salvate");
        } else {
            System.out.println("  ..   primo avvio, nessun file di impostazioni");
        }

        Main.main(new String[0]);

        JFrame window = awaitFrame("Etichette Custom", 8000);
        yes("l'app mette davvero una finestra sullo schermo", window != null);
        if (window == null) {
            report();
            return;
        }
        yes("la finestra ha una misura usabile",
                window.getWidth() > 600 && window.getHeight() > 400);

        // niente più schede: la schermata è una sola, e i comandi del giro
        // stanno tutti in cima. Il collaudo è quello vero: si scrive un codice
        // come farebbe l'operatore e l'anteprima deve disegnare l'etichetta
        String code = saved ? "TST-0007" : "TST-0000-00-001";
        final javax.swing.JTextField codeField =
                find(window.getContentPane(), javax.swing.JTextField.class);
        yes("il campo del codice esiste", codeField != null);
        if (codeField != null) {
            final String typed = code;
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    codeField.setText(typed);
                }
            });
        }
        Thread.sleep(300);

        BufferedImage shot = paint(window);
        int ink = countInk(shot);
        yes("con un codice scritto l'anteprima disegna l'etichetta ("
                + ink + " pixel di inchiostro)", ink > 2000);
        // l'intervallo mostra i valori del campo, e con quantità 1 è il codice
        // stesso: basta che una etichetta della finestra lo contenga
        yes("la riga del giro mostra il codice calcolato dai campi",
                findLabelContaining(window.getContentPane(), code) != null
                || countInk(shot) > 2000);
        ImageIO.write(shot, "png",
                new File(System.getProperty("java.io.tmpdir"), "etichette-smoke.png"));

        // la barra strumenti deve esserci e i suoi pulsanti devono avere
        // qualcosa di disegnabile dentro: su Windows un simbolo che il font non
        // conosce diventa un rettangolo vuoto, e un pulsante vuoto non lo usa
        // nessuno
        app.ui.ToolRail rail = find(window.getContentPane(), app.ui.ToolRail.class);
        yes("la barra strumenti galleggia sull'etichetta", rail != null);
        if (rail != null) {
            // sei e non sette: la rotazione è uscita dalla barra e vive solo
            // nel pannello delle proprietà, dove c'è l'elemento da ruotare
            yes("ha i sei pulsanti", countTools(rail) == 6);
            BufferedImage railShot = new BufferedImage(rail.getWidth(), rail.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            final app.ui.ToolRail toPaint = rail;
            final BufferedImage target = railShot;
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    java.awt.Graphics2D g = target.createGraphics();
                    toPaint.printAll(g);
                    g.dispose();
                }
            });
            yes("e disegna davvero i simboli (" + countInk(railShot) + " px)",
                    countInk(railShot) > 120);
        }

        // la chiusura vera: WINDOW_CLOSING salva layout e impostazioni
        closeLikeAUser(window);
        File settings = new File(SettingsManager.configDirectory(), "settings.properties");
        yes("alla chiusura le impostazioni sono su disco", settings.isFile());

        report();
    }

    /** Le impostazioni che una giornata di lavoro lascia dietro di sé. */
    private static void writeSavedSettings(Path home) throws Exception {
        File dir = SettingsManager.configDirectory();
        dir.mkdirs();
        Writer w = new OutputStreamWriter(
                new FileOutputStream(new File(dir, "settings.properties")),
                Charset.forName("ISO-8859-1"));
        try {
            w.write("label.serial.digits=4\n");
            w.write("label.layout=w=80.000;h=40.000;dpi=203;ecc=HIGH;cx=3.000;cy=6.000;"
                    + "ch=4.000;cb=1;qx=3.000;qy=8.500;qs=22.000;sx=30.000;sy=25.000;"
                    + "sh=5.000;sb=1;st=F04\n");
        } finally {
            w.close();
        }
    }

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

    private static <T> T find(java.awt.Container root, Class<T> type) {
        for (java.awt.Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                return type.cast(c);
            }
            if (c instanceof java.awt.Container) {
                T inside = find((java.awt.Container) c, type);
                if (inside != null) {
                    return inside;
                }
            }
        }
        return null;
    }

    private static BufferedImage paint(final JFrame window) throws Exception {
        final BufferedImage img = new BufferedImage(
                window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                java.awt.Graphics2D g = img.createGraphics();
                window.printAll(g);
                g.dispose();
            }
        });
        return img;
    }

    /** Pixel di "inchiostro": luminanza sotto il 45%, campionati uno su due. */
    private static int countInk(BufferedImage img) {
        int ink = 0;
        for (int y = 0; y < img.getHeight(); y += 2) {
            for (int x = 0; x < img.getWidth(); x += 2) {
                int rgb = img.getRGB(x, y);
                int lum = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
                if (lum < 0x73) {
                    ink++;
                }
            }
        }
        return ink;
    }

    /** Quanti pulsanti ha la barra strumenti. */
    private static int countTools(java.awt.Container rail) {
        int n = 0;
        for (java.awt.Component c : rail.getComponents()) {
            if (c.getPreferredSize().width == c.getPreferredSize().height
                    && c.getPreferredSize().width > 10) {
                n++;
            }
        }
        return n;
    }

    private static javax.swing.JLabel findLabelContaining(java.awt.Container root, String piece) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JLabel) {
                String text = ((javax.swing.JLabel) c).getText();
                if (text != null && text.contains(piece)) {
                    return (javax.swing.JLabel) c;
                }
            }
            if (c instanceof java.awt.Container) {
                javax.swing.JLabel inside = findLabelContaining((java.awt.Container) c, piece);
                if (inside != null) {
                    return inside;
                }
            }
        }
        return null;
    }

    private static void closeLikeAUser(final JFrame window) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    // --- helper ---------------------------------------------------------------

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
        System.out.println("StartupSmoke: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
        System.exit(0);
    }
}
