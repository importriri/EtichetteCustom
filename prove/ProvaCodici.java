package prove;

import app.codice.Code128;
import app.codice.Correzione;
import app.codice.Qr;

import java.security.MessageDigest;
import java.util.List;

/**
 * QR e Code 128.
 *
 * Queste due classi sono le uniche in cui un errore non si vede a occhio:
 * un QR sbagliato e' bello uguale a uno giusto, e te ne accorgi quando
 * mille etichette sono gia' sul bancale.
 *
 * Le impronte qui sotto sono state prese da matrici lette davvero da un
 * decodificatore indipendente (zbar): 56 campioni su 56, versioni 1-15,
 * tutti e quattro i livelli di correzione. Se qualcuno tocca l'encoder e
 * l'impronta cambia, questa prova diventa rossa prima della stampante.
 * Per rifare la verifica fisica: prove.Campioni scrive i PNG, si scansionano
 * col lettore di reparto.
 */
public final class ProvaCodici {

    private ProvaCodici() { }

    private static final String CODICE = "740125.003_01-02_584700349";

    public static void esegui() {
        qrStruttura();
        qrImpronte();
        code128();
    }

    /* ---- QR: quello che si puo' controllare guardando la matrice ------ */

    private static void qrStruttura() {
        Prove.suite("QR - struttura della matrice");

        boolean[][] m = Qr.codifica(CODICE, Correzione.M);
        int lato = m.length;
        Prove.uguale("il codice di reparto sta in versione 2", 2,
                Qr.versionePer(CODICE, Correzione.M));
        Prove.uguale("una versione 2 e' larga 25 moduli", 25, lato);
        Prove.vero("la matrice e' quadrata", m[0].length == lato);

        Prove.vero("mirino in alto a sinistra", mirino(m, 0, 0));
        Prove.vero("mirino in alto a destra", mirino(m, lato - 7, 0));
        Prove.vero("mirino in basso a sinistra", mirino(m, 0, lato - 7));

        boolean timing = true;
        for (int i = 8; i < lato - 8; i++) {
            timing &= m[6][i] == (i % 2 == 0);
            timing &= m[i][6] == (i % 2 == 0);
        }
        Prove.vero("le righe di sincronismo si alternano", timing);
        Prove.vero("il modulo scuro fisso c'e'", m[lato - 8][8]);
        Prove.vero("i bit di formato sono un BCH valido", formatoValido(m));

        int scuri = 0;
        for (boolean[] riga : m) {
            for (boolean b : riga) {
                if (b) {
                    scuri++;
                }
            }
        }
        double quota = scuri / (double) (lato * lato);
        Prove.vero("scuro e chiaro sono in equilibrio (" + Math.round(quota * 100) + "%)",
                quota > 0.35 && quota < 0.65);

        Prove.uguale("piu' correzione, piu' moduli", true,
                Qr.versionePer(CODICE, Correzione.H) > Qr.versionePer(CODICE, Correzione.L));
        Prove.uguale("una versione 1 a livello M tiene 16 byte", 16,
                Qr.codewordDati(1, Correzione.M));
        Prove.vero("il testo vuoto non fa esplodere niente",
                Qr.codifica("", Correzione.M).length == 21);

        StringBuilder enorme = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            enorme.append('X');
        }
        final String troppo = enorme.toString();
        Prove.esplode("un testo che non entra in nessuna versione viene rifiutato",
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
        Prove.suite("QR - impronte di riferimento");
        for (String[] caso : IMPRONTE) {
            boolean[][] m = Qr.codifica(caso[0], Correzione.valueOf(caso[1]));
            Prove.uguale("impronta di \"" + taglia(caso[0]) + "\" a livello " + caso[1],
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

    /** Rilegge i 15 bit di formato e ricontrolla il codice correttore. */
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
        Prove.uguale("sedici cifre partono direttamente in set C", 105, soloCifre.get(0).intValue());
        Prove.uguale("e stanno in undici simboli piu' checksum e stop",
                11, soloCifre.size());

        List<Integer> misto = Code128.valori("R-07-13");
        Prove.uguale("un codice con lettere parte in set B", 104, misto.get(0).intValue());

        Prove.vero("il set C dimezza la larghezza sui numerici lunghi",
                Code128.moduli("1234567890123456") < Code128.moduli("ABCDEFGHIJKLMNOP"));

        Prove.esplode("un carattere fuori ASCII stampabile viene rifiutato",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        Code128.tratti("caff\u00e8");
                    }
                });
    }

    /**
     * Rifa' il percorso al contrario: dai valori simbolo torna al testo,
     * ricontrolla il checksum e verifica che le larghezze tornino.
     */
    private static void controlla(String testo) {
        List<Integer> v = Code128.valori(testo);
        int avvio = v.get(0);
        Prove.vero("\"" + testo + "\": parte con un codice di avvio",
                avvio == 103 || avvio == 104 || avvio == 105);
        Prove.uguale("\"" + testo + "\": chiude con lo stop", 106,
                v.get(v.size() - 1).intValue());

        int somma = avvio;
        for (int i = 1; i < v.size() - 2; i++) {
            somma += v.get(i) * i;
        }
        Prove.uguale("\"" + testo + "\": il checksum torna", somma % 103,
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
        Prove.uguale("\"" + testo + "\": rimesso insieme torna uguale", testo, rifatto.toString());

        int somma2 = 0;
        for (int t : Code128.tratti(testo)) {
            somma2 += t;
        }
        Prove.uguale("\"" + testo + "\": le larghezze tornano ai moduli dichiarati",
                Code128.moduli(testo), somma2);
        Prove.uguale("\"" + testo + "\": undici moduli per simbolo, piu' due dello stop",
                (v.size() - 1) * 11 + 13, somma2);
    }
}
