package app.archivio;

import app.modello.Etichetta;
import app.modello.Libreria;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La cartella dove vivono le etichette. Un file per etichetta.
 *
 * Il salvataggio passa sempre da un file temporaneo e poi da una rinomina:
 * se qualcuno stacca la corrente al PC del reparto a meta' scrittura -
 * e succede - il file vecchio e' ancora intero.
 */
public class Archivio {

    private static final String CODA = ".etichetta";

    private final File cartella;
    private final Map<Etichetta, File> provenienza = new LinkedHashMap<Etichetta, File>();

    public Archivio(File cartella) {
        if (cartella == null) {
            throw new IllegalArgumentException("serve una cartella");
        }
        this.cartella = cartella;
    }

    public File cartella() {
        return cartella;
    }

    /**
     * Carica quello che c'e'. Se la cartella e' vuota o non esiste, ci
     * mette le etichette di partenza: al primo avvio in reparto nessuno
     * deve trovarsi davanti una vetrina vuota e chiedersi se e' rotto.
     */
    public List<Etichetta> carica() {
        provenienza.clear();
        List<Etichetta> out = new ArrayList<Etichetta>();
        File[] file = cartella.listFiles();
        if (file != null) {
            Arrays.sort(file, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            for (File f : file) {
                if (!f.isFile() || !f.getName().endsWith(CODA)) {
                    continue;
                }
                try {
                    Etichetta e = Formato.leggi(testoDi(f));
                    provenienza.put(e, f);
                    out.add(e);
                } catch (RuntimeException rotta) {
                    /* un file rovinato non deve impedire di aprire gli altri */
                    System.err.println("salto " + f.getName() + ": " + rotta.getMessage());
                } catch (IOException rotta) {
                    System.err.println("salto " + f.getName() + ": " + rotta.getMessage());
                }
            }
        }
        if (out.isEmpty()) {
            for (Etichetta e : Libreria.iniziale()) {
                out.add(e);
                try {
                    salva(e);
                } catch (IOException rotta) {
                    System.err.println("non riesco a scrivere in " + cartella + ": "
                            + rotta.getMessage());
                }
            }
        }
        return out;
    }

    public void salva(Etichetta e) throws IOException {
        if (!cartella.isDirectory() && !cartella.mkdirs()) {
            throw new IOException("non riesco a creare " + cartella);
        }
        File destinazione = provenienza.get(e);
        if (destinazione == null || !destinazione.getName().equals(nomeFile(e.nome()))) {
            File nuovo = liberoPer(e.nome(), destinazione);
            if (destinazione != null && destinazione.exists() && !destinazione.delete()) {
                System.err.println("non riesco a togliere il vecchio " + destinazione.getName());
            }
            destinazione = nuovo;
            provenienza.put(e, destinazione);
        }
        scriviAtomico(destinazione, Formato.scrivi(e));
    }

    public void elimina(Etichetta e) {
        File f = provenienza.remove(e);
        if (f != null && f.exists() && !f.delete()) {
            System.err.println("non riesco a cancellare " + f.getName());
        }
    }

    /** Dove finirebbe questa etichetta. Utile per dirlo all'operatore. */
    public File fileDi(Etichetta e) {
        return provenienza.get(e);
    }

    private File liberoPer(String nome, File tranne) {
        File primo = new File(cartella, nomeFile(nome));
        if (!primo.exists() || primo.equals(tranne)) {
            return primo;
        }
        for (int i = 2; i < 1000; i++) {
            File f = new File(cartella, nomeFile(nome + " " + i));
            if (!f.exists()) {
                return f;
            }
        }
        throw new IllegalStateException("troppe etichette con lo stesso nome: " + nome);
    }

    static String nomeFile(String nome) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < nome.length(); i++) {
            char c = Character.toLowerCase(nome.charAt(i));
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                b.append(c);
            } else if (b.length() > 0 && b.charAt(b.length() - 1) != '-') {
                b.append('-');
            }
        }
        while (b.length() > 0 && b.charAt(b.length() - 1) == '-') {
            b.setLength(b.length() - 1);
        }
        if (b.length() == 0) {
            b.append("etichetta");
        }
        if (b.length() > 60) {
            b.setLength(60);
        }
        return b + CODA;
    }

    private static void scriviAtomico(File destinazione, String contenuto) throws IOException {
        File temporaneo = new File(destinazione.getParentFile(),
                destinazione.getName() + ".parziale");
        FileOutputStream flusso = new FileOutputStream(temporaneo);
        try {
            Writer w = new OutputStreamWriter(flusso, "UTF-8");
            w.write(contenuto);
            w.flush();
            flusso.getFD().sync();
            w.close();
        } finally {
            flusso.close();
        }
        if (destinazione.exists() && !destinazione.delete()) {
            throw new IOException("non riesco a sostituire " + destinazione.getName());
        }
        if (!temporaneo.renameTo(destinazione)) {
            throw new IOException("non riesco a rinominare " + temporaneo.getName());
        }
    }

    private static String testoDi(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream fuori = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int quanti;
            while ((quanti = in.read(buffer)) > 0) {
                fuori.write(buffer, 0, quanti);
            }
            return new String(fuori.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
