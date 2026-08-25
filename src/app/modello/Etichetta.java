package app.modello;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Carta, dati ed elementi che compongono un modello di etichetta. */
public class Etichetta {

    private String nome;
    private double larghezza;
    private double altezza;
    private final List<Campo> campi = new ArrayList<Campo>();
    private final List<Elemento> elementi = new ArrayList<Elemento>();
    /* Compatibilita' con i file v1, che avevano un solo progressivo globale. */
    private Serie serieInAttesa;

    public Etichetta(String nome, double larghezza, double altezza) {
        this.nome = nome;
        this.larghezza = larghezza;
        this.altezza = altezza;
    }

    public String nome() { return nome; }
    public void nome(String v) { nome = v; }
    public double larghezza() { return larghezza; }
    public double altezza() { return altezza; }

    /** Scambia i lati; il rimappaggio degli elementi appartiene all'editor. */
    public void scambiaLati() {
        double t = larghezza;
        larghezza = altezza;
        altezza = t;
    }

    public List<Campo> campi() { return Collections.unmodifiableList(campi); }

    public Etichetta aggiungi(Campo c) {
        if (c == null) throw new IllegalArgumentException("campo nullo");
        if (campo(c.nome()) != null) {
            throw new IllegalArgumentException("esiste gia' il campo \"" + c.nome() + "\"");
        }
        if (serieInAttesa != null && c.comportamento() == Comportamento.PROGRESSIVO
                && c.serie() == null) {
            c.serie(serieInAttesa);
            serieInAttesa = null;
        }
        campi.add(c);
        return this;
    }

    public Campo campo(String nomeCampo) {
        if (nomeCampo == null) return null;
        for (Campo c : campi) if (c.nome().equals(nomeCampo)) return c;
        return null;
    }

    public List<Elemento> elementi() { return Collections.unmodifiableList(elementi); }
    public Etichetta aggiungi(Elemento e) { if (e != null) elementi.add(e); return this; }
    public void rimuovi(Elemento e) { elementi.remove(e); }

    /** Compatibilita' con il vecchio modello a progressivo singolo. */
    public Serie serie() {
        for (Campo c : campi) {
            if (c.comportamento() == Comportamento.PROGRESSIVO && c.serie() != null) {
                return c.serie();
            }
        }
        return serieInAttesa;
    }

    public Etichetta serie(Serie s) {
        for (Campo c : campi) {
            if (c.comportamento() == Comportamento.PROGRESSIVO) {
                c.serie(s);
                return this;
            }
        }
        serieInAttesa = s;
        return this;
    }

    public Serie serie(String nomeCampo) {
        Campo c = campo(nomeCampo);
        return c == null ? null : c.serie();
    }

    public Etichetta serie(String nomeCampo, Serie s) {
        Campo c = campo(nomeCampo);
        if (c == null) throw new IllegalArgumentException("campo sconosciuto: " + nomeCampo);
        c.serie(s);
        return this;
    }

    public List<Campo> progressivi() {
        List<Campo> out = new ArrayList<Campo>();
        for (Campo c : campi) if (c.comportamento() == Comportamento.PROGRESSIVO) out.add(c);
        return out;
    }

    public boolean cambiaFinestra(int cifre) {
        for (Campo c : progressivi()) return cambiaFinestra(c.nome(), cifre);
        return false;
    }

    public boolean cambiaFinestra(String nomeCampo, int cifre) {
        Campo c = campo(nomeCampo);
        if (c == null || c.serie() == null) return false;
        try {
            c.serie(new Serie(c.serie().codice(c.serie().prossimo()), cifre));
            return true;
        } catch (RuntimeException nonSiPuo) {
            return false;
        }
    }

    public boolean assicuraSerie(String nomeCampo, int cifre) {
        Campo c = campo(nomeCampo);
        if (c == null || c.serie() != null || c.valore().length() < cifre) return false;
        try {
            c.serie(new Serie(c.valore(), cifre));
            return true;
        } catch (RuntimeException nonNumerico) {
            return false;
        }
    }

    public List<Campo> daChiedere() {
        List<Campo> out = new ArrayList<Campo>();
        for (Campo c : campi) if (c.comportamento() == Comportamento.CHIESTO) out.add(c);
        return out;
    }

    /** Solo i dati effettivamente usati da almeno un elemento, nell'ordine del layout. */
    public List<Campo> campiUsati() {
        List<Campo> out = new ArrayList<Campo>();
        Set<String> visti = new LinkedHashSet<String>();
        for (Elemento e : elementi) {
            if (e.campo() != null && visti.add(e.campo())) {
                Campo c = campo(e.campo());
                if (c != null) out.add(c);
            }
        }
        return out;
    }

