package app.codice;

/**
 * Quanto danno regge un QR prima di diventare illeggibile.
 *
 * In reparto un'etichetta si sporca, si graffia, prende olio: alzare la
 * correzione costa moduli (quindi spazio) ma compra letture in piu'.
 */
public enum Correzione {

    /** ~7% di recupero. Solo se lo spazio e' tiranno e l'etichetta sta al chiuso. */
    L(0, 1, "L \u2014 bassa"),
    /** ~15%. Il compromesso normale. */
    M(1, 0, "M \u2014 media"),
    /** ~25%. */
    Q(2, 3, "Q \u2014 alta"),
    /** ~30%. Etichette che prendono botte. */
    H(3, 2, "H \u2014 massima");

    private final int indice;
    private final int bitDiFormato;
    private final String etichetta;

    Correzione(int indice, int bitDiFormato, String etichetta) {
        this.indice = indice;
        this.bitDiFormato = bitDiFormato;
        this.etichetta = etichetta;
    }

    public int indice() {
        return indice;
    }

    public int bitDiFormato() {
        return bitDiFormato;
    }

    @Override
    public String toString() {
        return etichetta;
    }
}
