package app.config;

import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import javax.swing.UIManager;

/**
 * Quanto vanno ingranditi caratteri e spaziature su questo schermo.
 *
 * <p>Serve per un difetto preciso visto su Windows: con lo schermo al 125% o
 * al 150% i numeri negli spinner uscivano tagliati e le etichette dei campi si
 * accavallavano. La causa non è Swing in sé, è mescolare due mondi — misure
 * scritte in pixel fissi dentro il codice e un carattere di sistema che invece
 * cresce con lo schermo.
 *
 * <p>La regola qui è una sola: <b>chi scala, scala da solo</b>. Da Java 9 in
 * poi il toolkit ridimensiona già tutto per conto suo, e in quel caso il
 * fattore vale 1 — moltiplicare due volte è come non scalare affatto, solo più
 * storto. Quando invece il toolkit non scala (Java 8, il caso di un PC di
 * reparto con la runtime vecchia) il fattore viene dai DPI dichiarati dallo
 * schermo.
 */
public final class UiScale {

    private static final double FACTOR = compute();

    private UiScale() {
    }

    /**
     * Forzatura manuale della scala, per due usi.
     *
     * <p>Il primo è il collaudo: su Linux non esiste uno schermo Windows al
     * 150%, e senza questa manopola i difetti da schermo ingrandito si
     * scoprirebbero solo in reparto, che è dove sono stati scoperti la prima
     * volta.
     *
     * <p>Il secondo è l'assistenza: se su un PC il carattere resta minuscolo
     * perché il driver dello schermo dichiara DPI sbagliati, si aggiunge
     * {@code -Detichette.uiscale=1.5} al collegamento e si va avanti a
     * lavorare, senza aspettare una versione nuova.
     */
    public static final String SCALE_PROPERTY = "etichette.uiscale";

    private static double compute() {
        String forced = System.getProperty(SCALE_PROPERTY);
        if (forced != null) {
            try {
                return clamp(Double.parseDouble(forced.trim().replace(',', '.')));
            } catch (NumberFormatException notANumber) {
                // valore scritto male nel collegamento: si continua col calcolo
            }
        }
        if (GraphicsEnvironment.isHeadless()) {
            return 1.0;
        }
        try {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            double toolkitScale = gc.getDefaultTransform().getScaleX();
            if (toolkitScale > 1.05) {
                return 1.0; // ci pensa già il toolkit
            }
            double dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            return clamp(dpi / 96.0);
        } catch (RuntimeException noScreen) {
            return 1.0;
        }
    }

    private static double clamp(double v) {
        if (Double.isNaN(v) || v < 1.0) {
            return 1.0;
        }
        return Math.min(2.5, v);
    }

    /** Il fattore di scala, sempre tra 1.0 e 2.5. */
    public static double factor() {
        return FACTOR;
    }

    /** Scala una misura in pixel. */
    public static int px(int value) {
        return (int) Math.round(value * FACTOR);
    }

    /** Scala un corpo di carattere. */
    public static float pt(float value) {
        return (float) (value * FACTOR);
    }

    /**
     * Il carattere dell'interfaccia di sistema, se il look and feel ne
     * dichiara uno: su Windows è Segoe UI alla misura scelta dall'utente, e
     * partire da lì è più corretto che imporre un corpo scritto a mano.
     */
    public static Font systemFont(Font fallback) {
        Font f = UIManager.getFont("Label.font");
        return f != null ? f : fallback;
    }
}
