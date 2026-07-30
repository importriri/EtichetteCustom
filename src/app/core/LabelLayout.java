package app.core;

import app.core.export.LabelCanvas;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

/**
 * Disegna un'etichetta su una {@link LabelCanvas}.
 *
 * <p>Questa classe è l'unico posto in cui esiste il layout: anteprima, PNG,
 * PDF, SVG e stampa passano tutte di qui. Se l'anteprima mostra una cosa e il
 * PDF ne stampa un'altra, il bug è in un backend, non in due layout diversi.
 *
 * <p>Anche la geometria è una sola: {@link #shapeOf} produce la forma di un
 * elemento e {@link #boundsMm} il rettangolo che la contiene. L'anteprima ci
 * fa il riquadro di selezione e il trascinamento, il modello ci calcola gli
 * avvisi di "esce dall'etichetta". Nessuno dei due rifà i conti per conto suo.
 *
 * <p>I testi vengono convertiti in profili vettoriali invece che scritti come
 * testo: il PDF e l'SVG non hanno bisogno di font incorporati e vengono
 * identici all'anteprima su qualsiasi PC, anche senza il font installato.
 */
public final class LabelLayout {

    /** Contesto di misura valido anche senza schermo (l'app deve girare pure headless nei test). */
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 100);
    private static final Font BASE_BOLD = BASE.deriveFont(Font.BOLD);

    private LabelLayout() {
    }

    /**
     * Disegna il contenuto dell'etichetta per il codice indicato.
     *
     * @param model il layout
     * @param code  il codice già serializzato: sostituisce {@code {codice}}
     *              dentro il contenuto di ogni elemento
     */
    public static void render(LabelModel model, String code, LabelCanvas canvas) {
        if (model == null || canvas == null) {
            throw new IllegalArgumentException("Modello o superficie nulli.");
        }
        for (LabelElement e : model.elements()) {
            String resolved = e.resolve(code);
            if (resolved.isEmpty()) {
                continue;
            }
            canvas.fill(shapeOf(e, resolved, model));
        }
    }

    /** Come {@link #render(LabelModel, String, LabelCanvas)}, ma con tutti i campi. */
    public static void render(LabelModel model, java.util.Map<String, String> values,
                              LabelCanvas canvas) {
        if (model == null || canvas == null) {
            throw new IllegalArgumentException("Modello o superficie nulli.");
        }
        for (LabelElement e : model.elements()) {
            String resolved = e.resolve(values);
            if (resolved.isEmpty()) {
                continue;
            }
            canvas.fill(shapeOf(e, resolved, model));
        }
    }

    /**
     * La forma di un elemento, in millimetri, rotazione compresa.
     *
     * @param resolved il contenuto con il codice già sostituito
     */
    public static Shape shapeOf(LabelElement element, String resolved, LabelModel model) {
        Shape raw = element.kind() == LabelElement.Kind.QR
                ? qrShape(element, resolved, model)
                : paragraphShape(element, resolved);
        return transformOf(element, resolved, model).createTransformedShape(raw);
    }

    /** Il rettangolo che contiene l'elemento disegnato, rotazione compresa. */
    public static Rectangle2D boundsMm(LabelElement element, String resolved, LabelModel model) {
        return shapeOf(element, resolved, model).getBounds2D();
    }

    /**
     * La trasformazione che porta la forma "cruda" al suo posto sull'etichetta:
     * prima l'allineamento, poi la rotazione attorno all'ancora, poi la
     * traslazione all'ancora stessa.
     *
     * <p>Ruotare attorno all'ancora e non al centro è una scelta: l'operatore
     * ha appena posizionato quel punto, girare l'elemento non deve spostarglielo.
     */
    public static AffineTransform transformOf(LabelElement element, String resolved,
                                              LabelModel model) {
        AffineTransform t = new AffineTransform();
        t.translate(element.xMm(), element.yMm());
        if (element.rotationDeg() != 0) {
            t.rotate(Math.toRadians(element.rotationDeg()));
        }
        double shift = alignShiftMm(element, resolved, model);
        if (shift != 0) {
            t.translate(shift, 0);
        }
        return t;
    }

    /** Di quanto va spostato il contenuto sull'asse x per rispettare l'allineamento. */
    private static double alignShiftMm(LabelElement element, String resolved, LabelModel model) {
        if (element.align() == LabelElement.Align.SINISTRA) {
            return 0;
        }
        double width = element.kind() == LabelElement.Kind.QR
                ? element.sizeMm()
                : paragraphWidthMm(element, resolved);
        return element.align() == LabelElement.Align.CENTRO ? -width / 2.0 : -width;
    }

    /**
     * Il QR come insieme di rettangoli, con origine in {@code (0, 0)}.
     *
     * <p>I moduli contigui sulla stessa riga diventano un rettangolo solo: meno
     * forme nel PDF e nell'SVG, e nessuna riga chiara fra un modulo e l'altro
     * quando il rasterizzatore arrotonda i bordi.
     */
    private static Shape qrShape(LabelElement element, String resolved, LabelModel model) {
        QrCode qr = QrCode.encode(resolved, model.ecc());
        double module = element.sizeMm() / qr.size();
        Area area = new Area();
        for (int y = 0; y < qr.size(); y++) {
            int run = 0;
            for (int x = 0; x <= qr.size(); x++) {
                boolean dark = x < qr.size() && qr.module(x, y);
                if (dark) {
                    run++;
                } else if (run > 0) {
                    area.add(new Area(new Rectangle2D.Double(
                            (x - run) * module, y * module, run * module, module)));
                    run = 0;
                }
            }
        }
        return area;
    }

    /**
     * Spezza il testo in righe che stanno dentro la larghezza dell'elemento.
     *
     * <p>Il taglio avviene sugli spazi, sui trattini e sui trattini bassi,
     * tenendo il separatore in fondo alla riga che chiude: un codice come
     * {@code DEMO-4410.07_A2-01_000001} si spezza dopo un underscore, dove
     * l'occhio se lo aspetta, e non a metà di un gruppo di cifre.
     *
     * <p>Una parola più larga del limite non viene tagliata a forza: sfora, e
     * l'avviso "esce dall'etichetta" lo dice. Meglio un difetto visibile in
     * anteprima che un codice troncato che nessuno nota.
     */
    public static java.util.List<String> wrapLines(LabelElement element, String text) {
        java.util.List<String> lines = new java.util.ArrayList<String>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        // gli a capo scritti a mano valgono sempre, anche senza larghezza
        String[] hard = text.split("\n", -1);
        if (!element.wraps()) {
            for (String piece : hard) {
                lines.add(piece);
            }
            return lines;
        }
        double limit = element.wrapWidthMm();
        for (String paragraph : hard) {
            java.util.List<String> pieces = breakPoints(paragraph);
            StringBuilder current = new StringBuilder();
            for (String piece : pieces) {
                String candidate = current + piece;
                if (current.length() > 0
                        && textWidthMm(candidate, element.bold(), element.sizeMm()) > limit) {
                    lines.add(current.toString());
                    current = new StringBuilder(piece.startsWith(" ")
                            ? piece.substring(1) : piece);
                } else {
                    current.append(piece);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    /** Divide il testo nei pezzi più piccoli su cui è lecito andare a capo. */
    private static java.util.List<String> breakPoints(String text) {
        java.util.List<String> pieces = new java.util.ArrayList<String>();
        StringBuilder piece = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            piece.append(c);
            boolean breakable = c == ' ' || c == '-' || c == '_';
            if (breakable) {
                pieces.add(piece.toString());
                piece.setLength(0);
            }
        }
        if (piece.length() > 0) {
            pieces.add(piece.toString());
        }
        return pieces;
    }

    /** Il profilo di un testo su una o più righe, con la prima linea di base sull'origine. */
    private static Shape paragraphShape(LabelElement element, String text) {
        java.util.List<String> lines = wrapLines(element, text);
        if (lines.size() == 1) {
            return textShape(lines.get(0), element.bold(), element.sizeMm());
        }
        java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
        double step = element.sizeMm() * element.lineSpacing();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                continue;
            }
            Shape glyphs = textShape(line, element.bold(), element.sizeMm());
            double dx = 0;
            if (element.align() != LabelElement.Align.SINISTRA) {
                // ogni riga si allinea dentro il blocco, non solo il blocco intero
                double block = paragraphWidthMm(element, text);
                double own = textWidthMm(line, element.bold(), element.sizeMm());
                dx = element.align() == LabelElement.Align.CENTRO
                        ? (block - own) / 2.0 : block - own;
            }
            path.append(AffineTransform.getTranslateInstance(dx, i * step)
                    .createTransformedShape(glyphs), false);
        }
        return path;
    }

    /** La larghezza del blocco di testo: quella della riga più lunga. */
    public static double paragraphWidthMm(LabelElement element, String text) {
        double widest = 0;
        for (String line : wrapLines(element, text)) {
            widest = Math.max(widest, textWidthMm(line, element.bold(), element.sizeMm()));
        }
        return widest;
    }

    /** Quante righe occuperà il testo con la larghezza impostata. */
    public static int lineCount(LabelElement element, String text) {
        return Math.max(1, wrapLines(element, text).size());
    }

    /**
     * Il profilo vettoriale di un testo, alto {@code heightMm} misurato sulle
     * maiuscole, con la linea di base sull'origine.
     */
    public static Shape textShape(String text, boolean bold, double heightMm) {
        Font font = bold ? BASE_BOLD : BASE;
        double scale = heightMm / capHeight(font);
        GlyphVector gv = font.createGlyphVector(FRC, text);
        return AffineTransform.getScaleInstance(scale, scale)
                .createTransformedShape(gv.getOutline());
    }

    /**
     * Larghezza in millimetri che occuperà quel testo a quell'altezza. Serve
     * all'anteprima e all'allineamento.
     */
    public static double textWidthMm(String text, boolean bold, double heightMm) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Font font = bold ? BASE_BOLD : BASE;
        GlyphVector gv = font.createGlyphVector(FRC, text);
        return gv.getLogicalBounds().getWidth() * (heightMm / capHeight(font));
    }

    /** L'altezza delle maiuscole del font, misurata sul carattere "0". */
    private static double capHeight(Font font) {
        Rectangle2D b = font.createGlyphVector(FRC, "0").getOutline().getBounds2D();
        return b.getHeight();
    }
}
