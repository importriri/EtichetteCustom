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

/** Human-readable, backward-compatible label storage format. */
public final class Formato {
    public static final int VERSIONE = 5;

    private Formato() { }

    public static String scrivi(Etichetta etichetta) {
        StringBuilder out = new StringBuilder();
        out.append("etichette-custom\t").append(VERSIONE).append('\n');
        out.append("nome\t").append(fuga(etichetta.nome())).append('\n');
        out.append("misura\t").append(num(etichetta.larghezza())).append('\t')
                .append(num(etichetta.altezza())).append('\n');

        for (Campo campo : etichetta.campi()) {
            out.append("campo\t").append(fuga(campo.nome())).append('\t')
                    .append(campo.comportamento().name()).append('\t')
                    .append(fuga(campo.valore())).append('\t');
            Serie serie = campo.serie();
            if (serie != null) {
                out.append(fuga(serie.codice(serie.prossimo()))).append('\t')
                        .append(serie.cifre());
            } else {
                out.append('\t');
            }
            out.append('\n');
        }

        for (Elemento elemento : etichetta.elementi()) {
            out.append("elemento\t")
                    .append(fuga(elemento.nome())).append('\t')
                    .append(elemento.tipo().name()).append('\t')
                    .append(fuga(elemento.campo() == null ? "" : elemento.campo())).append('\t')
                    .append(num(elemento.x())).append('\t')
                    .append(num(elemento.y())).append('\t')
                    .append(num(elemento.larghezza())).append('\t')
                    .append(num(elemento.altezza())).append('\t')
                    .append(num(elemento.corpo())).append('\t')
                    .append(elemento.grassetto() ? 1 : 0).append('\t')
                    .append(elemento.rotazione()).append('\t')
                    .append(elemento.massimoRighe()).append('\t')
                    .append(elemento.correzione().name()).append('\t')
                    .append(elemento.allineamento()).append('\t')
                    .append(elemento.mostraSeparatori() ? 1 : 0).append('\t')
                    .append(elemento.righePreferite()).append('\t')
                    .append(elemento.parteTesto())
                    .append('\n');
        }
        return out.toString();
    }

    public static Etichetta leggi(String testo) {
        if (testo == null || testo.trim().isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }
        String[] righe = testo.split("\n", -1);
        String[] header = pezzi(righe[0]);
        if (header.length != 2 || !"etichette-custom".equals(header[0])) {
            throw new IllegalArgumentException("this is not an Etichette Custom file");
        }

        int versione = intero(header[1], "format version");
        if (versione < 1) {
            throw new IllegalArgumentException("invalid format version: " + versione);
        }
        if (versione > VERSIONE) {
            throw new IllegalArgumentException(
                    "file written by a newer version (" + versione + "): update the application");
        }

        String nome = "senza nome";
        double larghezza = 50;
        double altezza = 30;
        Serie serieVecchia = null;
        List<Campo> campi = new ArrayList<Campo>();
        List<Elemento> elementi = new ArrayList<Elemento>();

        for (int i = 1; i < righe.length; i++) {
            String riga = righe[i];
            if (riga.trim().isEmpty() || riga.startsWith("#")) continue;
            String[] parti = pezzi(riga);
            String tipo = parti[0];
            try {
                if ("nome".equals(tipo)) {
                    richiede(parti, 2, tipo);
                    nome = parti[1];
                } else if ("misura".equals(tipo)) {
                    richiede(parti, 3, tipo);
                    larghezza = decimale(parti[1], "width");
                    altezza = decimale(parti[2], "height");
                } else if ("serie".equals(tipo)) {
                    richiede(parti, 4, tipo);
                    int cifre = intero(parti[2], "sequence digits");
                    int prossimo = intero(parti[3], "next sequence number");
                    StringBuilder coda = new StringBuilder(Integer.toString(prossimo));
                    while (coda.length() < cifre) coda.insert(0, '0');
                    serieVecchia = new Serie(parti[1] + coda, cifre);
                } else if ("campo".equals(tipo)) {
                    richiede(parti, 3, tipo);
                    Campo campo = new Campo(parti[1], Comportamento.valueOf(parti[2]),
                            parti.length > 3 ? parti[3] : "");
                    if (parti.length > 5 && !parti[4].isEmpty()) {
                        campo.serie(new Serie(parti[4],
                                intero(parti[5], "sequence digits for " + parti[1])));
                    }
                    campi.add(campo);
                } else if ("elemento".equals(tipo)) {
                    richiede(parti, 12, tipo);
                    elementi.add(elemento(parti));
                }
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "line " + (i + 1) + " (" + tipo + "): " + ex.getMessage(), ex);
            }
        }

