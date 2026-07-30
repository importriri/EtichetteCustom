package app.core.export;

import app.core.LabelLayout;
import app.core.LabelModel;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;

/** Scrive l'etichetta in SVG. Un solo tracciato, coordinate in millimetri. */
public final class SvgExporter implements LabelCanvas {

    private final StringBuilder path = new StringBuilder();

    private SvgExporter() {
    }

    /** Genera l'SVG dell'etichetta per il codice indicato. */
    public static String toSvg(LabelModel model, String code) {
        return toSvg(model, PngExporter.values(code));
    }

    /** Come sopra, ma con tutti i campi dell'etichetta. */
    public static String toSvg(LabelModel model, java.util.Map<String, String> fields) {
        SvgExporter canvas = new SvgExporter();
        LabelLayout.render(model, fields, canvas);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%smm\" height=\"%smm\" "
                + "viewBox=\"0 0 %s %s\">\n",
                fmt(model.widthMm()), fmt(model.heightMm()),
                fmt(model.widthMm()), fmt(model.heightMm())));
        sb.append(String.format(Locale.ROOT,
                "  <rect width=\"%s\" height=\"%s\" fill=\"#ffffff\"/>\n",
                fmt(model.widthMm()), fmt(model.heightMm())));
        sb.append("  <path fill=\"#000000\" fill-rule=\"nonzero\" d=\"")
          .append(canvas.path.toString().trim())
          .append("\"/>\n");
        sb.append("</svg>\n");
        return sb.toString();
    }

    /** Scrive l'SVG su file, in UTF-8. */
    public static void write(LabelModel model, String code, File target) throws IOException {
        write(model, PngExporter.values(code), target);
    }

    public static void write(LabelModel model, java.util.Map<String, String> fields, File target)
            throws IOException {
        Writer w = null;
        try {
            w = new OutputStreamWriter(new FileOutputStream(target), "UTF-8");
            w.write(toSvg(model, fields));
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }

    @Override
    public void fill(Shape shapeMm) {
        double[] c = new double[6];
        PathIterator it = shapeMm.getPathIterator(null);
        while (!it.isDone()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO:
                    path.append("M").append(fmt(c[0])).append(" ").append(fmt(c[1])).append(" ");
                    break;
                case PathIterator.SEG_LINETO:
                    path.append("L").append(fmt(c[0])).append(" ").append(fmt(c[1])).append(" ");
                    break;
                case PathIterator.SEG_QUADTO:
                    path.append("Q").append(fmt(c[0])).append(" ").append(fmt(c[1])).append(" ")
                        .append(fmt(c[2])).append(" ").append(fmt(c[3])).append(" ");
                    break;
                case PathIterator.SEG_CUBICTO:
                    path.append("C").append(fmt(c[0])).append(" ").append(fmt(c[1])).append(" ")
                        .append(fmt(c[2])).append(" ").append(fmt(c[3])).append(" ")
                        .append(fmt(c[4])).append(" ").append(fmt(c[5])).append(" ");
                    break;
                default:
                    path.append("Z ");
                    break;
            }
            it.next();
        }
    }

    private static String fmt(double v) {
        String s = String.format(Locale.ROOT, "%.4f", v);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s.isEmpty() ? "0" : s;
    }
}
