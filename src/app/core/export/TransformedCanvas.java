package app.core.export;

import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * Una superficie che applica una trasformazione e passa avanti.
 *
 * <p>Serve a ruotare un'etichetta intera senza toccare né il layout né i
 * backend: il verso di stampa diventa un anello in mezzo alla catena, e
 * stampa, PDF e taratura possono usare lo stesso identico giro di coordinate.
 */
public final class TransformedCanvas implements LabelCanvas {

    private final LabelCanvas target;
    private final AffineTransform transform;

    public TransformedCanvas(LabelCanvas target, AffineTransform transform) {
        if (target == null || transform == null) {
            throw new IllegalArgumentException("Superficie o trasformazione nulle.");
        }
        this.target = target;
        this.transform = new AffineTransform(transform);
    }

    /** Avvolge la superficie solo se serve davvero: identità = nessun anello in mezzo. */
    public static LabelCanvas wrap(LabelCanvas target, AffineTransform transform) {
        if (transform == null || transform.isIdentity()) {
            return target;
        }
        return new TransformedCanvas(target, transform);
    }

    @Override
    public void fill(Shape shapeMm) {
        target.fill(transform.createTransformedShape(shapeMm));
    }
}
