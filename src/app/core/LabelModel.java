package app.core;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Descrizione di un'etichetta: il supporto, la risoluzione, il livello di
 * correzione del QR e la lista degli elementi.
 *
 * <p>L'origine è l'angolo in alto a sinistra, la {@code y} cresce verso il
 * basso. Tutte le misure sono in millimetri: i pixel esistono solo al momento
 * di rasterizzare, e il DPI è un parametro a parte.
 *
 * <p>Gli elementi sono una lista, non tre campi fissi. Un'etichetta con due QR
 * e cinque righe di testo è lo stesso oggetto di una con un QR solo, e i layout
 * dei clienti restano fuori dal programma: si compongono qui dentro e si
 * salvano come layout dell'operatore, non come costanti nel codice.
 */
public final class LabelModel {

    /** Misure ammesse per il supporto, in millimetri. */
    public static final double MIN_SIDE_MM = 5.0;
    public static final double MAX_SIDE_MM = 300.0;

    /** Sotto questa dimensione di modulo la lettura diventa incerta. */
    public static final double DEFAULT_MODULE_WARN_MM = 0.30;

    private double widthMm = 50.0;
    private double heightMm = 30.0;
    private int dpi = 203; // nativo della Datamax E-Class
    private QrCode.Ecc ecc = QrCode.Ecc.MEDIUM;
    private double moduleWarnMm = DEFAULT_MODULE_WARN_MM;
    private double minQrSideMm = 0.0; // 0 = nessun minimo imposto dal capitolato
    private String templateName = Templates.NUOVA;

    private final List<LabelElement> elements = new ArrayList<LabelElement>();
    private final List<LabelField> fields = new ArrayList<LabelField>();

    private LabelModel() {
    }

    /** Un'etichetta di default già sensata: codice in alto, QR sotto, sigla accanto. */
    public static LabelModel defaults() {
        LabelModel m = new LabelModel();
        Templates.reset(m);
        return m;
    }

    /** Un modello vuoto: nessun elemento, misure di fabbrica. */
    public static LabelModel empty() {
        return new LabelModel();
    }

    public LabelModel copy() {
        LabelModel c = new LabelModel();
        c.copyFrom(this);
        return c;
    }

    /** Ricopia dentro di sé lo stato di un altro modello, riferimento invariato. */
    public void copyFrom(LabelModel other) {
        if (other == null) {
            throw new IllegalArgumentException("Modello nullo.");
        }
        this.widthMm = other.widthMm;
        this.heightMm = other.heightMm;
        this.dpi = other.dpi;
        this.ecc = other.ecc;
        this.moduleWarnMm = other.moduleWarnMm;
        this.minQrSideMm = other.minQrSideMm;
        this.templateName = other.templateName;
        this.elements.clear();
        for (LabelElement e : other.elements) {
            this.elements.add(e.copy());
        }
        this.fields.clear();
        for (LabelField f : other.fields) {
            this.fields.add(f.copy());
        }
    }

    // --- supporto -------------------------------------------------------------

    public double widthMm() {
        return widthMm;
    }

    public double heightMm() {
        return heightMm;
    }

    public void setSizeMm(double w, double h) {
        this.widthMm = clampSide(w, "larghezza");
        this.heightMm = clampSide(h, "altezza");
    }

    private static double clampSide(double v, String what) {
        if (Double.isNaN(v) || v < MIN_SIDE_MM || v > MAX_SIDE_MM) {
            throw new IllegalArgumentException("La " + what + " dell'etichetta deve stare tra "
                    + MIN_SIDE_MM + " e " + MAX_SIDE_MM + " mm.");
        }
        return v;
    }

    /**
     * Scambia larghezza e altezza del supporto.
     *
     * <p>Prima esistevano una combo "verso" e un bottone "ruota etichetta 90°"
     * che facevano ruotare anche il contenuto. Erano due comandi per una cosa
     * che l'operatore risolve cambiando due numeri, e ruotare il contenuto
     * gli spostava sotto i piedi tutto quello che aveva appena posizionato.
     * Adesso i lati si scambiano e basta: gli elementi restano dove sono, e se
     * qualcosa esce dal supporto l'avviso lo dice.
     */
    public void swapSides() {
        double w = widthMm;
        widthMm = heightMm;
        heightMm = w;
    }

