package app.stampa;

import app.modello.Etichetta;
import app.render.Disegno;
import app.render.SorgenteQr;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/**
 * Un giro di stampa: una pagina per etichetta, ognuna grande quanto
 * l'etichetta e senza margini.
 *
 * La pagina NON e' un A4 con l'etichetta in mezzo: la carta della Datamax
 * e' larga quanto l'etichetta, quindi il formato pagina viene costruito
 * sulle misure in millimetri e l'area stampabile e' tutta la pagina.
 *
 * Il disegno passa da {@link Disegno}, lo stesso che fa le anteprime.
 * Quello che l'operatore vede a schermo e' letteralmente lo stesso codice
 * che va sulla carta, solo con piu' pixel per millimetro.
 */
public class StampaGiro implements Pageable, Printable {

    /** Un punto tipografico e' 1/72 di pollice. */
    public static final double PUNTI_PER_MM = 72.0 / 25.4;

    private final Etichetta etichetta;
    private final SorgenteQr qr;
    private final int copie;

    public StampaGiro(Etichetta etichetta, SorgenteQr qr, int copie) {
        if (etichetta == null) {
            throw new IllegalArgumentException("serve un'etichetta");
        }
        if (copie < 1) {
            throw new IllegalArgumentException("almeno una copia");
        }
        this.etichetta = etichetta;
        this.qr = qr;
        this.copie = copie;
    }

    @Override
    public int getNumberOfPages() {
        return copie;
    }

    @Override
    public PageFormat getPageFormat(int pagina) {
        Paper carta = new Paper();
        double w = etichetta.larghezza() * PUNTI_PER_MM;
        double h = etichetta.altezza() * PUNTI_PER_MM;
        carta.setSize(w, h);
        carta.setImageableArea(0, 0, w, h);
        PageFormat formato = new PageFormat();
        formato.setPaper(carta);
        formato.setOrientation(PageFormat.PORTRAIT);
        return formato;
    }

    @Override
    public Printable getPrintable(int pagina) {
        return this;
    }

    @Override
    public int print(Graphics g, PageFormat formato, int indice) {
        if (indice < 0 || indice >= copie) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(formato.getImageableX(), formato.getImageableY());
            Disegno.disegna(g2, etichetta, PUNTI_PER_MM, qr, indice);
        } finally {
            g2.dispose();
        }
        return PAGE_EXISTS;
    }

    /**
     * Apre la finestra di stampa di Windows e stampa.
     *
     * @return falso se l'operatore ha annullato dalla finestra di sistema.
     */
    public boolean manda(String nomeGiro) throws PrinterException {
        PrinterJob lavoro = PrinterJob.getPrinterJob();
        lavoro.setJobName(nomeGiro == null ? etichetta.nome() : nomeGiro);
        lavoro.setPageable(this);
        if (!lavoro.printDialog()) {
            return false;
        }
        lavoro.print();
        return true;
    }
}
