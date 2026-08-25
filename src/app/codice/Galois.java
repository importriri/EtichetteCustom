package app.codice;

/**
 * Aritmetica nel campo di Galois GF(256) e codici Reed-Solomon.
 *
 * E' la parte che permette a un QR sporco di essere letto lo stesso:
 * ai dati si accodano dei codeword calcolati qui, e il lettore li usa
 * per ricostruire quello che non riesce a vedere.
 */
final class Galois {

    /** Polinomio del campo usato dallo standard QR: x^8 + x^4 + x^3 + x^2 + 1. */
    private static final int POLINOMIO = 0x11D;

    private Galois() { }

    /** Moltiplicazione nel campo, senza tabelle: 8 passi e via. */
    static int per(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * POLINOMIO);
            z ^= ((y >>> i) & 1) * x;
        }
        return z & 0xFF;
    }

    /** Il divisore di grado dato: (x - r^0)(x - r^1)... */
    static int[] divisore(int grado) {
        if (grado < 1 || grado > 255) {
            throw new IllegalArgumentException("grado fuori scala: " + grado);
        }
        int[] out = new int[grado];
        out[grado - 1] = 1;
        int radice = 1;
        for (int i = 0; i < grado; i++) {
            for (int j = 0; j < grado; j++) {
                out[j] = per(out[j] & 0xFF, radice);
                if (j + 1 < grado) {
                    out[j] ^= out[j + 1];
                }
            }
            radice = per(radice, 0x02);
        }
        return out;
    }

    /** I codeword di correzione per un blocco di dati. */
    static int[] resto(int[] dati, int[] divisore) {
        int[] out = new int[divisore.length];
        for (int b : dati) {
            int fattore = (b ^ out[0]) & 0xFF;
            System.arraycopy(out, 1, out, 0, out.length - 1);
            out[out.length - 1] = 0;
            for (int i = 0; i < out.length; i++) {
                out[i] ^= per(divisore[i] & 0xFF, fattore);
            }
        }
        return out;
    }
}
