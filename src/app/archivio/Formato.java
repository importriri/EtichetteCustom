package app.archivio;

import app.codice.Correzione;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Serie;
import app.modello.Tipo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Il formato con cui un'etichetta finisce su disco.
 *
 * Testo, una riga per cosa, campi separati da tabulazione. Si legge con
 * un editor, si confronta con diff, si aggiusta a mano quando serve: su
 * un PC di reparto, poter aprire il file e capire cosa c'e' scritto vale
 * piu' di qualunque formato compatto.
 *
 * I numeri sono scritti sempre con il punto decimale, non con la virgola:
 * un file salvato su un Windows italiano deve potersi aprire ovunque.
 */
public final class Formato {

    /** Cambiare questo numero solo insieme a chi legge i file vecchi. */
    public static final int VERSIONE = 2;

    private Formato() { }

    /* ================= scrittura ================= */

    public static String scrivi(Etichetta e) {
        StringBuilder b = new StringBuilder();
        b.append("etichette-custom\t").append(VERSIONE).append('\n');
        b.append("nome\t").append(fuga(e.nome())).append('\n');
        b.append("misura\t").append(num(e.larghezza())).append('\t')
                .append(num(e.altezza())).append('\n');

        for (Campo c : e.campi()) {
            b.append("campo\t").append(fuga(c.nome())).append('\t')
                    .append(c.comportamento().name()).append('\t')
                    .append(fuga(c.valore())).append('\t');
            Serie serie = c.serie();
            if (serie != null) {
                b.append(fuga(serie.codice(serie.prossimo()))).append('\t').append(serie.cifre());
            } else {
                b.append('\t');
            }
            b.append('\n');
        }
        for (Elemento el : e.elementi()) {
            b.append("elemento")
                    .append('\t').append(fuga(el.nome()))
                    .append('\t').append(el.tipo().name())
                    .append('\t').append(fuga(el.campo() == null ? "" : el.campo()))
                    .append('\t').append(num(el.x()))
                    .append('\t').append(num(el.y()))
                    .append('\t').append(num(el.larghezza()))
                    .append('\t').append(num(el.altezza()))
                    .append('\t').append(num(el.corpo()))
                    .append('\t').append(el.grassetto() ? 1 : 0)
                    .append('\t').append(el.rotazione())
                    .append('\t').append(el.massimoRighe())
                    .append('\t').append(el.correzione().name())
                    .append('\n');
        }
        return b.toString();
    }

    /* ================= lettura ================= */

    public static Etichetta leggi(String testo) {
        if (testo == null || testo.trim().isEmpty()) {
            throw new IllegalArgumentException("file vuoto");
        }
        String[] righe = testo.split("\n", -1);
        if (righe.length == 0 || !righe[0].startsWith("etichette-custom\t")) {
            throw new IllegalArgumentException(
                    "questo non sembra un file di Etichette Custom");
        }
        int versione = intero(pezzi(righe[0])[1], "versione del formato");
        if (versione > VERSIONE) {
            throw new IllegalArgumentException("file scritto da una versione piu' nuova ("
                    + versione + "): aggiorna il programma");
        }

        String nome = "senza nome";
        double larghezza = 50;
        double altezza = 30;
        Serie serieVecchia = null;
        List<Campo> campi = new ArrayList<Campo>();
        List<Elemento> elementi = new ArrayList<Elemento>();

        for (int i = 1; i < righe.length; i++) {
            String riga = righe[i];
            if (riga.trim().isEmpty() || riga.startsWith("#")) {
                continue;
            }
            String[] p = pezzi(riga);
            String tipoRiga = p[0];
            try {
                if ("nome".equals(tipoRiga)) {
                    nome = p[1];
                } else if ("misura".equals(tipoRiga)) {
                    larghezza = decimale(p[1], "larghezza");
                    altezza = decimale(p[2], "altezza");
                } else if ("serie".equals(tipoRiga)) {
                    int cifre = intero(p[2], "cifre della serie");
                    int prossimo = intero(p[3], "prossimo della serie");
                    StringBuilder coda = new StringBuilder(Integer.toString(prossimo));
                    while (coda.length() < cifre) {
                        coda.insert(0, '0');
                    }
                    serieVecchia = new Serie(p[1] + coda, cifre);
                } else if ("campo".equals(tipoRiga)) {
                    Campo c = new Campo(p[1], Comportamento.valueOf(p[2]),
                            p.length > 3 ? p[3] : "");
                    if (p.length > 5 && !p[4].isEmpty()) {
                        c.serie(new Serie(p[4], intero(p[5], "cifre della serie di " + p[1])));
                    }
                    campi.add(c);
                } else if ("elemento".equals(tipoRiga)) {
                    elementi.add(elemento(p));
                } else {
                    /* riga sconosciuta: la si salta invece di rifiutare tutto */
                    continue;
                }
            } catch (RuntimeException rotta) {
                throw new IllegalArgumentException(
                        "riga " + (i + 1) + " (" + tipoRiga + "): " + rotta.getMessage(), rotta);
            }
        }

        Etichetta e = new Etichetta(nome, larghezza, altezza);
        for (Campo c : campi) {
            e.aggiungi(c);
        }
        if (serieVecchia != null && e.serie() == null) {
            e.serie(serieVecchia);
        }
        for (Elemento el : elementi) {
            e.aggiungi(el);
        }
        return e;
    }

    private static Elemento elemento(String[] p) {
        String campo = p[3].isEmpty() ? null : p[3];
        Elemento el = new Elemento(p[1], Tipo.valueOf(p[2]), campo,
                decimale(p[4], "x"), decimale(p[5], "y"), decimale(p[6], "larghezza"));
        el.altezza(decimale(p[7], "altezza"));
        el.corpo(decimale(p[8], "corpo"));
        el.grassetto(!"0".equals(p[9]));
        el.rotazione(intero(p[10], "rotazione"));
        el.massimoRighe(intero(p[11], "righe"));
        if (p.length > 12 && !p[12].isEmpty()) {
            el.correzione(Correzione.valueOf(p[12]));
        }
        return el;
    }

    /* ================= minuterie ================= */

    private static String[] pezzi(String riga) {
        String[] grezzi = riga.split("\t", -1);
        String[] out = new String[grezzi.length];
        for (int i = 0; i < grezzi.length; i++) {
            out[i] = sfuga(grezzi[i]);
        }
        return out;
    }

    /** Tabulazioni e a capo dentro un valore non devono spezzare il file. */
    static String fuga(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                b.append("\\\\");
            } else if (c == '\t') {
                b.append("\\t");
            } else if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    static String sfuga(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char d = s.charAt(++i);
                if (d == 't') {
                    b.append('\t');
                } else if (d == 'n') {
                    b.append('\n');
                } else if (d == 'r') {
                    b.append('\r');
                } else {
                    b.append(d);
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    static String num(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static double decimale(String s, String cosa) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException rotta) {
            throw new IllegalArgumentException(cosa + " non e' un numero: \"" + s + "\"");
        }
    }

    private static int intero(String s, String cosa) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException rotta) {
            throw new IllegalArgumentException(cosa + " non e' un numero intero: \"" + s + "\"");
        }
    }
}
