package app.core;

import app.core.export.LabelCanvas;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Suite per {@link LabelLayout}: la geometria di rotazione e allineamento.
 * Runner a mano, niente JUnit, exit 1 se fallisce.
 *
 * <p>Qui si controlla il pezzo che prima non esisteva. Ruotare un elemento è
 * facile da far sembrare giusto a schermo e facilissimo da sbagliare di un
 * quarto di giro o di un'ancora: questi controlli fissano dove finisce ogni
 * angolo, in millimetri, per tutti e quattro i versi.
 */
public final class LayoutTest {

    private static int passed = 0;
    private static int failed = 0;

    /** Tolleranza in millimetri: i profili dei caratteri non sono numeri tondi. */
    private static final double EPS = 0.05;

    public static void main(String[] args) {
        text_unrotated_sitsOnItsBaseline();
        text_rotation_movesEveryCornerWhereExpected();
        qr_rotation_keepsTheSquareOnItsAnchor();
        align_shiftsTheContentAroundTheAnchor();
        qr_modules_landOnTheirNominalCentres();
        textWidth_scalesWithHeight();
        render_drawsOneShapePerNonEmptyElement();
        render_substitutesTheCodeEverywhere();

        System.out.println("Layout: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void text_unrotated_sitsOnItsBaseline() {
        LabelModel m = LabelModel.empty();
        LabelElement e = m.add(LabelElement.text("T", "000", 10, 20, 4));
        Rectangle2D b = LabelLayout.boundsMm(e, "000", m);
        // il riquadro è quello dell'inchiostro, non della cassa tipografica: il
        // glifo comincia una spalla dopo l'ancora e le curve dello zero sbordano
        // di un pelo sotto la linea di base. Sono decimi di millimetro, ma vanno
        // messi in conto qui invece di allargare la tolleranza ovunque
        between("il testo comincia subito dopo l'ancora", 10.0, 10.5, b.getMinX());
        between("la linea di base è l'ancora, a meno dello sbordo delle curve",
                19.9, 20.2, b.getMaxY());
        between("l'altezza è quella chiesta", 3.95, 4.25, b.getHeight());
        yes("e la larghezza è positiva", b.getWidth() > 4);
        near("la larghezza dichiarata comprende le spalle",
                LabelLayout.textWidthMm("000", false, 4),
                b.getWidth() + 2 * (b.getMinX() - 10.0));
    }

    private static void text_rotation_movesEveryCornerWhereExpected() {
        LabelModel m = LabelModel.empty();
        LabelElement e = m.add(LabelElement.text("T", "000", 10, 20, 4));
        Shape flat = LabelLayout.shapeOf(e, "000", m);

        // la verifica giusta non è "il riquadro finisce a questo millimetro" —
        // dipenderebbe dal font — ma "ruotare l'elemento equivale a ruotare il
        // disegno attorno alla sua ancora". Quella è esatta, e vale per ogni font
        for (double deg : new double[] {90, 180, 270, 45, 360}) {
            e.setRotationDeg(deg);
            Shape got = LabelLayout.shapeOf(e, "000", m);
            Shape expected = AffineTransform
                    .getRotateInstance(Math.toRadians(deg), 10, 20)
                    .createTransformedShape(flat);
            yes(String.format("%.0f°: identico al disegno ruotato attorno all'ancora", deg),
                    samePath(expected, got));
        }

        // e i quattro versi cardinali finiscono dove se li aspetta l'operatore
        e.setRotationDeg(0);
        Rectangle2D r0 = LabelLayout.boundsMm(e, "000", m);
        e.setRotationDeg(90);
        Rectangle2D r90 = LabelLayout.boundsMm(e, "000", m);
        yes("90°: il testo scende invece di andare a destra",
                r90.getHeight() > r90.getWidth() && r90.getMinY() >= 19.8);
        near("90°: largo quanto era alto", r0.getHeight(), r90.getWidth());
        near("90°: alto quanto era largo", r0.getWidth(), r90.getHeight());

        e.setRotationDeg(180);
        Rectangle2D r180 = LabelLayout.boundsMm(e, "000", m);
        yes("180°: il testo va a sinistra dell'ancora e sotto la linea di base",
                r180.getMaxX() <= 10.2 && r180.getMinY() >= 19.8);

        e.setRotationDeg(270);
        Rectangle2D r270 = LabelLayout.boundsMm(e, "000", m);
        yes("270°: il testo sale dall'ancora, alla sua sinistra",
                r270.getMaxY() <= 20.2 && r270.getMaxX() <= 10.2);
    }

    private static void qr_rotation_keepsTheSquareOnItsAnchor() {
        LabelModel m = LabelModel.empty();
        LabelElement e = m.add(LabelElement.qr("QR", "X1", 8, 6, 12));
        Rectangle2D flat = LabelLayout.boundsMm(e, "X1", m);
        near("il QR occupa esattamente il suo lato in larghezza", 12.0, flat.getWidth());
        near("e in altezza", 12.0, flat.getHeight());
        near("con l'angolo sull'ancora", 8.0, flat.getMinX());
        near("idem in verticale", 6.0, flat.getMinY());

        e.setRotationDeg(90);
        Rectangle2D r90 = LabelLayout.boundsMm(e, "X1", m);
        near("90°: resta quadrato", 12.0, r90.getWidth());
        near("90°: e scivola alla sinistra dell'ancora", 8.0 - 12.0, r90.getMinX());
        near("90°: restando appeso all'ancora in alto", 6.0, r90.getMinY());
    }

    private static void align_shiftsTheContentAroundTheAnchor() {
        LabelModel m = LabelModel.empty();
        LabelElement e = m.add(LabelElement.text("T", "000", 25, 20, 4));
        double w = LabelLayout.boundsMm(e, "000", m).getWidth();

        Rectangle2D left = LabelLayout.boundsMm(e, "000", m);
        double advance = LabelLayout.textWidthMm("000", false, 4);

        e.setAlign(LabelElement.Align.CENTRO);
        Rectangle2D centred = LabelLayout.boundsMm(e, "000", m);
        near("centrato: si sposta indietro di mezza larghezza",
                left.getMinX() - advance / 2, centred.getMinX());

        e.setAlign(LabelElement.Align.DESTRA);
        Rectangle2D right = LabelLayout.boundsMm(e, "000", m);
        near("a destra: si sposta indietro di una larghezza intera",
                left.getMinX() - advance, right.getMinX());
        yes("a destra: il testo sta tutto prima dell'ancora", right.getMaxX() <= 25.05);
        yes("la larghezza non cambia con l'allineamento",
                Math.abs(right.getWidth() - w) < 1e-9);
    }

    private static void qr_modules_landOnTheirNominalCentres() {
        LabelModel m = LabelModel.empty();
        String code = "TST-0000-00-001";
        LabelElement e = m.add(LabelElement.qr("QR", code, 5, 4, 21));
        Shape shape = LabelLayout.shapeOf(e, code, m);
        QrCode qr = QrCode.encode(code, m.ecc());
        double module = 21.0 / qr.size();
        int wrong = 0;
        for (int y = 0; y < qr.size(); y++) {
            for (int x = 0; x < qr.size(); x++) {
                double cx = 5 + (x + 0.5) * module;
                double cy = 4 + (y + 0.5) * module;
                if (shape.contains(cx, cy) != qr.module(x, y)) {
                    wrong++;
                }
            }
        }
        yes("i " + (qr.size() * qr.size()) + " moduli cadono al loro posto ("
                + wrong + " sbagliati)", wrong == 0);
    }

    private static void textWidth_scalesWithHeight() {
        double small = LabelLayout.textWidthMm("TST-0000-00-001", false, 2);
        double big = LabelLayout.textWidthMm("TST-0000-00-001", false, 4);
        near("raddoppiando l'altezza raddoppia la larghezza", small * 2, big);
        near("testo vuoto, larghezza zero", 0.0, LabelLayout.textWidthMm("", false, 4));
        Shape boldShape = LabelLayout.textShape("000", true, 4);
        Rectangle2D boldBounds = boldShape.getBounds2D();
        near("il grassetto conserva l'altezza richiesta", 4.0, boldBounds.getHeight());
        yes("il grassetto produce un contorno non vuoto", !boldBounds.isEmpty());
    }

    private static void render_drawsOneShapePerNonEmptyElement() {
        LabelModel m = LabelModel.empty();
        m.add(LabelElement.text("Uno", "AAA", 2, 5, 3));
        m.add(LabelElement.text("Vuoto", "", 2, 10, 3));
        m.add(LabelElement.qr("QR", LabelElement.CODE_TOKEN, 2, 12, 10));
        Recorder canvas = new Recorder();
        LabelLayout.render(m, "X1", canvas);
        same("gli elementi vuoti non arrivano al foglio", "2",
                String.valueOf(canvas.shapes.size()));
    }

    private static void render_substitutesTheCodeEverywhere() {
        LabelModel m = LabelModel.empty();
        m.add(LabelElement.qr("QR", "P_" + LabelElement.CODE_TOKEN, 2, 2, 15));
        Recorder canvas = new Recorder();
        LabelLayout.render(m, "X1", canvas);

        LabelModel reference = LabelModel.empty();
        reference.add(LabelElement.qr("QR", "P_X1", 2, 2, 15));
        Recorder expected = new Recorder();
        LabelLayout.render(reference, "ignorato", expected);

        near("il QR col segnaposto è identico a quello scritto per esteso",
                expected.shapes.get(0).getBounds2D().getWidth(),
                canvas.shapes.get(0).getBounds2D().getWidth());
        yes("e occupa la stessa area",
                samePath(expected.shapes.get(0), canvas.shapes.get(0)));
    }

    // --- helper ---------------------------------------------------------------

    /** Una superficie che invece di disegnare tiene da parte le forme. */
    private static final class Recorder implements LabelCanvas {
        private final List<Shape> shapes = new ArrayList<Shape>();

        public void fill(Shape shapeMm) {
            shapes.add(shapeMm);
        }
    }

    /**
     * Due forme coincidono punto per punto.
     *
     * <p>Il confronto passa dai tracciati appiattiti e non dalle operazioni
     * booleane su {@code Area}: sulle curve ruotate quelle lasciano schegge di
     * area grandi 1e-15 e farebbero fallire un controllo che invece è giusto.
     */
    private static boolean samePath(Shape a, Shape b) {
        PathIterator ia = a.getPathIterator(null, 0.005);
        PathIterator ib = b.getPathIterator(null, 0.005);
        double[] ca = new double[6];
        double[] cb = new double[6];
        while (!ia.isDone() && !ib.isDone()) {
            int sa = ia.currentSegment(ca);
            int sb = ib.currentSegment(cb);
            if (sa != sb) {
                return false;
            }
            for (int i = 0; i < 6; i++) {
                if (Math.abs(ca[i] - cb[i]) > 1e-6) {
                    return false;
                }
            }
            ia.next();
            ib.next();
        }
        return ia.isDone() && ib.isDone();
    }

    private static void between(String what, double low, double high, double actual) {
        if (actual >= low && actual <= high) {
            passed++;
            System.out.println(String.format("  ok  %s -> %.3f", what, actual));
        } else {
            failed++;
            System.out.println(String.format("FAIL  %s: atteso tra %.3f e %.3f, ottenuto %.3f",
                    what, low, high, actual));
        }
    }

    private static void near(String what, double expected, double actual) {
        if (Math.abs(expected - actual) <= EPS) {
            passed++;
            System.out.println(String.format("  ok  %s -> %.3f", what, actual));
        } else {
            failed++;
            System.out.println(String.format("FAIL  %s: atteso %.3f, ottenuto %.3f",
                    what, expected, actual));
        }
    }

    private static void same(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("  ok  " + what + " -> \"" + actual + "\"");
        } else {
            failed++;
            System.out.println("FAIL  " + what + ": atteso \"" + expected
                    + "\", ottenuto \"" + actual + "\"");
        }
    }

    private static void yes(String what, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ok  " + what);
        } else {
            failed++;
            System.out.println("FAIL  " + what);
        }
    }
}
