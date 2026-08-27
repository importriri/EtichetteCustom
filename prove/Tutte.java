package prove;

/** Runs every regression suite and exits with status 1 if any check fails. */
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
        ProvaMedia.esegui();
        System.exit(Prove.conclusione());
    }
}
