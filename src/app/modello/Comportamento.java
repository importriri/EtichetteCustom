package app.modello;

/** Defines how a content source behaves during a print run. */
public enum Comportamento {

    /** Keeps the same stored value. */
    FISSO("Fisso"),
    /** Advances the configured numeric window once per printed label. */
    PROGRESSIVO("Progressivo"),
    /** Requests a value at the beginning of each print run. */
    CHIESTO("Chiesto a ogni stampa");

    private final String etichetta;

    Comportamento(String etichetta) {
        this.etichetta = etichetta;
    }

    public String etichetta() {
        return etichetta;
    }

    @Override
    public String toString() {
        return etichetta;
    }
}
