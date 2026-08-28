package app.codice;

/**
 * QR error-correction level. Higher levels consume more modules but tolerate
 * more dirt, scratches and physical damage on production labels.
 */
public enum Correzione {

    /** About 7% recovery; useful when space is extremely limited. */
    L(0, 1, "L \u2014 bassa"),
    /** About 15%; the normal compromise. */
    M(1, 0, "M \u2014 media"),
    /** About 25% recovery. */
    Q(2, 3, "Q \u2014 alta"),
    /** About 30% recovery for labels exposed to damage. */
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
