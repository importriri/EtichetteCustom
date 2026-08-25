package app.modello;

/** Che cosa e' un elemento sulla carta. */
public enum Tipo {

    TESTO("Testo", "T"),
    CODICE("Codice", "#"),
    QR("QR", "\u25A6"),
    BARCODE("Barcode", "|||"),
    LINEA("Linea", "\u2500");

    private final String etichetta;
    private final String glifo;

    Tipo(String etichetta, String glifo) {
        this.etichetta = etichetta;
        this.glifo = glifo;
    }

    public String etichetta() {
        return etichetta;
    }

    public String glifo() {
        return glifo;
    }

    /** Vero se l'elemento e' una scritta e quindi puo' andare a capo. */
    public boolean scritto() {
        return this == TESTO || this == CODICE;
    }

    @Override
    public String toString() {
        return etichetta;
    }
}
