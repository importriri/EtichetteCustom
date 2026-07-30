package app.core;

import java.util.Locale;

/**
 * Un elemento dell'etichetta: un testo o un QR.
 *
 * <p>Il modello è volutamente generico. L'etichetta non ha "il codice, il QR e
 * la sigla": ha una lista di elementi, tutti della stessa specie, ciascuno con
 * la sua posizione, la sua misura e la sua rotazione. Aggiungere una seconda
 * riga di testo o un secondo QR non richiede una riga di codice nuova, e i
 * layout richiesti dai clienti diventano dati, non classi.
 *
 * <p>Il contenuto è un modello di testo: {@code {codice}} viene sostituito con
 * il codice dell'etichetta in stampa. Tutto il resto è letterale. Così il QR
 * di un'etichetta con codice composto — {@code DEMO-4410.07_A2-01_000001} —
 * si scrive {@code {articolo}.{revisione}_{seriale}}, e le righe leggibili
 * sotto sono altri elementi che pescano dagli stessi campi.
 *
 * <p>L'ancora è {@code (xMm, yMm)}: per un QR è l'angolo in alto a sinistra,
 * per un testo è l'inizio della linea di base. La rotazione avviene <b>attorno
 * all'ancora</b>, in gradi orari, così ruotare non sposta il punto che
 * l'operatore ha appena posizionato.
 */
public final class LabelElement {

    /** Che cosa disegna l'elemento. */
    public enum Kind {
        /** Testo vettoriale: la misura è l'altezza delle maiuscole. */
        TESTO("Testo"),
        /** Codice QR quadrato: la misura è il lato. */
        QR("QR");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Da dove parte il testo rispetto all'ancora. Il QR ignora l'allineamento. */
    public enum Align {
        SINISTRA("Sinistra"), CENTRO("Centro"), DESTRA("Destra");

        private final String label;

        Align(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Il segnaposto sostituito con il codice dell'etichetta in stampa. */
    public static final String CODE_TOKEN = "{codice}";

    /** Alias inglese dello stesso segnaposto: i layout girano anche fuori dall'Italia. */
    public static final String CODE_TOKEN_EN = "{code}";

    public static final double MIN_SIZE_MM = 0.5;
    public static final double MAX_SIZE_MM = 300.0;

    /** Interlinea di serie, in multipli dell'altezza delle maiuscole. */
    public static final double DEFAULT_LINE_SPACING = 1.35;

    private String name;
    private Kind kind;
    private String content;
    private double xMm;
    private double yMm;
    private double sizeMm;
    private double rotationDeg;
    private boolean bold;
    private Align align = Align.SINISTRA;
    private double wrapWidthMm = 0.0;
    private double lineSpacing = DEFAULT_LINE_SPACING;

    public LabelElement(String name, Kind kind, String content,
                        double xMm, double yMm, double sizeMm) {
        setName(name);
        setKind(kind);
        setContent(content);
        setPosition(xMm, yMm);
        setSizeMm(sizeMm);
    }

    /** Un elemento di testo pronto all'uso. */
    public static LabelElement text(String name, String content,
                                    double xMm, double yMm, double heightMm) {
        return new LabelElement(name, Kind.TESTO, content, xMm, yMm, heightMm);
    }

    /** Un QR pronto all'uso. */
    public static LabelElement qr(String name, String content,
                                  double xMm, double yMm, double sideMm) {
        return new LabelElement(name, Kind.QR, content, xMm, yMm, sideMm);
    }

    public LabelElement copy() {
        LabelElement c = new LabelElement(name, kind, content, xMm, yMm, sizeMm);
        c.rotationDeg = rotationDeg;
        c.bold = bold;
        c.align = align;
        c.wrapWidthMm = wrapWidthMm;
        c.lineSpacing = lineSpacing;
        return c;
    }

    // --- proprietà ------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = (name == null || name.trim().isEmpty()) ? "Elemento" : name.trim();
    }

    public Kind kind() {
        return kind;
    }

    public void setKind(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Tipo di elemento nullo.");
        }
        this.kind = kind;
    }

    public String content() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public double xMm() {
        return xMm;
    }

    public double yMm() {
        return yMm;
    }

    public void setPosition(double xMm, double yMm) {
        if (Double.isNaN(xMm) || Double.isNaN(yMm)) {
            throw new IllegalArgumentException("Posizione non numerica.");
        }
        this.xMm = xMm;
        this.yMm = yMm;
    }

