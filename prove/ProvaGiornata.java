package prove;

import app.archivio.Archivio;
import app.archivio.Registro;
import app.modello.Etichetta;
import app.modello.Serie;
import app.render.QrVero;
import app.render.SorgenteQr;
import app.stampa.StampaGiro;

import java.io.File;
import java.util.List;

/**
 * Una giornata tipo, dall'apertura del programma alla chiusura.
 *
 * Le altre prove guardano un pezzo per volta. Questa guarda l'ordine in
 * cui i pezzi si muovono, che e' dove nascono i guai veri: se il contatore
 * avanzasse prima della stampa, un giro annullato brucerebbe dodici numeri
 * e in magazzino resterebbe un buco che nessuno sa spiegare.
 */
public final class ProvaGiornata {

    private ProvaGiornata() { }

    private static final SorgenteQr QR = new QrVero();

    public static void esegui() throws Exception {
        Prove.suite("Giornata tipo - dall'apertura alla chiusura");

        File casa = ProvaArchivio.temporanea("giornata");
        Archivio archivio = new Archivio(new File(casa, "etichette"));
        Registro registro = new Registro(new File(casa, "log"));

        List<Etichetta> etichette = archivio.carica();
        Etichetta eti = etichette.get(0);
        Serie s = eti.serie();
        int partenza = s.prossimo();

        /* un giro annullato prima di stampare */
        String[] annullato = s.giro(12);
        Prove.uguale("guardare i codici di un giro non consuma niente",
                partenza, s.prossimo());
        Prove.uguale("il primo codice e' quello atteso", s.codice(partenza), annullato[0]);

        /* il giro buono: prima si stampa, poi si consuma */
        String[] codici = s.giro(12);
        StampaGiro giro = new StampaGiro(eti, QR, 12);
        Prove.uguale("dodici pagine pronte", 12, giro.getNumberOfPages());
        s.consuma(12);
        Prove.uguale("solo dopo la stampa il contatore avanza di dodici",
                partenza + 12, s.prossimo());

        File log = registro.annota(eti, codici, "Datamax E-4203");
        archivio.salva(eti);
        Prove.vero("il registro e' stato scritto", log.isFile());

        /* si chiude e si riapre il programma */
        Archivio domani = new Archivio(new File(casa, "etichette"));
        List<Etichetta> riprese = domani.carica();
        Etichetta stessa = null;
        for (Etichetta e : riprese) {
            if (e.nome().equals(eti.nome())) {
                stessa = e;
            }
        }
        Prove.vero("l'etichetta si ritrova", stessa != null);
        Prove.uguale("e riparte dal numero giusto, non da capo",
                partenza + 12, stessa.serie().prossimo());

        List<Registro.Giro> letti = registro.ultimi(5);
        Prove.uguale("nel registro c'e' un giro solo", 1, letti.size());
        Prove.uguale("di dodici etichette", 12, letti.get(0).quante());
        Prove.uguale("dal primo codice stampato", codici[0], letti.get(0).primo());
        Prove.uguale("all'ultimo", codici[11], letti.get(0).ultimo());

        /* una seconda stampa subito dopo non si confonde con la prima */
        String[] secondi = stessa.serie().giro(3);
        stessa.serie().consuma(3);
        registro.annota(stessa, secondi, "Datamax E-4203");
        List<Registro.Giro> dueGiri = registro.ultimi(5);
        Prove.uguale("due stampe, due giri distinti nel registro", 2, dueGiri.size());
        Prove.uguale("il piu' recente e' quello da tre", 3, dueGiri.get(0).quante());

        /* una nuova etichetta creata dalla vetrina */
        Etichetta nuova = new Etichetta("Nata oggi", 45, 25);
        domani.salva(nuova);
        Prove.uguale("riaprendo ancora, la nuova etichetta c'e'",
                riprese.size() + 1, new Archivio(new File(casa, "etichette")).carica().size());
    }
}
