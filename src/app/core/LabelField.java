package app.core;

import java.util.Locale;

/**
 * Un campo dell'etichetta: un valore con un nome, richiamabile dagli elementi.
 *
 * <p>È il pezzo che rende disegnabile qualsiasi etichetta senza toccare il
 * codice. Prima esisteva "il codice", uno solo, e tutto ruotava attorno a
 * quello: chi voleva un secondo numero progressivo, o un lotto che cambia una
 * volta al giorno, non aveva dove metterlo.
 *
 * <p>Adesso l'etichetta ha una lista di campi. Ognuno ha un nome — {@code lotto},
 * {@code seriale}, {@code disegno} — e gli elementi lo richiamano scrivendo
 * <code>{lotto}</code> dentro il loro contenuto. Un QR, una riga di testo o
 * dieci elementi diversi possono pescare dallo stesso campo o da campi diversi,
 * e la stessa etichetta può avere due progressivi che avanzano insieme.
 *
 * <p>Tre modi di riempirlo, e sono tutti quelli che servono in reparto:
 * <ul>
 *   <li>{@link Type#FISSO}: sempre lo stesso valore, tipo il numero di disegno;</li>
 *   <li>{@link Type#SEQUENZIALE}: le ultime N cifre avanzano di uno a ogni
 *       etichetta, esattamente come faceva il vecchio codice;</li>
 *   <li>{@link Type#CHIESTO}: il valore viene chiesto quando si lancia la
 *       stampa, per il lotto o il numero d'ordine che cambia ogni volta.</li>
 * </ul>
 */
public final class LabelField {

    /** Come si riempie il campo. */
    public enum Type {
        FISSO("Fisso"),
        SEQUENZIALE("Progressivo"),
        CHIESTO("Chiesto a ogni stampa");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Il nome del campo che gli elementi usano di default. */
    public static final String DEFAULT_NAME = "codice";

    private String name;
    private Type type = Type.FISSO;
    private String value = "";
    private int digits = 3;

    public LabelField(String name, Type type, String value) {
        setName(name);
        setType(type);
        setValue(value);
    }

    /** Un progressivo: le ultime {@code digits} cifre avanzano. */
    public static LabelField sequential(String name, String start, int digits) {
        LabelField f = new LabelField(name, Type.SEQUENZIALE, start);
        f.setDigits(digits);
        return f;
    }

    /** Un valore che non cambia mai. */
    public static LabelField fixed(String name, String value) {
        return new LabelField(name, Type.FISSO, value);
    }

    /** Un valore chiesto all'operatore prima di stampare. */
    public static LabelField asked(String name, String lastValue) {
        return new LabelField(name, Type.CHIESTO, lastValue);
    }

    public LabelField copy() {
        LabelField c = new LabelField(name, type, value);
        c.digits = digits;
        return c;
    }

    // --- proprietà ------------------------------------------------------------

    /** Il nome, senza graffe: è quello che si scrive fra <code>{ }</code>. */
    public String name() {
        return name;
    }

    /**
     * Il nome viene normalizzato: minuscole, niente spazi né graffe.
     *
     * <p>Non è pignoleria. Il nome finisce dentro un segnaposto e dentro il file
     * di impostazioni: se ci passa uno spazio o una parentesi, il segnaposto non
     * combacia più e l'operatore vede <code>{mio campo}</code> stampato sul
     * supporto invece del valore.
     */
    public void setName(String name) {
        String clean = name == null ? "" : name.trim().toLowerCase(Locale.ITALIAN);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-') {
                sb.append(c);
            } else if (c == ' ' || c == '_') {
                sb.append('_');
            }
        }
        if (sb.length() == 0) {
            throw new IllegalArgumentException(
                    "Il nome del campo non può essere vuoto: serve per richiamarlo con { }.");
        }
        this.name = sb.toString();
    }

    /** Il segnaposto pronto da incollare in un elemento. */
    public String token() {
        return "{" + name + "}";
    }

    public Type type() {
        return type;
    }

    public void setType(Type type) {
        this.type = type == null ? Type.FISSO : type;
    }

    /** Il valore attuale: per un progressivo è quello della prima etichetta. */
    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
    }

    /** Quante cifre finali avanzano, per un progressivo. */
    public int digits() {
        return digits;
    }

    public void setDigits(int digits) {
        if (digits < SerialWindow.MIN_WIDTH || digits > SerialWindow.MAX_WIDTH) {
            throw new IllegalArgumentException("Le cifre da incrementare devono stare tra "
                    + SerialWindow.MIN_WIDTH + " e " + SerialWindow.MAX_WIDTH + ".");
        }
        this.digits = digits;
    }

    // --- valori del giro ------------------------------------------------------

    /**
     * Il valore che questo campo assume alla {@code index}-esima etichetta.
     *
     * <p>Un campo fisso o chiesto vale sempre uguale; un progressivo avanza. È
     * l'unico punto in cui la differenza fra i tre tipi diventa visibile, ed è
     * il motivo per cui aggiungerne un quarto costerà tre righe.
     */
    public String valueAt(long index) {
        if (type != Type.SEQUENZIALE) {
            return value;
        }
        try {
            return SerialWindow.of(value, digits).at(index);
        } catch (RuntimeException notACounter) {
            // il valore non è (ancora) un contatore valido: succede a ogni
            // battuta mentre l'operatore riscrive il codice, e a metà parola il
            // campo è vuoto. L'anteprima deve mostrare quello che c'è, non
            // fermare l'applicazione: a dire che il codice non va bene ci
            // pensano gli avvisi, che si leggono senza far saltare niente
            return value;
        }
    }

    /**
     * Controlla che il campo regga un giro di {@code count} etichette.
     *
     * <p>Solo i progressivi possono esaurirsi, e quando succede il giro va
     * fermato <b>prima</b> di stampare la prima etichetta: riavvolgere il
     * contatore a zero vorrebbe dire due etichette con lo stesso QR.
     */
    public void checkRun(int count) {
        if (type == Type.SEQUENZIALE) {
            try {
                SerialWindow.of(value, digits).checkRun(count);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Campo \"" + name + "\": " + e.getMessage());
            }
        }
    }

    /** Quante etichette restano prima di esaurire il progressivo; -1 se non si esaurisce. */
    public long remaining() {
        if (type != Type.SEQUENZIALE) {
            return -1;
        }
        try {
            return SerialWindow.of(value, digits).remaining();
        } catch (RuntimeException notANumber) {
            return -1;
        }
    }

    // --- persistenza ----------------------------------------------------------

    private static final String SEP = ",";

    public String toStorage() {
        return LabelElement.esc(name) + SEP + type.name() + SEP + digits
                + SEP + LabelElement.esc(value);
    }

    /** Rilegge un campo serializzato, o {@code null} se la riga è illeggibile. */
    public static LabelField fromStorage(String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        String[] f = stored.split(SEP, -1);
        if (f.length < 4) {
            return null;
        }
        try {
            LabelField field = new LabelField(LabelElement.unesc(f[0]),
                    Type.valueOf(f[1]), LabelElement.unesc(f[3]));
            field.setDigits(Integer.parseInt(f[2]));
            return field;
        } catch (RuntimeException broken) {
            return null;
        }
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