    /** Altezza delle maiuscole per un testo, lato per un QR. */
    public double sizeMm() {
        return sizeMm;
    }

    public void setSizeMm(double sizeMm) {
        if (Double.isNaN(sizeMm) || sizeMm < MIN_SIZE_MM || sizeMm > MAX_SIZE_MM) {
            throw new IllegalArgumentException("Misura non valida per \"" + name + "\": "
                    + sizeMm + " mm (ammesso da " + MIN_SIZE_MM + " a " + MAX_SIZE_MM + ").");
        }
        this.sizeMm = sizeMm;
    }

    /** Ingrandisce o rimpicciolisce di un fattore, restando dentro i limiti. */
    public void scaleBy(double factor) {
        if (Double.isNaN(factor) || factor <= 0) {
            throw new IllegalArgumentException("Fattore di scala non valido: " + factor + ".");
        }
        setSizeMm(Math.max(MIN_SIZE_MM, Math.min(MAX_SIZE_MM, sizeMm * factor)));
    }

    /** Gradi in senso orario, sempre normalizzati in [0, 360). */
    public double rotationDeg() {
        return rotationDeg;
    }

    public void setRotationDeg(double deg) {
        if (Double.isNaN(deg) || Double.isInfinite(deg)) {
            throw new IllegalArgumentException("Rotazione non numerica.");
        }
        double d = deg % 360.0;
        this.rotationDeg = d < 0 ? d + 360.0 : d;
    }

    /** Ruota di un quarto di giro in senso orario: il gesto che serve il 99% delle volte. */
    public void rotateQuarterTurn() {
        setRotationDeg(rotationDeg + 90.0);
    }

    public boolean bold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public Align align() {
        return align;
    }

    public void setAlign(Align align) {
        this.align = align == null ? Align.SINISTRA : align;
    }

    /**
     * Larghezza entro cui il testo va a capo, in millimetri. Zero = riga unica.
     *
     * <p>È la misura che l'operatore stringe quando un codice lungo non ci sta:
     * a 40 mm resta su una riga, a 20 va su due, a 13 su tre. Il testo non
     * rimpicciolisce — resta leggibile e si dispone su più righe, che su
     * un'etichetta è quasi sempre quello che si vuole.
     *
     * <p>Il taglio avviene sugli spazi, sui trattini e sui trattini bassi.
     * Sono i punti dove i codici di reparto si spezzano naturalmente:
     * {@code DEMO-4410.07_A2-01_000001} si divide dopo un underscore, non a
     * metà di un gruppo di cifre.
     */
    public double wrapWidthMm() {
        return wrapWidthMm;
    }

    public void setWrapWidthMm(double mm) {
        if (Double.isNaN(mm) || mm < 0 || mm > MAX_SIZE_MM) {
            throw new IllegalArgumentException("Larghezza di a capo non valida per \""
                    + name + "\": " + mm + " mm (0 = nessun a capo).");
        }
        this.wrapWidthMm = mm;
    }

    /** {@code true} se il testo può andare a capo da solo. */
    public boolean wraps() {
        return kind == Kind.TESTO && wrapWidthMm > 0;
    }

    /** Distanza fra una riga e l'altra, in multipli dell'altezza delle maiuscole. */
    public double lineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(double factor) {
        if (Double.isNaN(factor) || factor < 1.0 || factor > 4.0) {
            throw new IllegalArgumentException(
                    "L'interlinea deve stare tra 1,0 e 4,0, ricevuto " + factor + ".");
        }
        this.lineSpacing = factor;
    }

    // --- contenuto risolto ----------------------------------------------------

    /** Il contenuto con {@code {codice}} già sostituito. */
    public String resolve(String code) {
        java.util.Map<String, String> one = new java.util.HashMap<String, String>();
        one.put(LabelField.DEFAULT_NAME, code == null ? "" : code);
        return resolve(one);
    }

