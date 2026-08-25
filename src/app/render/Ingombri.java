package app.render;

import app.modello.Elemento;
import app.modello.Etichetta;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ingombro visibile degli elementi, usato da disegno, hit-test e selezione. */
public final class Ingombri {
    private Ingombri() { }

    public static Map<Elemento, Rectangle2D.Double> calcola(Graphics2D g, Etichetta eti,
                                                            double mmPx, int copia) {
        Map<Elemento, Rectangle2D.Double> out = new LinkedHashMap<Elemento, Rectangle2D.Double>();
        for (Elemento e : eti.elementi()) out.put(e, di(g, eti, e, mmPx, copia));
        return out;
    }

    /** Ingombro locale, prima della rotazione. */
    public static Rectangle2D.Double base(Graphics2D g, Etichetta eti, Elemento e,
                                          double mmPx, int copia) {
        double w = e.larghezza();
        double h;
        switch (e.tipo()) {
            case QR:
                h = e.larghezza();
                break;
            case BARCODE:
            case LINEA:
                h = e.altezza();
                break;
            default:
                Testo.Esito esito = Testo.componi(eti.contenuto(e, copia), e.larghezza(),
                        e.corpo(), e.massimoRighe(), e.grassetto(), Disegno.misuratore(g, mmPx));
                Font f = Disegno.font(esito.corpo() * mmPx, e.grassetto());
                FontMetrics fm = g.getFontMetrics(f);
                h = esito.quanteRighe() * (fm.getAscent() + fm.getDescent()) / mmPx;
                double larga = 0;
                for (String r : esito.righe()) larga = Math.max(larga, fm.stringWidth(r) / mmPx);
                w = Math.min(e.larghezza(), Math.max(larga, 0.5));
                break;
        }
        return new Rectangle2D.Double(0, 0, w, h);
    }

    public static Rectangle2D.Double di(Graphics2D g, Etichetta eti, Elemento e,
                                        double mmPx, int copia) {
        Rectangle2D.Double r = base(g, eti, e, mmPx, copia);
        boolean scambia = e.rotazione() == 90 || e.rotazione() == 270;
        return new Rectangle2D.Double(e.x(), e.y(), scambia ? r.height : r.width,
                scambia ? r.width : r.height);
    }
}
