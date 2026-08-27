package app.modello;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Impostazioni persistenti della postazione. */
public class Impostazioni {
    private static final String STAMPANTE_PREDEFINITA = "Datamax E-4203";
    private final File file;
    private File cartellaLog;
    private File cartellaEtichette;
    private String stampante = STAMPANTE_PREDEFINITA;
    private int risoluzioneDpi = 203;

    public Impostazioni() {
        /*
         * v2 usa deliberatamente una radice nuova: un PC di produzione che
         * aveva provato versioni precedenti parte pulito, senza import impliciti.
         * I vecchi dati restano intatti nella vecchia cartella.
         */
        File casa = new File(System.getProperty("user.home", "."), "EtichetteCustom-v2");
        file = new File(casa, "impostazioni.properties");
        cartellaLog = new File(casa, "log");
        cartellaEtichette = new File(casa, "etichette");
        carica();
    }

    private void carica() {
        if (!file.isFile()) return;
        Properties p = new Properties();
        try {
            FileInputStream in = new FileInputStream(file);
            try { p.load(in); } finally { in.close(); }
            String v = p.getProperty("cartella.etichette");
            if (v != null && !v.trim().isEmpty()) cartellaEtichette = new File(v.trim());
            v = p.getProperty("cartella.registro");
            if (v != null && !v.trim().isEmpty()) cartellaLog = new File(v.trim());
            v = p.getProperty("stampante.nome");
            if (v != null && !v.trim().isEmpty()) stampante = v.trim();
            v = p.getProperty("stampante.dpi");
            if (v != null) {
                try {
                    int n = Integer.parseInt(v.trim());
                    if (n >= 100 && n <= 1200) risoluzioneDpi = n;
                } catch (NumberFormatException ignorato) { }
            }
        } catch (IOException ignorato) {
            /* I valori predefiniti permettono comunque di avviare l'app. */
        }
    }

    public void salva() throws IOException {
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("non riesco a creare " + parent);
        }
        Properties p = new Properties();
        p.setProperty("cartella.etichette", cartellaEtichette.getAbsolutePath());
        p.setProperty("cartella.registro", cartellaLog.getAbsolutePath());
        p.setProperty("stampante.nome", stampante);
        p.setProperty("stampante.dpi", Integer.toString(risoluzioneDpi));
        File tmp = new File(parent, file.getName() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            p.store(out, "Etichette Custom v2");
            out.getFD().sync();
        } finally { out.close(); }
        if (file.exists() && !file.delete()) {
            tmp.delete();
            throw new IOException("non riesco a sostituire " + file);
        }
        if (!tmp.renameTo(file)) {
            throw new IOException("non riesco a salvare " + file);
        }
    }

    public File file() { return file; }
    public File cartellaEtichette() { return cartellaEtichette; }
    public void cartellaEtichette(File f) { if (f == null) throw new IllegalArgumentException("le etichette devono stare da qualche parte"); cartellaEtichette = f; }
    public File cartellaLog() { return cartellaLog; }
    public void cartellaLog(File f) { if (f == null) throw new IllegalArgumentException("il registro deve avere una cartella"); cartellaLog = f; }
    public String stampante() { return stampante; }
    public void stampante(String s) { stampante = s == null || s.trim().isEmpty() ? "Stampante" : s.trim(); }
    public int risoluzioneDpi() { return risoluzioneDpi; }
    public void risoluzioneDpi(int d) { if (d < 100 || d > 1200) throw new IllegalArgumentException("risoluzione fuori intervallo: " + d); risoluzioneDpi = d; }
}
