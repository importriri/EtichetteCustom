package app.codice;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generatore di codici QR, in modalita' byte, versioni dalla 1 alla 40.
 *
 * Scritto a mano perche' la repo non tira dentro librerie: un jar in piu'
 * e' un jar che qualcuno deve aggiornare fra due anni.
 *
 * Le tabelle qui sotto sono quelle dello standard ISO/IEC 18004. Non
 * fidatevi di me: le prove decodificano i QR generati con un lettore
 * indipendente (zbar) e confrontano la stringa che torna fuori.
 */
public final class Qr {

    /** Codeword di correzione per ogni blocco, per livello e versione. */
    private static final int[][] CORREZIONE_PER_BLOCCO = {
        { 0, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28,
          28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30 },
        { 0, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26,
          26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28 },
        { 0, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26,
          30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30 },
        { 0, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26,
          28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30 },
    };

    /** In quanti blocchi vengono divisi i dati, per livello e versione. */
    private static final int[][] BLOCCHI = {
        { 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7,
          8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25 },
        { 0, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14,
          16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49 },
        { 0, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21,
          20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68 },
        { 0, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25,
          25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81 },
    };

    private final int versione;
    private final int lato;
    private final boolean[][] moduli;
    private final boolean[][] funzione;

    private Qr(int versione, Correzione livello, int[] datiCodificati) {
        this.versione = versione;
        this.lato = versione * 4 + 17;
        this.moduli = new boolean[lato][lato];
        this.funzione = new boolean[lato][lato];

        disegnaPatternDiServizio();
        posaCodeword(intrecciaConCorrezione(datiCodificati, versione, livello));
        int maschera = scegliMaschera(livello);
        applicaMaschera(maschera);
        disegnaFormato(livello, maschera);
    }

    /* ================================================================
       Ingresso pubblico
       ================================================================ */

    /** @return matrice quadrata di moduli, true = scuro. Niente zona di quiete. */
    public static boolean[][] codifica(String testo, Correzione livello) {
        if (testo == null) {
            testo = "";
        }
        if (livello == null) {
            livello = Correzione.M;
        }
        byte[] byteDati = byteDi(testo);
        int versione = versioneMinima(byteDati.length, livello);
        int[] flusso = costruisciFlusso(byteDati, versione, livello);
        return new Qr(versione, livello, flusso).moduli;
    }

    /** La versione (1-40) che verrebbe usata: serve per capire quanto sara' fitto. */
    public static int versionePer(String testo, Correzione livello) {
        return versioneMinima(byteDi(testo).length, livello == null ? Correzione.M : livello);
    }

    private static byte[] byteDi(String testo) {
        try {
            return testo.getBytes("UTF-8");
        } catch (UnsupportedEncodingException impossibile) {
            throw new IllegalStateException("UTF-8 non disponibile", impossibile);
        }
    }

    private static int versioneMinima(int quantiByte, Correzione livello) {
        for (int v = 1; v <= 40; v++) {
            int bitDisponibili = codewordDati(v, livello) * 8;
            int bitNecessari = 4 + bitDiConteggio(v) + quantiByte * 8;
            if (bitNecessari <= bitDisponibili) {
                return v;
            }
        }
        throw new IllegalArgumentException(
                "testo troppo lungo per un QR: " + quantiByte + " byte");
    }

    private static int bitDiConteggio(int versione) {
        return versione <= 9 ? 8 : 16;
    }

    /* ================================================================
       Dai byte al flusso di codeword
       ================================================================ */

    private static int[] costruisciFlusso(byte[] dati, int versione, Correzione livello) {
        List<Boolean> bit = new ArrayList<Boolean>();
        aggiungiBit(bit, 0x4, 4);                                  /* modalita' byte */
        aggiungiBit(bit, dati.length, bitDiConteggio(versione));
        for (byte b : dati) {
            aggiungiBit(bit, b & 0xFF, 8);
        }

        int capacita = codewordDati(versione, livello) * 8;
        for (int i = 0; i < 4 && bit.size() < capacita; i++) {
            bit.add(Boolean.FALSE);                                /* terminatore */
        }
        while (bit.size() % 8 != 0) {
            bit.add(Boolean.FALSE);
        }
        int riempimento = 0xEC;
        while (bit.size() < capacita) {
            aggiungiBit(bit, riempimento, 8);
            riempimento = riempimento == 0xEC ? 0x11 : 0xEC;
        }

        int[] codeword = new int[bit.size() / 8];
        for (int i = 0; i < bit.size(); i++) {
            if (bit.get(i)) {
                codeword[i >>> 3] |= 1 << (7 - (i & 7));
            }
        }
        return codeword;
    }

