package app.esporta;

import app.modello.Etichetta;
import app.render.SorgenteQr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Export facade that keeps the UI independent from individual output formats. */
public final class Esportazione {

    public enum Come {
        PNG("png", "immagine PNG"),
        SVG("svg", "vettoriale SVG"),
        PDF("pdf", "documento PDF");

        private final String coda;
        private final String etichetta;

        Come(String coda, String etichetta) {
            this.coda = coda;
            this.etichetta = etichetta;
        }

        public String coda() {
            return coda;
        }

        @Override
        public String toString() {
            return etichetta;
        }
    }

    private Esportazione() { }

    /**
     * @param quante number of labels to export; one for the current label or more for a run
     * @return files actually written
     */
    public static List<File> esporta(File dove, Etichetta eti, SorgenteQr qr,
                                     Come come, int quante, int dpi) throws IOException {
        if (quante < 1) {
            throw new IllegalArgumentException("almeno una etichetta");
        }
        List<File> scritti = new ArrayList<File>();
        String base = senzaCoda(dove.getName());
        File cartella = dove.getParentFile() == null ? new File(".") : dove.getParentFile();

        if (come == Come.PDF) {
            /* one PDF containing every page in the run */
            List<BufferedImage> pagine = new ArrayList<BufferedImage>();
            for (int i = 0; i < quante; i++) {
                pagine.add(Png.immagine(eti, qr, dpi, i));
            }
            File f = new File(cartella, base + ".pdf");
            Pdf.scrivi(f, pagine, eti.larghezza(), eti.altezza());
            scritti.add(f);
            return scritti;
        }

        for (int i = 0; i < quante; i++) {
            String nome = quante == 1 ? base : base + "-" + numerino(i + 1, quante);
            File f = new File(cartella, nome + "." + come.coda());
            if (come == Come.PNG) {
                Png.scrivi(f, eti, qr, dpi, i);
            } else {
                Svg.scrivi(f, eti, i);
            }
            scritti.add(f);
        }
        return scritti;
    }

    private static String numerino(int i, int totale) {
        int cifre = String.valueOf(totale).length();
        StringBuilder b = new StringBuilder(String.valueOf(i));
        while (b.length() < cifre) {
            b.insert(0, '0');
        }
        return b.toString();
    }

    private static String senzaCoda(String nome) {
        int punto = nome.lastIndexOf('.');
        return punto > 0 ? nome.substring(0, punto) : nome;
    }
}
