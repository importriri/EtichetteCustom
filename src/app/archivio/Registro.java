package app.archivio;

import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Etichetta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Date;

/**
 * Append-only print log with one row per printed label and one file per day.
 * The tab-separated format stays readable without the application.
 */
public class Registro {

    private final File cartella;
    private static int prossimoGiro;

    public Registro(File cartella) {
        if (cartella == null) {
            throw new IllegalArgumentException("il registro ha bisogno di una cartella");
        }
        this.cartella = cartella;
    }

    public File cartella() {
        return cartella;
    }

    public File fileDi(Date quando) {
        return new File(cartella, new SimpleDateFormat("yyyy-MM-dd").format(quando) + ".log");
    }

    /** Appends one complete print run, one row per label. */
    public File annota(Etichetta eti, String[] codici, String stampante) throws IOException {
        if (codici == null || codici.length == 0) {
            throw new IllegalArgumentException("un giro senza codici non si annota");
        }
        Date adesso = new Date();
        File file = fileDi(adesso);
        if (!cartella.isDirectory() && !cartella.mkdirs()) {
            throw new IOException("cannot create print-log directory: " + cartella);
        }
        boolean nuovo = !file.exists() || file.length() == 0;

        StringBuilder b = new StringBuilder();
        if (nuovo) {
            b.append("# etichette-custom - registro delle stampe\n");
            b.append("giro\tora\tetichetta\tcodice\tcampi chiesti\tstampante\n");
        }
        /* The run suffix separates two prints completed during the same millisecond. */
        String giro = new SimpleDateFormat("HHmmssSSS").format(adesso) + "-" + (prossimoGiro++);
        String ora = new SimpleDateFormat("HH:mm:ss").format(adesso);
        String chiesti = chiesti(eti);
        String nome = Formato.fuga(eti.nome());
        String dove = Formato.fuga(stampante == null ? "" : stampante);
        for (String c : codici) {
            b.append(giro).append('\t').append(ora).append('\t').append(nome).append('\t')
                    .append(Formato.fuga(c)).append('\t')
                    .append(chiesti).append('\t').append(dove).append('\n');
        }
        accoda(file, b.toString());
        return file;
    }

    private static String chiesti(Etichetta eti) {
        StringBuilder b = new StringBuilder();
        for (Campo c : eti.campi()) {
            if (c.comportamento() != Comportamento.CHIESTO) {
                continue;
            }
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(Formato.fuga(c.nome())).append('=').append(Formato.fuga(c.valore()));
        }
        return b.toString();
    }

    /** One print run reconstructed from the daily log. */
    public static final class Giro {
        private final String quando;
        private final String etichetta;
        private final String primo;
        private final String ultimo;
        private final int quante;

        Giro(String quando, String etichetta, String primo, String ultimo, int quante) {
            this.quando = quando;
            this.etichetta = etichetta;
            this.primo = primo;
            this.ultimo = ultimo;
            this.quante = quante;
        }

        public String quando() { return quando; }
        public String etichetta() { return etichetta; }
        public String primo() { return primo; }
        public String ultimo() { return ultimo; }
        public int quante() { return quante; }
    }

    /** Returns recent print runs, newest first, by grouping rows from daily logs. */
    public List<Giro> ultimi(int quanti) {
        List<Giro> out = new ArrayList<Giro>();
        Calendar c = Calendar.getInstance();
        for (int giorno = 0; giorno < 14 && out.size() < quanti; giorno++) {
            File f = fileDi(c.getTime());
            c.add(Calendar.DAY_OF_MONTH, -1);
            if (!f.isFile()) {
                continue;
            }
            List<Giro> delGiorno = leggiGiorno(f, giorno);
            for (int i = delGiorno.size() - 1; i >= 0 && out.size() < quanti; i--) {
                out.add(delGiorno.get(i));
            }
        }
        return out;
    }

    private List<Giro> leggiGiorno(File f, int quantiGiorniFa) {
        List<Giro> out = new ArrayList<Giro>();
        String etichetta = null;
        String giro = null;
        String ora = null;
        String primo = null;
        String ultimo = null;
        int quante = 0;
        for (String riga : righeDi(f)) {
            if (riga.isEmpty() || riga.startsWith("#") || riga.startsWith("giro\t")) {
                continue;
            }
            String[] p = riga.split("\t", -1);
            if (p.length < 4) {
                continue;
            }
            if (!p[0].equals(giro)) {
                if (quante > 0) {
                    out.add(new Giro(quando(ora, quantiGiorniFa), etichetta, primo, ultimo, quante));
                }
                giro = p[0];
                ora = p[1];
                etichetta = p[2];
                primo = p[3];
                quante = 0;
            }
            ultimo = p[3];
            quante++;
        }
        if (quante > 0) {
            out.add(new Giro(quando(ora, quantiGiorniFa), etichetta, primo, ultimo, quante));
        }
        return out;
    }

    private static String quando(String ora, int quantiGiorniFa) {
        String breve = ora != null && ora.length() >= 5 ? ora.substring(0, 5) : "";
        if (quantiGiorniFa == 0) {
            return breve;
        }
        if (quantiGiorniFa == 1) {
            return "ieri " + breve;
        }
        return quantiGiorniFa + " giorni fa";
    }

    private static List<String> righeDi(File f) {
        List<String> out = new ArrayList<String>();
        try {
            java.io.InputStream in = new java.io.FileInputStream(f);
            try {
                java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    b.write(buffer, 0, n);
                }
                for (String riga : new String(b.toByteArray(), "UTF-8").split("\n")) {
                    out.add(riga);
                }
            } finally {
                in.close();
            }
        } catch (IOException ignored) {
            /* An unreadable history file must not block production work. */
            return out;
        }
        return out;
    }

    private static void accoda(File file, String testo) throws IOException {
        FileOutputStream flusso = new FileOutputStream(file, true);
        try {
            Writer w = new OutputStreamWriter(flusso, "UTF-8");
            w.write(testo);
            w.flush();
        } finally {
            flusso.close();
        }
    }
}