        Etichetta etichetta = new Etichetta(nome, larghezza, altezza);
        for (Campo campo : campi) etichetta.aggiungi(campo);
        if (serieVecchia != null && etichetta.serie() == null) etichetta.serie(serieVecchia);
        for (Elemento elemento : elementi) etichetta.aggiungi(elemento);
        return etichetta;
    }

    private static Elemento elemento(String[] parti) {
        String campo = parti[3].isEmpty() ? null : parti[3];
        Elemento elemento = new Elemento(parti[1], Tipo.valueOf(parti[2]), campo,
                decimale(parti[4], "x"), decimale(parti[5], "y"),
                decimale(parti[6], "width"));
        elemento.altezza(decimale(parti[7], "height"));
        elemento.corpo(decimale(parti[8], "text size"));
        elemento.grassetto(!"0".equals(parti[9]));
        elemento.rotazione(intero(parti[10], "rotation"));
        elemento.massimoRighe(intero(parti[11], "lines"));
        if (parti.length > 12 && !parti[12].isEmpty()) {
            elemento.correzione(Correzione.valueOf(parti[12]));
        }
        if (parti.length > 13 && !parti[13].isEmpty()) {
            elemento.allineamento(intero(parti[13], "alignment"));
        }
        if (parti.length > 14 && !parti[14].isEmpty()) {
            elemento.mostraSeparatori(!"0".equals(parti[14]));
        }
        if (parti.length > 15 && !parti[15].isEmpty()) {
            elemento.righePreferite(intero(parti[15], "preferred lines"));
        }
        if (parti.length > 16 && !parti[16].isEmpty()) {
            elemento.parteTesto(intero(parti[16], "text part"));
        }
        return elemento;
    }

    private static void richiede(String[] parts, int minimum, String record) {
        if (parts.length < minimum) {
            throw new IllegalArgumentException(record + " record is incomplete");
        }
    }

    private static String[] pezzi(String riga) {
        String[] grezzi = riga.split("\t", -1);
        String[] risultati = new String[grezzi.length];
        for (int i = 0; i < grezzi.length; i++) risultati[i] = sfuga(grezzi[i]);
        return risultati;
    }

    static String fuga(String testo) {
        if (testo == null) return "";
        StringBuilder out = new StringBuilder(testo.length());
        for (int i = 0; i < testo.length(); i++) {
            char c = testo.charAt(i);
            if (c == '\\') out.append("\\\\");
            else if (c == '\t') out.append("\\t");
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else out.append(c);
        }
        return out.toString();
    }

    static String sfuga(String testo) {
        if (testo.indexOf('\\') < 0) return testo;
        StringBuilder out = new StringBuilder(testo.length());
        for (int i = 0; i < testo.length(); i++) {
            char c = testo.charAt(i);
            if (c == '\\' && i + 1 < testo.length()) {
                char escaped = testo.charAt(++i);
                if (escaped == 't') out.append('\t');
                else if (escaped == 'n') out.append('\n');
                else if (escaped == 'r') out.append('\r');
                else out.append(escaped);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static String num(double valore) {
        String out = String.format(Locale.ROOT, "%.2f", valore);
        while (out.endsWith("0")) out = out.substring(0, out.length() - 1);
        if (out.endsWith(".")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private static double decimale(String testo, String cosa) {
        try {
            return Double.parseDouble(testo.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(cosa + " is not a number: \"" + testo + "\"");
        }
    }

    private static int intero(String testo, String cosa) {
        try {
            return Integer.parseInt(testo.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(cosa + " is not an integer: \"" + testo + "\"");
        }
    }
}