    private static void aggiungiBit(List<Boolean> dove, int valore, int quanti) {
        for (int i = quanti - 1; i >= 0; i--) {
            dove.add(((valore >>> i) & 1) != 0);
        }
    }

    /** Moduli disponibili ai dati, tolto tutto quello che e' di servizio. */
    public static int moduliGrezzi(int versione) {
        int risultato = (16 * versione + 128) * versione + 64;
        if (versione >= 2) {
            int quanti = versione / 7 + 2;
            risultato -= (25 * quanti - 10) * quanti - 55;
            if (versione >= 7) {
                risultato -= 36;
            }
        }
        return risultato;
    }

    /** Quanti byte utili entrano in una versione, a quel livello di correzione. */
    public static int codewordDati(int versione, Correzione livello) {
        int l = livello.indice();
        return moduliGrezzi(versione) / 8
                - CORREZIONE_PER_BLOCCO[l][versione] * BLOCCHI[l][versione];
    }

    /**
     * Divide i dati in blocchi, calcola la correzione di ciascuno e li
     * intreccia: cosi' una macchia che rovina una zona del codice colpisce
     * un pezzetto di ogni blocco invece di distruggerne uno intero.
     */
    private static int[] intrecciaConCorrezione(int[] dati, int versione, Correzione livello) {
        int l = livello.indice();
        int quantiBlocchi = BLOCCHI[l][versione];
        int correzionePerBlocco = CORREZIONE_PER_BLOCCO[l][versione];
        int totale = moduliGrezzi(versione) / 8;
        int blocchiCorti = quantiBlocchi - totale % quantiBlocchi;
        int lunghezzaCorta = totale / quantiBlocchi;

        /*
         * Tutti i blocchi vengono allocati della stessa lunghezza, con la
         * correzione appoggiata in fondo. I blocchi corti restano cosi' con
         * un buco a meta', nell'unico punto in cui l'intreccio sa di dover
         * saltare: e' il patto che tiene allineate le due meta' del lavoro.
         */
        int lunghezzaBlocco = lunghezzaCorta + 1;
        int[][] blocchi = new int[quantiBlocchi][];
        int[] divisore = Galois.divisore(correzionePerBlocco);
        for (int i = 0, k = 0; i < quantiBlocchi; i++) {
            int quantiDati = lunghezzaCorta - correzionePerBlocco + (i < blocchiCorti ? 0 : 1);
            int[] parte = new int[quantiDati];
            System.arraycopy(dati, k, parte, 0, quantiDati);
            k += quantiDati;
            int[] correzione = Galois.resto(parte, divisore);
            int[] intero = new int[lunghezzaBlocco];
            System.arraycopy(parte, 0, intero, 0, quantiDati);
            System.arraycopy(correzione, 0, intero,
                    lunghezzaBlocco - correzionePerBlocco, correzionePerBlocco);
            blocchi[i] = intero;
        }

        int[] out = new int[totale];
        int p = 0;
        for (int i = 0; i < lunghezzaBlocco; i++) {
            for (int j = 0; j < quantiBlocchi; j++) {
                boolean buco = i == lunghezzaCorta - correzionePerBlocco && j < blocchiCorti;
                if (!buco) {
                    out[p++] = blocchi[j][i];
                }
            }
        }
        return out;
    }

    /* ================================================================
       Disegno della matrice
       ================================================================ */

    private void servizio(int x, int y, boolean scuro) {
        moduli[y][x] = scuro;
        funzione[y][x] = true;
    }

