package app.ui;

import app.config.AppTheme;
import app.config.UiScale;
import app.core.LabelElement;
import app.core.LabelLayout;
import app.core.LabelModel;
import app.core.export.Graphics2DCanvas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JPanel;

/**
 * Anteprima dell'etichetta in scala, con gli elementi manovrabili col mouse.
 *
 * <p>Disegna passando dallo stesso {@link LabelLayout} usato da PNG, PDF, SVG e
 * stampa: quello che si vede qui è quello che esce, non una sua imitazione.
 *
 * <p>Tre gesti, tutti sull'oggetto selezionato: si trascina il corpo per
 * spostarlo, il quadratino in basso a destra per ingrandirlo, il pallino in
 * alto a destra per ruotarlo. Con la rotellina si cambia misura senza mirare
 * niente, con le frecce ci si sposta di un decimo di millimetro alla volta —
 * perché su un'etichetta da 50 mm la precisione del mouse non basta.
 */
public final class PreviewPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Avvisa il tab quando l'operatore sposta, ridimensiona o seleziona. */
    public interface Listener {
        void elementChanged(LabelElement element);

        void elementSelected(LabelElement element);
    }

    private enum Grip {
        CORPO, MISURA, ROTAZIONE
    }

    private LabelModel model = LabelModel.defaults();
    private java.util.Map<String, String> values =
            new java.util.HashMap<String, String>();
    private LabelElement selected;
    private Listener listener;
    private boolean showGrid = true;

    private double scale = 1;
    private int originX;
    private int originY;

    private Grip dragging;
    private double grabDxMm;
    private double grabDyMm;
    private double grabSizeMm;
    private double grabDistanceMm;
    private double grabAngleDeg;

    public PreviewPanel() {
        setBackground(AppTheme.PREVIEW_MAT);
        setPreferredSize(new Dimension(UiScale.px(440), UiScale.px(320)));
        setFocusable(true);
        Mouse mouse = new Mouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(new Wheel());
        addKeyListener(new Keys());
        setToolTipText("Trascina per spostare, quadratino per la misura, "
                + "pallino per ruotare. Frecce: 0,1 mm.");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setModel(LabelModel model) {
        this.model = model;
        // niente selezione automatica: l'applicazione parte mostrando
        // l'etichetta, non un elemento a caso. Si seleziona cliccando, che è
        // anche l'unico modo per cui l'operatore sa perché il pannello di
        // destra è cambiato
        if (selected != null && !model.elements().contains(selected)) {
            selected = null;
        }
        repaint();
    }

    /** I valori della prima etichetta del giro: quello che l'anteprima mostra. */
    public void setValues(java.util.Map<String, String> values) {
        this.values = values == null
                ? new java.util.HashMap<String, String>()
                : new java.util.HashMap<String, String>(values);
        repaint();
    }

    public LabelElement selected() {
        return selected;
    }

    public void setSelected(LabelElement element) {
        this.selected = element;
        repaint();
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        repaint();
    }

    public boolean showGrid() {
        return showGrid;
    }

    // --- disegno --------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int ruler = UiScale.px(20);
            int pad = ruler + UiScale.px(14);
            double fitX = (getWidth() - 2.0 * pad) / model.widthMm();
            double fitY = (getHeight() - 2.0 * pad) / model.heightMm();
            scale = Math.max(1.0, Math.min(fitX, fitY));
            int w = (int) Math.round(model.widthMm() * scale);
            int h = (int) Math.round(model.heightMm() * scale);
            originX = (getWidth() - w) / 2;
            originY = (getHeight() - h) / 2;

            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(originX + 3, originY + 5, w, h, UiScale.px(6), UiScale.px(6));
            g.setColor(AppTheme.PREVIEW_PAPER);
            g.fillRect(originX, originY, w, h);

            if (showGrid) {
                drawGrid(g, w, h);
            }

            g.setColor(AppTheme.SURFACE2);
            g.drawRect(originX, originY, w, h);

            drawRulers(g, w, h, ruler);

            Graphics2D label = (Graphics2D) g.create();
            try {
                label.translate(originX, originY);
                label.clipRect(0, 0, w, h);
                label.setColor(AppTheme.PREVIEW_INK);
                LabelLayout.render(model, values, new Graphics2DCanvas(label, scale));
            } catch (RuntimeException notRenderable) {
                label.setColor(AppTheme.RED);
                label.setFont(AppTheme.UI_SMALL);
                label.drawString("Contenuto non disegnabile: " + notRenderable.getMessage(),
                        UiScale.px(8), UiScale.px(20));
            } finally {
                label.dispose();
            }

            drawGuides(g, w, h);
            drawSelection(g);
            drawCaption(g);
        } finally {
            g.dispose();
        }
    }

    private void drawGrid(Graphics2D g, int w, int h) {
        g.setColor(AppTheme.blend(AppTheme.MAUVE, AppTheme.PREVIEW_PAPER, 0.10f));
        g.setStroke(new BasicStroke(1f));
        for (double x = 5; x < model.widthMm(); x += 5) {
            int px = originX + (int) Math.round(x * scale);
            g.drawLine(px, originY, px, originY + h);
        }
        for (double y = 5; y < model.heightMm(); y += 5) {
            int py = originY + (int) Math.round(y * scale);
            g.drawLine(originX, py, originX + w, py);
        }
    }

    /**
     * I due righelli in millimetri, sopra e a sinistra dell'etichetta.
     *
     * <p>Servono a leggere una posizione senza aprire niente: l'operatore vede
     * dove sta un elemento guardando l'etichetta, non un numero in un campo.
     * La tacca è ogni 5 mm, la cifra ogni 10.
     */
    private void drawRulers(Graphics2D g, int w, int h, int ruler) {
        g.setColor(AppTheme.blend(AppTheme.SURFACE0, AppTheme.MANTLE, 0.55f));
        g.fillRect(originX, originY - ruler, w, ruler);
        g.fillRect(originX - ruler, originY, ruler, h);

        g.setFont(AppTheme.UI_SMALL.deriveFont((float) AppTheme.UI_SMALL.getSize() - 1));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.setStroke(new BasicStroke(1f));

        for (double mm = 0; mm <= model.widthMm() + 0.01; mm += 5) {
            int px = originX + (int) Math.round(mm * scale);
            boolean big = Math.abs(mm % 10) < 0.01;
            g.setColor(AppTheme.OVERLAY0);
            g.drawLine(px, originY - (big ? UiScale.px(7) : UiScale.px(4)), px, originY - 1);
            if (big) {
                String text = String.valueOf((int) Math.round(mm));
                g.setColor(AppTheme.SUBTEXT0);
                g.drawString(text, px - fm.stringWidth(text) / 2,
                        originY - ruler + fm.getAscent());
            }
        }
        for (double mm = 0; mm <= model.heightMm() + 0.01; mm += 5) {
            int py = originY + (int) Math.round(mm * scale);
            boolean big = Math.abs(mm % 10) < 0.01;
            g.setColor(AppTheme.OVERLAY0);
            g.drawLine(originX - (big ? UiScale.px(7) : UiScale.px(4)), py, originX - 1, py);
            if (big) {
                String text = String.valueOf((int) Math.round(mm));
                g.setColor(AppTheme.SUBTEXT0);
                g.drawString(text, originX - ruler + UiScale.px(2), py + fm.getAscent() / 2);
            }
        }
    }

    private void drawGuides(Graphics2D g, int w, int h) {
        g.setColor(AppTheme.PEACH);
        g.setStroke(new BasicStroke(1f));
        if (!Double.isNaN(guideXMm)) {
            int px = originX + (int) Math.round(guideXMm * scale);
            g.drawLine(px, originY, px, originY + h);
        }
        if (!Double.isNaN(guideYMm)) {
            int py = originY + (int) Math.round(guideYMm * scale);
            g.drawLine(originX, py, originX + w, py);
        }
    }

    private void drawSelection(Graphics2D g) {
        Rectangle box = selectionBoxPx();
        if (box == null) {
            return;
        }
        g.setColor(AppTheme.MAUVE);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[] {4f, 3f}, 0f));
        g.drawRect(box.x, box.y, box.width, box.height);

        g.setStroke(new BasicStroke(1.2f));
        Rectangle2D sizeGrip = sizeGripPx(box);
        g.setColor(AppTheme.PEACH);
        g.fill(sizeGrip);
        g.setColor(AppTheme.PAPER);
        g.draw(sizeGrip);

        Ellipse2D rotateGrip = rotateGripPx(box);
        g.setColor(AppTheme.GREEN);
        g.fill(rotateGrip);
        g.setColor(AppTheme.PAPER);
        g.draw(rotateGrip);
        g.setColor(AppTheme.GREEN);
        g.draw(new Line2D.Double(box.x + box.width, box.y,
                rotateGrip.getCenterX(), rotateGrip.getCenterY()));
    }

    private void drawCaption(Graphics2D g) {
        g.setColor(AppTheme.SUBTEXT0);
        g.setFont(AppTheme.UI_SMALL);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.1f x %.1f mm  ·  %d dpi",
                model.widthMm(), model.heightMm(), model.dpi()));
        if (selected != null) {
            sb.append(String.format("  ·  %s: %.1f,%.1f mm  %.1f mm  %.0f°",
                    selected.name(), selected.xMm(), selected.yMm(),
                    selected.sizeMm(), selected.rotationDeg()));
        }
        g.drawString(sb.toString(), UiScale.px(10), getHeight() - UiScale.px(8));
    }

    // --- geometria ------------------------------------------------------------

    /** Il rettangolo occupato dall'elemento selezionato, in pixel dello schermo. */
    private Rectangle selectionBoxPx() {
        if (selected == null) {
            return null;
        }
        Rectangle2D mm = boundsOf(selected);
        if (mm == null) {
            return null;
        }
        int x = originX + (int) Math.round(mm.getX() * scale) - 2;
        int y = originY + (int) Math.round(mm.getY() * scale) - 2;
        int w = (int) Math.round(mm.getWidth() * scale) + 4;
        int h = (int) Math.round(mm.getHeight() * scale) + 4;
        return new Rectangle(x, y, Math.max(w, UiScale.px(6)), Math.max(h, UiScale.px(6)));
    }

    private Rectangle2D boundsOf(LabelElement element) {
        String resolved = element.resolve(values);
        if (resolved.isEmpty()) {
            return null;
        }
        try {
            return LabelLayout.boundsMm(element, resolved, model);
        } catch (RuntimeException notRenderable) {
            return null;
        }
    }

    private Rectangle2D sizeGripPx(Rectangle box) {
        int s = UiScale.px(9);
        return new Rectangle2D.Double(box.x + box.width - s / 2.0,
                box.y + box.height - s / 2.0, s, s);
    }

    private Ellipse2D rotateGripPx(Rectangle box) {
        int s = UiScale.px(11);
        return new Ellipse2D.Double(box.x + box.width - s / 2.0,
                box.y - UiScale.px(16) - s / 2.0, s, s);
    }

    private double toMmX(int px) {
        return (px - originX) / scale;
    }

    private double toMmY(int px) {
        return (px - originY) / scale;
    }

    /**
     * Aggancio: con la griglia accesa ci si incastra ogni 5 mm quando si passa
     * vicino, altrimenti si va di decimo di millimetro. Shift disattiva
     * l'aggancio, per quando serve un valore che la griglia non prevede.
     */
    private double snap(double mm, boolean freeHand) {
        if (!freeHand && showGrid) {
            double nearest = Math.round(mm / 5.0) * 5.0;
            if (Math.abs(nearest - mm) < 0.6) {
                return nearest;
            }
        }
        return Math.round(mm / 0.1) * 0.1;
    }

    /** Le coordinate su cui l'elemento trascinato si è agganciato a un altro. */
    private double guideXMm = Double.NaN;
    private double guideYMm = Double.NaN;

    /**
     * Aggancia l'elemento trascinato al bordo o al centro di un altro elemento.
     *
     * <p>È il modo per centrare un testo sotto un QR senza fare i conti: quando
     * i due si allineano compare una riga arancio e la posizione si incastra.
     * Vale per il bordo sinistro, il centro e il bordo destro — gli unici tre
     * punti che su un'etichetta interessano davvero.
     */
    private double alignToNeighbours(double proposed, boolean horizontal) {
        if (selected == null) {
            return proposed;
        }
        Rectangle2D own = boundsOf(selected);
        if (own == null) {
            return proposed;
        }
        double offset = horizontal ? own.getX() - selected.xMm() : own.getY() - selected.yMm();
        double span = horizontal ? own.getWidth() : own.getHeight();
        double tolerance = 0.5;
        double best = proposed;
        double guide = Double.NaN;

        java.util.List<Double> targets = new java.util.ArrayList<Double>();
        targets.add(horizontal ? model.widthMm() / 2 : model.heightMm() / 2);
        for (LabelElement other : model.elements()) {
            if (other == selected) {
                continue;
            }
            Rectangle2D b = boundsOf(other);
            if (b == null) {
                continue;
            }
            targets.add(horizontal ? b.getMinX() : b.getMinY());
            targets.add(horizontal ? b.getCenterX() : b.getCenterY());
            targets.add(horizontal ? b.getMaxX() : b.getMaxY());
        }
        for (Double target : targets) {
            double[] mine = {proposed + offset, proposed + offset + span / 2,
                             proposed + offset + span};
            for (int edge = 0; edge < mine.length; edge++) {
                if (Math.abs(mine[edge] - target) < tolerance) {
                    best = target - offset - span * edge / 2.0;
                    guide = target;
                    break;
                }
            }
            if (!Double.isNaN(guide)) {
                break;
            }
        }
        if (horizontal) {
            guideXMm = guide;
        } else {
            guideYMm = guide;
        }
        return best;
    }

    private void fireChanged() {
        if (listener != null && selected != null) {
            listener.elementChanged(selected);
        }
        repaint();
    }

    // --- mouse ----------------------------------------------------------------

    private final class Mouse extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            Rectangle box = selectionBoxPx();
            if (box != null && selected != null) {
                if (rotateGripPx(box).contains(e.getPoint())) {
                    startRotate(e);
                    return;
                }
                if (sizeGripPx(box).contains(e.getPoint())) {
                    startResize(e);
                    return;
                }
            }
            double mx = toMmX(e.getX());
            double my = toMmY(e.getY());
            List<LabelElement> elements = model.elements();
            for (int i = elements.size() - 1; i >= 0; i--) {
                LabelElement candidate = elements.get(i);
                Rectangle2D b = boundsOf(candidate);
                if (b != null && b.contains(mx, my)) {
                    selected = candidate;
                    dragging = Grip.CORPO;
                    grabDxMm = mx - candidate.xMm();
                    grabDyMm = my - candidate.yMm();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    if (listener != null) {
                        listener.elementSelected(candidate);
                    }
                    repaint();
                    return;
                }
            }
        }

        private void startRotate(MouseEvent e) {
            dragging = Grip.ROTAZIONE;
            grabAngleDeg = selected.rotationDeg() - angleToPointer(e);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private void startResize(MouseEvent e) {
            dragging = Grip.MISURA;
            grabSizeMm = selected.sizeMm();
            grabDistanceMm = Math.max(0.5, distanceToAnchorMm(e));
            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragging == null || selected == null) {
                return;
            }
            boolean coarse = e.isShiftDown();
            switch (dragging) {
                case CORPO: {
                    double x = snap(toMmX(e.getX()) - grabDxMm, coarse);
                    double y = snap(toMmY(e.getY()) - grabDyMm, coarse);
                    x = alignToNeighbours(x, true);
                    y = alignToNeighbours(y, false);
                    selected.setPosition(x, y);
                    break;
                }
                case MISURA: {
                    if (selected.wraps()) {
                        // un testo che va a capo si stringe in larghezza: il
                        // carattere resta della stessa altezza e il testo si
                        // dispone su più righe, che è quello che si vuole quando
                        // un codice lungo non ci sta
                        double wanted = Math.max(2.0, toMmX(e.getX()) - selected.xMm());
                        selected.setWrapWidthMm(Math.round(wanted * 10.0) / 10.0);
                    } else {
                        double factor = distanceToAnchorMm(e) / grabDistanceMm;
                        double wanted = Math.max(LabelElement.MIN_SIZE_MM,
                                Math.min(LabelElement.MAX_SIZE_MM, grabSizeMm * factor));
                        selected.setSizeMm(Math.round(wanted * 10.0) / 10.0);
                    }
                    break;
                }
                default: {
                    double deg = grabAngleDeg + angleToPointer(e);
                    // con Shift la rotazione va a scatti di 15°: gli angoli utili
                    // su un'etichetta sono pochi e sempre gli stessi
                    selected.setRotationDeg(coarse ? Math.round(deg / 15.0) * 15.0 : deg);
                    break;
                }
            }
            fireChanged();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = null;
            guideXMm = Double.NaN;
            guideYMm = Double.NaN;
            repaint();
            setCursor(Cursor.getDefaultCursor());
        }

        private double angleToPointer(MouseEvent e) {
            double ax = originX + selected.xMm() * scale;
            double ay = originY + selected.yMm() * scale;
            return Math.toDegrees(Math.atan2(e.getY() - ay, e.getX() - ax));
        }

        private double distanceToAnchorMm(MouseEvent e) {
            double dx = toMmX(e.getX()) - selected.xMm();
            double dy = toMmY(e.getY()) - selected.yMm();
            return Math.sqrt(dx * dx + dy * dy);
        }
    }

    private final class Wheel implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (selected == null) {
                return;
            }
            double factor = e.getWheelRotation() < 0 ? 1.06 : 1 / 1.06;
            try {
                selected.scaleBy(factor);
                fireChanged();
            } catch (RuntimeException atTheLimit) {
                // già al minimo o al massimo: non c'è niente da fare
            }
        }
    }

    private final class Keys extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (selected == null) {
                return;
            }
            double step = e.isShiftDown() ? 1.0 : 0.1;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    selected.setPosition(selected.xMm() - step, selected.yMm());
                    break;
                case KeyEvent.VK_RIGHT:
                    selected.setPosition(selected.xMm() + step, selected.yMm());
                    break;
                case KeyEvent.VK_UP:
                    selected.setPosition(selected.xMm(), selected.yMm() - step);
                    break;
                case KeyEvent.VK_DOWN:
                    selected.setPosition(selected.xMm(), selected.yMm() + step);
                    break;
                case KeyEvent.VK_R:
                    selected.rotateQuarterTurn();
                    break;
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_ADD:
                case KeyEvent.VK_EQUALS:
                    selected.scaleBy(1.1);
                    break;
                case KeyEvent.VK_MINUS:
                case KeyEvent.VK_SUBTRACT:
                    selected.scaleBy(1 / 1.1);
                    break;
                default:
                    return;
            }
            e.consume();
            fireChanged();
        }
    }
}
