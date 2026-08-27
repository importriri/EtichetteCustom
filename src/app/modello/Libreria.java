package app.modello;

import java.util.ArrayList;
import java.util.List;

/** Etichetta iniziale mostrata soltanto quando il nuovo archivio e' vuoto. */
public final class Libreria {

    private Libreria() { }

    public static List<Etichetta> iniziale() {
        List<Etichetta> out = new ArrayList<Etichetta>();
        out.add(esempio());
        return out;
    }

    /**
     * Un solo esempio, vicino all'uso reale: stesso dato per QR e testo.
     * Il punto resta nel dato/QR; il testo puo' nascondere i separatori.
     */
    public static Etichetta esempio() {
        Etichetta e = new Etichetta("Esempio", 50, 30);
        String codice = "210150.002_02-01.262350009";
        e.aggiungi(new Campo("codice", Comportamento.FISSO, codice));

        Elemento qr = new Elemento("QR", Tipo.QR, "codice", 4.0, 2.5, 13.5);
        e.aggiungi(qr);

        Elemento testo = new Elemento("Testo", Tipo.CODICE, "codice", 4.0, 18.0, 42.0)
                .corpo(3.5, false);
        testo.altezza(10.0);
        testo.massimoRighe(3);
        testo.allineamento(0);
        testo.mostraSeparatori(false);
        e.aggiungi(testo);
        return e;
    }

    /* Nomi mantenuti per compatibilita' con prove o codice esterno. */
    public static Etichetta articolo() { return esempio(); }
    public static Etichetta imballo() { return esempio().copia(); }
    public static Etichetta scaffale() { return esempio().copia(); }
    public static Etichetta cespite() { return esempio().copia(); }
    public static Etichetta minima() { return esempio().copia(); }
}
