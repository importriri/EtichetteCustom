package app;

import app.render.QrVero;
import app.render.SorgenteQr;
import app.stile.Stile;
import app.ui.Cornice;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto di partenza. */
public final class Avvio {

    private Avvio() { }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorata) {
            /* si prosegue col look and feel di serie: l'app resta usabile */
        }
        Stile.adottaFontDiSistema();
        UIManager.put("ToolTip.font", Stile.piccolo());

        final SorgenteQr qr = new QrVero();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Cornice(qr).setVisible(true);
            }
        });
    }
}
