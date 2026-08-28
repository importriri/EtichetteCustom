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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** File-backed label archive with one file per label. */
public class Archivio {
    private static final String CODA = ".etichetta";

    private final File cartella;
    private final Map<Etichetta, File> provenienza = new LinkedHashMap<Etichetta, File>();

    public Archivio(File cartella) {
        if (cartella == null) throw new IllegalArgumentException("serve una cartella");
        this.cartella = cartella;
    }

    public File cartella() { return cartella; }

    public List<Etichetta> carica() {
        provenienza.clear();
        List<Etichetta> out = new ArrayList<Etichetta>();
        File[] file = cartella.listFiles();
        if (file != null) {
            Arrays.sort(file, new Comparator<File>() {
                @Override public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            for (File f : file) {
                if (!f.isFile() || !f.getName().endsWith(CODA)) continue;
                try {
                    Etichetta e = Formato.leggi(testoDi(f));
                    provenienza.put(e, f);
                    out.add(e);
                } catch (RuntimeException broken) {
                    System.err.println("skipping " + f.getName() + ": " + broken.getMessage());
                } catch (IOException broken) {
                    System.err.println("skipping " + f.getName() + ": " + broken.getMessage());
                }
            }
        }

        if (out.isEmpty()) {
            for (Etichetta e : Libreria.inizialeV2()) {
                out.add(e);
                try {
                    salva(e);
                } catch (IOException broken) {
                    System.err.println("non riesco a scrivere in " + cartella + ": "
                            + broken.getMessage());
                }
            }
        }
        return out;
    }

    public void salva(Etichetta e) throws IOException {
        if (!cartella.isDirectory() && !cartella.mkdirs()) {
            throw new IOException("cannot create " + cartella);
        }

        File precedente = provenienza.get(e);
        File destinazione = precedente;
        boolean nomeCambiato = destinazione == null
                || !destinazione.getName().equals(nomeFile(e.nome()));
        if (nomeCambiato) destinazione = liberoPer(e.nome(), precedente);

        scriviAtomico(destinazione, Formato.scrivi(e));

        if (nomeCambiato) {
            provenienza.put(e, destinazione);
            if (precedente != null && !precedente.equals(destinazione)
                    && precedente.exists() && !precedente.delete()) {
                System.err.println("cannot remove previous file " + precedente.getName());
            }
        }
    }

    public void elimina(Etichetta e) {
        File f = provenienza.remove(e);
        if (f != null && f.exists() && !f.delete()) {
            System.err.println("non riesco a cancellare " + f.getName());
        }
    }

    public File fileDi(Etichetta e) { return provenienza.get(e); }

    private File liberoPer(String nome, File tranne) {
        File primo = new File(cartella, nomeFile(nome));
        if (!primo.exists() || primo.equals(tranne)) return primo;
        for (int i = 2; i < 1000; i++) {
            File f = new File(cartella, nomeFile(nome + " " + i));
            if (!f.exists()) return f;
        }
        throw new IllegalStateException("troppe etichette con lo stesso nome: " + nome);
    }

    static String nomeFile(String nome) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < nome.length(); i++) {
            char c = Character.toLowerCase(nome.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                b.append(c);
            } else if (b.length() > 0 && b.charAt(b.length() - 1) != '-') {
                b.append('-');
            }
        }
        while (b.length() > 0 && b.charAt(b.length() - 1) == '-') {
            b.setLength(b.length() - 1);
        }
        if (b.length() == 0) b.append("etichetta");
        if (b.length() > 60) b.setLength(60);
        return b + CODA;
    }

    private static void scriviAtomico(File destinazione, String contenuto) throws IOException {
        File temporaneo = new File(destinazione.getParentFile(),
                destinazione.getName() + ".parziale");
        FileOutputStream flusso = new FileOutputStream(temporaneo);
        Writer writer = new OutputStreamWriter(flusso, "UTF-8");
        try {
            writer.write(contenuto);
            writer.flush();
            flusso.getFD().sync();
        } finally {
            writer.close();
        }

        try {
            Files.move(temporaneo.toPath(), destinazione.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporaneo.toPath(), destinazione.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            temporaneo.delete();
            throw failure;
        }
    }

    private static String testoDi(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream fuori = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int quanti;
            while ((quanti = in.read(buffer)) > 0) fuori.write(buffer, 0, quanti);
            return new String(fuori.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
