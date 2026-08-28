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
 * One print run with one marginless page per label. Page dimensions match the
 * physical label rather than embedding it in a larger sheet. Rendering goes
 * through {@link Disegno}, the same path used by previews and exports.
 */
public class StampaGiro implements Pageable, Printable {

    /** One typographic point is 1/72 inch. */
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
     * Opens the system print dialog and prints the run.
     *
     * @return false when the operator cancels the system dialog
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
