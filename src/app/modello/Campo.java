package app.modello;

/** Un dato dell'etichetta, indipendente dagli elementi che lo mostrano. */
public class Campo {

    private final String nome;
    private Comportamento comportamento;
    private String valore;
    private Serie serie;

    public Campo(String nome, Comportamento comportamento, String valore) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("il campo deve avere un nome");
        }
        this.nome = nome.trim();
        this.comportamento = comportamento == null ? Comportamento.FISSO : comportamento;
        this.valore = valore == null ? "" : valore;
    }

    public String nome() { return nome; }
    public Comportamento comportamento() { return comportamento; }

    public void comportamento(Comportamento nuovo) {
        Comportamento destinazione = nuovo == null ? Comportamento.FISSO : nuovo;
        if (comportamento == Comportamento.PROGRESSIVO
                && destinazione != Comportamento.PROGRESSIVO && serie != null) {
            valore = serie.codice(serie.prossimo());
            serie = null;
        }
        comportamento = destinazione;
    }

    public String valore() { return valore; }
    public void valore(String v) { valore = v == null ? "" : v; }

    public Serie serie() { return serie; }

    public void serie(Serie s) {
        serie = s;
        if (s != null) {
            comportamento = Comportamento.PROGRESSIVO;
            valore = s.codice(s.prossimo());
        }
    }

    /** Il valore che verrebbe usato adesso, senza consumare il progressivo. */
    public String corrente() {
        if (comportamento == Comportamento.PROGRESSIVO && serie != null) {
            return serie.codice(serie.prossimo());
        }
        return valore;
    }

    public Campo copia() {
        Campo c = new Campo(nome, comportamento, valore);
        if (serie != null) {
            c.serie(new Serie(serie.codice(serie.prossimo()), serie.cifre()));
        }
        return c;
    }

    @Override public String toString() { return nome; }
}
