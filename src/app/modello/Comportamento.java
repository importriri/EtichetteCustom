package app.modello;

/** Come si comporta un campo quando parte un giro di stampa. */
public enum Comportamento {

    /** Vale sempre lo stesso: una sigla, un reparto, una revisione. */
    FISSO("Fisso"),
    /** Fa +1 a ogni etichetta dentro la finestra di cifre decisa dall'operatore. */
    PROGRESSIVO("Progressivo"),
    /** L'app lo chiede all'inizio di ogni giro: un lotto, una destinazione. */
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
