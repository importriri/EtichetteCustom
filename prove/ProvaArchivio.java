package prove;

import app.archivio.Archivio;
import app.archivio.Formato;
import app.archivio.Registro;
import app.codice.Correzione;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Libreria;
import app.modello.Tipo;
import java.io.File;
import java.util.List;

/** Persistence, reload and print-log regression coverage. */
public final class ProvaArchivio {
    private ProvaArchivio() { }

    public static void esegui() throws Exception {
        formato();
        cartella();
        registro();
    }

    private static void formato() {
        Prove.suite("Format round trip");
        for (Etichetta original : Libreria.iniziale()) {
            String written = Formato.scrivi(original);
            Etichetta restored = Formato.leggi(written);
            Prove.uguale(original.nome() + ": name survives", original.nome(), restored.nome());
            Prove.vicino(original.nome() + ": width survives",
                    original.larghezza(), restored.larghezza(), .001);
            Prove.uguale(original.nome() + ": every field survives",
                    original.campi().size(), restored.campi().size());
            Prove.uguale(original.nome() + ": every element survives",
                    original.elementi().size(), restored.elementi().size());
            Prove.uguale(original.nome() + ": rewrite is stable",
                    written, Formato.scrivi(restored));
            if (original.serie() != null) {
                Prove.uguale(original.nome() + ": sequence resumes at the same value",
                        original.serie().codice(original.serie().prossimo()),
                        restored.serie().codice(restored.serie().prossimo()));
            }
        }

        Etichetta escaped = new Etichetta("Name\twith\ttabs\nand newline", 40, 20);
        escaped.aggiungi(new Campo("odd", Comportamento.FISSO, "value\twith\ttab"));
        escaped.aggiungi(new Elemento("El", Tipo.TESTO, "odd", 1, 1, 10));
        Etichetta escapedRoundTrip = Formato.leggi(Formato.scrivi(escaped));
        Prove.uguale("tabs and newlines in a name do not split the file",
                escaped.nome(), escapedRoundTrip.nome());
        Prove.uguale("tabs inside a value survive too", "value\twith\ttab",
                escapedRoundTrip.campo("odd").valore());

        Etichetta withQr = new Etichetta("With QR", 50, 30);
        withQr.aggiungi(new Campo("c", Comportamento.FISSO, "X"));
        Elemento qr = new Elemento("QR", Tipo.QR, "c", 2, 2, 20);
        qr.correzione(Correzione.H);
        qr.rotazione(270);
        withQr.aggiungi(qr);
        Etichetta qrRoundTrip = Formato.leggi(Formato.scrivi(withQr));

        Etichetta window = Libreria.articolo();
        Prove.vero("increment window can be widened", window.cambiaFinestra(6));
        Prove.uguale("widening does not change the current code",
                "740125.003_01-02_584700349",
                window.serie().codice(window.serie().prossimo()));
        Prove.uguale("only the configured digits move", 6, window.serie().cifre());
        Prove.vero("nine digits still fit the source", window.cambiaFinestra(9));
        Prove.vero("ten digits are rejected before crossing the separator",
                !window.cambiaFinestra(10));
        Prove.uguale("a rejected change leaves the previous window intact",
                9, window.serie().cifre());

        Etichetta noSeries = new Etichetta("No sequence", 40, 20);
        noSeries.aggiungi(new Campo("code", Comportamento.FISSO, "PZ-000045"));
        Prove.vero("switching a field to progressive can create its sequence",
                noSeries.assicuraSerie("code", 3));
        Prove.uguale("the new sequence starts from the existing trailing digits",
                45, noSeries.serie().prossimo());

        Prove.uguale("QR correction level is persisted",
                Correzione.H, qrRoundTrip.elementi().get(0).correzione());
        Prove.uguale("rotation is persisted", 270,
                qrRoundTrip.elementi().get(0).rotazione());

        Prove.esplode("foreign file formats are rejected", IllegalArgumentException.class,
                new Runnable() {
                    @Override public void run() {
                        Formato.leggi("hello\nthis is not a label file");
                    }
                });
        Prove.esplode("empty files are rejected", IllegalArgumentException.class,
                new Runnable() {
                    @Override public void run() {
                        Formato.leggi("");
                    }
                });

        String unknownLine = Formato.scrivi(Libreria.articolo())
                + "unknown-record\tsomething\n";
        Prove.vero("unknown records are skipped for forward compatibility",
                Formato.leggi(unknownLine).elementi().size() == 4);

        Etichetta copy = Libreria.articolo().copia();
        Prove.uguale("a deep copy initially serializes identically",
                Formato.scrivi(Libreria.articolo()), Formato.scrivi(copy));
        copy.elementi().get(0).x(99);
        Prove.vero("the deep copy is independent",
                Libreria.articolo().elementi().get(0).x() != 99);
    }

