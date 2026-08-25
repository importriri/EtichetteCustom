package prove;

import app.modello.Serie;

/** La finestra dell'incremento: il pezzo dove sbagliare costa etichette buttate. */
public final class ProvaSerie {

    private ProvaSerie() { }

    public static void esegui() {
        Prove.suite("Serie - finestra dell'incremento");

        final Serie s = new Serie("740125.003_01-02_584700349", 3);
        Prove.uguale("il prefisso e' tutto cio' che sta a sinistra della finestra",
                "740125.003_01-02_584700", s.prefisso());
        Prove.uguale("il numero di partenza e' quello scritto nel codice", 349, s.prossimo());
        Prove.uguale("ricomposto torna il codice originale",
                "740125.003_01-02_584700349", s.codice(s.prossimo()));
        Prove.uguale("gli zeri davanti restano", "007", s.finestra(7));
        Prove.uguale("la finestra di 3 cifre arriva a 999", 999, s.massimo());

        String[] giro = s.giro(3);
        Prove.uguale("un giro da 3 tira fuori 3 codici", 3, giro.length);
        Prove.uguale("il primo e' il prossimo", "740125.003_01-02_584700349", giro[0]);
        Prove.uguale("l'ultimo e' il prossimo piu' due", "740125.003_01-02_584700351", giro[2]);
        Prove.uguale("guardare il giro non consuma numeri", 349, s.prossimo());

        s.consuma(3);
        Prove.uguale("dopo la stampa il contatore avanza", 352, s.prossimo());

        final Serie quasiPiena = new Serie("AB-998", 3);
        Prove.esplode("un giro che sfora la finestra si ferma prima di stampare",
                IllegalStateException.class, new Runnable() {
                    @Override
                    public void run() {
                        quasiPiena.giro(5);
                    }
                });
        Prove.uguale("un giro che sfora non tocca il contatore", 998, quasiPiena.prossimo());

        Prove.esplode("una coda non numerica viene rifiutata subito",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("CODICE-AB", 3);
                    }
                });
        Prove.esplode("una finestra a zero cifre non ha senso",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("123456", 0);
                    }
                });
        Prove.esplode("una finestra piu' lunga del codice viene rifiutata",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("12", 5);
                    }
                });

        Prove.esplode("oltre nove cifre l'intero non ce la fa e la finestra viene rifiutata",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("00000000000000", 10);
                    }
                });

        Serie unaCifra = new Serie("LOTTO-7", 1);
        Prove.uguale("una finestra di una cifra sola funziona", "LOTTO-", unaCifra.prefisso());
        Prove.uguale("e arriva a 9", 9, unaCifra.massimo());
    }
}
