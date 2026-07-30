package app.core.export;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/** Adattatore verso Swing e verso la stampante: millimetri -> unità del contesto grafico. */
public final class Graphics2DCanvas implements LabelCanvas {

    private final Graphics2D g;
    private final AffineTransform mmToDevice;

    public Graphics2DCanvas(Graphics2D g, double unitsPerMm) {
        this.g = g;
        this.mmToDevice = AffineTransform.getScaleInstance(unitsPerMm, unitsPerMm);
    }

    @Override
    public void fill(Shape shapeMm) {
        g.fill(mmToDevice.createTransformedShape(shapeMm));
    }
}