    public String nomeCampoUnico(String base) {
        String radice = base == null || base.trim().isEmpty() ? "dato" : base.trim();
        if (campo(radice) == null) return radice;
        int n = 2;
        while (campo(radice + " " + n) != null) n++;
        return radice + " " + n;
    }

    /** Separa solo l'elemento indicato dal dato che condivideva con altri. */
    public Campo rendiIndipendente(Elemento e) {
        if (e == null || e.campo() == null) return null;
        Campo originale = campo(e.campo());
        if (originale == null) return null;
        Campo copia = copiaConNome(originale, nomeCampoUnico(originale.nome()));
        aggiungi(copia);
        e.campo(copia.nome());
        return copia;
    }

    private static Campo copiaConNome(Campo originale, String nomeNuovo) {
        Campo c = new Campo(nomeNuovo, originale.comportamento(), originale.valore());
        if (originale.serie() != null) {
            c.serie(new Serie(originale.serie().codice(originale.serie().prossimo()),
                    originale.serie().cifre()));
        }
        return c;
    }

    public List<Elemento> elementiPerCampo(Campo c) {
        List<Elemento> out = new ArrayList<Elemento>();
        if (c == null) return out;
        for (Elemento e : elementi) if (c.nome().equals(e.campo())) out.add(e);
        return out;
    }

    /** Dati che finiscono materialmente in un QR, barcode o codice leggibile. */
    public List<Campo> campiCodice() {
        List<Campo> out = new ArrayList<Campo>();
        Set<String> visti = new LinkedHashSet<String>();
        for (Elemento e : elementi) {
            if (e.tipo() == Tipo.QR || e.tipo() == Tipo.BARCODE || e.tipo() == Tipo.CODICE) {
                if (e.campo() != null && visti.add(e.campo())) {
                    Campo c = campo(e.campo());
                    if (c != null) out.add(c);
                }
            }
        }
        return out;
    }

    public String contenuto(Elemento e, int copia) {
        Campo c = campo(e.campo());
        if (c == null) return e.nome();
        if (c.comportamento() == Comportamento.PROGRESSIVO && c.serie() != null) {
            return c.serie().codice(c.serie().prossimo() + copia);
        }
        if (c.comportamento() == Comportamento.CHIESTO && c.valore().isEmpty()) return "?";
        return c.valore();
    }

    public void validaGiro(int copie) {
        for (Campo c : progressivi()) {
            if (c.serie() == null) {
                throw new IllegalStateException("manca il codice iniziale del dato \"" + c.nome() + "\"");
            }
            c.serie().giro(copie);
        }
    }

    public void consumaProgressivi(int copie) {
        validaGiro(copie);
        for (Campo c : progressivi()) {
            c.serie().consuma(copie);
            c.valore(c.serie().codice(c.serie().prossimo()));
        }
    }

    public String[] codiciGiro(int copie) {
        if (copie < 1) throw new IllegalArgumentException("almeno una copia");
        List<Campo> codici = campiCodice();
        String[] out = new String[copie];
        for (int i = 0; i < copie; i++) {
            if (codici.isEmpty()) {
                out[i] = nome;
            } else if (codici.size() == 1) {
                out[i] = valoreAllaCopia(codici.get(0), i);
            } else {
                StringBuilder b = new StringBuilder();
                for (Campo c : codici) {
                    if (b.length() > 0) b.append("  |  ");
                    b.append(c.nome()).append('=').append(valoreAllaCopia(c, i));
                }
                out[i] = b.toString();
            }
        }
        return out;
    }

    public String valoreAllaCopia(Campo c, int copia) {
        if (c.comportamento() == Comportamento.PROGRESSIVO && c.serie() != null) {
            return c.serie().codice(c.serie().prossimo() + copia);
        }
        return c.valore();
    }

    public Etichetta copia() {
        Etichetta c = new Etichetta(nome, larghezza, altezza);
        for (Campo campo : campi) c.aggiungi(campo.copia());
        for (Elemento e : elementi) c.aggiungi(e.copia());
        if (serieInAttesa != null) {
            c.serieInAttesa = new Serie(serieInAttesa.codice(serieInAttesa.prossimo()),
                    serieInAttesa.cifre());
        }
        return c;
    }

    public void riprendi(Etichetta altra) {
        nome = altra.nome();
        larghezza = altra.larghezza();
        altezza = altra.altezza();
        campi.clear();
        for (Campo campo : altra.campi()) campi.add(campo.copia());
        elementi.clear();
        for (Elemento e : altra.elementi()) elementi.add(e.copia());
        serieInAttesa = altra.serieInAttesa == null ? null
                : new Serie(altra.serieInAttesa.codice(altra.serieInAttesa.prossimo()),
                        altra.serieInAttesa.cifre());
    }

    @Override public String toString() { return nome; }
}
