package app.render;

import java.util.ArrayList;
import java.util.List;

/** Balanced layout for long label text while preserving natural code groups. */
public final class Testo {
    private static final String SEPARATORS = " _-./:;,";

    private Testo() { }

    public static final class Esito {
        private final String[] righe;
        private final double corpo;
        private final boolean rimpicciolito;

        Esito(String[] righe, double corpo, boolean rimpicciolito) {
            this.righe = righe;
            this.corpo = corpo;
            this.rimpicciolito = rimpicciolito;
        }

        public String[] righe() { return righe; }
        public double corpo() { return corpo; }
        public boolean rimpicciolito() { return rimpicciolito; }
        public int quanteRighe() { return righe.length; }
    }

    /** Returns logical alphanumeric groups without changing the underlying source value. */
    public static String[] parti(String testo) {
        if (testo == null || testo.isEmpty()) return new String[] {""};
        List<String> parti = new ArrayList<String>();
        StringBuilder corrente = new StringBuilder();
        for (int i = 0; i < testo.length(); i++) {
            char carattere = testo.charAt(i);
            if (SEPARATORS.indexOf(carattere) >= 0) {
                if (corrente.length() > 0) {
                    parti.add(corrente.toString());
                    corrente.setLength(0);
                }
            } else {
                corrente.append(carattere);
            }
        }
        if (corrente.length() > 0) parti.add(corrente.toString());
        if (parti.isEmpty()) return new String[] {""};
        return parti.toArray(new String[parti.size()]);
    }

    /** 0 returns the full source; 1..n return one logical group. */
    public static String parte(String testo, int indice) {
        String completo = testo == null ? "" : testo;
        if (indice <= 0) return completo;
        String[] gruppi = parti(completo);
        return indice <= gruppi.length ? gruppi[indice - 1] : "";
    }

    public static Esito componi(String testo, double larghezza, double corpo,
                                int massimoRighe, boolean grassetto, Misuratore misuratore) {
        return componi(testo, larghezza, corpo, massimoRighe, 0, grassetto, misuratore);
    }

    public static Esito componi(String testo, double larghezza, double corpo,
                                int massimoRighe, int righePreferite,
                                boolean grassetto, Misuratore misuratore) {
        if (testo == null) testo = "";
        if (larghezza <= 0 || corpo <= 0) {
            throw new IllegalArgumentException("width and text size must be positive");
        }

        massimoRighe = Math.max(1, Math.min(3, massimoRighe));
        righePreferite = Math.max(0, Math.min(massimoRighe, righePreferite));
        double minimo = corpo * .4;

        /*
         * Structured codes are allowed to shrink before a numeric/alphanumeric
         * group is broken. Unstructured text has no natural boundary to protect,
         * so it can wrap at full size before shrinking.
         */
        if (haSeparatore(testo)) {
            Esito naturale = prova(testo, larghezza, corpo, minimo, massimoRighe,
                    righePreferite, grassetto, misuratore, true);
            if (naturale != null) return naturale;
        }

        Esito ripiego = prova(testo, larghezza, corpo, minimo, massimoRighe,
                righePreferite, grassetto, misuratore, false);
        if (ripiego != null) return ripiego;

        int righe = righePreferite > 0 ? righePreferite : massimoRighe;
        return new Esito(senzaVincolo(testo, righe), arrotonda(minimo), true);
    }

    static String[] dividi(String testo, int righe, double larghezza, double corpo,
                           boolean grassetto, Misuratore misuratore) {
        int massimo = Math.max(1, Math.min(3, righe));
        if (haSeparatore(testo)) {
            for (int n = 1; n <= massimo; n++) {
                String[] risultato = bilancia(testo, n, larghezza, corpo,
                        grassetto, misuratore, true, true);
                if (risultato != null) return risultato;
            }
        }
        for (int n = 1; n <= massimo; n++) {
            String[] risultato = bilancia(testo, n, larghezza, corpo,
                    grassetto, misuratore, true, false);
            if (risultato != null) return risultato;
        }
        return null;
    }

    private static boolean haSeparatore(String testo) {
        if (testo == null) return false;
        for (int i = 0; i < testo.length(); i++) {
            if (SEPARATORS.indexOf(testo.charAt(i)) >= 0) return true;
        }
        return false;
    }

    private static Esito prova(String testo, double larghezza, double corpo,
                               double minimo, int massimoRighe, int righePreferite,
                               boolean grassetto, Misuratore misuratore,
                               boolean soloTagliNaturali) {
        for (double corrente = corpo; corrente + 1e-4 >= minimo; corrente -= corpo * .05) {
            if (righePreferite > 0) {
                String[] righe = bilancia(testo, righePreferite, larghezza, corrente,
                        grassetto, misuratore, true, soloTagliNaturali);
                if (righe != null) {
                    return new Esito(righe, arrotonda(corrente), corrente < corpo - 1e-4);
                }
            } else {
                Candidato migliore = null;
                for (int n = 1; n <= massimoRighe; n++) {
                    String[] righe = bilancia(testo, n, larghezza, corrente,
                            grassetto, misuratore, true, soloTagliNaturali);
                    if (righe == null) continue;
                    double punteggio = score(righe, corrente, grassetto, misuratore) + n * .08;
                    if (migliore == null || punteggio < migliore.score) {
                        migliore = new Candidato(righe, punteggio);
                    }
                }
                if (migliore != null) {
                    return new Esito(migliore.righe, arrotonda(corrente), corrente < corpo - 1e-4);
                }
            }
        }
        return null;
    }

