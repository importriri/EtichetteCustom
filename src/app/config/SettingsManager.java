package app.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Impostazioni persistenti dell'applicazione.
 *
 * <p>Il salvataggio è atomico: si scrive un file temporaneo affianco e lo si
 * sposta sopra quello vero. Se il PC viene spento mentre si salva, ci si ritrova
 * con le impostazioni vecchie, mai con un file mezzo scritto.
 */
public final class SettingsManager {

    public static final String KEY_LOG_DIR = "label.log.dir";
    public static final String KEY_LOG_PATTERN = "label.log.pattern";
    public static final String KEY_SERIAL_DIGITS = "label.serial.digits";
    public static final String KEY_WIDTH_MM = "label.width.mm";
    public static final String KEY_HEIGHT_MM = "label.height.mm";
    public static final String KEY_DPI = "label.dpi";
    public static final String KEY_ECC = "label.qr.ecc";
    public static final String KEY_LAST_EXPORT_DIR = "label.export.dir";
    public static final String KEY_LAYOUT = "label.layout";
    /** Taratura della stampa, serializzata da {@code app.core.PrintSetup}. */
    public static final String KEY_PRINT_SETUP = "label.print.setup";
    /** Variante grafica: {@code latte} chiara oppure {@code mocha} scura. */
    public static final String KEY_UI_FLAVOR = "ui.flavor";
    /** Il codice di prova mostrato nelle impostazioni. */
    public static final String KEY_SAMPLE = "label.sample";

    private static final SettingsManager INSTANCE = new SettingsManager();

    private final File file;
    private final Properties props = new Properties();

    private SettingsManager() {
        this.file = new File(configDirectory(), "settings.properties");
        load();
    }

    public static SettingsManager get() {
        return INSTANCE;
    }

    /** Dove finiscono le impostazioni: APPDATA su Windows, ~/.config altrove. */
    public static File configDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        File base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = (appData != null && !appData.isEmpty())
                    ? new File(appData)
                    : new File(System.getProperty("user.home"));
            return new File(base, "EtichetteCustom");
        }
        base = new File(System.getProperty("user.home"), ".config");
        return new File(base, "etichette-custom");
    }

    private void load() {
        if (!file.isFile()) {
            return;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
        } catch (IOException e) {
            // impostazioni illeggibili: si riparte dai default, non è un motivo per non avviarsi
        } finally {
            close(in);
        }
    }

    /** Scrive su disco. Restituisce {@code false} se non ci è riuscito. */
    public boolean save() {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            return false;
        }
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        OutputStream out = null;
        try {
            out = new FileOutputStream(temp);
            props.store(out, "Etichette Custom");
            out.flush();
            close(out);
            out = null;
            try {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            close(out);
            if (temp.isFile()) {
                temp.delete();
            }
        }
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // niente da fare
            }
        }
    }

    // --- accessori tipizzati --------------------------------------------------

    public String getString(String key, String fallback) {
        String v = props.getProperty(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public void setString(String key, String value) {
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setInt(String key, int value) {
        props.setProperty(key, String.valueOf(value));
    }

    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(props.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setDouble(String key, double value) {
        props.setProperty(key, String.valueOf(value));
    }

    /** La cartella di log configurata, oppure {@code null} se non ne è stata scelta una. */
    public File logDirectory() {
        String v = props.getProperty(KEY_LOG_DIR);
        return (v == null || v.isEmpty()) ? null : new File(v);
    }

    /** Ripiego quando la cartella scelta non è utilizzabile: accanto alle impostazioni. */
    public static File fallbackLogDirectory() {
        return new File(configDirectory(), "log");
    }
}
