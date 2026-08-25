package prove;

import app.esporta.Esportazione;
import app.esporta.Png;
import app.esporta.Svg;
import app.modello.Etichetta;
import app.modello.Libreria;
import app.render.QrVero;
import app.render.SorgenteQr;
import app.stampa.StampaGiro;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.util.List;

/**
 * Stampa ed esportazione.
 *
 * Il controllo che vale piu' di tutti: la pagina uno e la pagina due di
 * un giro progressivo devono essere DIVERSE. Se qualcuno rompe il legame
 * fra il numero di copia e il codice disegnato, la stampante sputa dodici
 * etichette identiche e nessuno se ne accorge fino al magazzino.
 */
public final class ProvaStampa {

    private ProvaStampa() { }

    private static final SorgenteQr QR = new QrVero();

    public static void esegui() throws Exception {
        stampa();
        esporta();
    }

    private static void stampa() throws Exception {
        Prove.suite("Stampa - pagine e misure");

        Etichetta eti = Libreria.articolo();
        StampaGiro giro = new StampaGiro(eti, QR, 12);
        Prove.uguale("dodici copie, dodici pagine", 12, giro.getNumberOfPages());

        PageFormat f = giro.getPageFormat(0);
        Prove.vicino("la pagina e' larga quanto l'etichetta, in punti",
                50 * 72 / 25.4, f.getWidth(), 0.01);
        Prove.vicino("e alta quanto l'etichetta", 30 * 72 / 25.4, f.getHeight(), 0.01);
        Prove.vicino("l'area stampabile parte dall'angolo", 0, f.getImageableX(), 0.01);
        Prove.vicino("e non lascia margini in larghezza",
                f.getWidth(), f.getImageableWidth(), 0.01);
        Prove.vicino("ne' in altezza", f.getHeight(), f.getImageableHeight(), 0.01);

        BufferedImage prima = pagina(giro, f, 0);
        BufferedImage seconda = pagina(giro, f, 1);
        Prove.vero("la prima pagina ha inchiostro", ProvaDisegno.quotaInchiostro(prima) > 0.01);
        Prove.vero("la seconda pagina e' DIVERSA dalla prima: il codice avanza",
                !ProvaDisegno.identiche(prima, seconda));

        BufferedImage ancoraPrima = pagina(giro, f, 0);
        Prove.vero("ristampare la stessa pagina da lo stesso risultato",
                ProvaDisegno.identiche(prima, ancoraPrima));

        Prove.uguale("oltre l'ultima pagina non c'e' niente da stampare",
                Printable.NO_SUCH_PAGE, giro.print(nuovoContesto(10, 10), f, 12));

        Prove.esplode("un giro da zero copie non ha senso",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new StampaGiro(Libreria.articolo(), QR, 0);
                    }
                });
    }

    private static void esporta() throws Exception {
        Prove.suite("Esporta - PNG, SVG, PDF");

        Etichetta eti = Libreria.articolo();
        File dove = ProvaArchivio.temporanea("export");

        BufferedImage png = Png.immagine(eti, QR, 600, 0);
        Prove.uguale("a 600 dpi un'etichetta da 50 mm e' larga 1181 px", 1181, png.getWidth());
        Prove.uguale("e alta 709 px", 709, png.getHeight());
        Prove.vero("con dell'inchiostro sopra", ProvaDisegno.quotaInchiostro(png) > 0.01);
        Prove.esplode("una risoluzione assurda viene rifiutata",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        Png.immagine(Libreria.articolo(), QR, 100000, 0);
                    }
                });

        String svg = Svg.testo(eti, 0);
        Prove.vero("l'SVG si dichiara tale", svg.startsWith("<?xml"));
        Prove.vero("misura in millimetri, come l'etichetta vera",
                svg.contains("width=\"50mm\"") && svg.contains("height=\"30mm\""));
        Prove.vero("il codice progressivo e' scritto dentro, come testo",
                svg.contains("584700349"));
        Prove.vero("il QR e' fatto di rettangoli, non di un'immagine",
                svg.contains("<rect") && !svg.contains("<image"));
        Prove.vero("le due righe del codice ci sono entrambe",
                conta(svg, "<tspan") >= 2);

        List<File> tuttiPng = Esportazione.esporta(new File(dove, "giro.png"), eti, QR,
                Esportazione.Come.PNG, 3, 300);
        Prove.uguale("tre etichette, tre PNG", 3, tuttiPng.size());
        Prove.vero("numerati in ordine", tuttiPng.get(0).getName().equals("giro-1.png"));
        Prove.vero("e scritti davvero", tuttiPng.get(2).length() > 100);

        List<File> pdf = Esportazione.esporta(new File(dove, "giro.pdf"), eti, QR,
                Esportazione.Come.PDF, 3, 300);
        Prove.uguale("il PDF e' uno solo, con dentro tutto il giro", 1, pdf.size());
        byte[] dentro = byteDi(pdf.get(0));
        Prove.vero("comincia come un PDF", new String(dentro, 0, 8, "ISO-8859-1")
                .startsWith("%PDF-1."));
        String testa = new String(dentro, "ISO-8859-1");
        Prove.vero("dichiara tre pagine", testa.contains("/Count 3"));
        Prove.vero("le pagine misurano l'etichetta in punti",
                testa.contains("/MediaBox[0 0 141.732 85.039]"));
        Prove.vero("l'immagine e' compressa", testa.contains("/FlateDecode"));
        Prove.vero("e chiude come si deve", testa.trim().endsWith("%%EOF"));
    }

    private static BufferedImage pagina(StampaGiro giro, PageFormat f, int indice) {
        int w = (int) Math.ceil(f.getWidth() * 4);
        int h = (int) Math.ceil(f.getHeight() * 4);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.scale(4, 4);
            giro.print(g, f, indice);
        } finally {
            g.dispose();
        }
        return im;
    }

    private static Graphics2D nuovoContesto(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).createGraphics();
    }

    private static int conta(String dove, String cosa) {
        int quanti = 0;
        int da = 0;
        while ((da = dove.indexOf(cosa, da)) >= 0) {
            quanti++;
            da += cosa.length();
        }
        return quanti;
    }

    private static byte[] byteDi(File f) throws Exception {
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