    public int dpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        if (dpi < 72 || dpi > 1200) {
            throw new IllegalArgumentException("DPI fuori scala: " + dpi + ".");
        }
        this.dpi = dpi;
    }

    /** Quanti pixel vale un millimetro alla risoluzione impostata. */
    public double pxPerMm() {
        return dpi / 25.4;
    }

    public QrCode.Ecc ecc() {
        return ecc;
    }

    public void setEcc(QrCode.Ecc ecc) {
        if (ecc == null) {
            throw new IllegalArgumentException("Livello di correzione nullo.");
        }
        this.ecc = ecc;
    }

    public double moduleWarnMm() {
        return moduleWarnMm;
    }

    public void setModuleWarnMm(double mm) {
        if (Double.isNaN(mm) || mm < 0 || mm > 5) {
            throw new IllegalArgumentException("Soglia del modulo fuori scala: " + mm + " mm.");
        }
        this.moduleWarnMm = mm;
    }

    /** Lato minimo del QR imposto dal capitolato del cliente; 0 = nessuno. */
    public double minQrSideMm() {
        return minQrSideMm;
    }

    public void setMinQrSideMm(double mm) {
        if (Double.isNaN(mm) || mm < 0 || mm > MAX_SIDE_MM) {
            throw new IllegalArgumentException("Lato minimo del QR fuori scala: " + mm + " mm.");
        }
        this.minQrSideMm = mm;
    }

    /** Il nome del modello di partenza, solo per mostrarlo nell'interfaccia. */
    public String templateName() {
        return templateName;
    }

    public void setTemplateName(String name) {
        this.templateName = (name == null || name.trim().isEmpty())
                ? "Personalizzato" : name.trim();
    }

    // --- elementi -------------------------------------------------------------

    /** La lista viva degli elementi: si aggiunge, si toglie e si riordina qui. */
    public List<LabelElement> elements() {
        return elements;
    }

    public LabelElement add(LabelElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Elemento nullo.");
        }
        elements.add(element);
        return element;
    }

    public void remove(LabelElement element) {
        elements.remove(element);
    }

    /** Sposta un elemento avanti o indietro nell'ordine di disegno. */
    public void move(LabelElement element, int delta) {
        int from = elements.indexOf(element);
        if (from < 0) {
            return;
        }
        int to = Math.max(0, Math.min(elements.size() - 1, from + delta));
        elements.remove(from);
        elements.add(to, element);
    }

    /** Il primo elemento il cui contenuto dipende dal codice: quello che l'anteprima segue. */
    public LabelElement firstCodeElement() {
        for (LabelElement e : elements) {
            if (e.usesCode()) {
                return e;
            }
        }
        return elements.isEmpty() ? null : elements.get(0);
    }

    // --- campi ----------------------------------------------------------------

    /**
     * I campi dell'etichetta: i valori con un nome che gli elementi richiamano
     * scrivendo <code>{nome}</code>.
     *
     * <p>Un'etichetta con due progressivi che avanzano insieme, o con un lotto
     * chiesto a ogni stampa accanto a un seriale automatico, è la stessa classe
     * di una con un codice solo: cambia la lista, non il codice del programma.
     */
    public List<LabelField> fields() {
        return fields;
    }

    public LabelField addField(LabelField field) {
        if (field == null) {
            throw new IllegalArgumentException("Campo nullo.");
        }
        if (field(field.name()) != null) {
            throw new IllegalArgumentException("Esiste già un campo di nome \""
                    + field.name() + "\": i segnaposto andrebbero in conflitto.");
        }
        fields.add(field);
        return field;
    }

    public LabelField field(String name) {
        for (LabelField f : fields) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return null;
    }

    public void removeField(LabelField field) {
        fields.remove(field);
    }

    /** Il campo principale: il primo progressivo, o il primo campo che c'è. */
    public LabelField mainField() {
        for (LabelField f : fields) {
            if (f.type() == LabelField.Type.SEQUENZIALE) {
                return f;
            }
        }
        return fields.isEmpty() ? null : fields.get(0);
    }

    /** I valori di tutti i campi alla {@code index}-esima etichetta del giro. */
    public Map<String, String> valuesAt(long index) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (LabelField f : fields) {
            values.put(f.name(), f.valueAt(index));
        }
        return values;
    }

    /**
     * Ferma il giro prima di stampare se un progressivo non ci arriva.
     *
     * @throws IllegalArgumentException con il nome del campo che si esaurisce
     */
    public void checkRun(int count) {
        for (LabelField f : fields) {
            f.checkRun(count);
        }
    }

    /** Il giro completo: una mappa di valori per ogni etichetta da stampare. */
    public List<Map<String, String>> run(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("La quantità deve essere almeno 1.");
        }
        checkRun(count);
        List<Map<String, String>> out = new ArrayList<Map<String, String>>(count);
        for (int i = 0; i < count; i++) {
            out.add(valuesAt(i));
        }
        return out;
    }

    /** I segnaposto usati dagli elementi che non corrispondono a nessun campo. */
    public List<String> unknownTokens() {
        List<String> missing = new ArrayList<String>();
        for (LabelElement e : elements) {
            for (String token : e.tokens()) {
                if (field(token) == null && !missing.contains(token)) {
                    missing.add(token);
                }
            }
        }
        return missing;
    }

    // --- controlli ------------------------------------------------------------

    /**
     * Elenca in italiano i problemi del layout: elementi che escono dal
     * supporto, QR troppo piccolo per il lettore o sotto il minimo del
     * capitolato, contenuti impossibili da codificare. Lista vuota =
     * etichetta stampabile.
     */
    public List<String> warnings(String sampleCode) {
        List<String> out = new ArrayList<String>();
        if (elements.isEmpty()) {
            out.add("L'etichetta non ha nessun elemento: aggiungi almeno un testo o un QR.");
            return out;
        }
        for (LabelField f : fields) {
            if (f.type() == LabelField.Type.SEQUENZIALE) {
                try {
                    SerialWindow.of(f.value(), f.digits());
                } catch (RuntimeException notACounter) {
                    out.add("Campo {" + f.name() + "}: " + notACounter.getMessage());
                }
            }
        }
        for (String orphan : unknownTokens()) {
            out.add("Il segnaposto {" + orphan + "} non corrisponde a nessun campo: "
                    + "verrà stampato così com'è. Aggiungi il campo oppure correggi il nome.");
        }
        for (LabelElement e : elements) {
            String resolved = e.resolve(sampleCode);
            if (resolved.isEmpty()) {
                // un elemento vuoto non è un difetto: semplicemente non si disegna.
                // Nell'elenco compare come "(vuoto)", che è il posto giusto per dirlo
                // senza riempire di avvisi chi la sigla non la usa.
                continue;
            }
            Rectangle2D box;
            try {
                box = LabelLayout.boundsMm(e, resolved, this);
            } catch (RuntimeException impossible) {
                out.add("\"" + e.name() + "\": " + impossible.getMessage());
                continue;
            }
            if (box.getMinX() < -0.05 || box.getMinY() < -0.05
                    || box.getMaxX() > widthMm + 0.05 || box.getMaxY() > heightMm + 0.05) {
                out.add(String.format(Locale.ITALIAN,
                        "\"%s\" esce dall'etichetta: occupa da %.1f,%.1f a %.1f,%.1f mm "
                        + "su un supporto di %.1f x %.1f.",
                        e.name(), box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY(),
                        widthMm, heightMm));
            }
            if (e.kind() == LabelElement.Kind.QR) {
                if (minQrSideMm > 0 && e.sizeMm() + 0.05 < minQrSideMm) {
                    out.add(String.format(Locale.ITALIAN,
                            "\"%s\" ha il lato di %.1f mm: il capitolato ne chiede almeno %.1f.",
                            e.name(), e.sizeMm(), minQrSideMm));
                }
                try {
                    QrCode qr = QrCode.encode(resolved, ecc);
                    double module = e.sizeMm() / qr.size();
                    if (module < moduleWarnMm) {
                        out.add(String.format(Locale.ITALIAN,
                                "\"%s\": modulo a %.2f mm (versione %d, %d moduli). Sotto %.2f mm "
                                + "la lettura diventa incerta: allarga il QR o accorcia il codice.",
                                e.name(), module, qr.version, qr.size, moduleWarnMm));
                    }
                    double printedModulePx = module * pxPerMm();
                    if (printedModulePx < 2.0) {
                        out.add(String.format(Locale.ITALIAN,
                                "\"%s\": a %d dpi ogni modulo è %.1f punti di stampa. Sotto 2 "
                                + "punti la stampante arrotonda e il QR esce sporco.",
                                e.name(), dpi, printedModulePx));
                    }
                } catch (RuntimeException tooLong) {
                    out.add("\"" + e.name() + "\": " + tooLong.getMessage());
                }
            }
        }
        return out;
    }

    // --- persistenza ----------------------------------------------------------

    private static final int STORAGE_VERSION = 2;

    /** Serializza il layout in una riga sola, da mettere nelle impostazioni. */
    public String toStorage() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("v", String.valueOf(STORAGE_VERSION));
        m.put("w", num(widthMm));
        m.put("h", num(heightMm));
        m.put("dpi", String.valueOf(dpi));
        m.put("ecc", ecc.name());
        m.put("warn", num(moduleWarnMm));
        m.put("minqr", num(minQrSideMm));
        m.put("tpl", LabelElement.esc(templateName));
        StringBuilder els = new StringBuilder();
        for (LabelElement e : elements) {
            if (els.length() > 0) {
                els.append('|');
            }
            els.append(e.toStorage());
        }
        m.put("el", els.toString());
        StringBuilder fs = new StringBuilder();
        for (LabelField f : fields) {
            if (fs.length() > 0) {
                fs.append('|');
            }
            fs.append(f.toStorage());
        }
        m.put("fld", fs.toString());

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /** Rilegge un layout serializzato; su dati rotti torna ai default senza lamentarsi. */
    public static LabelModel fromStorage(String stored) {
        if (stored == null || stored.isEmpty()) {
            return defaults();
        }
        try {
            Map<String, String> map = new LinkedHashMap<String, String>();
            for (String part : stored.split(";")) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    map.put(part.substring(0, eq), part.substring(eq + 1));
                }
            }
            if (!map.containsKey("v") || !map.containsKey("el")) {
                return fromLegacyStorage(map);
            }
            LabelModel m = new LabelModel();
            m.setSizeMm(dbl(map, "w", 50), dbl(map, "h", 30));
            m.setDpi((int) dbl(map, "dpi", 203));
            m.setEcc(QrCode.Ecc.valueOf(map.containsKey("ecc") ? map.get("ecc") : "MEDIUM"));
            m.setModuleWarnMm(dbl(map, "warn", DEFAULT_MODULE_WARN_MM));
            m.setMinQrSideMm(dbl(map, "minqr", 0));
            m.setTemplateName(LabelElement.unesc(
                    map.containsKey("tpl") ? map.get("tpl") : Templates.NUOVA));
            for (String piece : map.get("el").split("\\|")) {
                LabelElement e = LabelElement.fromStorage(piece);
                if (e != null) {
                    m.elements.add(e);
                }
            }
            if (map.containsKey("fld")) {
                for (String piece : map.get("fld").split("\\|")) {
                    LabelField f = LabelField.fromStorage(piece);
                    if (f != null && m.field(f.name()) == null) {
                        m.fields.add(f);
                    }
                }
            }
            if (m.fields.isEmpty()) {
                // layout salvato prima dei campi: si ricostruisce quello unico di allora
                m.fields.add(LabelField.sequential(LabelField.DEFAULT_NAME, "", 3));
            }
            return m.elements.isEmpty() ? defaults() : m;
        } catch (RuntimeException broken) {
            return defaults();
        }
    }

    /**
     * Rilegge il formato della prima versione, quello a tre elementi fissi.
     *
     * <p>Chi ha già usato l'app in reparto ha un layout salvato in quel formato:
     * al primo avvio della versione nuova se lo ritrova convertito, non azzerato.
     */
    private static LabelModel fromLegacyStorage(Map<String, String> map) {
        LabelModel m = new LabelModel();
        m.setSizeMm(dbl(map, "w", 50), dbl(map, "h", 30));
        m.setDpi((int) dbl(map, "dpi", 203));
        m.setEcc(QrCode.Ecc.valueOf(map.containsKey("ecc") ? map.get("ecc") : "MEDIUM"));
        m.setTemplateName(Templates.NUOVA);

        m.fields.add(LabelField.sequential(LabelField.DEFAULT_NAME, "", 3));
        LabelElement code = LabelElement.text("Codice", LabelElement.CODE_TOKEN,
                dbl(map, "cx", 3), dbl(map, "cy", 6), dbl(map, "ch", 4));
        code.setBold(!"0".equals(map.get("cb")));
        m.add(code);
        m.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN,
                dbl(map, "qx", 3), dbl(map, "qy", 8.5), dbl(map, "qs", 18)));
        String sigla = map.containsKey("st") ? map.get("st") : "";
        if (!sigla.isEmpty()) {
            LabelElement tag = LabelElement.text("Sigla", sigla,
                    dbl(map, "sx", 24), dbl(map, "sy", 18), dbl(map, "sh", 5));
            tag.setBold(!"0".equals(map.get("sb")));
            m.add(tag);
        }
        return m;
    }

    private static double dbl(Map<String, String> m, String k, double fallback) {
        try {
            return m.containsKey(k) ? Double.parseDouble(m.get(k)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String num(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
