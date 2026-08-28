package app.render;

import app.modello.Elemento;
import app.modello.Etichetta;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Visible element bounds used by rendering, hit testing and selection. */
public final class Ingombri {
    private Ingombri() { }

    public static Map<Elemento, Rectangle2D.Double> calcola(
            Graphics2D g, Etichetta etichetta, double mmPx, int copia) {
        Map<Elemento, Rectangle2D.Double> risultato =
                new LinkedHashMap<Elemento, Rectangle2D.Double>();
        for (Elemento elemento : etichetta.elementi()) {
            risultato.put(elemento, di(g, etichetta, elemento, mmPx, copia));
        }
        return risultato;
    }

    public static Rectangle2D.Double base(Graphics2D g, Etichetta etichetta,
                                          Elemento elemento, double mmPx, int copia) {
        double larghezza = elemento.larghezza();
        double altezza;
        switch (elemento.tipo()) {
            case QR:
                altezza = elemento.larghezza();
                break;
            case BARCODE:
            case LINEA:
                altezza = elemento.altezza();
                break;
            default:
                String mostrato = Disegno.testoElemento(etichetta, elemento, copia);
                Testo.Esito esito = Testo.componi(mostrato, elemento.larghezza(),
                        elemento.corpo(), elemento.massimoRighe(), elemento.righePreferite(),
                        elemento.grassetto(), Disegno.misuratore(g, mmPx));
                Font font = Disegno.font(esito.corpo() * mmPx, elemento.grassetto());
                FontMetrics metriche = g.getFontMetrics(font);
                altezza = esito.quanteRighe()
                        * (metriche.getAscent() + metriche.getDescent()) / mmPx;
                larghezza = elemento.larghezza();
                break;
        }
        return new Rectangle2D.Double(0, 0, larghezza, altezza);
    }

    public static Rectangle2D.Double di(Graphics2D g, Etichetta etichetta,
                                        Elemento elemento, double mmPx, int copia) {
        Rectangle2D.Double base = base(g, etichetta, elemento, mmPx, copia);
        boolean scambia = elemento.rotazione() == 90 || elemento.rotazione() == 270;
        return new Rectangle2D.Double(elemento.x(), elemento.y(),
                scambia ? base.height : base.width,
                scambia ? base.width : base.height);
    }
}
