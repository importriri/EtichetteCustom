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
 * End-to-end production-day regression. It verifies ordering across preview,
 * print, sequence consumption, persistence and log reload.
 */
public final class ProvaGiornata {

    private ProvaGiornata() { }

    private static final SorgenteQr QR = new QrVero();

    public static void esegui() throws Exception {
        Prove.suite("Typical production day");

        File casa = ProvaArchivio.temporanea("giornata");
        Archivio archivio = new Archivio(new File(casa, "etichette"));
        Registro registro = new Registro(new File(casa, "log"));

        List<Etichetta> etichette = archivio.carica();
        Etichetta eti = etichette.get(0);
        Serie s = eti.serie();
        int partenza = s.prossimo();

        /* A run cancelled before printing must consume nothing. */
        String[] annullato = s.giro(12);
        Prove.uguale("previewing run codes consumes nothing",
                partenza, s.prossimo());
        Prove.uguale("the first code matches the expected value", s.codice(partenza), annullato[0]);

        /* A successful run is rendered first and consumed afterwards. */
        String[] codici = s.giro(12);
        StampaGiro giro = new StampaGiro(eti, QR, 12);
        Prove.uguale("twelve pages are prepared", 12, giro.getNumberOfPages());
        s.consuma(12);
        Prove.uguale("the counter advances only after the run is consumed",
                partenza + 12, s.prossimo());

        File log = registro.annota(eti, codici, "Datamax E-4203");
        archivio.salva(eti);
        Prove.vero("the print log is written", log.isFile());

        /* Simulate closing and reopening the application. */
        Archivio domani = new Archivio(new File(casa, "etichette"));
        List<Etichetta> riprese = domani.carica();
        Etichetta stessa = null;
        for (Etichetta e : riprese) {
            if (e.nome().equals(eti.nome())) {
                stessa = e;
            }
        }
        Prove.vero("the label is restored after restart", stessa != null);
        Prove.uguale("the restored sequence resumes at the correct value",
                partenza + 12, stessa.serie().prossimo());

        List<Registro.Giro> letti = registro.ultimi(5);
        Prove.uguale("the log initially contains one run", 1, letti.size());
        Prove.uguale("the first run contains twelve labels", 12, letti.get(0).quante());
        Prove.uguale("the logged first code is exact", codici[0], letti.get(0).primo());
        Prove.uguale("the logged last code is exact", codici[11], letti.get(0).ultimo());

        /* A second run must remain distinct from the first. */
        String[] secondi = stessa.serie().giro(3);
        stessa.serie().consuma(3);
        registro.annota(stessa, secondi, "Datamax E-4203");
        List<Registro.Giro> dueGiri = registro.ultimi(5);
        Prove.uguale("two prints create two distinct log runs", 2, dueGiri.size());
        Prove.uguale("the newest run is the three-label run", 3, dueGiri.get(0).quante());

        /* A newly created gallery label must persist too. */
        Etichetta nuova = new Etichetta("Nata oggi", 45, 25);
        domani.salva(nuova);
        Prove.uguale("a newly created label survives another restart",
                riprese.size() + 1, new Archivio(new File(casa, "etichette")).carica().size());
    }
}
