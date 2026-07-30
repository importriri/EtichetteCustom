package app.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Come si parla con la stampante: quale coda, quanto è grande la pagina, di
 * quanto va corretto il tiro e se mandare disegno vettoriale o immagine.
 *
 * <p>Esiste perché una stampante termica tarata male non dà errori: stampa, e
 * stampa male. Tutti i parametri che decidono se un'etichetta esce centrata o
 * a cavallo di due supporti stanno qui, in chiaro, salvati e ripetibili —
 * invece che nascosti nei default della coda di Windows.
 */
public final class PrintSetup {

    /** Da dove arriva la misura della pagina mandata al driver. */
    public enum PageMode {
        /**
         * La pagina è grande esattamente quanto l'etichetta. È il caso giusto
         * quando il driver ha già il formato del supporto configurato.
         */
        ETICHETTA("Come l'etichetta"),
        /**
         * La pagina è quella che dichiara il driver. Da usare quando il
         * formato in Windows è già tarato e non lo si vuole toccare.
         */
        STAMPANTE("Quella della stampante"),
        /**
         * Misura scritta a mano: serve quando il supporto ha un passo diverso
         * dall'area stampata — etichette con gap, o due etichette per passo.
         */
        PERSONALIZZATA("Misura personalizzata");

        private final String label;

        PageMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Di quanto esce ruotata l'etichetta rispetto a come è disegnata.
     *
     * <p>Serve perché il verso in cui il supporto entra nella stampante non ha
     * niente a che vedere con il verso in cui l'etichetta è stata composta. In
     * reparto il PDF si stampava da Chrome scegliendo a mano "Orientamento
     * orizzontale": questa è la stessa cosa, ma dentro il programma, salvata, e
     * uguale per la stampa diretta e per il PDF.
     *
     * <p>La geometria vive qui e in nessun altro posto: stampa vettoriale,
     * stampa a immagine, PDF e pagina di taratura chiedono tutte la stessa
     * {@link #transformMm}.
     */
    public enum Turn {
        GRADI_0("Dritta (0°)", 0),
        GRADI_90("Ruotata a destra (90°)", 90),
        GRADI_180("Capovolta (180°)", 180),
        GRADI_270("Ruotata a sinistra (270°)", 270);

        private final String label;
        private final int degrees;

        Turn(String label, int degrees) {
            this.label = label;
            this.degrees = degrees;
        }

        public int degrees() {
            return degrees;
        }

        /** {@code true} se ruotando si scambiano larghezza e altezza. */
        public boolean swapsSides() {
            return degrees == 90 || degrees == 270;
        }

        /**
         * La trasformazione, in millimetri, che ruota un'etichetta larga
         * {@code w} e alta {@code h} lasciandola appoggiata all'angolo in alto
         * a sinistra della pagina.
         *
         * <p>Ruotare e basta manderebbe il disegno fuori dal foglio: dopo la
         * rotazione il rettangolo va riportato sull'origine, ed è questa
         * traslazione — diversa per ogni quarto di giro — la parte che si
         * sbaglia a occhio e che i controlli fissano.
         */
        public java.awt.geom.AffineTransform transformMm(double w, double h) {
            java.awt.geom.AffineTransform t = new java.awt.geom.AffineTransform();
            switch (this) {
                case GRADI_90:
                    t.translate(h, 0);
                    break;
                case GRADI_180:
                    t.translate(w, h);
                    break;
                case GRADI_270:
                    t.translate(0, w);
                    break;
                default:
                    break;
            }
            if (degrees != 0) {
                t.rotate(Math.toRadians(degrees));
            }
            return t;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Che cosa riceve il driver. */
    public enum Render {
        /**
         * Immagine già rasterizzata alla risoluzione della stampante. È il
         * default perché i driver termici riscalano i tracciati vettoriali
         * ognuno a modo suo, mentre di un'immagine 1:1 non possono sbagliare
         * niente.
         */
        IMMAGINE("Immagine al DPI della stampante"),
        /** Tracciati vettoriali, come nel PDF. Più nitido dove il driver lo regge. */
        VETTORIALE("Vettoriale");

        private final String label;

        Render(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String printerName = "";
    private PageMode pageMode = PageMode.ETICHETTA;
    private double pageWidthMm = 50.0;
    private double pageHeightMm = 30.0;
    private double offsetXMm = 0.0;
    private double offsetYMm = 0.0;
    private double scalePercent = 100.0;
    private Render render = Render.IMMAGINE;
    private Turn turn = Turn.GRADI_0;
    private boolean showDialog = true;

    public static PrintSetup defaults() {
        return new PrintSetup();
    }

    public PrintSetup copy() {
        PrintSetup c = new PrintSetup();
        c.printerName = printerName;
        c.pageMode = pageMode;
        c.pageWidthMm = pageWidthMm;
        c.pageHeightMm = pageHeightMm;
        c.offsetXMm = offsetXMm;
        c.offsetYMm = offsetYMm;
        c.scalePercent = scalePercent;
        c.render = render;
        c.turn = turn;
        c.showDialog = showDialog;
        return c;
    }

    // --- proprietà ------------------------------------------------------------

    /** Il nome della coda scelta; vuoto = quella di sistema. */
    public String printerName() {
        return printerName;
    }

    public void setPrinterName(String name) {
        this.printerName = name == null ? "" : name.trim();
    }

    public PageMode pageMode() {
        return pageMode;
    }

    public void setPageMode(PageMode mode) {
        this.pageMode = mode == null ? PageMode.ETICHETTA : mode;
    }

    public double pageWidthMm() {
        return pageWidthMm;
    }

    public double pageHeightMm() {
        return pageHeightMm;
    }

    public void setPageSizeMm(double w, double h) {
        this.pageWidthMm = side(w, "larghezza della pagina");
        this.pageHeightMm = side(h, "altezza della pagina");
    }

    private static double side(double v, String what) {
        if (Double.isNaN(v) || v < 5 || v > 1000) {
            throw new IllegalArgumentException("La " + what
                    + " deve stare tra 5 e 1000 mm, ricevuto " + v + ".");
        }
        return v;
    }

    /** Correzione orizzontale in millimetri: positiva sposta a destra. */
    public double offsetXMm() {
        return offsetXMm;
    }

    /** Correzione verticale in millimetri: positiva sposta in basso. */
    public double offsetYMm() {
        return offsetYMm;
    }

    public void setOffsetMm(double x, double y) {
        this.offsetXMm = offset(x, "orizzontale");
        this.offsetYMm = offset(y, "verticale");
    }

    private static double offset(double v, String what) {
        if (Double.isNaN(v) || Math.abs(v) > 50) {
            throw new IllegalArgumentException("La correzione " + what
                    + " deve stare tra -50 e +50 mm, ricevuto " + v + ".");
        }
        return v;
    }

    /**
     * Correzione di scala in percentuale.
     *
     * <p>L'ultima risorsa quando il driver riscala per conto suo: si stampa la
     * pagina di taratura, si misura il lato del riquadro e si scrive qui il
     * rapporto. Cento vuol dire "non toccare niente", ed è il valore giusto
     * quasi sempre.
     */
    public double scalePercent() {
        return scalePercent;
    }

    public void setScalePercent(double percent) {
        if (Double.isNaN(percent) || percent < 50 || percent > 200) {
            throw new IllegalArgumentException(
                    "La scala di stampa deve stare tra 50 e 200%, ricevuto " + percent + ".");
        }
        this.scalePercent = percent;
    }

    /** Il fattore di scala pronto da moltiplicare. */
    public double scaleFactor() {
        return scalePercent / 100.0;
    }

    public Render render() {
        return render;
    }

    public void setRender(Render render) {
        this.render = render == null ? Render.IMMAGINE : render;
    }

    /** Di quanto esce ruotata l'etichetta rispetto a come è disegnata. */
    public Turn turn() {
        return turn;
    }

    public void setTurn(Turn turn) {
        this.turn = turn == null ? Turn.GRADI_0 : turn;
    }

    /** {@code true} se prima di stampare va aperta la finestra di scelta stampante. */
    public boolean showDialog() {
        return showDialog;
    }

    public void setShowDialog(boolean showDialog) {
        this.showDialog = showDialog;
    }

    // --- persistenza ----------------------------------------------------------

    public String toStorage() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("printer", LabelElement.esc(printerName));
        m.put("mode", pageMode.name());
        m.put("pw", num(pageWidthMm));
        m.put("ph", num(pageHeightMm));
        m.put("ox", num(offsetXMm));
        m.put("oy", num(offsetYMm));
        m.put("scale", num(scalePercent));
        m.put("render", render.name());
        m.put("turn", turn.name());
        m.put("dialog", showDialog ? "1" : "0");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /** Rilegge una taratura salvata; su dati rotti torna ai default. */
    public static PrintSetup fromStorage(String stored) {
        PrintSetup s = new PrintSetup();
        if (stored == null || stored.isEmpty()) {
            return s;
        }
        try {
            Map<String, String> map = new LinkedHashMap<String, String>();
            for (String part : stored.split(";")) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    map.put(part.substring(0, eq), part.substring(eq + 1));
                }
            }
            s.setPrinterName(LabelElement.unesc(get(map, "printer", "")));
            s.setPageMode(PageMode.valueOf(get(map, "mode", PageMode.ETICHETTA.name())));
            s.setPageSizeMm(dbl(map, "pw", 50), dbl(map, "ph", 30));
            s.setOffsetMm(dbl(map, "ox", 0), dbl(map, "oy", 0));
            s.setScalePercent(dbl(map, "scale", 100));
            s.setRender(Render.valueOf(get(map, "render", Render.IMMAGINE.name())));
            s.setTurn(Turn.valueOf(get(map, "turn", Turn.GRADI_0.name())));
            s.setShowDialog(!"0".equals(get(map, "dialog", "1")));
            return s;
        } catch (RuntimeException broken) {
            return new PrintSetup();
        }
    }

    private static String get(Map<String, String> m, String k, String fallback) {
        String v = m.get(k);
        return v == null ? fallback : v;
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

    @Override
    public String toString() {
        return String.format(Locale.ITALIAN,
                "%s, pagina %s, %s, correzione %+.1f/%+.1f mm, %s",
                printerName.isEmpty() ? "stampante di sistema" : printerName,
                pageMode, turn, offsetXMm, offsetYMm, render);
    }
}
