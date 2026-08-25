package app.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Andata a capo dei codici lunghi.
 *
 * Prima si prova l'andata a capo fino al numero di righe concesso; solo
 * quando non basta si riduce il corpo. I codici senza separatori possono
 * essere spezzati in qualunque punto e la misura usa le metriche del font.
 */
public final class Testo {

    private Testo() { }

    /** Dove si preferisce spezzare, quando c'e' la possibilita' di scegliere. */
    private static final String SEPARATORI = " _-./:";

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

    /**
     * @param testo      quello che va stampato
     * @param larghezza  spazio disponibile, in mm
     * @param corpo      corpo scelto dall'operatore, in mm
     * @param massimoRighe quante righe si accettano
     * @param m          come si misura
     */
    public static Esito componi(String testo, double larghezza, double corpo,
                                int massimoRighe, boolean grassetto, Misuratore m) {
        if (testo == null) {
            testo = "";
        }
        if (larghezza <= 0 || corpo <= 0) {
            throw new IllegalArgumentException("larghezza e corpo devono essere positivi");
        }
        int righeMax = Math.max(1, massimoRighe);

        /* Prima si prova a corpo pieno, con una riga in piu' alla volta. */
        for (int righe = 1; righe <= righeMax; righe++) {
            String[] r = dividi(testo, righe, larghezza, corpo, grassetto, m);
            if (r != null) {
                return new Esito(r, corpo, false);
            }
        }

        /* Solo adesso si rimpicciolisce, un passo per volta. */
        double minimo = corpo * 0.4;
        for (double c = corpo * 0.95; c >= minimo; c -= corpo * 0.05) {
            for (int righe = 1; righe <= righeMax; righe++) {
                String[] r = dividi(testo, righe, larghezza, c, grassetto, m);
                if (r != null) {
                    return new Esito(r, arrotonda(c), true);
                }
            }
        }

        /* Non ci sta comunque: si taglia al minimo e si lascia sfora'to
           visibile, invece di far finta di niente. */
        String[] r = dividi(testo, Integer.MAX_VALUE, larghezza, minimo, grassetto, m);
        if (r != null && r.length > righeMax) {
            String[] tagliato = new String[righeMax];
            System.arraycopy(r, 0, tagliato, 0, righeMax);
            r = tagliato;
        }
        return new Esito(r == null ? new String[] { testo } : r, arrotonda(minimo), true);
    }

    /**
     * Prova a sistemare il testo in al massimo <code>righe</code> righe.
     * Torna null se non ci sta.
     */
    static String[] dividi(String testo, int righe, double larghezza, double corpo,
                           boolean grassetto, Misuratore m) {
        if (testo.isEmpty()) {
            return new String[] { "" };
        }
        List<String> out = new ArrayList<String>();
        int i = 0;
        while (i < testo.length()) {
            int fine = i;
            int ultimoSeparatore = -1;
            while (fine < testo.length()) {
                if (m.larghezza(testo.substring(i, fine + 1), corpo, grassetto) > larghezza) {
                    break;
                }
                if (SEPARATORI.indexOf(testo.charAt(fine)) >= 0) {
                    ultimoSeparatore = fine;
                }
                fine++;
            }
            if (fine == i) {
                /* Nemmeno un carattere ci sta: inutile insistere a questo corpo. */
                return null;
            }
            int taglio = fine;
            if (fine < testo.length() && ultimoSeparatore > i) {
                taglio = ultimoSeparatore + 1;
            }
            out.add(testo.substring(i, taglio));
            i = taglio;
            if (out.size() > righe) {
                return null;
            }
        }
        if (out.size() > righe) {
            return null;
        }
        return out.toArray(new String[out.size()]);
    }

    private static double arrotonda(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
