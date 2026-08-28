package app.modello;

import app.codice.Correzione;

/** One visual element placed on the label; all geometry is stored in millimetres. */
public class Elemento {
    private String nome;
    private final Tipo tipo;
    private String campo;
    private double x;
    private double y;
    private double larghezza;
    private double altezza;
    private double corpo = 3.0;
    private boolean grassetto;
    private int rotazione;
    private int massimoRighe = 3;
    private Correzione correzione = Correzione.M;
    private int allineamento;
    private boolean mostraSeparatori = true;
    private int righePreferite;
    private int parteTesto;

    public Elemento(String nome, Tipo tipo, String campo,
                    double x, double y, double larghezza) {
        this.nome = nome;
        this.tipo = tipo;
        this.campo = campo;
        this.x = x;
        this.y = y;
        this.larghezza = larghezza;
        this.altezza = larghezza;
    }

    public String nome() { return nome; }
    public void nome(String valore) { nome = valore; }
    public Tipo tipo() { return tipo; }
    public String campo() { return campo; }
    public void campo(String valore) { campo = valore; }
    public double x() { return x; }
    public void x(double valore) { x = arrotonda(valore); }
    public double y() { return y; }
    public void y(double valore) { y = arrotonda(valore); }
    public double larghezza() { return larghezza; }
    public void larghezza(double valore) { larghezza = Math.max(1.0, arrotonda(valore)); }
    public double altezza() { return altezza; }
    public void altezza(double valore) { altezza = Math.max(.4, arrotonda(valore)); }
    public double corpo() { return corpo; }
    public void corpo(double valore) { corpo = Math.max(1.0, arrotonda(valore)); }
    public boolean grassetto() { return grassetto; }
    public void grassetto(boolean valore) { grassetto = valore; }
    public int rotazione() { return rotazione; }

    public void rotazione(int gradi) {
        int valore = ((gradi % 360) + 360) % 360;
        if (valore != 0 && valore != 90 && valore != 180 && valore != 270) {
            throw new IllegalArgumentException(
                    "rotation must be 0, 90, 180 or 270 degrees: " + gradi);
        }
        rotazione = valore;
    }

    public Correzione correzione() { return correzione; }
    public void correzione(Correzione valore) {
        correzione = valore == null ? Correzione.M : valore;
    }

    public int massimoRighe() { return massimoRighe; }
    public void massimoRighe(int valore) {
        massimoRighe = Math.max(1, Math.min(3, valore));
        if (righePreferite > massimoRighe) righePreferite = massimoRighe;
    }

    public int righePreferite() { return righePreferite; }
    public void righePreferite(int valore) {
        righePreferite = Math.max(0, Math.min(3, valore));
        if (righePreferite > massimoRighe) massimoRighe = righePreferite;
    }

    public int allineamento() { return allineamento; }
    public void allineamento(int valore) {
        allineamento = Math.max(0, Math.min(2, valore));
    }

    public boolean mostraSeparatori() { return mostraSeparatori; }
    public void mostraSeparatori(boolean valore) { mostraSeparatori = valore; }

    /** 0 renders the complete source; 1..n render one logical source group. */
    public int parteTesto() { return parteTesto; }
    public void parteTesto(int valore) { parteTesto = Math.max(0, valore); }

    public Elemento corpo(double valore, boolean bold) {
        corpo(valore);
        grassetto(bold);
        return this;
    }

    public Elemento altezzaDi(double valore) {
        altezza(valore);
        return this;
    }

    public Elemento presenta(int allineamento, boolean separatori, int righe) {
        allineamento(allineamento);
        mostraSeparatori(separatori);
        massimoRighe(righe);
        return this;
    }

    public Elemento copia() {
        Elemento copia = new Elemento(nome, tipo, campo, x, y, larghezza);
        copia.altezza(altezza);
        copia.corpo(corpo);
        copia.grassetto(grassetto);
        copia.rotazione(rotazione);
        copia.massimoRighe(massimoRighe);
        copia.righePreferite(righePreferite);
        copia.correzione(correzione);
        copia.allineamento(allineamento);
        copia.mostraSeparatori(mostraSeparatori);
        copia.parteTesto(parteTesto);
        return copia;
    }

    private static double arrotonda(double valore) {
        return Math.round(valore * 10.0) / 10.0;
    }

    @Override public String toString() { return nome + " (" + tipo + ")"; }
}
