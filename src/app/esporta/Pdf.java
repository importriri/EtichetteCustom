package app.esporta;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;

/**
 * Minimal dependency-free PDF writer.
 *
 * Each label becomes a page with the exact physical dimensions and a high-DPI
 * raster image, so viewers do not need to infer any scale. Embedding vector
 * fonts would add substantial complexity without improving the calibrated print path.
 */
public final class Pdf {

    private static final double PUNTI_PER_MM = 72.0 / 25.4;

    private Pdf() { }

    public static void scrivi(File dove, List<BufferedImage> pagine,
                              double larghezzaMm, double altezzaMm) throws IOException {
        if (pagine == null || pagine.isEmpty()) {
            throw new IllegalArgumentException("un PDF senza pagine non si scrive");
        }
        byte[] documento = costruisci(pagine, larghezzaMm, altezzaMm);
        OutputStream out = new FileOutputStream(dove);
        try {
            out.write(documento);
        } finally {
            out.close();
        }
    }

    static byte[] costruisci(List<BufferedImage> pagine, double larghezzaMm, double altezzaMm)
            throws IOException {
        double w = larghezzaMm * PUNTI_PER_MM;
        double h = altezzaMm * PUNTI_PER_MM;

        /* catalog, page list, then page/content/image objects for each label */
        int quante = pagine.size();
        List<byte[]> oggetti = new ArrayList<byte[]>();

        StringBuilder figli = new StringBuilder();
        for (int i = 0; i < quante; i++) {
            figli.append(3 + i * 3).append(" 0 R ");
        }
        oggetti.add(byteDi("<</Type/Catalog/Pages 2 0 R>>"));
        oggetti.add(byteDi("<</Type/Pages/Kids[" + figli.toString().trim()
                + "]/Count " + quante + ">>"));

        for (int i = 0; i < quante; i++) {
            int idPagina = 3 + i * 3;
            int idContenuto = idPagina + 1;
            int idImmagine = idPagina + 2;
            BufferedImage im = pagine.get(i);

            oggetti.add(byteDi("<</Type/Page/Parent 2 0 R/MediaBox[0 0 "
                    + n(w) + " " + n(h) + "]"
                    + "/Resources<</XObject<</Im0 " + idImmagine + " 0 R>>>>"
                    + "/Contents " + idContenuto + " 0 R>>"));

            String flusso = "q " + n(w) + " 0 0 " + n(h) + " 0 0 cm /Im0 Do Q\n";
            byte[] corpoFlusso = byteDi(flusso);
            oggetti.add(unisci(byteDi("<</Length " + corpoFlusso.length + ">>\nstream\n"),
                    corpoFlusso, byteDi("\nendstream")));

            byte[] pixel = pixelRgb(im);
            byte[] compressi = comprimi(pixel);
            oggetti.add(unisci(byteDi("<</Type/XObject/Subtype/Image"
                            + "/Width " + im.getWidth() + "/Height " + im.getHeight()
                            + "/ColorSpace/DeviceRGB/BitsPerComponent 8"
                            + "/Filter/FlateDecode/Length " + compressi.length + ">>\nstream\n"),
                    compressi, byteDi("\nendstream")));
        }

        ByteArrayOutputStream fuori = new ByteArrayOutputStream();
        fuori.write(byteDi("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n"));
        int[] posizioni = new int[oggetti.size() + 1];
        for (int i = 0; i < oggetti.size(); i++) {
            posizioni[i + 1] = fuori.size();
            fuori.write(byteDi((i + 1) + " 0 obj\n"));
            fuori.write(oggetti.get(i));
            fuori.write(byteDi("\nendobj\n"));
        }

        int inizioXref = fuori.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(oggetti.size() + 1).append('\n');
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= oggetti.size(); i++) {
            xref.append(String.format(Locale.ROOT, "%010d 00000 n \n", posizioni[i]));
        }
        xref.append("trailer\n<</Size ").append(oggetti.size() + 1)
                .append("/Root 1 0 R>>\nstartxref\n").append(inizioXref).append("\n%%EOF\n");
        fuori.write(byteDi(xref.toString()));
        return fuori.toByteArray();
    }

    private static byte[] pixelRgb(BufferedImage im) {
        int w = im.getWidth();
        int h = im.getHeight();
        byte[] out = new byte[w * h * 3];
        int k = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = im.getRGB(x, y);
                out[k++] = (byte) (rgb >> 16);
                out[k++] = (byte) (rgb >> 8);
                out[k++] = (byte) rgb;
            }
        }
        return out;
    }

    private static byte[] comprimi(byte[] dati) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            d.setInput(dati);
            d.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(dati.length / 4 + 64);
            byte[] buffer = new byte[16384];
            while (!d.finished()) {
                int quanti = d.deflate(buffer);
                out.write(buffer, 0, quanti);
            }
            return out.toByteArray();
        } finally {
            d.end();
        }
    }

    private static byte[] unisci(byte[]... parti) {
        int totale = 0;
        for (byte[] p : parti) {
            totale += p.length;
        }
        byte[] out = new byte[totale];
        int k = 0;
        for (byte[] p : parti) {
            System.arraycopy(p, 0, out, k, p.length);
            k += p.length;
        }
        return out;
    }

    private static byte[] byteDi(String s) {
        try {
            return s.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException impossibile) {
            throw new IllegalStateException(impossibile);
        }
    }

    private static String n(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
