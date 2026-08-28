package app.codice;

import java.util.ArrayList;
import java.util.List;

/**
 * Code 128 encoder for letters, digits and punctuation.
 *
 * It switches automatically between set B and set C, using digit pairs to
 * reduce the width of long numeric runs.
 */
public final class Code128 {

    /**
     * Bar/space widths for each symbol value. Normal symbols contain six runs;
     * stop value 106 has seven because it ends with an extra bar.
     */
    private static final String[] DISEGNI = {
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312",
        "132212", "221213", "221312", "231212", "112232", "122132", "122231", "113222",
        "123122", "123221", "223211", "221132", "221231", "213212", "223112", "312131",
        "311222", "321122", "321221", "312212", "322112", "322211", "212123", "212321",
        "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121",
        "313121", "211331", "231131", "213113", "213311", "213131", "311123", "311321",
        "331121", "312113", "312311", "332111", "314111", "221411", "431111", "111224",
        "111422", "121124", "121421", "141122", "141221", "112214", "112412", "122114",
        "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112",
        "421211", "212141", "214121", "412121", "111143", "111341", "131141", "114113",
        "114311", "411113", "411311", "113141", "114131", "311141", "411131", "211412",
        "211214", "211232", "2331112",
    };

    private static final int AVVIO_B = 104;
    private static final int AVVIO_C = 105;
    private static final int PASSA_A_C = 99;
    private static final int PASSA_A_B = 100;
    private static final int STOP = 106;

    private Code128() { }

    /** Run widths in modules, starting with a bar and alternating bar/space. */
    public static int[] tratti(String testo) {
        List<Integer> valori = valori(testo);
        StringBuilder disegno = new StringBuilder();
        for (int v : valori) {
            disegno.append(DISEGNI[v]);
        }
        int[] out = new int[disegno.length()];
        for (int i = 0; i < disegno.length(); i++) {
            out[i] = disegno.charAt(i) - '0';
        }
        return out;
    }

    /** Total width in modules, used to validate physical fit. */
    public static int moduli(String testo) {
        int somma = 0;
        for (int t : tratti(testo)) {
            somma += t;
        }
        return somma;
    }

    /** Symbol values including start, checksum and stop. */
    public static List<Integer> valori(String testo) {
        if (testo == null) {
            testo = "";
        }
        for (int i = 0; i < testo.length(); i++) {
            char c = testo.charAt(i);
            if (c < 32 || c > 126) {
                throw new IllegalArgumentException(
                        "Code 128 qui gestisce solo caratteri stampabili ASCII: \"" + c + "\"");
            }
        }

        List<Integer> valori = new ArrayList<Integer>();
        int i = 0;
        boolean inC = convieneC(testo, 0, true);
        valori.add(inC ? AVVIO_C : AVVIO_B);

        while (i < testo.length()) {
            if (inC) {
                if (i + 1 < testo.length() && cifra(testo, i) && cifra(testo, i + 1)) {
                    valori.add((testo.charAt(i) - '0') * 10 + (testo.charAt(i + 1) - '0'));
                    i += 2;
                } else {
                    valori.add(PASSA_A_B);
                    inC = false;
                }
            } else {
                if (convieneC(testo, i, false)) {
                    valori.add(PASSA_A_C);
                    inC = true;
                } else {
                    valori.add(testo.charAt(i) - 32);
                    i++;
                }
            }
        }

        int somma = valori.get(0);
        for (int k = 1; k < valori.size(); k++) {
            somma += valori.get(k) * k;
        }
        valori.add(somma % 103);
        valori.add(STOP);
        return valori;
    }

    private static boolean cifra(String s, int i) {
        return i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9';
    }

    /**
     * Set C is worthwhile for runs of at least four digits at an edge or six
     * digits in the middle, where two set switches must be paid for.
     */
    private static boolean convieneC(String s, int da, boolean inizio) {
        int quante = 0;
        while (cifra(s, da + quante)) {
            quante++;
        }
        if (quante % 2 != 0) {
            quante--;                    /* an odd run must leave one digit for set B */
        }
        boolean fino = da + quante >= s.length();
        int soglia = inizio || fino ? 4 : 6;
        return quante >= soglia;
    }
}
