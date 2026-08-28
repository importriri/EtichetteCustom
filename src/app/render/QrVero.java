package app.render;

import app.codice.Correzione;
import app.codice.Qr;

/** Production QR source backed by the repository encoder. */
public class QrVero implements SorgenteQr {

    @Override
    public boolean[][] matrice(String contenuto, Correzione livello) {
        return Qr.codifica(contenuto == null ? "" : contenuto,
                livello == null ? Correzione.M : livello);
    }
}