    private static void cartella() throws Exception {
        Prove.suite("Archive directory");
        File dir = temporanea("archive");
        Archivio archive = new Archivio(dir);
        List<Etichetta> initial = archive.carica();
        Prove.uguale("first run seeds exactly one example label", 1, initial.size());
        Prove.uguale("first run writes exactly one label file", 1, quantiFile(dir));
        Prove.uguale("the seeded label is the v2 example", "Esempio", initial.get(0).nome());

        Etichetta example = initial.get(0);
        example.serie().consuma(4);
        archive.salva(example);
        Archivio reopened = new Archivio(dir);
        List<Etichetta> restored = reopened.carica();
        Prove.uguale("reopen returns only saved labels", 1, restored.size());
        Prove.vero("sequence state survives application restart",
                restored.get(0).serie().prossimo() == example.serie().prossimo());

        Etichetta newLabel = new Etichetta("Test / with \\ odd characters: 100%", 30, 20);
        newLabel.aggiungi(new Campo("c", Comportamento.FISSO, "x"));
        newLabel.aggiungi(new Elemento("T", Tipo.TESTO, "c", 1, 1, 20));
        reopened.salva(newLabel);
        Prove.vero("unsafe label names become safe file names",
                reopened.fileDi(newLabel).getName().matches("[a-z0-9-]+\\.etichetta"));

        newLabel.nome("Renamed");
        reopened.salva(newLabel);
        Prove.uguale("renaming removes the old file", 2, quantiFile(dir));
        Prove.vero("the replacement file uses the new name",
                reopened.fileDi(newLabel).getName().startsWith("renamed"));
        reopened.elimina(newLabel);
        Prove.uguale("deleting a label removes its file", 1, quantiFile(dir));

        File corrupt = new File(dir, "corrupt.etichetta");
        scrivi(corrupt, "not a valid format");
        Archivio tolerant = new Archivio(dir);
        Prove.uguale("one corrupt file does not block the remaining archive",
                1, tolerant.carica().size());
    }

    private static void registro() throws Exception {
        Prove.suite("Print log");
        File dir = temporanea("log");
        Registro log = new Registro(dir);
        Etichetta label = Libreria.articolo();
        String[] codes = label.serie().giro(3);
        File first = log.annota(label, codes, "Datamax E-4203");
        Prove.vero("daily log file is created", first.isFile());

        String text = leggi(first);
        Prove.uguale("three labels produce three rows plus header and comment",
                5, text.split("\n").length);
        Prove.vero("each data row carries a run identifier", text.contains("\t" + ora(text)));
        Prove.vero("the first code is logged", text.contains(codes[0]));
        Prove.vero("the last code is logged", text.contains(codes[2]));
        Prove.vero("the printer name is logged", text.contains("Datamax E-4203"));
        Prove.vero("asked-at-print values are logged", text.contains("lotto=4802-X"));

        log.annota(label, codes, "Datamax E-4203");
        String appended = leggi(first);
        Prove.uguale("a second run appends without repeating the header",
                8, appended.split("\n").length);

        List<Registro.Giro> runs = log.ultimi(10);
        Prove.uguale("reading the log restores two runs", 2, runs.size());
        Prove.uguale("the newest run is first", label.nome(), runs.get(0).etichetta());
        Prove.uguale("the restored run contains three labels", 3, runs.get(0).quante());
        Prove.uguale("the restored first code is exact", codes[0], runs.get(0).primo());
        Prove.uguale("the restored last code is exact", codes[2], runs.get(0).ultimo());

        Registro empty = new Registro(temporanea("empty-log"));
        Prove.uguale("a never-written log returns an empty list", 0, empty.ultimi(5).size());
        Prove.esplode("a run with no codes cannot be logged", IllegalArgumentException.class,
                new Runnable() {
                    @Override public void run() {
                        try {
                            new Registro(new File(".")).annota(
                                    Libreria.articolo(), new String[0], "x");
                        } catch (java.io.IOException impossible) {
                            throw new IllegalStateException(impossible);
                        }
                    }
                });
    }

    private static String ora(String log) {
        for (String row : log.split("\n")) {
            if (!row.startsWith("#") && !row.startsWith("giro\t") && row.contains("\t")) {
                return row.split("\t")[1];
            }
        }
        return "";
    }

    static File temporanea(String label) throws Exception {
        File file = File.createTempFile("etichette-" + label, "");
        if (!file.delete() || !file.mkdirs()) {
            throw new IllegalStateException("cannot prepare " + file);
        }
        file.deleteOnExit();
        return file;
    }

    private static int quantiFile(File dir) {
        File[] files = dir.listFiles();
        int count = 0;
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".etichetta")) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void scrivi(File file, String text) throws Exception {
        java.io.Writer writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(file), "UTF-8");
        try {
            writer.write(text);
        } finally {
            writer.close();
        }
    }

    private static String leggi(File file) throws Exception {
        java.io.InputStream input = new java.io.FileInputStream(file);
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), "UTF-8");
        } finally {
            input.close();
        }
    }
}
