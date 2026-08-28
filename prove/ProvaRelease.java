package prove;

import app.archivio.Formato;
import app.modello.Campo;
import app.modello.Comportamento;
import app.modello.Elemento;
import app.modello.Etichetta;
import app.modello.Impostazioni;
import app.modello.Serie;
import app.modello.Storia;
import app.modello.Tipo;
import app.render.Ingombri;
import app.render.Testo;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

/** Release-level compatibility and behavior checks. */
public final class ProvaRelease {
    private static int ok;
    private static int ko;

    private ProvaRelease() { }

    public static void main(String[] args) throws Exception {
        multiData();
        textParts();
        roundTrip();
        malformedStorage();
        undoRedo();
        settings();
        rotations();
        System.out.println(ok + " behavior checks, " + ko + " failed");
        if (ko != 0) System.exit(1);
    }

    private static Etichetta sample() {
        Etichetta label = new Etichetta("Produzione", 50, 30);
        Campo first = new Campo("codice", Comportamento.PROGRESSIVO, "A-001");
        first.serie(new Serie("A-001", 3));
        Campo second = new Campo("codice 2", Comportamento.PROGRESSIVO, "B-0900");
        second.serie(new Serie("B-0900", 4));
        Campo lot = new Campo("lotto", Comportamento.CHIESTO, "L-44");
        label.aggiungi(first).aggiungi(second).aggiungi(lot);
        label.aggiungi(new Elemento("QR", Tipo.QR, "codice", 3, 3, 12));
        Elemento text = new Elemento("Testo", Tipo.CODICE, "codice", 18, 3, 25);
        text.allineamento(1);
        text.mostraSeparatori(false);
        text.righePreferite(3);
        text.rotazione(270);
        label.aggiungi(text);
        label.aggiungi(new Elemento("QR 2", Tipo.QR, "codice 2", 3, 17, 10));
        label.aggiungi(new Elemento("Lotto", Tipo.TESTO, "lotto", 18, 18, 25));
        return label;
    }

    private static void multiData() {
        Etichetta label = sample();
        check("two independent progressives are retained", label.progressivi().size() == 2);
        check("shared QR and text remain one data source",
                label.elementiPerCampo(label.campo("codice")).size() == 2);
        check("first progressive starts at A-001",
                "A-001".equals(label.valoreAllaCopia(label.campo("codice"), 0)));
        check("second progressive starts at B-0900",
                "B-0900".equals(label.valoreAllaCopia(label.campo("codice 2"), 0)));
        check("both progressives validate one run", valid(label, 12));
        label.consumaProgressivi(12);
        check("first progressive advances independently",
                "A-013".equals(label.campo("codice").corrente()));
        check("second progressive advances independently",
                "B-0912".equals(label.campo("codice 2").corrente()));
        Elemento text = label.elementi().get(1);
        Campo independent = label.rendiIndipendente(text);
        check("make independent creates another data id",
                independent != null && !"codice".equals(independent.nome()));
        check("original source is now used only once",
                label.elementiPerCampo(label.campo("codice")).size() == 1);
    }

    private static void textParts() {
        Etichetta label = sample();
        Elemento qr = label.elementi().get(0);
        Elemento text = label.elementi().get(1);
        text.parteTesto(2);
        check("logical code groups can be selected independently",
                "001".equals(Testo.parte(label.contenuto(text, 0), text.parteTesto())));
        check("text-part selection never changes the shared source",
                "A-001".equals(label.contenuto(text, 0)));
        check("QR still receives the complete source",
                "A-001".equals(label.contenuto(qr, 0)));
    }

