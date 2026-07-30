package app.core.export;

import java.awt.Shape;

/**
 * Superficie su cui l'etichetta viene disegnata.
 *
 * <p>Un metodo solo, di proposito: tutto ciò che finisce su un'etichetta —
 * moduli del QR, lettere del codice, sigla — arriva qui già ridotto a una forma
 * geometrica in millimetri. Così il layout esiste una volta sola e PNG, PDF, SVG,
 * anteprima e stampa non possono divergere fra loro.
 */
public interface LabelCanvas {

    /** Riempie la forma, espressa in millimetri con origine in alto a sinistra. */
    void fill(Shape shapeMm);
}
