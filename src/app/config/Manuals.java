package app.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * I manuali viaggiano <b>dentro il JAR</b>.
 *
 * <p>La versione precedente li cercava su disco accanto al programma, e se non
 * li trovava apriva GitHub. Sul PC di reparto è la scelta sbagliata due volte:
 * chi copia il solo JAR resta senza manuale, e quella macchina spesso non ha né
 * un browser aperto né internet. I due file sotto {@code src/app/docs} sono
 * l'unica copia che conta — il README ci punta, la finestra delle impostazioni
 * li mostra.
 *
 * <p>Non lancia mai e non torna mai {@code null}: una build a cui manca la
 * risorsa apre lo stesso il manuale, e ci scrive che manca.
 */
public final class Manuals {

    public static final String IT = "it";
    public static final String EN = "en";

    private static final String RESOURCE = "/app/docs/MANUAL.";
    private static final String SOURCE = "src/app/docs/MANUAL.";

    /** Dove leggere la versione aggiornata, e dove segnalare un errore. */
    public static final String REPOSITORY = "https://github.com/importriri/etichette-custom";

    private Manuals() {
    }

    /** Tutto ciò che non è {@code en} si legge come italiano: la lingua del reparto. */
    public static String text(String lang) {
        String code = EN.equalsIgnoreCase(lang) ? EN : IT;
        String body = fromClasspath(RESOURCE + code + ".md");
        if (body == null) {
            body = fromWorkingDir(SOURCE + code + ".md");
        }
        if (body == null) {
            return "# Manuale non disponibile\n\n"
                    + "Questa build non contiene il file del manuale. Trovi la versione "
                    + "aggiornata nel repository, in `src/app/docs`.";
        }
        return body;
    }

    private static String fromClasspath(String path) {
        InputStream in = Manuals.class.getResourceAsStream(path);
        if (in == null) {
            return null;
        }
        try {
            return read(in);
        } catch (IOException unreadable) {
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    /** Avviato da una cartella di classi in cui le risorse non sono state copiate. */
    private static String fromWorkingDir(String path) {
        File f = new File(path);
        if (!f.isFile()) {
            return null;
        }
        InputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            return read(in);
        } catch (IOException unreadable) {
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    private static String read(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(in, Charset.forName("UTF-8")));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException alreadyRead) {
            // chiudere una risorsa già letta non è un problema di nessuno
        }
    }
}
