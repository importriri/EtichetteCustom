package app.ui;

import app.archivio.Archivio;
import app.archivio.Registro;
import app.modello.Etichetta;
import app.modello.Impostazioni;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.banco.Banco;
import app.ui.finestre.Finestre;
import app.ui.operatore.Operatore;
import app.ui.vetrina.Vetrina;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** Main window hosting the gallery, print preparation and layout editor. */
public class Cornice extends JFrame {
    private final CardLayout carte = new CardLayout();
    private final JPanel radice = new JPanel(carte);
    private final SorgenteQr qr;
    private final Impostazioni impostazioni = new Impostazioni();
    private final Archivio archivio;
    private final Registro registro;
    private final List<Etichetta> etichette;
    private final Vetrina vetrina;
    private Banco banco;
    private Operatore operatore;

    public Cornice(SorgenteQr qr) {
        super("Etichette Custom");
        this.qr = qr;
        this.archivio = new Archivio(impostazioni.cartellaEtichette());
        this.registro = new Registro(impostazioni.cartellaLog());
        this.etichette = archivio.carica();

        vetrina = new Vetrina(etichette, qr, new Vetrina.Comandi() {
            public void apri(Etichetta e) { apriOperatore(e); }
            public void modifica(Etichetta e) { apriBanco(e); }
            public void nuova() {
                Etichetta e = Finestre.nuovaEtichetta(radice);
                if (e == null) return;
                etichette.add(e);
                salva(e);
                apriBanco(e);
            }
            public void rinomina(Etichetta e) {
                String nuovo = Finestre.rinomina(radice, e.nome());
                if (nuovo == null) return;
                e.nome(nuovo);
                salva(e);
                aggiornaVetrina();
            }
            public void duplica(Etichetta e) {
                Etichetta copia = e.copia();
                copia.nome(e.nome() + " (copia)");
                etichette.add(etichette.indexOf(e) + 1, copia);
                salva(copia);
                aggiornaVetrina();
            }
            public void elimina(Etichetta e) {
                if (!Finestre.conferma(radice, "Elimina",
                        "Elimino \"" + e.nome() + "\"? Il file viene cancellato.")) return;
                etichette.remove(e);
                archivio.elimina(e);
                aggiornaVetrina();
            }
            public void stampante() { Finestre.impostazioni(radice, impostazioni); }
            public void impostazioni() { Finestre.impostazioni(radice, impostazioni); }
        }, registro);

        radice.add(vetrina, "vetrina");
        radice.setBackground(Stile.BASE);
        setContentPane(radice);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(Stile.px(980), Stile.px(640)));
        setSize(new Dimension(Stile.px(1280), Stile.px(820)));
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                if (banco != null) banco.salva();
                if (operatore != null) operatore.salva();
            }
        });
    }

    private void salva(Etichetta e) {
        try { archivio.salva(e); }
        catch (Exception rotta) {
            Finestre.guaio(radice, "Salvataggio", "Non riesco a salvare: " + rotta.getMessage());
        }
    }

    private void apriOperatore(final Etichetta e) {
        if (operatore != null) radice.remove(operatore);
        operatore = new Operatore(e, qr, impostazioni, archivio, registro,
                new Runnable() { public void run() { allaVetrina(); } },
                new Runnable() { public void run() { apriBanco(e); } });
        radice.add(operatore, "operatore");
        carte.show(radice, "operatore");
        setTitle("Etichette Custom — " + e.nome() + " · Stampa");
    }

    private void apriBanco(final Etichetta e) {
        if (banco != null) radice.remove(banco);
        banco = new Banco(e, qr, impostazioni, archivio, registro,
                new Runnable() { public void run() { allaVetrina(); } },
                new Runnable() { public void run() { banco.salva(); apriOperatore(e); } });
        radice.add(banco, "banco");
        carte.show(radice, "banco");
        setTitle("Etichette Custom — " + e.nome() + " · Modifica layout");
    }

    private void allaVetrina() {
        carte.show(radice, "vetrina");
        setTitle("Etichette Custom");
        aggiornaVetrina();
    }

    private void aggiornaVetrina() { vetrina.popola(etichette, qr); }
}
