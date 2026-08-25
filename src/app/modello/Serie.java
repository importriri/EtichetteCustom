package app.modello;

/**
 * La finestra dell'incremento.
 *
 * Un codice come <code>740125.003_01-02_584700001</code> non e' un numero:
 * e' un prefisso immutabile piu' le ultime N cifre, che sono le uniche a
 * muoversi. N lo decide l'operatore. Tutto quello che sta a sinistra della
 * finestra non viene toccato mai.
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
            /* dieci cifre non entrano in un int: meglio dirlo che contare
               a rovescio da un numero negativo in mezzo a un giro */
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

    /** Il piu' grande numero che la finestra riesce a contenere. */
    public int massimo() {
        int m = 1;
        for (int i = 0; i < cifre; i++) {
            m *= 10;
        }
        return m - 1;
    }

    /** Solo la parte che si muove, con gli zeri davanti. */
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
     * I codici di un giro, a partire da {@link #prossimo()}.
     * Non avanza il contatore: quello lo fa {@link #consuma(int)} a stampa
     * riuscita, cosi' un giro annullato non brucia numeri.
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

    /** Da chiamare solo quando la stampa e' andata a buon fine. */
    public void consuma(int copie) {
        giro(copie);
        prossimo += copie;
    }

    @Override
    public String toString() {
        return codice(prossimo);
    }
}
