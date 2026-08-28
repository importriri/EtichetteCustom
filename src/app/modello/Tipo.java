package app.modello;

/** Type of visual element placed on a label. */
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

    /** Returns true for text elements that can wrap onto multiple lines. */
    public boolean scritto() {
        return this == TESTO || this == CODICE;
    }

    @Override
    public String toString() {
        return etichetta;
    }
}
