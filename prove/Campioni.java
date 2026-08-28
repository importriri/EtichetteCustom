package prove;

import app.codice.Code128;
import app.codice.Correzione;
import app.codice.Qr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import javax.imageio.ImageIO;

/**
 * Writes QR and barcode PNG samples together with their expected payloads for
 * independent physical scanner verification.
 *
 *   java -cp build:prove-build prove.Campioni samples/
 */
public final class Campioni {

    private Campioni() { }

    public static void main(String[] args) throws Exception {
        File dove = new File(args.length > 0 ? args[0] : "campioni");
        if (!dove.isDirectory() && !dove.mkdirs()) {
            throw new IllegalStateException("cannot create " + dove);
        }

        String[] testi = {
            "740125.003_01-02_584700349",
            "D04",
            "BOX-4802-0117",
            "R-07-13",
            "AST-1042",
            "0000000000000000000000000001",
        };

        PrintWriter elenco = new PrintWriter(new File(dove, "cosa-devono-dire.txt"), "UTF-8");
        elenco.println("file\tsimbologia\tcontenuto atteso");
        int n = 0;
        for (String t : testi) {
            for (Correzione c : Correzione.values()) {
                String nome = String.format("qr-%02d-%s.png", n++, c.name());
                matrice(Qr.codifica(t, c), new File(dove, nome), 8, 4);
                elenco.println(nome + "\tQR " + c.name() + "\t" + t);
            }
        }
        for (String t : testi) {
            if (t.length() > 20) {
                continue;
            }
            String nome = String.format("code128-%02d.png", n++);
            barre(Code128.tratti(t), new File(dove, nome), 4, 90, 20);
            elenco.println(nome + "\tCode 128\t" + t);
        }
        elenco.close();
        System.out.println("scritti " + n + " campioni in " + dove.getAbsolutePath());
        System.out.println("scansionali col lettore e confronta con cosa-devono-dire.txt");
    }

    private static void matrice(boolean[][] m, File f, int scala, int quiete) throws Exception {
        int lato = m.length;
        int tot = (lato + quiete * 2) * scala;
        BufferedImage im = new BufferedImage(tot, tot, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tot, tot);
            g.setColor(Color.BLACK);
            for (int y = 0; y < lato; y++) {
                for (int x = 0; x < lato; x++) {
                    if (m[y][x]) {
                        g.fillRect((x + quiete) * scala, (y + quiete) * scala, scala, scala);
                    }
                }
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(im, "png", f);
    }

    private static void barre(int[] tratti, File f, int scala, int alto, int quiete)
            throws Exception {
        int moduli = 0;
        for (int t : tratti) {
            moduli += t;
        }
        int w = (moduli + quiete * 2) * scala;
        BufferedImage im = new BufferedImage(w, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, alto);
            g.setColor(Color.BLACK);
            int x = quiete * scala;
            for (int i = 0; i < tratti.length; i++) {
                int largo = tratti[i] * scala;
                if (i % 2 == 0) {
                    g.fillRect(x, 0, largo, alto);
                }
                x += largo;
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(im, "png", f);
    }
}
