package app.core;

import app.config.SettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * I layout salvati dall'operatore.
 *
 * <p>Prima l'applicazione arrivava con dentro i disegni di clienti veri. Era
 * comodo da mostrare e sbagliato da consegnare: quei numeri sono di chi li ha
 * commissionati, non del programma, e un'app che si porta dietro il capitolato
 * di un cliente non si può dare al successivo.
 *
 * <p>Adesso l'etichetta la disegna chi la usa, e la salva. Un layout è un file
 * di testo dentro la cartella di configurazione: si salva con un nome, si
 * riapre dall'elenco, si cancella. Il programma non sa e non deve sapere che
 * cosa ci sia scritto dentro.
 *
 * <p>Il formato è lo stesso di {@link LabelModel#toStorage()}: una riga sola.
 * Copiare un layout su un'altra postazione vuol dire copiare un file da pochi
 * byte, non esportare niente.
 */
public final class LayoutStore {

    /** Estensione dei file di layout. */
    public static final String EXTENSION = ".etichetta";

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private LayoutStore() {
    }

    /** La cartella dei layout, creata alla prima occorrenza. */
    public static File directory() {
        File dir = new File(SettingsManager.configDirectory(), "layouts");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** I nomi dei layout salvati, in ordine alfabetico. */
    public static List<String> names() {
        List<String> out = new ArrayList<String>();
        File[] files = directory().listFiles();
        if (files == null) {
            return out;
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (File f : files) {
            String name = f.getName();
            if (f.isFile() && name.toLowerCase(Locale.ITALIAN).endsWith(EXTENSION)) {
                out.add(name.substring(0, name.length() - EXTENSION.length()));
            }
        }
        return out;
    }

    /** Il file di un layout, esista o no. */
    public static File fileFor(String name) {
        return new File(directory(), safeName(name) + EXTENSION);
    }

    public static boolean exists(String name) {
        return fileFor(name).isFile();
    }

    /**
     * Salva il layout con quel nome, sovrascrivendo se già c'è.
     *
     * <p>Il nome diventa un nome di file, quindi i caratteri che i filesystem
     * non digeriscono vengono sostituiti. Meglio un nome un po' diverso da
     * quello scritto che un salvataggio che fallisce in silenzio.
     */
    public static void save(String name, LabelModel model) throws IOException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Dai un nome al layout prima di salvarlo.");
        }
        if (model == null) {
            throw new IllegalArgumentException("Nessun layout da salvare.");
        }
        File target = fileFor(name);
        Writer w = new OutputStreamWriter(new FileOutputStream(target), UTF8);
        try {
            w.write(model.toStorage());
            w.write('\n');
        } finally {
            w.close();
        }
    }

    /** Rilegge un layout salvato. */
    public static LabelModel load(String name) throws IOException {
        File source = fileFor(name);
        if (!source.isFile()) {
            throw new IOException("Il layout \"" + name + "\" non c'è più in "
                    + directory().getAbsolutePath() + ".");
        }
        StringBuilder sb = new StringBuilder();
        Reader r = new InputStreamReader(new FileInputStream(source), UTF8);
        try {
            char[] buffer = new char[4096];
            int read;
            while ((read = r.read(buffer)) > 0) {
                sb.append(buffer, 0, read);
            }
        } finally {
            r.close();
        }
        LabelModel model = LabelModel.fromStorage(sb.toString().trim());
        model.setTemplateName(name);
        return model;
    }

    /** Cancella un layout salvato; {@code false} se non c'era. */
    public static boolean delete(String name) {
        File f = fileFor(name);
        return f.isFile() && f.delete();
    }

    /** Ripulisce un nome perché possa diventare un nome di file. */
    public static String safeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?'
                    || c == '"' || c == '<' || c == '>' || c == '|' || c < 32) {
                sb.append('-');
            } else {
                sb.append(c);
            }
        }
        String clean = sb.toString().trim();
        return clean.isEmpty() ? "layout" : clean;
    }
}
