package prove;

import app.archivio.Formato;
import app.modello.Etichetta;
import app.modello.Libreria;
import app.modello.Storia;

/**
 * L'annullamento.
 *
 * La trappola classica: la copia messa da parte condivide gli stessi
 * oggetti dell'originale, quindi "annulla" non riporta indietro niente
 * perche' la copia e' cambiata insieme all'originale.
 */
public final class ProvaStoria {

    private ProvaStoria() { }

    public static void esegui() {
        Prove.suite("Storia - annulla");

        Etichetta e = Libreria.articolo();
        Storia s = new Storia();
        Prove.vero("all'inizio non c'e' niente da annullare", !s.qualcosaDaAnnullare());

        String partenza = Formato.scrivi(e);
        s.segna(e);
        e.elementi().get(0).x(40);
        Prove.vero("la mossa ha cambiato qualcosa", !Formato.scrivi(e).equals(partenza));

        Prove.vero("annulla dice di aver fatto qualcosa", s.annulla(e));
        Prove.uguale("e l'etichetta e' tornata com'era", partenza, Formato.scrivi(e));

        s.segna(e);
        e.elementi().get(0).x(10);
        s.segna(e);
        e.elementi().get(0).y(9);
        s.annulla(e);
        Prove.vicino("due mosse, due annullamenti: il primo torna alla y di prima",
                1.6, e.elementi().get(0).y(), 0.001);
        s.annulla(e);
        Prove.vicino("il secondo torna alla x di prima", 2, e.elementi().get(0).x(), 0.001);

        s.segna(e);
        s.segna(e);
        s.segna(e);
        Prove.vero("segnare tre volte senza cambiare niente non impila tre copie",
                s.annulla(e) == false || Formato.scrivi(e).equals(Formato.scrivi(e)));

        Etichetta f = Libreria.articolo();
        Storia t = new Storia();
        t.segna(f);
        f.rimuovi(f.elementi().get(0));
        Prove.uguale("dopo aver tolto un elemento ne restano tre", 3, f.elementi().size());
        t.annulla(f);
        Prove.uguale("annullando torna anche l'elemento eliminato", 4, f.elementi().size());

        Etichetta g = Libreria.articolo();
        Storia u = new Storia();
        u.segna(g);
        g.serie().consuma(5);
        u.annulla(g);
        Prove.uguale("e anche il contatore della serie torna indietro", 349, g.serie().prossimo());

        Storia vuota = new Storia();
        Prove.vero("annullare quando non c'e' niente non esplode",
                !vuota.annulla(Libreria.articolo()));
    }
}
