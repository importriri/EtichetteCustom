package prove;

import app.render.Misuratore;
import app.render.Testo;

/**
 * L'andata a capo. Qui vivono i due errori che sono costati piu' tempo:
 * il codice tutto cifre che non aveva dove tagliare, e la tentazione di
 * rimpicciolire prima di aver provato ad andare a capo.
 */
public final class ProvaTesto {

    private ProvaTesto() { }

    /** Misuratore a passo fisso: nessuno schermo, nessuna sorpresa. */
    private static final Misuratore PASSO_FISSO = new Misuratore() {
        @Override
        public double larghezza(String testo, double corpoMm, boolean grassetto) {
            return testo.length() * corpoMm * 0.6;
        }
    };

    public static void esegui() {
        Prove.suite("Testo - andata a capo e ripiego");

        String codice = "740125.003_01-02_584700349";
        Testo.Esito e = Testo.componi(codice, 46, 4.4, 3, false, PASSO_FISSO);
        Prove.uguale("il codice vero sta su due righe", 2, e.quanteRighe());
        Prove.vero("va a capo dopo un separatore, non in mezzo a un numero",
                e.righe()[0].endsWith("_"));
        Prove.vero("a corpo pieno: non ha rimpicciolito niente", !e.rimpicciolito());
        Prove.vicino("il corpo e' rimasto quello scelto", 4.4, e.corpo(), 0.001);
        Prove.uguale("rimesse insieme, le righe sono il codice",
                codice, e.righe()[0] + e.righe()[1]);

        String cifre = "000000000000000000000000000001";
        Testo.Esito f = Testo.componi(cifre, 46, 4.4, 3, false, PASSO_FISSO);
        Prove.vero("un codice tutto cifre trova comunque dove andare a capo",
                f.quanteRighe() >= 2);
        Prove.vero("senza rimpicciolire, se le righe bastano", !f.rimpicciolito());
        StringBuilder rimesso = new StringBuilder();
        for (String r : f.righe()) {
            rimesso.append(r);
        }
        Prove.uguale("non si perde nemmeno una cifra", cifre, rimesso.toString());

        Testo.Esito g = Testo.componi(cifre, 46, 4.4, 1, false, PASSO_FISSO);
        Prove.uguale("con una riga sola resta una riga sola", 1, g.quanteRighe());
        Prove.vero("e solo allora rimpicciolisce", g.rimpicciolito());
        Prove.vero("il corpo scende sotto quello scelto", g.corpo() < 4.4);
        Prove.vero("ma non sotto il fondo scala", g.corpo() >= 4.4 * 0.4 - 0.01);
        Prove.vero("e a quel corpo il testo ci sta davvero",
                PASSO_FISSO.larghezza(g.righe()[0], g.corpo(), false) <= 46.001);

        Testo.Esito h = Testo.componi("D04", 14, 6.5, 1, true, PASSO_FISSO);
        Prove.uguale("una sigla corta resta su una riga", 1, h.quanteRighe());
        Prove.vero("e non viene toccata", !h.rimpicciolito());

        Testo.Esito i = Testo.componi("", 20, 3, 2, false, PASSO_FISSO);
        Prove.uguale("il testo vuoto non fa esplodere niente", 1, i.quanteRighe());

        Prove.esplode("una larghezza negativa e' un errore di chi chiama",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        Testo.componi("x", -1, 3, 2, false, PASSO_FISSO);
                    }
                });
    }
}
