package app.esporta;

import app.codice.Code128;
import app.codice.Qr;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.render.Disegno;
import app.render.Ingombri;
import app.render.Testo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;

/** Export SVG con le stesse misure e rotazioni del renderer. */
public final class Svg {
    private Svg() { }

    public static String testo(Etichetta eti, int copia) {
        BufferedImage finta = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = finta.createGraphics();
        try { return costruisci(eti, copia, g); }
        finally { g.dispose(); }
    }

    public static void scrivi(File dove, Etichetta eti, int copia) throws IOException {
        FileOutputStream flusso = new FileOutputStream(dove);
        try {
            Writer w = new OutputStreamWriter(flusso, "UTF-8");
            w.write(testo(eti, copia));
            w.flush();
        } finally { flusso.close(); }
    }

    private static String costruisci(Etichetta eti, int copia, Graphics2D g) {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\"\n")
                .append("     width=\"").append(n(eti.larghezza())).append("mm\"")
                .append(" height=\"").append(n(eti.altezza())).append("mm\"\n")
                .append("     viewBox=\"0 0 ").append(n(eti.larghezza())).append(' ')
                .append(n(eti.altezza())).append("\">\n");
        b.append("  <rect width=\"").append(n(eti.larghezza())).append("\" height=\"")
                .append(n(eti.altezza())).append("\" fill=\"#ffffff\"/>\n");
        b.append("  <g fill=\"#11111b\" shape-rendering=\"crispEdges\">\n");
        for (Elemento e : eti.elementi()) {
            Rectangle2D.Double base = Ingombri.base(g, eti, e, 12, copia);
            b.append("    <g transform=\"").append(trasformazione(e, base)).append("\">\n");
            switch (e.tipo()) {
                case QR: qr(b, eti.contenuto(e, copia), e); break;
                case BARCODE: barcode(b, eti.contenuto(e, copia), e); break;
                case LINEA:
                    b.append("      <rect x=\"0\" y=\"0\" width=\"")
                            .append(n(e.larghezza())).append("\" height=\"")
                            .append(n(e.altezza())).append("\"/>\n");
                    break;
                default: scritta(b, eti, e, copia, g); break;
            }
            b.append("    </g>\n");
        }
        b.append("  </g>\n</svg>\n");
        return b.toString();
    }

    private static String trasformazione(Elemento e, Rectangle2D.Double r) {
        StringBuilder t = new StringBuilder("translate(").append(n(e.x())).append(' ')
                .append(n(e.y())).append(')');
        switch (e.rotazione()) {
            case 90:  t.append(" translate(").append(n(r.height)).append(" 0) rotate(90)"); break;
            case 180: t.append(" translate(").append(n(r.width)).append(' ').append(n(r.height)).append(") rotate(180)"); break;
            case 270: t.append(" translate(0 ").append(n(r.width)).append(") rotate(270)"); break;
            default: break;
        }
        return t.toString();
    }

    private static void scritta(StringBuilder b, Etichetta eti, Elemento e, int copia, Graphics2D g) {
        double mmPx = 12;
        Testo.Esito esito = Testo.componi(eti.contenuto(e, copia), e.larghezza(), e.corpo(),
                e.massimoRighe(), e.grassetto(), Disegno.misuratore(g, mmPx));
        Font f = Disegno.font(esito.corpo() * mmPx, e.grassetto());
        FontMetrics fm = g.getFontMetrics(f);
        double salita = fm.getAscent() / mmPx;
        double interlinea = (fm.getAscent() + fm.getDescent()) / mmPx;
        b.append("      <text x=\"0\" y=\"").append(n(salita)).append("\"")
                .append(" font-family=\"").append(fuga(Disegno.famiglia())).append(", sans-serif\"")
                .append(" font-size=\"").append(n(esito.corpo())).append('"');
        if (e.grassetto()) b.append(" font-weight=\"bold\"");
        b.append(">\n");
        boolean prima = true;
        for (String riga : esito.righe()) {
            b.append("        <tspan x=\"0\" dy=\"").append(n(prima ? 0 : interlinea))
                    .append("\">").append(fuga(riga)).append("</tspan>\n");
            prima = false;
        }
        b.append("      </text>\n");
    }

    private static void qr(StringBuilder b, String contenuto, Elemento e) {
        boolean[][] m = Qr.codifica(contenuto, e.correzione());
        double passo = e.larghezza() / m.length;
        for (int r = 0; r < m.length; r++) {
            int c = 0;
            while (c < m.length) {
                if (!m[r][c]) { c++; continue; }
                int quanti = 1;
                while (c + quanti < m.length && m[r][c + quanti]) quanti++;
                b.append("      <rect x=\"").append(n(c * passo)).append("\" y=\"")
                        .append(n(r * passo)).append("\" width=\"").append(n(quanti * passo))
                        .append("\" height=\"").append(n(passo)).append("\"/>\n");
                c += quanti;
            }
        }
    }

    private static void barcode(StringBuilder b, String contenuto, Elemento e) {
        int[] tratti;
        try { tratti = Code128.tratti(contenuto); }
        catch (RuntimeException nonCodificabile) { return; }
        int totale = 0; for (int t : tratti) totale += t;
        double u = e.larghezza() / totale;
        double x = 0;
        for (int i = 0; i < tratti.length; i++) {
            if (i % 2 == 0) {
                b.append("      <rect x=\"").append(n(x)).append("\" y=\"0\" width=\"")
                        .append(n(tratti[i] * u)).append("\" height=\"")
                        .append(n(e.altezza())).append("\"/>\n");
            }
            x += tratti[i] * u;
        }
    }

    private static String n(double v) {
        String s = String.format(Locale.ROOT, "%.4f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String fuga(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