    /**
     * Il contenuto con ogni segnaposto sostituito dal valore del suo campo.
     *
     * <p>Un segnaposto che non corrisponde a nessun campo viene lasciato com'è:
     * stampare <code>{lotto}</code> sul supporto è brutto, ma è un difetto che
     * si vede subito in anteprima — mentre cancellarlo in silenzio produrrebbe
     * un'etichetta incompleta che nessuno nota fino al cliente.
     */
    public String resolve(java.util.Map<String, String> values) {
        if (content.indexOf('{') < 0) {
            return content;
        }
        StringBuilder out = new StringBuilder(content.length() + 16);
        int i = 0;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c != '{') {
                out.append(c);
                i++;
                continue;
            }
            int close = content.indexOf('}', i);
            if (close < 0) {
                out.append(content.substring(i));
                break;
            }
            String key = normalizeToken(content.substring(i + 1, close));
            String value = values == null ? null : values.get(key);
            out.append(value != null ? value : content.substring(i, close + 1));
            i = close + 1;
        }
        return out.toString();
    }

    /** {@code code} è l'alias inglese di {@code codice}: i layout girano anche fuori. */
    private static String normalizeToken(String raw) {
        String k = raw.trim().toLowerCase(java.util.Locale.ITALIAN);
        return "code".equals(k) ? LabelField.DEFAULT_NAME : k;
    }

    /** I nomi dei campi richiamati da questo elemento, nell'ordine in cui compaiono. */
    public java.util.List<String> tokens() {
        java.util.List<String> names = new java.util.ArrayList<String>();
        int i = 0;
        while ((i = content.indexOf('{', i)) >= 0) {
            int close = content.indexOf('}', i);
            if (close < 0) {
                break;
            }
            String key = normalizeToken(content.substring(i + 1, close));
            if (!key.isEmpty() && !names.contains(key)) {
                names.add(key);
            }
            i = close + 1;
        }
        return names;
    }

    /** {@code true} se il contenuto dipende da almeno un campo. */
    public boolean usesCode() {
        return !tokens().isEmpty();
    }

    /** {@code true} se il contenuto richiama proprio quel campo. */
    public boolean uses(String fieldName) {
        return tokens().contains(fieldName);
    }

    // --- persistenza ----------------------------------------------------------

    private static final String FIELD_SEPARATOR = ",";

    /** Serializza l'elemento in un campo solo, separatori compresi. */
    public String toStorage() {
        StringBuilder sb = new StringBuilder();
        sb.append(esc(name)).append(FIELD_SEPARATOR);
        sb.append(kind.name()).append(FIELD_SEPARATOR);
        sb.append(num(xMm)).append(FIELD_SEPARATOR);
        sb.append(num(yMm)).append(FIELD_SEPARATOR);
        sb.append(num(sizeMm)).append(FIELD_SEPARATOR);
        sb.append(num(rotationDeg)).append(FIELD_SEPARATOR);
        sb.append(bold ? "1" : "0").append(FIELD_SEPARATOR);
        sb.append(align.name()).append(FIELD_SEPARATOR);
        sb.append(num(wrapWidthMm)).append(FIELD_SEPARATOR);
        sb.append(num(lineSpacing)).append(FIELD_SEPARATOR);
        sb.append(esc(content));
        return sb.toString();
    }

    /**
     * Rilegge un elemento serializzato.
     *
     * @return l'elemento, oppure {@code null} se la riga è illeggibile: un
     *         layout salvato male fa perdere un elemento, non l'avvio dell'app
     */
    public static LabelElement fromStorage(String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        String[] f = stored.split(FIELD_SEPARATOR, -1);
        if (f.length < 9) {
            return null;
        }
        try {
            // dieci campi in su = formato con a capo e interlinea; nove = quello
            // di prima, che si rilegge lo stesso con i valori di serie
            boolean wrapped = f.length >= 11;
            LabelElement e = new LabelElement(unesc(f[0]), Kind.valueOf(f[1]),
                    unesc(f[wrapped ? 10 : 8]),
                    Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                    Double.parseDouble(f[4]));
            e.setRotationDeg(Double.parseDouble(f[5]));
            e.setBold("1".equals(f[6]));
            e.setAlign(Align.valueOf(f[7]));
            if (wrapped) {
                e.setWrapWidthMm(Double.parseDouble(f[8]));
                e.setLineSpacing(Double.parseDouble(f[9]));
            }
            return e;
        } catch (RuntimeException broken) {
            return null;
        }
    }

    /**
     * I separatori del formato non possono comparire nei dati: chi scrive
     * {@code A;B=C, D} in una sigla non deve spaccare il file di impostazioni.
     */
    static String esc(String s) {
        return s.replace("%", "%25").replace(",", "%2C").replace("|", "%7C")
                .replace(";", "%3B").replace("=", "%3D").replace("\n", "%0A");
    }

    static String unesc(String s) {
        return s.replace("%0A", "\n").replace("%3D", "=").replace("%3B", ";")
                .replace("%7C", "|").replace("%2C", ",").replace("%25", "%");
    }

    private static String num(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    @Override
    public String toString() {
        return name + " (" + kind + ")";
    }
}
