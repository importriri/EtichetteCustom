package app.modello;

/**
 * Numeric sequence window embedded in a larger code.
 *
 * A code such as <code>740125.003_01-02_584700001</code> is not treated as one
 * number. Its prefix stays immutable and only the final N digits advance.
 */
public class Serie {

    private final String prefisso;
    private final int cifre;
    private int prossimo;

    public Serie(String codiceIniziale, int cifre) {
        if (cifre < 1) {
            throw new IllegalArgumentException("la finestra deve avere almeno una cifra");
        }
        if (cifre > 9) {
            /* Ten digits do not fit safely in the int-backed sequence counter. */
            throw new IllegalArgumentException(
                    "la finestra non puo' superare le 9 cifre: " + cifre);
        }
        if (codiceIniziale == null || codiceIniziale.length() < cifre) {
            throw new IllegalArgumentException(
                    "il codice e' piu' corto della finestra di " + cifre + " cifre");
        }
        String coda = codiceIniziale.substring(codiceIniziale.length() - cifre);
        for (int i = 0; i < coda.length(); i++) {
            if (!Character.isDigit(coda.charAt(i))) {
                throw new IllegalArgumentException(
                        "le ultime " + cifre + " posizioni non sono tutte cifre: \"" + coda + "\"");
            }
        }
        this.prefisso = codiceIniziale.substring(0, codiceIniziale.length() - cifre);
        this.cifre = cifre;
        this.prossimo = Integer.parseInt(coda);
    }

    public String prefisso() {
        return prefisso;
    }

    public int cifre() {
        return cifre;
    }

    public int prossimo() {
        return prossimo;
    }

    public void prossimo(int valore) {
        if (valore < 0 || valore > massimo()) {
            throw new IllegalArgumentException(
                    "fuori dalla finestra di " + cifre + " cifre: " + valore);
        }
        prossimo = valore;
    }

    /** Largest value that fits in the configured numeric window. */
    public int massimo() {
        int m = 1;
        for (int i = 0; i < cifre; i++) {
            m *= 10;
        }
        return m - 1;
    }

    /** Formats only the advancing part, including leading zeroes. */
    public String finestra(int numero) {
        StringBuilder b = new StringBuilder(Integer.toString(numero));
        while (b.length() < cifre) {
            b.insert(0, '0');
        }
        return b.toString();
    }

    public String codice(int numero) {
        return prefisso + finestra(numero);
    }

    /**
     * Returns one run starting at {@link #prossimo()} without advancing state.
     * State moves only through {@link #consuma(int)} after a successful print.
     */
    public String[] giro(int copie) {
        if (copie < 1) {
            throw new IllegalArgumentException("almeno una copia");
        }
        long ultimo = (long) prossimo + copie - 1;
        if (ultimo > massimo()) {
            throw new IllegalStateException(
                    "il giro sfora la finestra di " + cifre + " cifre: servirebbe "
                            + ultimo + ", il massimo e' " + massimo());
        }
        String[] out = new String[copie];
        for (int i = 0; i < copie; i++) {
            out[i] = codice(prossimo + i);
        }
        return out;
    }

    /** Advances state after a successful print run. */
    public void consuma(int copie) {
        giro(copie);
        prossimo += copie;
    }

    @Override
    public String toString() {
        return codice(prossimo);
    }
}
