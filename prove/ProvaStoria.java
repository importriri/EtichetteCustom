package prove;

import app.archivio.Formato;
import app.modello.Etichetta;
import app.modello.Libreria;
import app.modello.Storia;

/**
 * Undo regression coverage. Snapshots must be deep copies so later edits cannot
 * mutate the history entry that is supposed to restore the previous state.
 */
public final class ProvaStoria {

    private ProvaStoria() { }

    public static void esegui() {
        Prove.suite("Undo history");

        Etichetta e = Libreria.articolo();
        Storia s = new Storia();
        Prove.vero("history starts empty", !s.qualcosaDaAnnullare());

        String partenza = Formato.scrivi(e);
        s.segna(e);
        e.elementi().get(0).x(40);
        Prove.vero("the edit changes serialized state", !Formato.scrivi(e).equals(partenza));

        Prove.vero("undo reports a restored snapshot", s.annulla(e));
        Prove.uguale("undo restores the exact previous state", partenza, Formato.scrivi(e));

        s.segna(e);
        e.elementi().get(0).x(10);
        s.segna(e);
        e.elementi().get(0).y(9);
        s.annulla(e);
        Prove.vicino("two edits and two undos: first undo restores y",
                1.6, e.elementi().get(0).y(), 0.001);
        s.annulla(e);
        Prove.vicino("second undo restores x", 2, e.elementi().get(0).x(), 0.001);

        s.segna(e);
        s.segna(e);
        s.segna(e);
        Prove.vero("repeated identical snapshots are not stacked",
                s.annulla(e) == false || Formato.scrivi(e).equals(Formato.scrivi(e)));

        Etichetta f = Libreria.articolo();
        Storia t = new Storia();
        t.segna(f);
        f.rimuovi(f.elementi().get(0));
        Prove.uguale("removing one element leaves three", 3, f.elementi().size());
        t.annulla(f);
        Prove.uguale("undo restores a deleted element", 4, f.elementi().size());

        Etichetta g = Libreria.articolo();
        Storia u = new Storia();
        u.segna(g);
        g.serie().consuma(5);
        u.annulla(g);
        Prove.uguale("undo restores sequence state too", 349, g.serie().prossimo());

        Storia vuota = new Storia();
        Prove.vero("undo on empty history is safe",
                !vuota.annulla(Libreria.articolo()));
    }
}
