package prove;

import app.modello.Serie;

/** Sequence-window rules that prevent invalid or duplicated production labels. */
public final class ProvaSerie {

    private ProvaSerie() { }

    public static void esegui() {
        Prove.suite("Sequence window");

        final Serie s = new Serie("740125.003_01-02_584700349", 3);
        Prove.uguale("prefix contains everything left of the increment window",
                "740125.003_01-02_584700", s.prefisso());
        Prove.uguale("starting number matches the source code", 349, s.prossimo());
        Prove.uguale("recomposed code matches the original",
                "740125.003_01-02_584700349", s.codice(s.prossimo()));
        Prove.uguale("leading zeroes are preserved", "007", s.finestra(7));
        Prove.uguale("a three-digit window reaches 999", 999, s.massimo());

        String[] giro = s.giro(3);
        Prove.uguale("a three-label run produces three codes", 3, giro.length);
        Prove.uguale("the first run value is the next code", "740125.003_01-02_584700349", giro[0]);
        Prove.uguale("the last run value is next plus two", "740125.003_01-02_584700351", giro[2]);
        Prove.uguale("previewing a run does not consume values", 349, s.prossimo());

        s.consuma(3);
        Prove.uguale("consuming a run advances the counter", 352, s.prossimo());

        final Serie quasiPiena = new Serie("AB-998", 3);
        Prove.esplode("a run that overflows the window is rejected before printing",
                IllegalStateException.class, new Runnable() {
                    @Override
                    public void run() {
                        quasiPiena.giro(5);
                    }
                });
        Prove.uguale("a rejected run does not change the counter", 998, quasiPiena.prossimo());

        Prove.esplode("a non-numeric sequence tail is rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("CODICE-AB", 3);
                    }
                });
        Prove.esplode("a zero-digit sequence window is rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("123456", 0);
                    }
                });
        Prove.esplode("a window longer than the source code is rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("12", 5);
                    }
                });

        Prove.esplode("sequence windows above nine digits are rejected",
                IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        new Serie("00000000000000", 10);
                    }
                });

        Serie unaCifra = new Serie("LOTTO-7", 1);
        Prove.uguale("a one-digit window is supported", "LOTTO-", unaCifra.prefisso());
        Prove.uguale("a one-digit window reaches 9", 9, unaCifra.massimo());
    }
}