    private void disegnaPatternDiServizio() {
        for (int i = 0; i < lato; i++) {
            servizio(6, i, i % 2 == 0);
            servizio(i, 6, i % 2 == 0);
        }
        mirino(3, 3);
        mirino(lato - 4, 3);
        mirino(3, lato - 4);

        int[] posizioni = posizioniAllineamento();
        for (int i = 0; i < posizioni.length; i++) {
            for (int j = 0; j < posizioni.length; j++) {
                boolean angoloOccupato = (i == 0 && j == 0)
                        || (i == 0 && j == posizioni.length - 1)
                        || (i == posizioni.length - 1 && j == 0);
                if (!angoloOccupato) {
                    allineamento(posizioni[i], posizioni[j]);
                }
            }
        }

        disegnaFormato(Correzione.L, 0);   /* segnaposto: riscritto alla fine */
        disegnaVersione();
    }

    private void mirino(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int distanza = Math.max(Math.abs(dx), Math.abs(dy));
                int x = cx + dx;
                int y = cy + dy;
                if (x >= 0 && x < lato && y >= 0 && y < lato) {
                    servizio(x, y, distanza != 2 && distanza != 4);
                }
            }
        }
    }

    private void allineamento(int cx, int cy) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                servizio(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    private int[] posizioniAllineamento() {
        if (versione == 1) {
            return new int[0];
        }
        int quanti = versione / 7 + 2;
        int passo = versione == 32 ? 26
                : (versione * 4 + quanti * 2 + 1) / (quanti * 2 - 2) * 2;
        int[] out = new int[quanti];
        out[0] = 6;
        int pos = versione * 4 + 10;
        for (int i = quanti - 1; i >= 1; i--) {
            out[i] = pos;
            pos -= passo;
        }
        return out;
    }

    private void disegnaFormato(Correzione livello, int maschera) {
        int dati = livello.bitDiFormato() << 3 | maschera;
        int resto = dati;
        for (int i = 0; i < 10; i++) {
            resto = (resto << 1) ^ ((resto >>> 9) * 0x537);
        }
        int bit = (dati << 10 | resto) ^ 0x5412;

        for (int i = 0; i <= 5; i++) {
            servizio(8, i, prendiBit(bit, i));
        }
        servizio(8, 7, prendiBit(bit, 6));
        servizio(8, 8, prendiBit(bit, 7));
        servizio(7, 8, prendiBit(bit, 8));
        for (int i = 9; i < 15; i++) {
            servizio(14 - i, 8, prendiBit(bit, i));
        }

        for (int i = 0; i < 8; i++) {
            servizio(lato - 1 - i, 8, prendiBit(bit, i));
        }
        for (int i = 8; i < 15; i++) {
            servizio(8, lato - 15 + i, prendiBit(bit, i));
        }
        servizio(8, lato - 8, true);      /* il modulo scuro fisso */
    }

    private void disegnaVersione() {
        if (versione < 7) {
            return;
        }
        int resto = versione;
        for (int i = 0; i < 12; i++) {
            resto = (resto << 1) ^ ((resto >>> 11) * 0x1F25);
        }
        int bit = versione << 12 | resto;
        for (int i = 0; i < 18; i++) {
            boolean scuro = prendiBit(bit, i);
            int a = lato - 11 + i % 3;
            int b = i / 3;
            servizio(a, b, scuro);
            servizio(b, a, scuro);
        }
    }

    private void posaCodeword(int[] dati) {
        int i = 0;
        for (int destra = lato - 1; destra >= 1; destra -= 2) {
            if (destra == 6) {
                destra = 5;
            }
            for (int verticale = 0; verticale < lato; verticale++) {
                for (int j = 0; j < 2; j++) {
                    int x = destra - j;
                    boolean versoAlto = ((destra + 1) & 2) == 0;
                    int y = versoAlto ? lato - 1 - verticale : verticale;
                    if (!funzione[y][x] && i < dati.length * 8) {
                        moduli[y][x] = prendiBit(dati[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    private void applicaMaschera(int maschera) {
        for (int y = 0; y < lato; y++) {
            for (int x = 0; x < lato; x++) {
                if (funzione[y][x]) {
                    continue;
                }
                boolean inverti;
                switch (maschera) {
                    case 0: inverti = (x + y) % 2 == 0; break;
                    case 1: inverti = y % 2 == 0; break;
                    case 2: inverti = x % 3 == 0; break;
                    case 3: inverti = (x + y) % 3 == 0; break;
                    case 4: inverti = (x / 3 + y / 2) % 2 == 0; break;
                    case 5: inverti = x * y % 2 + x * y % 3 == 0; break;
                    case 6: inverti = (x * y % 2 + x * y % 3) % 2 == 0; break;
                    case 7: inverti = ((x + y) % 2 + x * y % 3) % 2 == 0; break;
                    default: throw new IllegalArgumentException("maschera " + maschera);
                }
                moduli[y][x] ^= inverti;
            }
        }
    }

    /**
     * Le otto maschere si provano tutte e vince quella con meno difetti:
     * grandi macchie uniformi e finti mirini sono quello che manda in crisi
     * un lettore a mano libera.
     */
    private int scegliMaschera(Correzione livello) {
        int migliore = 0;
        int punteggioMigliore = Integer.MAX_VALUE;
        for (int m = 0; m < 8; m++) {
            applicaMaschera(m);
            disegnaFormato(livello, m);
            int punteggio = penalita();
            applicaMaschera(m);              /* la maschera e' il suo stesso inverso */
            if (punteggio < punteggioMigliore) {
                punteggioMigliore = punteggio;
                migliore = m;
            }
        }
        return migliore;
    }

    private int penalita() {
        int p = 0;
        /* 1 - file di cinque o piu' moduli uguali */
        for (int y = 0; y < lato; y++) {
            boolean colore = moduli[y][0];
            int quanti = 1;
            for (int x = 1; x < lato; x++) {
                if (moduli[y][x] == colore) {
                    quanti++;
                } else {
                    p += punteggioFila(quanti);
                    colore = moduli[y][x];
                    quanti = 1;
                }
            }
            p += punteggioFila(quanti);
        }
        for (int x = 0; x < lato; x++) {
            boolean colore = moduli[0][x];
            int quanti = 1;
            for (int y = 1; y < lato; y++) {
                if (moduli[y][x] == colore) {
                    quanti++;
                } else {
                    p += punteggioFila(quanti);
                    colore = moduli[y][x];
                    quanti = 1;
                }
            }
            p += punteggioFila(quanti);
        }
        /* 2 - quadrati 2x2 dello stesso colore */
        for (int y = 0; y < lato - 1; y++) {
            for (int x = 0; x < lato - 1; x++) {
                boolean c = moduli[y][x];
                if (c == moduli[y][x + 1] && c == moduli[y + 1][x] && c == moduli[y + 1][x + 1]) {
                    p += 3;
                }
            }
        }
        /* 3 - sequenze che somigliano a un mirino */
        for (int y = 0; y < lato; y++) {
            for (int x = 0; x < lato - 10; x++) {
                if (somigliaAMirino(y, x, true)) {
                    p += 40;
                }
            }
        }
        for (int x = 0; x < lato; x++) {
            for (int y = 0; y < lato - 10; y++) {
                if (somigliaAMirino(x, y, false)) {
                    p += 40;
                }
            }
        }
        /* 4 - sbilanciamento fra scuro e chiaro */
        int scuri = 0;
        for (int y = 0; y < lato; y++) {
            for (int x = 0; x < lato; x++) {
                if (moduli[y][x]) {
                    scuri++;
                }
            }
        }
        int totale = lato * lato;
        int scarto = Math.abs(scuri * 100 / totale - 50) / 5;
        p += scarto * 10;
        return p;
    }

    private static int punteggioFila(int quanti) {
        return quanti >= 5 ? 3 + (quanti - 5) : 0;
    }

    private static final boolean[] MIRINO_FINTO =
            { true, false, true, true, true, false, true, false, false, false, false };

    private boolean somigliaAMirino(int fisso, int inizio, boolean orizzontale) {
        for (int i = 0; i < 11; i++) {
            boolean m = orizzontale ? moduli[fisso][inizio + i] : moduli[inizio + i][fisso];
            if (m != MIRINO_FINTO[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean prendiBit(int valore, int posizione) {
        return ((valore >>> posizione) & 1) != 0;
    }
}
