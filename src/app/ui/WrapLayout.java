package app.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Un {@link FlowLayout} che va davvero a capo.
 *
 * <p>Quello di serie manda a capo i componenti quando li dispone, ma poi
 * dichiara una misura preferita da una riga sola: il risultato è un contenitore
 * alto quanto una riga con dentro tre righe di roba, e metà dei comandi
 * tagliati.
 *
 * <p>Serve alla riga del giro. Su un monitor largo i comandi stanno tutti
 * affiancati; su un portatile di reparto da 1366 punti scendono su due righe
 * invece di uscire dallo schermo — che è esattamente il difetto trovato dal
 * controllo automatico sul layout, con la finestra che nasceva larga 1550 px
 * su uno schermo da 1280.
 */
public final class WrapLayout extends FlowLayout {

    private static final long serialVersionUID = 1L;

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= getHgap() + 1;
        return minimum;
    }

    /**
     * La misura che serve davvero, contando le righe una per una.
     *
     * @param preferred {@code true} per usare la misura preferita dei figli,
     *                  {@code false} per quella minima
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null) {
                container = container.getParent();
            }
            targetWidth = container.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            Insets insets = target.getInsets();
            int horizontalFrame = insets.left + insets.right + getHgap() * 2;
            int maxWidth = targetWidth - horizontalFrame;

            Dimension size = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                    addRow(size, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) {
                    rowWidth += getHgap();
                }
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(size, rowWidth, rowHeight);

            size.width += horizontalFrame;
            size.height += insets.top + insets.bottom + getVgap() * 2;

            // dentro un'area di scorrimento serve un punto in più, altrimenti
            // la barra orizzontale compare e sparisce a ogni ridisegno
            if (SwingUtilities.getAncestorOfClass(JScrollPane.class, target) != null
                    && target.isValid()) {
                size.width -= getHgap() + 1;
            }
            return size;
        }
    }

    private void addRow(Dimension size, int rowWidth, int rowHeight) {
        size.width = Math.max(size.width, rowWidth);
        if (size.height > 0) {
            size.height += getVgap();
        }
        size.height += rowHeight;
    }
}
