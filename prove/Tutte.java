package prove;

/** Esegue tutte le suite e torna 1 se qualcosa non va. */
public final class Tutte {

    private Tutte() { }

    public static void main(String[] args) throws Exception {
        ProvaCodici.esegui();
        ProvaSerie.esegui();
        ProvaTesto.esegui();
        ProvaDisegno.esegui();
        ProvaStoria.esegui();
        ProvaArchivio.esegui();
        ProvaStampa.esegui();
        ProvaGiornata.esegui();
        ProvaSchermate.esegui();
        System.exit(Prove.conclusione());
    }
}