    private static boolean valid(Etichetta label, int copies) {
        try {
            label.validaGiro(copies);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void roundTrip() {
        Etichetta label = sample();
        label.elementi().get(1).parteTesto(2);
        label.consumaProgressivi(7);
        String text = Formato.scrivi(label);
        Etichetta restored = Formato.leggi(text);
        check("storage round-trip keeps both progressives", restored.progressivi().size() == 2);
        check("storage round-trip keeps first counter",
                "A-008".equals(restored.campo("codice").corrente()));
        check("storage round-trip keeps second counter",
                "B-0907".equals(restored.campo("codice 2").corrente()));
        check("storage round-trip keeps shared links",
                restored.elementiPerCampo(restored.campo("codice")).size() == 2);
        Elemento restoredText = restored.elementi().get(1);
        check("storage keeps centered text", restoredText.allineamento() == 1);
        check("storage keeps hidden separators", !restoredText.mostraSeparatori());
        check("storage keeps explicit three-line layout", restoredText.righePreferite() == 3);
        check("storage keeps 270 degree rotation", restoredText.rotazione() == 270);
        check("storage keeps derived text-part selection", restoredText.parteTesto() == 2);

        String v3 = "etichette-custom\t3\n"
                + "nome\tVecchia\n"
                + "misura\t50\t30\n"
                + "campo\tc\tFISSO\tABC.123\t\t\n"
                + "elemento\tTesto\tCODICE\tc\t2\t2\t40\t40\t4\t0\t0\t3\tM\t1\t0\n";
        Etichetta oldV3 = Formato.leggi(v3);
        check("v3 labels still load", oldV3.elementi().size() == 1);
        check("v3 labels default to automatic line layout",
                oldV3.elementi().get(0).righePreferite() == 0);
        check("v3 labels default to the complete text source",
                oldV3.elementi().get(0).parteTesto() == 0);

        String v4 = "etichette-custom\t4\n"
                + "nome\tVersione 4\n"
                + "misura\t50\t30\n"
                + "campo\tc\tFISSO\tABC.123\t\t\n"
                + "elemento\tTesto\tCODICE\tc\t2\t2\t40\t40\t4\t0\t0\t3\tM\t1\t0\t2\n";
        Etichetta oldV4 = Formato.leggi(v4);
        check("v4 labels still load", oldV4.elementi().size() == 1);
        check("v4 labels default to the complete text source",
                oldV4.elementi().get(0).parteTesto() == 0);
    }

    private static void malformedStorage() {
        check("missing format version is rejected cleanly",
                rejected("etichette-custom\n"));
        check("incomplete element records are rejected cleanly",
                rejected("etichette-custom\t5\n"
                        + "nome\tBroken\n"
                        + "misura\t50\t30\n"
                        + "elemento\tOnly-a-name\n"));
        check("invalid zero format versions are rejected cleanly",
                rejected("etichette-custom\t0\n"));
    }

    private static boolean rejected(String text) {
        try {
            Formato.leggi(text);
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static void undoRedo() {
        Etichetta label = sample();
        Storia history = new Storia();
        history.segna(label);
        double before = label.elementi().get(0).x();
        label.elementi().get(0).x(before + 5);
        check("undo is available after a recorded edit", history.qualcosaDaAnnullare());
        check("undo restores geometry",
                history.annulla(label) && Math.abs(label.elementi().get(0).x() - before) < .001);
        check("redo becomes available", history.qualcosaDaRipetere());
        check("redo reapplies geometry",
                history.ripeti(label)
                        && Math.abs(label.elementi().get(0).x() - (before + 5)) < .001);
    }

    private static void settings() throws Exception {
        String old = System.getProperty("user.home");
        File home = Files.createTempDirectory("etichette-settings-").toFile();
        try {
            System.setProperty("user.home", home.getAbsolutePath());
            Impostazioni first = new Impostazioni();
            first.cartellaEtichette(new File(home, "layout lungo reparto A"));
            first.cartellaLog(new File(home, "registro turno serale"));
            first.stampante("Datamax test");
            first.risoluzioneDpi(300);
            first.salva();
            Impostazioni restored = new Impostazioni();
            check("settings persist printer", "Datamax test".equals(restored.stampante()));
            check("settings persist dpi", restored.risoluzioneDpi() == 300);
            check("settings persist label path",
                    restored.cartellaEtichette().getName().equals("layout lungo reparto A"));
            check("settings persist log path",
                    restored.cartellaLog().getName().equals("registro turno serale"));
            File temporary = new File(first.file().getParentFile(), first.file().getName() + ".tmp");
            check("settings save leaves no temporary replacement file", !temporary.exists());
        } finally {
            if (old != null) System.setProperty("user.home", old);
        }
    }

    private static void rotations() {
        Etichetta label = new Etichetta("Rotazioni", 60, 40);
        label.aggiungi(new Campo("c", Comportamento.FISSO, "HELLO-12345"));
        Elemento text = new Elemento("Codice", Tipo.CODICE, "c", 10, 10, 25).corpo(5, false);
        label.aggiungi(text);
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            text.rotazione(0);
            Rectangle2D.Double b0 = Ingombri.di(graphics, label, text, 10, 0);
            text.rotazione(90);
            Rectangle2D.Double b90 = Ingombri.di(graphics, label, text, 10, 0);
            text.rotazione(180);
            Rectangle2D.Double b180 = Ingombri.di(graphics, label, text, 10, 0);
            text.rotazione(270);
            Rectangle2D.Double b270 = Ingombri.di(graphics, label, text, 10, 0);
            check("90 degree hitbox swaps width and height",
                    near(b0.width, b90.height) && near(b0.height, b90.width));
            check("180 degree hitbox keeps visible size",
                    near(b0.width, b180.width) && near(b0.height, b180.height));
            check("270 degree hitbox swaps width and height",
                    near(b0.width, b270.height) && near(b0.height, b270.width));
            check("rotated hitboxes stay anchored to visible top-left",
                    near(b0.x, b90.x) && near(b0.y, b90.y));
        } finally {
            graphics.dispose();
        }
    }

    private static boolean near(double first, double second) {
        return Math.abs(first - second) < .05;
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            ok++;
            System.out.println("  ok   " + name);
        } else {
            ko++;
            System.out.println("  FAIL " + name);
        }
    }
}
