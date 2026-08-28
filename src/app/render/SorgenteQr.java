package app.render;

import app.codice.Correzione;

/**
 * Source of the QR module matrix.
 *
 * Rendering only needs a grid of dark and light modules. Production uses
 * {@link QrVero}; tests can provide a deterministic source without touching UI code.
 */
public interface SorgenteQr {

    /** @return square matrix, true for a dark module, without the quiet zone. */
    boolean[][] matrice(String contenuto, Correzione livello);
}
