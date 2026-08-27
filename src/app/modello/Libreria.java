package app.modello;

import java.util.ArrayList;
import java.util.List;

/** Etichette di esempio e fixture del programma. */
public final class Libreria {

    private Libreria() { }

    /** Fixture storiche usate dai test e da installazioni precedenti. */
    public static List<Etichetta> iniziale() {
        List<Etichetta> out = new ArrayList<Etichetta>();
        out.add(articolo());
        out.add(imballo());
        out.add(scaffale());
        out.add(cespite());
        out.add(minima());
        return out;
    }

    /** Sul nuovo archivio v2 compare una sola etichetta semplice e modificabile. */
    public static List<Etichetta> inizialeV2() {
        List<Etichetta> out = new ArrayList<Etichetta>();
        out.add(esempio());
        return out;
    }

    public static Etichetta esempio() {
        Etichetta e = new Etichetta("Esempio", 50, 30);
        String codice = "210150.002_02-01.262350009";
        e.serie(new Serie(codice, 3));
        e.aggiungi(new Campo("codice", Comportamento.PROGRESSIVO, codice));
        e.aggiungi(new Elemento("QR", Tipo.QR, "codice", 4.0, 2.5, 13.5));
        Elemento testo = new Elemento("Testo", Tipo.CODICE, "codice", 4.0, 18.0, 42.0)
                .corpo(3.5, false);
        testo.altezza(10.0);
        testo.massimoRighe(3);
        testo.allineamento(0);
        testo.mostraSeparatori(false);
        e.aggiungi(testo);
        return e;
    }

    public static Etichetta articolo() {
        Etichetta e = new Etichetta("Articolo demo", 50, 30);
        e.serie(new Serie("740125.003_01-02_584700349", 3));
        e.aggiungi(new Campo("codice", Comportamento.PROGRESSIVO, "740125.003_01-02_584700349"));
        e.aggiungi(new Campo("sigla", Comportamento.FISSO, "D04"));
        e.aggiungi(new Campo("lotto", Comportamento.CHIESTO, "4802-X"));
        e.aggiungi(new Campo("revisione", Comportamento.FISSO, "03_01-02"));
        e.aggiungi(new Elemento("Codice", Tipo.CODICE, "codice", 2, 1.6, 46).corpo(4.4, false));
        e.aggiungi(new Elemento("QR", Tipo.QR, "codice", 2.6, 12.6, 15.4));
        e.aggiungi(new Elemento("Sigla", Tipo.TESTO, "sigla", 21.5, 14, 14).corpo(6.5, true));
        e.aggiungi(new Elemento("Lotto", Tipo.TESTO, "lotto", 21.5, 23.4, 24).corpo(2.9, false));
        return e;
    }

    public static Etichetta imballo() {
        Etichetta e = new Etichetta("Imballo", 100, 70);
        e.serie(new Serie("BOX-4802-0118", 4));
        e.aggiungi(new Campo("collo", Comportamento.PROGRESSIVO, "BOX-4802-0118"));
        e.aggiungi(new Campo("destinazione", Comportamento.CHIESTO, "Bolzano"));
        e.aggiungi(new Campo("pezzi", Comportamento.CHIESTO, "24 pz"));
        e.aggiungi(new Elemento("Collo", Tipo.CODICE, "collo", 5, 5, 90).corpo(9, true));
        e.aggiungi(new Elemento("Riga", Tipo.LINEA, null, 5, 16.5, 90).altezzaDi(0.4));
        e.aggiungi(new Elemento("QR", Tipo.QR, "collo", 5, 21, 34));
        e.aggiungi(new Elemento("Destinazione", Tipo.TESTO, "destinazione", 44, 23, 52).corpo(6, false));
        e.aggiungi(new Elemento("Pezzi", Tipo.TESTO, "pezzi", 44, 45, 52).corpo(8, true));
        return e;
    }

    public static Etichetta scaffale() {
        Etichetta e = new Etichetta("Scaffale", 62, 29);
        e.serie(new Serie("R-07-13", 2));
        e.aggiungi(new Campo("posizione", Comportamento.PROGRESSIVO, "R-07-13"));
        e.aggiungi(new Elemento("Posizione", Tipo.TESTO, "posizione", 4, 3, 54).corpo(11, true));
        e.aggiungi(new Elemento("Barcode", Tipo.BARCODE, "posizione", 4, 17, 54).altezzaDi(8));
        return e;
    }

    public static Etichetta cespite() {
        Etichetta e = new Etichetta("Cespite interno", 70, 40);
        e.serie(new Serie("AST-1042", 4));
        e.aggiungi(new Campo("cespite", Comportamento.PROGRESSIVO, "AST-1042"));
        e.aggiungi(new Campo("reparto", Comportamento.FISSO, "Area D04"));
        e.aggiungi(new Elemento("Cespite", Tipo.CODICE, "cespite", 4, 4, 44).corpo(7, true));
        e.aggiungi(new Elemento("Reparto", Tipo.TESTO, "reparto", 4, 15, 44).corpo(4, false));
        e.aggiungi(new Elemento("QR", Tipo.QR, "cespite", 50, 11, 17));
        return e;
    }

    public static Etichetta minima() {
        Etichetta e = new Etichetta("Minima", 40, 15);
        e.aggiungi(new Campo("codice", Comportamento.CHIESTO, "5847 0034 8"));
        e.aggiungi(new Elemento("QR", Tipo.QR, "codice", 1.5, 1.5, 12));
        e.aggiungi(new Elemento("Codice", Tipo.CODICE, "codice", 16, 5, 22).corpo(3.2, false));
        return e;
    }
}
