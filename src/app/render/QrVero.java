package app.render;

import app.codice.Correzione;
import app.codice.Qr;

/** La sorgente di produzione: l'encoder QR della repo. */
public class QrVero implements SorgenteQr {

    @Override
    public boolean[][] matrice(String contenuto, Correzione livello) {
        return Qr.codifica(contenuto == null ? "" : contenuto,
                livello == null ? Correzione.M : livello);
    }
}
