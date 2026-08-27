package app;

import app.render.QrVero;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.Cornice;
import javax.swing.SwingUtilities;

/** Punto di partenza. */
public final class Avvio {
    private Avvio(){}
    public static void main(String[] args){Stile.installaLookAndFeel();Stile.adottaFontDiSistema();final SorgenteQr qr=new QrVero();SwingUtilities.invokeLater(new Runnable(){@Override public void run(){new Cornice(qr).setVisible(true);}});}
}