    private static String[] bilancia(String testo, int numeroRighe, double larghezza,
                                     double corpo, boolean grassetto, Misuratore misuratore,
                                     boolean limita, boolean soloTagliNaturali) {
        if (testo == null || testo.isEmpty()) return new String[] {""};
        if (numeroRighe <= 1) {
            return !limita || misuratore.larghezza(testo, corpo, grassetto) <= larghezza
                    ? new String[] {testo} : null;
        }
        if (testo.length() < numeroRighe) return null;

        Taglio migliore = null;
        if (numeroRighe == 2) {
            for (int a = 1; a < testo.length(); a++) {
                String[] righe = {testo.substring(0, a), testo.substring(a)};
                Taglio candidato = valuta(righe, new int[] {a}, larghezza, corpo,
                        grassetto, misuratore, limita, testo, soloTagliNaturali);
                if (candidato != null && (migliore == null || candidato.score < migliore.score)) {
                    migliore = candidato;
                }
            }
        } else {
            for (int a = 1; a < testo.length() - 1; a++) {
                for (int b = a + 1; b < testo.length(); b++) {
                    String[] righe = {
                        testo.substring(0, a),
                        testo.substring(a, b),
                        testo.substring(b)
                    };
                    Taglio candidato = valuta(righe, new int[] {a, b}, larghezza, corpo,
                            grassetto, misuratore, limita, testo, soloTagliNaturali);
                    if (candidato != null
                            && (migliore == null || candidato.score < migliore.score)) {
                        migliore = candidato;
                    }
                }
            }
        }
        return migliore == null ? null : migliore.righe;
    }

    private static Taglio valuta(String[] gruppi, int[] tagli, double larghezzaMassima,
                                 double corpo, boolean grassetto, Misuratore misuratore,
                                 boolean limita, String originale, boolean soloTagliNaturali) {
        if (soloTagliNaturali) {
            for (int taglio : tagli) {
                if (!taglioNaturale(originale, taglio)) return null;
            }
        }

        String[] righe = new String[gruppi.length];
        double[] larghezze = new double[gruppi.length];
        double somma = 0;
        for (int i = 0; i < gruppi.length; i++) {
            righe[i] = trimSpazi(gruppi[i]);
            if (righe[i].isEmpty()) return null;
            larghezze[i] = misuratore.larghezza(righe[i], corpo, grassetto);
            if (limita && larghezze[i] > larghezzaMassima + 1e-4) return null;
            somma += larghezze[i];
        }

        double media = somma / righe.length;
        double punteggio = 0;
        for (double larghezza : larghezze) {
            double differenza = larghezza - media;
            punteggio += differenza * differenza;
        }
        if (!soloTagliNaturali) {
            for (int taglio : tagli) punteggio += costoTaglio(originale, taglio);
        }
        return new Taglio(righe, punteggio);
    }

    private static boolean taglioNaturale(String testo, int posizione) {
        return posizione > 0
                && posizione <= testo.length()
                && SEPARATORS.indexOf(testo.charAt(posizione - 1)) >= 0;
    }

    private static double costoTaglio(String testo, int posizione) {
        if (taglioNaturale(testo, posizione)) return -1000;
        if (posizione < testo.length() && SEPARATORS.indexOf(testo.charAt(posizione)) >= 0) {
            return -700;
        }
        return 100;
    }

    private static String trimSpazi(String testo) {
        int inizio = 0;
        int fine = testo.length();
        while (inizio < fine && testo.charAt(inizio) == ' ') inizio++;
        while (fine > inizio && testo.charAt(fine - 1) == ' ') fine--;
        return testo.substring(inizio, fine);
    }

    private static String[] senzaVincolo(String testo, int numeroRighe) {
        if (testo == null || testo.isEmpty()) return new String[] {""};
        numeroRighe = Math.max(1, Math.min(3, Math.min(numeroRighe, testo.length())));
        if (numeroRighe == 1) return new String[] {testo};

        List<String> risultato = new ArrayList<String>();
        int da = 0;
        for (int restanti = numeroRighe; restanti > 1; restanti--) {
            int caratteri = testo.length() - da;
            int ideale = da + (int) Math.round(caratteri / (double) restanti);
            int taglio = vicino(testo, da + 1, testo.length() - (restanti - 1), ideale);
            risultato.add(trimSpazi(testo.substring(da, taglio)));
            da = taglio;
        }
        risultato.add(trimSpazi(testo.substring(da)));
        return risultato.toArray(new String[risultato.size()]);
    }

    private static int vicino(String testo, int minimo, int massimo, int ideale) {
        int migliore = Math.max(minimo, Math.min(massimo, ideale));
        int distanza = Integer.MAX_VALUE;
        for (int i = minimo; i <= massimo; i++) {
            if (taglioNaturale(testo, i) && Math.abs(i - ideale) < distanza) {
                migliore = i;
                distanza = Math.abs(i - ideale);
            }
        }
        return migliore;
    }

    private static double score(String[] righe, double corpo, boolean grassetto,
                                Misuratore misuratore) {
        double somma = 0;
        double[] larghezze = new double[righe.length];
        for (int i = 0; i < righe.length; i++) {
            larghezze[i] = misuratore.larghezza(righe[i], corpo, grassetto);
            somma += larghezze[i];
        }
        double media = somma / righe.length;
        double punteggio = 0;
        for (double larghezza : larghezze) {
            double differenza = larghezza - media;
            punteggio += differenza * differenza;
        }
        return punteggio;
    }

    private static double arrotonda(double valore) {
        return Math.round(valore * 100.0) / 100.0;
    }

    private static final class Candidato {
        final String[] righe;
        final double score;

        Candidato(String[] righe, double score) {
            this.righe = righe;
            this.score = score;
        }
    }

    private static final class Taglio {
        final String[] righe;
        final double score;

        Taglio(String[] righe, double score) {
            this.righe = righe;
            this.score = score;
        }
    }
}
