package app.core;

import java.util.Locale;

/**
 * Finestra di incremento seriale.
 *
 * <p>Le ultime {@code width} posizioni del codice sono la finestra che viene
 * incrementata a ogni etichetta. Tutto ciò che sta alla sua sinistra è prefisso
 * e non viene mai toccato. La finestra si conta da destra verso sinistra, che è
 * esattamente la scelta lasciata all'operatore nel tab Impostazioni.
 *
 * <p>Esempio con {@code width = 3}:
 * <pre>
 *   TST-0000-00-001  ->  TST-0000-00-002  ->  TST-0000-00-003
 *   ^^^^^^^^^^^^ prefisso   ^^^ finestra
 * </pre>
 *
 * <p>La classe è immutabile e non conosce Swing: i messaggi delle eccezioni sono
 * già in italiano e possono finire dritti in un dialog.
 *
 * <p>Politica di esaurimento: <b>fail-closed</b>. Quando la finestra è piena non
 * si riavvolge a zero e non riporta sul prefisso, perché entrambe le cose
 * produrrebbero due etichette con lo stesso QR. Si ferma e lo dice.
 */
public final class SerialWindow {

    /** Larghezze ammesse. Nove cifre sono un miliardo di etichette: oltre non è più un contatore. */
    public static final int MIN_WIDTH = 1;
    public static final int MAX_WIDTH = 9;

    private final String prefix;
    private final int width;
    private final long start;

    private SerialWindow(String prefix, int width, long start) {
        this.prefix = prefix;
        this.width = width;
        this.start = start;
    }

    /**
     * Costruisce la finestra sul codice inserito dall'operatore.
     *
     * @param code  il codice completo, prefisso incluso
     * @param width quante cifre finali entrano nell'incremento
     * @throws IllegalArgumentException se la finestra non sta sul codice o non
     *                                  cade su cifre; il messaggio è già in italiano
     */
    public static SerialWindow of(String code, int width) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Il codice è vuoto.");
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new IllegalArgumentException(
                    "Le cifre da incrementare devono essere tra " + MIN_WIDTH + " e " + MAX_WIDTH
                    + ", ricevuto " + width + ".");
        }
        if (code.length() < width) {
            throw new IllegalArgumentException(
                    "Il codice \"" + code + "\" ha " + code.length()
                    + " caratteri: non può avere " + width + " cifre di incremento.");
        }
        int cut = code.length() - width;
        String tail = code.substring(cut);
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException(
                        "Le ultime " + width + " posizioni del codice non sono tutte cifre: \""
                        + tail + "\". Riduci le cifre di incremento o correggi il codice.");
            }
        }
        return new SerialWindow(code.substring(0, cut), width, Long.parseLong(tail));
    }

    /** La parte immutabile, alla sinistra della finestra. */
    public String prefix() {
        return prefix;
    }

    /** Quante cifre finali entrano nell'incremento. */
    public int width() {
        return width;
    }

    /** Il valore di partenza letto dal codice inserito. */
    public long start() {
        return start;
    }

    /** La sola finestra del codice di partenza, zero-padded: quello che la UI evidenzia. */
    public String tail() {
        return format(start);
    }

    /** Quanti valori distinti entrano nella finestra: 10^width. */
    public long capacity() {
        long max = 1L;
        for (int i = 0; i < width; i++) {
            max *= 10L;
        }
        return max;
    }

    /** Quante etichette si possono ancora stampare prima di esaurire la finestra. */
    public long remaining() {
        return capacity() - start;
    }

    /** L'ultimo codice che questa finestra può produrre. */
    public String last() {
        return prefix + format(capacity() - 1);
    }

    /**
     * Pre-flight dell'intero giro. Va chiamato <b>prima</b> di mandare in stampa,
     * non a metà rullo: l'operatore deve scoprire che il contatore non basta
     * davanti all'anteprima, non con trenta etichette già stampate.
     *
     * @throws IllegalArgumentException se la quantità non ha senso
     * @throws IllegalStateException    se il giro sfonda la finestra
     */
    public void checkRun(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("La quantità deve essere almeno 1.");
        }
        if (count > remaining()) {
            throw new IllegalStateException(
                    "Con " + width + " cifre l'ultimo codice stampabile è " + last()
                    + ": da " + at(0) + " restano " + remaining()
                    + " etichette, ne hai chieste " + count + ".");
        }
    }

    /**
     * Il codice della i-esima etichetta del giro, dove {@code 0} è la prima.
     *
     * @throws IllegalStateException se il valore esce dalla finestra
     */
    public String at(long index) {
        if (index < 0) {
            throw new IllegalArgumentException("Indice negativo: " + index + ".");
        }
        long value = start + index;
        if (value >= capacity()) {
            throw new IllegalStateException(
                    "Contatore esaurito: la finestra a " + width + " cifre si ferma a "
                    + last() + ".");
        }
        return prefix + format(value);
    }

    /**
     * L'intero giro già materializzato. Fa il pre-flight per conto suo, quindi o
     * restituisce tutti i codici o non ne restituisce nessuno.
     */
    public String[] run(int count) {
        checkRun(count);
        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = at(i);
        }
        return codes;
    }

    private String format(long value) {
        return String.format(Locale.ROOT, "%0" + width + "d", value);
    }

    @Override
    public String toString() {
        return "SerialWindow[" + prefix + "|" + tail() + ", width=" + width
                + ", remaining=" + remaining() + "]";
    }
}
