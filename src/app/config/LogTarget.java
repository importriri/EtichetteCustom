package app.config;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Destinazione del log giornaliero: una cartella scelta dall'operatore più un
 * pattern di nome file. Nuovo giorno = nuovo file, il vecchio resta dov'è.
 *
 * <p>La cartella si verifica <b>quando l'operatore la sceglie</b>, non alla prima
 * stampa: in fabbrica un percorso di rete sparisce senza avvisare, e la
 * differenza tra scoprirlo nelle impostazioni e scoprirlo a metà giro è tutta.
 */
public final class LogTarget {

    /** Il {@code %s} viene sostituito con la data in formato yyyy-MM-dd. */
    public static final String DEFAULT_PATTERN = "etichette-%s.log";

    private final File directory;
    private final String pattern;

    public LogTarget(File directory, String pattern) {
        if (directory == null) {
            throw new IllegalArgumentException("Cartella di log non impostata.");
        }
        if (pattern == null || !pattern.contains("%s")) {
            throw new IllegalArgumentException("Il pattern del log deve contenere %s per la data.");
        }
        this.directory = directory;
        this.pattern = pattern;
    }

    public LogTarget(File directory) {
        this(directory, DEFAULT_PATTERN);
    }

    public File directory() {
        return directory;
    }

    public String pattern() {
        return pattern;
    }

    /**
     * Prova davvero a scrivere nella cartella. Da chiamare nel tab Impostazioni
     * appena l'operatore conferma la scelta, e di nuovo all'avvio dell'app.
     *
     * @throws IOException se la cartella non esiste o non è scrivibile; il
     *                     messaggio è già in italiano
     */
    public static void assertUsable(File dir) throws IOException {
        if (dir == null) {
            throw new IOException("Nessuna cartella di log selezionata.");
        }
        if (!dir.isDirectory()) {
            throw new IOException("La cartella non esiste o non è raggiungibile: "
                    + dir.getAbsolutePath());
        }
        File probe;
        try {
            probe = File.createTempFile("etichette-", ".tmp", dir);
        } catch (IOException e) {
            throw new IOException("Non ho i permessi per scrivere in "
                    + dir.getAbsolutePath() + " (" + e.getMessage() + ").", e);
        }
        if (!probe.delete()) {
            probe.deleteOnExit();
        }
    }

    /** Verifica la cartella di questa destinazione. */
    public void assertUsable() throws IOException {
        assertUsable(directory);
    }

    /** Il file di log di oggi. */
    public File fileForToday() {
        return fileFor(new Date());
    }

    /** Il file di log di un giorno preciso. */
    public File fileFor(Date day) {
        String stamp = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(day);
        return new File(directory, String.format(Locale.ROOT, pattern, stamp));
    }

    @Override
    public String toString() {
        return fileForToday().getAbsolutePath();
    }
}
