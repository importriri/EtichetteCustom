package prove;

import app.codice.Code128;
import app.codice.Correzione;
import app.codice.Qr;

import java.security.MessageDigest;
import java.util.List;

/**
 * QR and Code 128 encoder regressions. Machine-readable symbols can look valid
 * even when their payload is wrong, so reference fingerprints are backed by
 * independent decoding and physical scanner checks.
 */
public final class ProvaCodici {

    private ProvaCodici() { }

    private static final String CODICE = "740125.003_01-02_584700349";

    public static void esegui() {
        qrStruttura();
        qrImpronte();
        code128();
    }

    /* ---- QR structural checks ----------------------------------------- */

    private static void qrStruttura() {
        Prove.suite("QR matrix structure");

        boolean[][] m = Qr.codifica(CODICE, Correzione.M);
        int lato = m.length;
        Prove.uguale("the production sample fits QR version 2", 2,
                Qr.versionePer(CODICE, Correzione.M));
        Prove.uguale("QR version 2 is 25 modules wide", 25, lato);
        Prove.vero("the QR matrix is square", m[0].length == lato);

        Prove.vero("top-left finder pattern is valid", mirino(m, 0, 0));
        Prove.vero("top-right finder pattern is valid", mirino(m, lato - 7, 0));
        Prove.vero("bottom-left finder pattern is valid", mirino(m, 0, lato - 7));

        boolean timing = true;
        for (int i = 8; i < lato - 8; i++) {
            timing &= m[6][i] == (i % 2 == 0);
            timing &= m[i][6] == (i % 2 == 0);
        }
        Prove.vero("timing patterns alternate correctly", timing);
        Prove.vero("fixed dark module is present", m[lato - 8][8]);
        Prove.vero("format bits contain a valid BCH code", formatoValido(m));

        int scuri = 0;
        for (boolean[] riga : m) {
            for (boolean b : riga) {
                if (b) {
                    scuri++;
                }
            }
        }
        double quota = scuri / (double) (lato * lato);
        Prove.vero("dark and light modules are balanced (" + Math.round(quota * 100) + "%)",
                quota > 0.35 && quota < 0.65);

        Prove.uguale("higher correction requires more modules", true,
                Qr.versionePer(CODICE, Correzione.H) > Qr.versionePer(CODICE, Correzione.L));
        Prove.uguale("version 1 at level M holds 16 payload bytes", 16,
                Qr.codewordDati(1, Correzione.M));
        Prove.vero("empty payload is handled safely",
                Qr.codifica("", Correzione.M).length == 21);

        StringBuilder enorme = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            enorme.append('X');
        }
        final String troppo = enorme.toString();
        Prove.esplode("payloads too large for every QR version are rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        Qr.codifica(troppo, Correzione.H);
                    }
                });
    }

    /* ---- QR: impronte di riferimento ---------------------------------- */

    private static final String[][] IMPRONTE = {
        { "740125.003_01-02_584700349", "M", "3cd3c0fee802080e" },
        { "740125.003_01-02_584700349", "H", "399667c625628cef" },
        { "D04", "L", "d81f697f24621321" },
        { "BOX-4802-0117", "Q", "332d44c35664094d" },
        { "R-07-13", "M", "87e4f70d5eb6375a" },
        { "0000000000000000000000000001", "M", "c3b00223e6784cc5" },
    };

    private static void qrImpronte() {
        Prove.suite("QR reference fingerprints");
        for (String[] caso : IMPRONTE) {
            boolean[][] m = Qr.codifica(caso[0], Correzione.valueOf(caso[1]));
            Prove.uguale("fingerprint of \"" + taglia(caso[0]) + "\" at level " + caso[1],
                    caso[2], impronta(m));
        }
    }

    private static String taglia(String s) {
        return s.length() <= 18 ? s : s.substring(0, 15) + "\u2026";
    }

    static String impronta(boolean[][] m) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            for (boolean[] riga : m) {
                byte[] b = new byte[riga.length];
                for (int i = 0; i < riga.length; i++) {
                    b[i] = (byte) (riga[i] ? 1 : 0);
                }
                d.update(b);
            }
            byte[] h = d.digest();
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                s.append(String.format("%02x", h[i]));
            }
            return s.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean mirino(boolean[][] m, int ox, int oy) {
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                int d = Math.max(Math.abs(x - 3), Math.abs(y - 3));
                if (m[oy + y][ox + x] != (d != 2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Reads the 15 format bits back and validates their correction code. */
    private static boolean formatoValido(boolean[][] m) {
        int bit = 0;
        for (int i = 0; i <= 5; i++) {
            bit |= (m[i][8] ? 1 : 0) << i;
        }
        bit |= (m[7][8] ? 1 : 0) << 6;
        bit |= (m[8][8] ? 1 : 0) << 7;
        bit |= (m[8][7] ? 1 : 0) << 8;
        for (int i = 9; i < 15; i++) {
            bit |= (m[8][14 - i] ? 1 : 0) << i;
        }
        int senzaMaschera = bit ^ 0x5412;
        int dati = senzaMaschera >>> 10;
        int resto = dati;
        for (int i = 0; i < 10; i++) {
            resto = (resto << 1) ^ ((resto >>> 9) * 0x537);
        }
        return (dati << 10 | resto) == senzaMaschera;
    }

    /* ---- Code 128 ------------------------------------------------------ */

    private static void code128() {
        Prove.suite("Code 128");

        controlla("R-07-13");
        controlla("AST-1042");
        controlla("740125");
        controlla("0123456789");
        controlla("1234567890123456");
        controlla("9876543210987654321");
        controlla("AB12cd-.$/+%");
        controlla("X1");
        controlla(" ");

        List<Integer> soloCifre = Code128.valori("1234567890123456");
        Prove.uguale("sixteen digits start directly in set C", 105, soloCifre.get(0).intValue());
        Prove.uguale("they fit in eleven symbols including checksum and stop",
                11, soloCifre.size());

        List<Integer> misto = Code128.valori("R-07-13");
        Prove.uguale("a code containing letters starts in set B", 104, misto.get(0).intValue());

        Prove.vero("set C reduces width for long numeric runs",
                Code128.moduli("1234567890123456") < Code128.moduli("ABCDEFGHIJKLMNOP"));

        Prove.esplode("characters outside printable ASCII are rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        Code128.tratti("caff\u00e8");
                    }
                });
    }

    /** Reconstructs text from symbol values and revalidates checksum and widths. */
    private static void controlla(String testo) {
        List<Integer> v = Code128.valori(testo);
        int avvio = v.get(0);
        Prove.vero("\"" + testo + "\": starts with a valid start symbol",
                avvio == 103 || avvio == 104 || avvio == 105);
        Prove.uguale("\"" + testo + "\": ends with the stop symbol", 106,
                v.get(v.size() - 1).intValue());

        int somma = avvio;
        for (int i = 1; i < v.size() - 2; i++) {
            somma += v.get(i) * i;
        }
        Prove.uguale("\"" + testo + "\": checksum matches", somma % 103,
                v.get(v.size() - 2).intValue());

        StringBuilder rifatto = new StringBuilder();
        boolean inC = avvio == 105;
        for (int i = 1; i < v.size() - 2; i++) {
            int valore = v.get(i);
            if (valore == 99) {
                inC = true;
            } else if (valore == 100) {
                inC = false;
            } else if (inC) {
                rifatto.append(valore < 10 ? "0" + valore : String.valueOf(valore));
            } else {
                rifatto.append((char) (valore + 32));
            }
        }
        Prove.uguale("\"" + testo + "\": decoded text matches the source", testo, rifatto.toString());

        int somma2 = 0;
        for (int t : Code128.tratti(testo)) {
            somma2 += t;
        }
        Prove.uguale("\"" + testo + "\": run widths match declared modules",
                Code128.moduli(testo), somma2);
        Prove.uguale("\"" + testo + "\": module count matches Code 128 structure",
                (v.size() - 1) * 11 + 13, somma2);
    }
}
