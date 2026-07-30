package app.core;

import app.config.LogTarget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Registro giornaliero in append: un file per giorno, nessuna riga sovrascritta.
 *
 * <p>Se la cartella scelta non è raggiungibile — un percorso di rete che sparisce
 * è la normalità in reparto — si ripiega sulla cartella locale e lo si segnala,
 * invece di bloccare la stampa. Un log che non si scrive non è un buon motivo per
 * fermare la produzione, ma deve saperlo l'operatore.
 */
public final class DayLog {

    private final LogTarget primary;
    private final LogTarget fallback;
    private volatile boolean degraded;

    public DayLog(LogTarget primary, LogTarget fallback) {
        if (primary == null || fallback == null) {
            throw new IllegalArgumentException("Destinazioni di log mancanti.");
        }
        this.primary = primary;
        this.fallback = fallback;
    }

    /** {@code true} se stiamo scrivendo sul ripiego perché la cartella scelta non va. */
    public boolean isDegraded() {
        return degraded;
    }

    /** Il file su cui si sta effettivamente scrivendo oggi. */
    public File currentFile() {
        return (degraded ? fallback : primary).fileForToday();
    }

    /** Aggiunge una riga con l'ora davanti. Non lancia: al massimo ripiega. */
    public synchronized void append(String message) {
        String line = new SimpleDateFormat("HH:mm:ss", Locale.ITALIAN).format(new Date())
                + "  " + message;
        if (!degraded && tryWrite(primary, line)) {
            return;
        }
        degraded = true;
        tryWrite(fallback, line);
    }

    /** Registra una stampa: quante etichette, da quale codice a quale. */
    public void logRun(String from, String to, int count, String where) {
        append(String.format(Locale.ITALIAN, "%-9s %4d etichette  %s -> %s", where, count, from, to));
    }

    private boolean tryWrite(LogTarget target, String line) {
        Writer w = null;
        try {
            File dir = target.directory();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return false;
            }
            w = new OutputStreamWriter(new FileOutputStream(target.fileForToday(), true), "UTF-8");
            w.write(line);
            w.write(System.getProperty("line.separator", "\n"));
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException ignored) {
                    // il file resta com'è
                }
            }
        }
    }
}
