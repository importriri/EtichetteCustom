package app.render;

import app.codice.Correzione;

/**
 * Da dove arriva la matrice del QR.
 *
 * Il disegno non deve sapere come si codifica un QR: gli basta una griglia
 * di moduli accesi e spenti. In produzione c'e' {@link QrVero}; nelle prove
 * si puo' infilare qualcosa di finto senza toccare una riga di interfaccia.
 */
public interface SorgenteQr {

    /** @return matrice quadrata, true = modulo scuro, senza zona di quiete. */
    boolean[][] matrice(String contenuto, Correzione livello);
}
